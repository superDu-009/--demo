package com.lanyan.aidrama.module.workflow.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.common.ShotStatus;
import com.lanyan.aidrama.entity.AiTask;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.mapper.AiTaskMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.module.aitask.service.AiTaskService;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图片生成节点执行器 (系分 5.2)
 * 职责：遍历待处理的分镜，调用 AiTaskService 提交图片生成任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenNodeExecutor implements NodeExecutor {

    private final ShotMapper shotMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AiTaskService aiTaskService;
    private final ObjectMapper objectMapper;

    @Override
    public String getStepType() {
        return "image_gen";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        log.info("开始执行图片生成节点, projectId: {}, episodeId: {}", projectId, episodeId);

        // 查询当前分集下所有待处理的分镜
        LambdaQueryWrapper<Shot> shotWrapper = new LambdaQueryWrapper<>();
        shotWrapper.eq(Shot::getStatus, ShotStatus.PENDING)
                   .eq(Shot::getDeleted, 0)
                   .orderByAsc(Shot::getSortOrder);
        List<Shot> pendingShots = shotMapper.selectList(shotWrapper);

        if (pendingShots.isEmpty()) {
            log.info("没有待处理的分镜, episodeId: {}", episodeId);
            return NodeResult.success("{}");
        }

        // 提交图片生成任务
        List<Long> submittedTaskIds = new ArrayList<>();
        int totalShots = pendingShots.size();
        int currentIndex = 0;

        for (Shot shot : pendingShots) {
            currentIndex++;
            log.info("提交分镜图片生成任务, shotId: {}, 进度: {}/{}", shot.getId(), currentIndex, totalShots);

            try {
                Long taskId = aiTaskService.submitImageGenTask(shot);
                submittedTaskIds.add(taskId);

                // 更新分镜状态为"生成中"
                shot.setStatus(ShotStatus.GENERATING);
                shot.setGenerationAttempts(shot.getGenerationAttempts() + 1);
                shotMapper.updateById(shot);
            } catch (Exception e) {
                log.error("提交图片生成任务失败, shotId: {}", shot.getId(), e);
                // 继续处理下一个分镜，不阻断整体流程
            }
        }

        // 保存结果到 output_data
        String outputData;
        try {
            outputData = objectMapper.writeValueAsString(Map.of(
                    "taskIds", submittedTaskIds,
                    "totalShots", totalShots,
                    "submittedCount", submittedTaskIds.size()
            ));
        } catch (JsonProcessingException e) {
            return NodeResult.fail("序列化 output_data 失败", "upload_tos");
        }

        log.info("图片生成节点执行完成, episodeId: {}, 提交任务数: {}", episodeId, submittedTaskIds.size());
        return NodeResult.success(outputData);
    }
}
