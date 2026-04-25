package com.lanyan.aidrama.module.asset.service;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.asset.dto.*;

import java.util.List;

/**
 * 资产服务接口 (系分 v1.2 第 7.4 节)
 */
public interface AssetService {

    /**
     * 查项目下资产列表
     */
    List<AssetVO> listAssets(Long projectId, String assetType);

    /**
     * 创建资产
     */
    Long createAsset(Long projectId, AssetCreateRequest req);

    /**
     * 更新资产
     */
    void updateAsset(Long id, AssetUpdateRequest req);

    /**
     * 逻辑删除资产（校验是否被分镜绑定）
     */
    void deleteAsset(Long id);

    /**
     * 确认资产（需至少有 1 张参考图）
     */
    void confirmAsset(Long id);

    /**
     * 提取资产（异步，返回 taskId）
     */
    Long extractAssets(Long projectId, Long episodeId);

    /**
     * 查询资产提取任务状态
     */
    com.lanyan.aidrama.module.task.dto.TaskVO getExtractTaskStatus(Long projectId);

    /**
     * 重复资产检测
     */
    List<AssetDuplicateVO> getDuplicateAssets(Long projectId);

    /**
     * 资产关系树
     */
    List<AssetTreeNode> getAssetTree(Long projectId);

    /**
     * 更新资产关系
     */
    void updateAssetRelations(Long id, String parentIds);

    /**
     * 资产引用查询
     */
    PageResult<ShotReferenceVO> getAssetReferences(Long assetId, int page, int size);
}
