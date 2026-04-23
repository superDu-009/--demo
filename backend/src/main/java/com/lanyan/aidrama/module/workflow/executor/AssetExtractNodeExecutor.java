package com.lanyan.aidrama.module.workflow.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.entity.Asset;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.mapper.AssetMapper;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资产提取节点执行器 (系分 5.2)
 * 职责：读取分集内容，通过 Doubao AI 提取资产草稿（角色/场景/物品）
 * 幂等保证：创建 asset 前先查 project_id + name + asset_type 是否已存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetExtractNodeExecutor implements NodeExecutor {

    private final EpisodeMapper episodeMapper;
    private final AssetMapper assetMapper;
    private final DoubaoClient doubaoClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getStepType() {
        return "asset_extract";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        log.info("开始执行资产提取节点, projectId: {}, episodeId: {}", projectId, episodeId);

        // 获取当前分集内容
        if (episodeId == null) {
            return NodeResult.fail("asset_extract 节点需要 episodeId", "submit");
        }
        Episode episode = episodeMapper.selectById(episodeId);
        if (episode == null) {
            return NodeResult.fail("分集不存在", "submit");
        }
        if (episode.getContent() == null || episode.getContent().isBlank()) {
            return NodeResult.fail("分集内容为空", "submit");
        }

        // 调用 Doubao AI 提取资产
        String jsonResponse = doubaoClient.extractAssets(episode.getContent());
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return NodeResult.fail("AI 资产提取失败，返回为空", "submit");
        }

        // 解析 AI 返回的 JSON
        List<Map<String, Object>> extractedAssets;
        try {
            extractedAssets = objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 AI 返回的资产 JSON 失败, response: {}", jsonResponse, e);
            return NodeResult.fail("解析 AI 资产 JSON 失败: " + e.getMessage(), "submit");
        }

        if (extractedAssets.isEmpty()) {
            return NodeResult.fail("资产提取结果为空", "submit");
        }

        // 幂等创建 asset
        List<Long> createdAssetIds = new ArrayList<>();
        for (Map<String, Object> assetInfo : extractedAssets) {
            String name = (String) assetInfo.get("name");
            String assetType = (String) assetInfo.get("type");

            if (name == null || assetType == null) {
                log.warn("AI 返回的资产数据不完整，跳过: {}", assetInfo);
                continue;
            }

            // 幂等检查
            LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Asset::getProjectId, projectId)
                   .eq(Asset::getName, name)
                   .eq(Asset::getAssetType, assetType);
            if (assetMapper.selectOne(wrapper) != null) {
                log.info("资产已存在，跳过, name: {}, type: {}", name, assetType);
                continue;
            }

            Asset asset = new Asset();
            asset.setProjectId(projectId);
            asset.setAssetType(assetType);
            asset.setName(name);
            asset.setDescription((String) assetInfo.getOrDefault("description", ""));
            asset.setStatus(0); // 草稿状态
            assetMapper.insert(asset);

            createdAssetIds.add(asset.getId());
            log.info("创建资产成功, assetId: {}, name: {}, type: {}", asset.getId(), name, assetType);
        }

        // 保存结果到 output_data
        String outputData;
        try {
            outputData = objectMapper.writeValueAsString(Map.of("assetIds", createdAssetIds));
        } catch (JsonProcessingException e) {
            return NodeResult.fail("序列化 output_data 失败", "upload_tos");
        }

        log.info("资产提取节点执行完成, episodeId: {}, 创建资产数: {}", episodeId, createdAssetIds.size());
        return NodeResult.success(outputData);
    }
}
