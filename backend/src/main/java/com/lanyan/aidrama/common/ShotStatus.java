package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 分镜状态枚举 (系分 v1.2 第 5.4 节)
 * 对应 shot 表 promptStatus/imageStatus/videoStatus 字段
 */
@Getter
public enum ShotStatus {

    PENDING("pending", "待处理"),
    GENERATING("generating", "生成中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败");

    /** 数据库存储值 */
    private final String code;
    /** 描述 */
    private final String desc;

    ShotStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
