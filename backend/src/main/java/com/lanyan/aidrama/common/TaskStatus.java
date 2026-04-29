package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 统一任务状态枚举 (系分 v1.2 第 5.7 节)
 * 对应 task 表 status 字段
 */
@Getter
public enum TaskStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    /** 数据库存储值 */
    private final int code;
    /** 描述 */
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 编译期常量，用于 switch case 和 MyBatis 条件查询
    public static final int PENDING_CODE = 0;
    public static final int PROCESSING_CODE = 1;
    public static final int SUCCESS_CODE = 2;
    public static final int FAILED_CODE = 3;
}
