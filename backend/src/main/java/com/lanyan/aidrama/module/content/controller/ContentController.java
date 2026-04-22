package com.lanyan.aidrama.module.content.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.content.dto.*;
import com.lanyan.aidrama.module.content.service.ContentService;
import com.lanyan.aidrama.module.project.dto.ShotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容模块 Controller (系分 4.4.1)
 * 提供分集/分场/分镜 CRUD、批量审核、资产绑定
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "内容模块", description = "分集、分场、分镜 CRUD")
public class ContentController {

    private final ContentService contentService;

    // ===== 分集 =====

    @GetMapping("/project/{projectId}/episodes")
    @Operation(summary = "分集列表", description = "查询项目下所有分集")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<List<EpisodeVO>> listEpisodes(@PathVariable Long projectId) {
        return Result.ok(contentService.listEpisodes(projectId));
    }

    @PostMapping("/project/{projectId}/episodes")
    @Operation(summary = "创建分集", description = "在项目下创建分集，sort_order自动递增")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<Long> createEpisode(@PathVariable Long projectId,
                                       @Valid @RequestBody EpisodeCreateRequest req) {
        return Result.ok(contentService.createEpisode(projectId, req));
    }

    @PutMapping("/episode/{id}")
    @Operation(summary = "更新分集", description = "更新分集基本信息")
    @Parameter(name = "id", description = "分集ID", required = true)
    public Result<Void> updateEpisode(@PathVariable Long id,
                                       @Valid @RequestBody EpisodeUpdateRequest req) {
        contentService.updateEpisode(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/episode/{id}")
    @Operation(summary = "删除分集", description = "级联删除分集及其分场、分镜")
    @Parameter(name = "id", description = "分集ID", required = true)
    public Result<Void> deleteEpisode(@PathVariable Long id) {
        contentService.deleteEpisode(id);
        return Result.ok(null);
    }

    // ===== 分场 =====

    @GetMapping("/episode/{episodeId}/scenes")
    @Operation(summary = "分场列表", description = "查询分集下所有分场")
    @Parameter(name = "episodeId", description = "分集ID", required = true)
    public Result<List<SceneVO>> listScenes(@PathVariable Long episodeId) {
        return Result.ok(contentService.listScenes(episodeId));
    }

    @PostMapping("/episode/{episodeId}/scenes")
    @Operation(summary = "创建分场", description = "在分集下创建分场")
    @Parameter(name = "episodeId", description = "分集ID", required = true)
    public Result<Long> createScene(@PathVariable Long episodeId,
                                     @Valid @RequestBody SceneCreateRequest req) {
        return Result.ok(contentService.createScene(episodeId, req));
    }

    @PutMapping("/scene/{id}")
    @Operation(summary = "更新分场", description = "更新分场基本信息")
    @Parameter(name = "id", description = "分场ID", required = true)
    public Result<Void> updateScene(@PathVariable Long id,
                                     @Valid @RequestBody SceneUpdateRequest req) {
        contentService.updateScene(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/scene/{id}")
    @Operation(summary = "删除分场", description = "级联删除分场及其分镜")
    @Parameter(name = "id", description = "分场ID", required = true)
    public Result<Void> deleteScene(@PathVariable Long id) {
        contentService.deleteScene(id);
        return Result.ok(null);
    }

    // ===== 分镜 =====

    @GetMapping("/scene/{sceneId}/shots")
    @Operation(summary = "分镜列表", description = "分页查询分场下所有分镜")
    @Parameter(name = "sceneId", description = "分场ID", required = true)
    public Result<PageResult<ShotVO>> listShots(
            @PathVariable Long sceneId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(contentService.listShots(sceneId, page, size, status));
    }

    @PostMapping("/scene/{sceneId}/shots")
    @Operation(summary = "创建分镜", description = "在分场下创建分镜")
    @Parameter(name = "sceneId", description = "分场ID", required = true)
    public Result<Long> createShot(@PathVariable Long sceneId,
                                    @Valid @RequestBody ShotCreateRequest req) {
        return Result.ok(contentService.createShot(sceneId, req));
    }

    @PutMapping("/shot/{id}")
    @Operation(summary = "更新分镜", description = "更新分镜信息（提示词、生成结果等）")
    @Parameter(name = "id", description = "分镜ID", required = true)
    public Result<Void> updateShot(@PathVariable Long id,
                                    @Valid @RequestBody ShotUpdateRequest req) {
        contentService.updateShot(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/shot/{id}")
    @Operation(summary = "删除分镜", description = "删除分镜及其关联资产")
    @Parameter(name = "id", description = "分镜ID", required = true)
    public Result<Void> deleteShot(@PathVariable Long id) {
        contentService.deleteShot(id);
        return Result.ok(null);
    }

    @PostMapping("/shot/batch-review")
    @Operation(summary = "批量审核", description = "批量通过/打回分镜，返回成功/失败明细")
    public Result<BatchReviewResult> batchReviewShots(@Valid @RequestBody BatchReviewRequest req) {
        return Result.ok(contentService.batchReviewShots(req.getShotIds(), req.getAction(), req.getComment()));
    }

    @PostMapping("/shot/{shotId}/assets")
    @Operation(summary = "绑定资产到分镜", description = "将资产关联到分镜用于AI生成一致性")
    @Parameter(name = "shotId", description = "分镜ID", required = true)
    public Result<Void> bindAssetToShot(@PathVariable Long shotId,
                                         @RequestParam Long assetId,
                                         @RequestParam String assetType) {
        contentService.bindAssetToShot(shotId, assetId, assetType);
        return Result.ok(null);
    }

    @DeleteMapping("/shot/{shotId}/assets/{assetId}")
    @Operation(summary = "解绑分镜资产", description = "从分镜解绑关联的资产")
    @Parameter(name = "shotId", description = "分镜ID", required = true)
    @Parameter(name = "assetId", description = "资产ID", required = true)
    public Result<Void> unbindAssetFromShot(@PathVariable Long shotId,
                                             @PathVariable Long assetId) {
        contentService.unbindAssetFromShot(shotId, assetId);
        return Result.ok(null);
    }
}
