package com.lanyan.aidrama.module.storage.controller;

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
 * 存储模块 Controller (系分 4.7.1)
 * 负责TOS预签名URL生成和上传完成通知
 */
@RestController
@RequestMapping("/api/tos")
@RequiredArgsConstructor
@Tag(name = "存储模块", description = "TOS 预签名 URL、上传通知")
public class TosController {

    private final TosService tosService;

    /**
     * 获取预签名上传URL (POST /api/tos/presign)
     * 需要登录鉴权，生成PUT预签名URL供前端直传TOS
     */
    @PostMapping("/presign")
    @Operation(summary = "获取预签名上传URL", description = "生成TOS预签名PUT上传URL，有效期3600秒")
    public Result<PresignResult> presign(@Valid @RequestBody TosPresignRequest req) {
        // 调用TosService生成预签名URL
        PresignResult result = tosService.generatePresignUrl(
                req.getFileName(),
                req.getContentType(),
                req.getSource().name(),
                req.getBusinessId()
        );
        return Result.ok(result);
    }

    /**
     * 上传完成通知 (POST /api/tos/complete)
     * 前端直传TOS成功后，调用此接口校验文件存在并返回公网URL
     */
    @PostMapping("/complete")
    @Operation(summary = "上传完成通知", description = "校验TOS文件真实存在，返回公网访问URL")
    public Result<?> complete(@Valid @RequestBody TosCompleteRequest req) {
        // 调用TosService校验文件存在，返回公网URL
        String publicUrl = tosService.completeUpload(req);
        return Result.ok(publicUrl);
    }
}
