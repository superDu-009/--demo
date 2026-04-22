package com.lanyan.aidrama.module.asset.service;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.asset.dto.*;

import java.util.List;

/**
 * 资产服务接口 (系分 4.3.2)
 * 负责角色/场景/物品/声音资产 CRUD，支持多参考图
 */
public interface AssetService {

    /**
     * 查项目下某类型资产列表
     */
    List<AssetVO> listAssets(Long projectId, String assetType);

    /**
     * 创建资产，校验 projectId 归属
     */
    Long createAsset(Long projectId, AssetCreateRequest req);

    /**
     * 更新资产
     */
    void updateAsset(Long id, AssetUpdateRequest req);

    /**
     * 逻辑删除，需检查 shot_asset_ref 是否有关联
     */
    void deleteAsset(Long id);

    /**
     * 确认资产（status=1）
     */
    void confirmAsset(Long id);

    /**
     * 查询资产被哪些分镜引用（分页）
     */
    PageResult<ShotReferenceVO> getAssetReferences(Long assetId, int page, int size);
}
