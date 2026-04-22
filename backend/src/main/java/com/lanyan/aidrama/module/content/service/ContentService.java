package com.lanyan.aidrama.module.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.content.dto.*;

import java.util.List;

/**
 * 内容服务接口 (系分 4.4.2)
 * 负责分集/分场/分镜 CRUD、批量审核、资产绑定
 */
public interface ContentService {

    /**
     * 查项目下分集
     */
    List<EpisodeVO> listEpisodes(Long projectId);

    /**
     * 创建分集，sort_order 自动递增
     */
    Long createEpisode(Long projectId, EpisodeCreateRequest req);

    /**
     * 更新分集
     */
    void updateEpisode(Long id, EpisodeUpdateRequest req);

    /**
     * 级联逻辑删除（分场→分镜→资产关联）
     */
    void deleteEpisode(Long id);

    /**
     * 查分集下分场
     */
    List<SceneVO> listScenes(Long episodeId);

    /**
     * 创建分场
     */
    Long createScene(Long episodeId, SceneCreateRequest req);

    /**
     * 更新分场
     */
    void updateScene(Long id, SceneUpdateRequest req);

    /**
     * 级联删除分镜及资产关联
     */
    void deleteScene(Long id);

    /**
     * 分页查分场下分镜，含 assetRefs + currentAiTask
     */
    PageResult<com.lanyan.aidrama.module.project.dto.ShotVO> listShots(Long sceneId, int page, int size, Integer status);

    /**
     * 创建分镜
     */
    Long createShot(Long sceneId, ShotCreateRequest req);

    /**
     * 更新分镜
     */
    void updateShot(Long id, ShotUpdateRequest req);

    /**
     * 删除分镜
     */
    void deleteShot(Long id);

    /**
     * 批量审核，返回成功/失败明细
     */
    BatchReviewResult batchReviewShots(List<Long> shotIds, String action, String comment);

    /**
     * 绑定资产到分镜
     */
    void bindAssetToShot(Long shotId, Long assetId, String assetType);

    /**
     * 解绑分镜资产
     */
    void unbindAssetFromShot(Long shotId, Long assetId);
}
