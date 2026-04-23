package com.lanyan.aidrama.module.workflow.dto;

import lombok.Data;

/**
 * 步骤配置 (系分 5.1)
 * 从 project.workflow_config JSON 中解析
 */
@Data
public class StepConfig {

    /** 步骤类型: import/asset_extract/shot_gen/image_gen/video_gen/export */
    private String stepType;

    /** 是否启用 */
    private boolean enabled;

    /** 是否需要审核 */
    private boolean review;

    /** 步骤顺序 */
    private int stepOrder;

    /** 步骤级额外配置 */
    private Object config;
}
