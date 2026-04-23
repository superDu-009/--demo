package com.lanyan.aidrama.module.workflow.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import com.lanyan.aidrama.module.workflow.engine.PromptEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分镜生成节点执行器 (系分 5.2)
 * 职责：根据分场+资产，通过 Doubao AI + PromptEngine 生成分镜 prompt（中英文），创建 shot 记录
 * 幂等保证：创建 shot 前先查 scene_id + sort_order 是否已存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShotGenNodeExecutor implements NodeExecutor {

    private final EpisodeMapper episodeMapper;
    private final SceneMapper sceneMapper;
    private final ShotMapper shotMapper;
    private final AssetMapper assetMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final PromptEngine promptEngine;
    private final DoubaoClient doubaoClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getStepType() {
        return "shot_gen";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        log.info("开始执行分镜生成节点, projectId: {}, episodeId: {}", projectId, episodeId);

        if (episodeId == null) {
            return NodeResult.fail("shot_gen 节点需要 episodeId", "submit");
        }

        // 获取当前分集下的所有分场
        LambdaQueryWrapper<Scene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(Scene::getEpisodeId, episodeId)
                    .orderByAsc(Scene::getSortOrder);
        List<Scene> scenes = sceneMapper.selectList(sceneWrapper);

        if (scenes.isEmpty()) {
            return NodeResult.fail("分集下没有分场，无法生成分镜", "submit");
        }

        // 获取项目下所有资产，用于分镜绑定
        List<Asset> assets = getProjectAssets(projectId);

        // 遍历每个分场，生成分镜
        List<Long> createdShotIds = new ArrayList<>();
        int shotSortOrder = 0;
        for (Scene scene : scenes) {
            // 幂等检查：先查 scene_id + sort_order 是否已存在
            LambdaQueryWrapper<Shot> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(Shot::getSceneId, scene.getId())
                        .eq(Shot::getSortOrder, shotSortOrder);
            if (shotMapper.selectOne(existWrapper) != null) {
                log.info("分镜已存在, sceneId: {}, sortOrder: {}", scene.getId(), shotSortOrder);
                shotSortOrder++;
                continue;
            }

            // 使用 PromptEngine 生成中文提示词
            String promptCn = promptEngine.generatePrompt(scene, assets);
            // 通过 Doubao AI 翻译为英文
            String promptEn = doubaoClient.translatePrompt(promptCn);

            Shot shot = new Shot();
            shot.setSceneId(scene.getId());
            shot.setSortOrder(shotSortOrder);
            shot.setPrompt(promptCn);
            shot.setPromptEn(promptEn);
            shot.setStatus(0); // 待处理
            shot.setVersion(1);
            shot.setGenerationAttempts(0);
            shotMapper.insert(shot);

            // 绑定场景关联的资产到分镜
            bindSceneAssets(shot.getId(), scene.getId(), assets);

            createdShotIds.add(shot.getId());
            log.info("创建分镜成功, shotId: {}, sceneId: {}", shot.getId(), scene.getId());
            shotSortOrder++;
        }

        // 保存结果到 output_data
        String outputData;
        try {
            outputData = objectMapper.writeValueAsString(Map.of("shotIds", createdShotIds, "totalCount", createdShotIds.size()));
        } catch (JsonProcessingException e) {
            return NodeResult.fail("序列化 output_data 失败", "upload_tos");
        }

        log.info("分镜生成节点执行完成, episodeId: {}, 创建分镜数: {}", episodeId, createdShotIds.size());
        return NodeResult.success(outputData);
    }

    /**
     * 获取项目下所有资产
     */
    private List<Asset> getProjectAssets(Long projectId) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getProjectId, projectId);
        return assetMapper.selectList(wrapper);
    }

    /**
     * 绑定场景相关资产到分镜（角色/场景等）
     */
    private void bindSceneAssets(Long shotId, Long sceneId, List<Asset> assets) {
        // 简化实现：绑定项目下所有资产
        for (Asset asset : assets) {
            ShotAssetRef ref = new ShotAssetRef();
            ref.setShotId(shotId);
            ref.setAssetId(asset.getId());
            ref.setAssetType(asset.getAssetType());
            shotAssetRefMapper.insert(ref);
        }
    }
}
