package com.lanyan.aidrama.module.asset.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.asset.dto.*;
import com.lanyan.aidrama.module.asset.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产模块 Controller (系分 v1.2 第 7.4 节)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "资产模块", description = "资产 CRUD、提取、重复检测、关系树")
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/project/{projectId}/assets")
    @Operation(summary = "资产列表", description = "查询项目下资产，支持类型过滤")
    public Result<List<AssetVO>> listAssets(
            @PathVariable Long projectId,
            @RequestParam(required = false) String assetType) {
        return Result.ok(assetService.listAssets(projectId, assetType));
    }

    @PostMapping("/project/{projectId}/assets")
    @Operation(summary = "创建资产", description = "在项目下创建新资产")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<Long> createAsset(@PathVariable Long projectId,
                                     @Valid @RequestBody AssetCreateRequest req) {
        return Result.ok(assetService.createAsset(projectId, req));
    }

    @PutMapping("/asset/{id}")
    @Operation(summary = "更新资产", description = "更新资产基本信息")
    public Result<Void> updateAsset(@PathVariable Long id,
                                     @Valid @RequestBody AssetUpdateRequest req) {
        assetService.updateAsset(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/asset/{id}")
    @Operation(summary = "删除资产", description = "逻辑删除资产，被分镜绑定的资产禁止删除")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return Result.ok(null);
    }

    @PutMapping("/asset/{id}/confirm")
    @Operation(summary = "确认资产", description = "将资产状态设置为已确认，需至少有1张参考图")
    public Result<Void> confirmAsset(@PathVariable Long id) {
        assetService.confirmAsset(id);
        return Result.ok(null);
    }

    @PostMapping("/project/{projectId}/assets/extract")
    @Operation(summary = "提取资产", description = "按分集异步提取资产，返回 taskId")
    public Result<Long> extractAssets(@PathVariable Long projectId,
                                       @RequestParam Long episodeId) {
        return Result.ok(assetService.extractAssets(projectId, episodeId));
    }

    @GetMapping("/project/{projectId}/assets/extract/status")
    @Operation(summary = "提取资产状态", description = "查询资产提取进度")
    public Result<com.lanyan.aidrama.module.task.dto.TaskVO> getExtractStatus(@PathVariable Long projectId) {
        // 查询该项目最新的 asset_extract 任务
        return Result.ok(assetService.getExtractTaskStatus(projectId));
    }

    @GetMapping("/project/{projectId}/assets/duplicates")
    @Operation(summary = "重复资产检测", description = "按名称/描述相似度大于80%聚合")
    public Result<List<AssetDuplicateVO>> getDuplicateAssets(@PathVariable Long projectId) {
        return Result.ok(assetService.getDuplicateAssets(projectId));
    }

    @GetMapping("/project/{projectId}/assets/tree")
    @Operation(summary = "资产关系树", description = "查询父子资产关系树")
    public Result<List<AssetTreeNode>> getAssetTree(@PathVariable Long projectId) {
        return Result.ok(assetService.getAssetTree(projectId));
    }

    @PutMapping("/asset/{id}/relations")
    @Operation(summary = "更新资产关系", description = "更新父子资产关系")
    public Result<Void> updateAssetRelations(@PathVariable Long id,
                                              @RequestParam String parentIds) {
        assetService.updateAssetRelations(id, parentIds);
        return Result.ok(null);
    }

    @GetMapping("/asset/{id}/references")
    @Operation(summary = "资产引用查询", description = "分页查询资产被哪些分镜引用")
    public Result<PageResult<ShotReferenceVO>> getAssetReferences(
            @PathVariable Long assetId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return Result.ok(assetService.getAssetReferences(assetId, page, size));
    }
}
