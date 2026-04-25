package com.lanyan.aidrama.module.content.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.content.dto.*;
import com.lanyan.aidrama.module.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容模块 Controller (系分 v1.2 第 7.3 节)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "内容模块", description = "分集/分镜 CRUD、AI 生成")
public class ContentController {

    private final ContentService contentService;

    // ===== 分集 =====

    @GetMapping("/project/{projectId}/episodes")
    @Operation(summary = "分集列表", description = "查询项目下所有分集")
    public Result<List<EpisodeVO>> listEpisodes(@PathVariable Long projectId) {
        return Result.ok(contentService.listEpisodes(projectId));
    }

    @PostMapping("/project/{projectId}/episodes/analyze")
    @Operation(summary = "剧本分析", description = "异步分析小说内容生成多个分集，返回 taskId")
    public Result<Long> analyzeScript(@PathVariable Long projectId) {
        return Result.ok(contentService.analyzeScript(projectId));
    }

    @GetMapping("/project/{projectId}/episodes/analyze/status")
    @Operation(summary = "剧本分析状态", description = "查询剧本分析进度")
    public Result<ScriptAnalyzeStatusVO> getAnalyzeStatus(@PathVariable Long projectId) {
        return Result.ok(contentService.getAnalyzeStatus(projectId));
    }

    @PostMapping("/project/{projectId}/episodes")
    @Operation(summary = "手动创建分集", description = "手动创建分集")
    public Result<Long> createEpisode(@PathVariable Long projectId,
                                       @Valid @RequestBody EpisodeCreateRequest req) {
        return Result.ok(contentService.createEpisode(projectId, req));
    }

    @PutMapping("/episode/{id}")
    @Operation(summary = "更新分集", description = "更新分集基本信息")
    public Result<Void> updateEpisode(@PathVariable Long id,
                                       @Valid @RequestBody EpisodeUpdateRequest req) {
        contentService.updateEpisode(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/episode/{id}")
    @Operation(summary = "删除分集", description = "级联删除分集及其分镜")
    public Result<Void> deleteEpisode(@PathVariable Long id) {
        contentService.deleteEpisode(id);
        return Result.ok(null);
    }

    // ===== 分镜 =====

    @GetMapping("/episode/{episodeId}/shots")
    @Operation(summary = "分镜列表", description = "查询分集下所有分镜，支持状态过滤")
    public Result<List<ShotVO>> listShots(
            @PathVariable Long episodeId,
            @RequestParam(required = false) String promptStatus,
            @RequestParam(required = false) String imageStatus,
            @RequestParam(required = false) String videoStatus) {
        return Result.ok(contentService.listShots(episodeId, promptStatus, imageStatus, videoStatus));
    }

    @PostMapping("/episode/{episodeId}/shots/split")
    @Operation(summary = "分镜拆分", description = "按时长枚举（10/12/15秒）拆分分镜")
    public Result<Long> splitShots(@PathVariable Long episodeId,
                                    @RequestParam(required = false) Integer duration) {
        return Result.ok(contentService.splitShots(episodeId, duration));
    }

    @PostMapping("/episode/{episodeId}/shots")
    @Operation(summary = "手动创建分镜", description = "手动创建单个分镜")
    public Result<Long> createShot(@PathVariable Long episodeId,
                                    @Valid @RequestBody ShotCreateRequest req) {
        return Result.ok(contentService.createShot(episodeId, req));
    }

    @PutMapping("/shot/{id}")
    @Operation(summary = "更新分镜", description = "更新分镜信息")
    public Result<Void> updateShot(@PathVariable Long id,
                                    @Valid @RequestBody ShotUpdateRequest req) {
        contentService.updateShot(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/shot/{id}")
    @Operation(summary = "删除分镜", description = "删除分镜")
    public Result<Void> deleteShot(@PathVariable Long id) {
        contentService.deleteShot(id);
        return Result.ok(null);
    }

    @PutMapping("/shot/{id}/sort")
    @Operation(summary = "分镜排序", description = "更新分镜排序号")
    public Result<Void> sortShot(@PathVariable Long id,
                                  @RequestParam Integer sortOrder) {
        contentService.sortShot(id, sortOrder);
        return Result.ok(null);
    }

    @PostMapping("/shot/{id}/draft")
    @Operation(summary = "保存草稿", description = "保存分镜草稿到 draft_content")
    public Result<Void> saveDraft(@PathVariable Long id,
                                   @RequestBody String draftContent) {
        contentService.saveDraft(id, draftContent);
        return Result.ok(null);
    }

    @PostMapping("/shot/{id}/prompt/generate")
    @Operation(summary = "生成提示词", description = "调用 AI 生成分镜英文提示词")
    public Result<Long> generatePrompt(@PathVariable Long id) {
        return Result.ok(contentService.generatePrompt(id));
    }

    @PostMapping("/shot/{id}/image/generate")
    @Operation(summary = "生成图片", description = "调用 AI 生成分镜图片")
    public Result<Long> generateImage(@PathVariable Long id) {
        return Result.ok(contentService.generateImage(id));
    }

    @PostMapping("/shot/{id}/video/generate")
    @Operation(summary = "生成视频", description = "调用 AI 生成分镜视频，支持承接上一分镜尾帧")
    public Result<Long> generateVideo(@PathVariable Long id) {
        return Result.ok(contentService.generateVideo(id));
    }

    // ===== 批量操作 =====

    @PostMapping("/episode/{episodeId}/shots/batch/prompt")
    @Operation(summary = "批量生成提示词", description = "为分集下所有分镜批量生成提示词")
    public Result<BatchResultVO> batchPrompt(@PathVariable Long episodeId) {
        return Result.ok(contentService.batchPrompt(episodeId));
    }

    @PostMapping("/episode/{episodeId}/shots/batch/image")
    @Operation(summary = "批量生成图片", description = "为分集下所有分镜批量生成图片")
    public Result<BatchResultVO> batchImage(@PathVariable Long episodeId) {
        return Result.ok(contentService.batchImage(episodeId));
    }

    @PostMapping("/episode/{episodeId}/shots/batch/video")
    @Operation(summary = "批量生成视频", description = "为分集下所有分镜批量生成视频")
    public Result<BatchResultVO> batchVideo(@PathVariable Long episodeId) {
        return Result.ok(contentService.batchVideo(episodeId));
    }
}
