package com.lanyan.aidrama.module.content.service;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.content.dto.*;

import java.util.List;

/**
 * 内容服务接口 (系分 v1.2 第 7.3 节)
 * 负责分集/分镜 CRUD、剧本分析、分镜拆分、AI 生成、草稿、批量操作
 */
public interface ContentService {

    // ===== 分集相关 =====

    /**
     * 查项目下分集列表
     */
    List<EpisodeVO> listEpisodes(Long projectId);

    /**
     * 剧本分析（异步，返回 taskId）
     */
    Long analyzeScript(Long projectId);

    /**
     * 查询剧本分析状态
     */
    ScriptAnalyzeStatusVO getAnalyzeStatus(Long projectId);

    /**
     * 手动创建分集
     */
    Long createEpisode(Long projectId, EpisodeCreateRequest req);

    /**
     * 更新分集
     */
    void updateEpisode(Long id, EpisodeUpdateRequest req);

    /**
     * 逻辑删除分集（级联删除分镜）
     */
    void deleteEpisode(Long id);

    // ===== 分镜相关 =====

    /**
     * 分镜列表（支持按 promptStatus/imageStatus/videoStatus 过滤）
     */
    List<ShotVO> listShots(Long episodeId, String promptStatus, String imageStatus, String videoStatus);

    /**
     * 分镜拆分（异步，返回 taskId）
     */
    Long splitShots(Long episodeId, Integer duration);

    /**
     * 手动创建分镜
     */
    Long createShot(Long episodeId, ShotCreateRequest req);

    /**
     * 更新分镜（提示词、景别、运镜、台词、承接开关、绑定资产）
     */
    void updateShot(Long id, ShotUpdateRequest req);

    /**
     * 删除分镜
     */
    void deleteShot(Long id);

    /**
     * 分镜排序
     */
    void sortShot(Long id, Integer sortOrder);

    /**
     * 保存草稿
     */
    void saveDraft(Long id, String draftContent);

    /**
     * 生成提示词（异步，返回 taskId）
     */
    Long generatePrompt(Long id);

    /**
     * 生成图片（异步，返回 taskId）
     */
    Long generateImage(Long id);

    /**
     * 生成视频（异步，返回 taskId）
     */
    Long generateVideo(Long id);

    // ===== 批量操作 =====

    /**
     * 批量生成提示词，返回 batchId + taskIds
     */
    BatchResultVO batchPrompt(Long episodeId);

    /**
     * 批量生成图片，返回 batchId + taskIds
     */
    BatchResultVO batchImage(Long episodeId);

    /**
     * 批量生成视频，返回 batchId + taskIds
     */
    BatchResultVO batchVideo(Long episodeId);
}
