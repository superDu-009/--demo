package com.lanyan.aidrama.module.asset.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.entity.Asset;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.entity.ShotAssetRef;
import com.lanyan.aidrama.entity.Task;
import com.lanyan.aidrama.mapper.AssetMapper;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.mapper.ShotAssetRefMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.mapper.TaskMapper;
import com.lanyan.aidrama.module.asset.dto.*;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.entity.PromptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产服务实现类 (系分 v1.2 第 7.4 节)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;
    private final ProjectMapper projectMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final ShotMapper shotMapper;
    private final EpisodeMapper episodeMapper;
    private final TaskMapper taskMapper;
    private final com.lanyan.aidrama.mapper.PromptConfigMapper promptConfigMapper;
    private final DoubaoClient doubaoClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<AssetVO> listAssets(Long projectId, String assetType) {
        getProjectById(projectId);

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getProjectId, projectId)
               .eq(assetType != null, Asset::getAssetType, assetType)
               .orderByDesc(Asset::getCreateTime);

        return assetMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public Long createAsset(Long projectId, AssetCreateRequest req) {
        getProjectById(projectId);

        Asset asset = new Asset();
        asset.setProjectId(projectId);
        asset.setAssetType(req.getAssetType());
        asset.setName(req.getName());
        asset.setDescription(req.getDescription());
        asset.setReferenceImages(req.getReferenceImages());
        asset.setParentIds(req.getParentIds());
        asset.setStatus("draft");

        assetMapper.insert(asset);
        log.info("创建资产成功, assetId: {}", asset.getId());
        return asset.getId();
    }

    @Override
    public void updateAsset(Long id, AssetUpdateRequest req) {
        Asset asset = getOwnedAsset(id);

        if (req.getName() != null) asset.setName(req.getName());
        if (req.getDescription() != null) asset.setDescription(req.getDescription());
        if (req.getReferenceImages() != null) asset.setReferenceImages(req.getReferenceImages());
        if (req.getParentIds() != null) asset.setParentIds(req.getParentIds());

        assetMapper.updateById(asset);
        log.info("更新资产成功, assetId: {}", id);
    }

    @Override
    public void deleteAsset(Long id) {
        Asset asset = getOwnedAsset(id);

        // 检查是否被分镜绑定（被绑定禁止删除）
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getAssetId, id);
        Long refCount = shotAssetRefMapper.selectCount(refWrapper);
        if (refCount > 0) {
            throw new BusinessException(ErrorCode.ASSET_REFERENCED);
        }

        assetMapper.deleteById(id);
        log.info("删除资产成功, assetId: {}", id);
    }

    @Override
    public void confirmAsset(Long id) {
        Asset asset = getOwnedAsset(id);

        // 确认前必须至少有 1 张参考图
        if (asset.getReferenceImages() == null || asset.getReferenceImages().isBlank()) {
            throw new BusinessException(40900, "资产必须至少有 1 张参考图才能确认");
        }

        asset.setStatus("confirmed");
        assetMapper.updateById(asset);
        log.info("确认资产成功, assetId: {}", id);
    }

    @Override
    public Long extractAssets(Long projectId, Long episodeId) {
        getProjectById(projectId);

        Task task = new Task();
        task.setType("asset_extract");
        task.setProjectId(projectId);
        task.setEpisodeId(episodeId);
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        executeAssetExtractAsync(task.getId(), episodeId);
        return task.getId();
    }

    @Override
    public com.lanyan.aidrama.module.task.dto.TaskVO getExtractTaskStatus(Long projectId) {
        getProjectById(projectId);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProjectId, projectId)
               .eq(Task::getType, "asset_extract")
               .orderByDesc(Task::getId)
               .last("LIMIT 1");
        Task task = taskMapper.selectOne(wrapper);
        if (task == null) {
            return null;
        }
        com.lanyan.aidrama.module.task.dto.TaskVO vo = new com.lanyan.aidrama.module.task.dto.TaskVO();
        vo.setId(task.getId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setResultData(task.getResultData());
        vo.setResultUrl(task.getResultUrl());
        vo.setBatchId(task.getBatchId());
        return vo;
    }

    @Async("aiTaskExecutor")
    public void executeAssetExtractAsync(Long taskId, Long episodeId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            Episode episode = episodeMapper.selectById(episodeId);
            if (episode == null || episode.getContent() == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            // 调用 AI 提取资产
            String systemPrompt = buildSystemPrompt("asset_extract", "asset_extract_response.json");
            String aiResult = doubaoClient.chat(systemPrompt, episode.getContent());

            // 解析 AI 结果并创建资产
            parseAndCreateAssetsFromAI(episode.getProjectId(), aiResult);

            task.setStatus(2);
            task.setResultData(aiResult);
            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("资产提取失败, episodeId: {}", episodeId, e);
            task.setStatus(3);
            task.setErrorMsg(e.getMessage());
            taskMapper.updateById(task);
        }
    }

    @Override
    public List<AssetDuplicateVO> getDuplicateAssets(Long projectId) {
        getProjectById(projectId);

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getProjectId, projectId);
        List<Asset> assets = assetMapper.selectList(wrapper);

        // 按名称相似度大于 80% 聚合
        List<AssetDuplicateVO> duplicates = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();

        for (int i = 0; i < assets.size(); i++) {
            if (processed.contains(i)) continue;

            List<Long> groupIds = new ArrayList<>();
            List<String> groupNames = new ArrayList<>();
            groupIds.add(assets.get(i).getId());
            groupNames.add(assets.get(i).getName());

            for (int j = i + 1; j < assets.size(); j++) {
                if (processed.contains(j)) continue;

                double similarity = calculateNameSimilarity(
                        assets.get(i).getName(), assets.get(j).getName());
                if (similarity > 80) {
                    groupIds.add(assets.get(j).getId());
                    groupNames.add(assets.get(j).getName());
                    processed.add(j);
                }
            }

            if (groupIds.size() > 1) {
                processed.add(i);
                AssetDuplicateVO vo = new AssetDuplicateVO();
                vo.setAssetIds(groupIds);
                vo.setAssetNames(groupNames);
                vo.setSimilarity(85.0); // 简化处理
                duplicates.add(vo);
            }
        }

        return duplicates;
    }

    @Override
    public List<AssetTreeNode> getAssetTree(Long projectId) {
        getProjectById(projectId);

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getProjectId, projectId);
        List<Asset> allAssets = assetMapper.selectList(wrapper);

        Map<Long, AssetTreeNode> nodeMap = new HashMap<>();
        for (Asset asset : allAssets) {
            AssetTreeNode node = new AssetTreeNode();
            node.setId(asset.getId());
            node.setName(asset.getName());
            node.setAssetType(asset.getAssetType());
            node.setIsSubAsset(asset.getIsSubAsset() == 1);
            node.setChildren(new ArrayList<>());
            nodeMap.put(asset.getId(), node);
        }

        // 构建树
        List<AssetTreeNode> roots = new ArrayList<>();
        for (Asset asset : allAssets) {
            AssetTreeNode node = nodeMap.get(asset.getId());
            if (asset.getIsSubAsset() == 1 && asset.getParentIds() != null) {
                try {
                    JsonNode arr = objectMapper.readTree(asset.getParentIds());
                    if (arr.isArray() && arr.size() > 0) {
                        Long parentId = arr.get(0).asLong();
                        AssetTreeNode parent = nodeMap.get(parentId);
                        if (parent != null) {
                            parent.getChildren().add(node);
                            continue;
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析 parentIds 失败, assetId: {}", asset.getId());
                }
            }
            roots.add(node);
        }

        return roots;
    }

    @Override
    public void updateAssetRelations(Long id, String parentIds) {
        Asset asset = getOwnedAsset(id);

        asset.setParentIds(parentIds);
        asset.setIsSubAsset(parentIds != null && !parentIds.isBlank() ? 1 : 0);
        assetMapper.updateById(asset);
        log.info("更新资产关系成功, assetId: {}", id);
    }

    @Override
    public PageResult<ShotReferenceVO> getAssetReferences(Long assetId, int page, int size) {
        Asset asset = getOwnedAsset(assetId);

        Page<ShotAssetRef> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getAssetId, assetId);

        IPage<ShotAssetRef> refPage = shotAssetRefMapper.selectPage(pageParam, refWrapper);

        List<ShotReferenceVO> voList = refPage.getRecords().stream()
                .map(ref -> {
                    ShotReferenceVO vo = new ShotReferenceVO();
                    vo.setShotId(ref.getShotId());
                    Shot shot = shotMapper.selectById(ref.getShotId());
                    if (shot != null) {
                        Episode episode = episodeMapper.selectById(shot.getEpisodeId());
                        if (episode != null) vo.setEpisodeId(episode.getId());
                    }
                    vo.setCreateTime(ref.getCreateTime());
                    return vo;
                }).toList();

        PageResult<ShotReferenceVO> result = new PageResult<>();
        result.setList(voList);
        result.setTotal(refPage.getTotal());
        result.setPage((int) refPage.getCurrent());
        result.setSize((int) refPage.getSize());
        result.setHasNext(refPage.getCurrent() * refPage.getSize() < refPage.getTotal());
        return result;
    }

    // ==================== 内部方法 ====================

    private Project getProjectById(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }
        return project;
    }

    private AssetVO toVO(Asset asset) {
        AssetVO vo = new AssetVO();
        vo.setId(asset.getId());
        vo.setProjectId(asset.getProjectId());
        vo.setAssetType(asset.getAssetType());
        vo.setName(asset.getName());
        vo.setDescription(asset.getDescription());
        vo.setReferenceImages(asset.getReferenceImages());
        vo.setParentIds(asset.getParentIds());
        vo.setIsSubAsset(asset.getIsSubAsset());
        vo.setStatus(asset.getStatus());
        vo.setCreateTime(asset.getCreateTime());
        vo.setUpdateTime(asset.getUpdateTime());
        return vo;
    }

    private String buildSystemPrompt(String promptKey, String templateFileName) {
        String responseTemplate = getResponseTemplate(templateFileName);
        LambdaQueryWrapper<PromptConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptConfig::getPromptKey, promptKey)
                .eq(PromptConfig::getDeleted, 0)
                .last("LIMIT 1");
        PromptConfig config = promptConfigMapper.selectOne(wrapper);
        if (config != null && config.getPromptText() != null && !config.getPromptText().isBlank()) {
            return config.getPromptText() + "\n\n请严格按以下 JSON 模板返回，不要输出额外说明：\n" + responseTemplate;
        }
        return "请仅输出合法 JSON，且结构必须与以下模板完全一致：\n" + responseTemplate;
    }

    private String getResponseTemplate(String templateFileName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/ai/" + templateFileName);
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = FileCopyUtils.copyToByteArray(is);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("读取静态模板失败, file: {}", templateFileName, e);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND.getCode(), "AI 返回模板不存在");
        }
    }

    private Asset getOwnedAsset(Long assetId) {
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        getProjectById(asset.getProjectId());
        return asset;
    }

    private void parseAndCreateAssetsFromAI(Long projectId, String aiResult) {
        try {
            JsonNode array = objectMapper.readTree(aiResult);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    Asset asset = new Asset();
                    asset.setProjectId(projectId);
                    asset.setName(node.has("name") ? node.get("name").asText() : "未命名");
                    asset.setAssetType(node.has("type") ? node.get("type").asText() : "character");
                    asset.setDescription(node.has("description") ? node.get("description").asText() : "");
                    asset.setStatus("draft");
                    assetMapper.insert(asset);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 AI 资产结果失败", e);
        }
    }

    /**
     * 计算两个名称的相似度（简化：基于编辑距离）
     */
    private double calculateNameSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 100;
        int distance = levenshteinDistance(s1, s2);
        return (1.0 - (double) distance / maxLen) * 100;
    }

    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[m][n];
    }
}
