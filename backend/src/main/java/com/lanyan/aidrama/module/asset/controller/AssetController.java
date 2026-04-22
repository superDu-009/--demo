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
 * 资产模块 Controller (系分 4.3.1)
 * 提供资产 CRUD、确认、引用查询
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "资产模块", description = "资产 CRUD、确认、引用查询")
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/project/{projectId}/assets")
    @Operation(summary = "资产列表", description = "按类型分组查询项目下资产")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<List<AssetVO>> listAssets(
            @PathVariable Long projectId,
            @RequestParam(required = false) String assetType) {
        List<AssetVO> list = assetService.listAssets(projectId, assetType);
        return Result.ok(list);
    }

    @PostMapping("/project/{projectId}/assets")
    @Operation(summary = "创建资产", description = "在项目下创建新资产，支持多参考图")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<Long> createAsset(@PathVariable Long projectId,
                                     @Valid @RequestBody AssetCreateRequest req) {
        Long assetId = assetService.createAsset(projectId, req);
        return Result.ok(assetId);
    }

    @PutMapping("/asset/{id}")
    @Operation(summary = "更新资产", description = "更新资产基本信息")
    @Parameter(name = "id", description = "资产ID", required = true)
    public Result<Void> updateAsset(@PathVariable Long id,
                                     @Valid @RequestBody AssetUpdateRequest req) {
        assetService.updateAsset(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/asset/{id}")
    @Operation(summary = "删除资产", description = "逻辑删除资产，需检查是否被分镜引用")
    @Parameter(name = "id", description = "资产ID", required = true)
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return Result.ok(null);
    }

    @PutMapping("/asset/{id}/confirm")
    @Operation(summary = "确认资产", description = "将资产状态设置为已确认")
    @Parameter(name = "id", description = "资产ID", required = true)
    public Result<Void> confirmAsset(@PathVariable Long id) {
        assetService.confirmAsset(id);
        return Result.ok(null);
    }

    @GetMapping("/asset/{assetId}/references")
    @Operation(summary = "资产引用查询", description = "分页查询资产被哪些分镜引用")
    @Parameter(name = "assetId", description = "资产ID", required = true)
    public Result<PageResult<ShotReferenceVO>> getAssetReferences(
            @PathVariable Long assetId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        PageResult<ShotReferenceVO> result = assetService.getAssetReferences(assetId, page, size);
        return Result.ok(result);
    }
}
