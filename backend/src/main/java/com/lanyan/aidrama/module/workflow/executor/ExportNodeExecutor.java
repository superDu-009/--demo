package com.lanyan.aidrama.module.workflow.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.module.storage.service.TosService;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导出节点执行器 (系分 5.2)
 * 职责：收集所有视频 URL，FFmpeg 合并为最终视频，上传 TOS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportNodeExecutor implements NodeExecutor {

    private final ShotMapper shotMapper;
    private final TosService tosService;
    private final ObjectMapper objectMapper;

    @Override
    public String getStepType() {
        return "export";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        log.info("开始执行导出节点, projectId: {}", projectId);

        // 收集所有已生成视频的分镜
        LambdaQueryWrapper<Shot> shotWrapper = new LambdaQueryWrapper<>();
        shotWrapper.isNotNull(Shot::getGeneratedVideoUrl)
                   .eq(Shot::getDeleted, 0)
                   .orderByAsc(Shot::getSceneId)
                   .orderByAsc(Shot::getSortOrder);
        List<Shot> videoShots = shotMapper.selectList(shotWrapper);

        if (videoShots.isEmpty()) {
            return NodeResult.fail("没有可导出的视频分镜", "submit");
        }

        // 使用 FFmpeg 合并视频
        String outputFilePath;
        try {
            outputFilePath = mergeVideos(videoShots);
        } catch (Exception e) {
            log.error("FFmpeg 合并视频失败", e);
            return NodeResult.fail("视频合并失败: " + e.getMessage(), "submit");
        }

        if (outputFilePath == null || outputFilePath.isBlank()) {
            return NodeResult.fail("视频合并输出文件为空", "download");
        }

        // 上传合并后的视频到 TOS
        String targetKey = "projects/" + projectId + "/output/final_" + projectId + "_" + System.currentTimeMillis() + ".mp4";
        try {
            byte[] videoData = Files.readAllBytes(Path.of(outputFilePath));
            String finalUrl = tosService.uploadFromBytes(videoData, targetKey);

            String outputData = objectMapper.writeValueAsString(Map.of(
                    "finalVideoUrl", finalUrl,
                    "totalVideos", videoShots.size()
            ));

            // 清理临时文件
            Files.deleteIfExists(Path.of(outputFilePath));

            log.info("导出节点执行完成, projectId: {}, finalUrl: {}", projectId, finalUrl);
            return NodeResult.success(outputData);
        } catch (Exception e) {
            log.error("上传最终视频到 TOS 失败, projectId: {}", projectId, e);
            return NodeResult.fail("上传最终视频失败: " + e.getMessage(), "upload_tos");
        }
    }

    /**
     * 使用 FFmpeg 合并多个视频
     * @return 输出文件路径
     */
    private String mergeVideos(List<Shot> videoShots) throws Exception {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("ffmpeg_merge_" + System.currentTimeMillis());
        String outputFilePath = tempDir.resolve("final.mp4").toString();

        // 生成 FFmpeg 文件列表
        Path listFile = tempDir.resolve("files.txt");
        StringBuilder fileList = new StringBuilder();
        for (Shot shot : videoShots) {
            fileList.append("file '").append(shot.getGeneratedVideoUrl()).append("'\n");
        }
        Files.writeString(listFile, fileList);

        // 构建 FFmpeg 命令：concat 协议合并
        List<String> command = List.of(
                "ffmpeg",
                "-y",                    // 覆盖输出文件
                "-f", "concat",          // concat 格式
                "-safe", "0",            // 允许非安全路径
                "-i", listFile.toString(),
                "-c", "copy",            // 直接复制流，不重新编码
                outputFilePath
        );

        log.info("执行 FFmpeg 合并, 命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取 FFmpeg 输出日志
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg 合并失败, exitCode: {}, output: {}", exitCode, output);
            throw new RuntimeException("FFmpeg 合并失败, exitCode: " + exitCode);
        }

        log.info("FFmpeg 合并成功, output: {}", output);
        return outputFilePath;
    }
}
