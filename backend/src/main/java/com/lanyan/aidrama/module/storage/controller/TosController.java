package com.lanyan.aidrama.module.storage.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.storage.dto.PresignResult;
import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import com.lanyan.aidrama.module.storage.dto.TosPresignRequest;
import com.lanyan.aidrama.module.storage.service.TosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存储模块 Controller (系分 v1.2 第 7.6 节)
 */
@RestController
@RequestMapping("/api/storage/tos")
@RequiredArgsConstructor
@Tag(name = "存储模块", description = "TOS 预签名 URL、上传通知")
public class TosController {

    private final TosService tosService;

    /**
     * 获取预签名上传信息 (POST /api/storage/tos/presign)
     */
    @PostMapping("/presign")
    @Operation(summary = "获取预签名上传信息", description = "生成 TOS 预签名 PUT 上传 URL")
    public Result<PresignResult> presign(@Valid @RequestBody TosPresignRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        PresignResult result = tosService.generatePresignUrlWithUser(
                req.getFileName(), req.getContentType(), req.getSource(), req.getBusinessId(), userId);
        return Result.ok(result);
    }

    /**
     * 上传完成通知 (POST /api/storage/tos/complete)
     */
    @PostMapping("/complete")
    @Operation(summary = "上传完成通知", description = "校验 TOS 文件真实存在，返回公网 URL")
    public Result<String> complete(@Valid @RequestBody TosCompleteRequest req) {
        req.setUserId(StpUtil.getLoginIdAsLong());
        String publicUrl = tosService.completeUpload(req);
        return Result.ok(publicUrl);
    }
}
