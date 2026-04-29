package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 统一任务类型枚举 (系分 v1.2 第 5.7 节)
 * 对应 task 表 type 字段
 */
@Getter
public enum TaskType {

    SCRIPT_ANALYZE("script_analyze", "剧本分析"),
    SHOT_SPLIT("shot_split", "分镜拆分"),
    ASSET_EXTRACT("asset_extract", "资产提取"),
    PROMPT_GEN("prompt_gen", "提示词生成"),
    IMAGE_GEN("image_gen", "图片生成"),
    VIDEO_GEN("video_gen", "视频生成");

    /** 数据库存储值 */
    private final String code;
    /** 描述 */
    private final String desc;

    TaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
