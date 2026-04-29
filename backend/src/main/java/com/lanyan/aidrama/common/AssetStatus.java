package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 资产状态枚举 (系分 v1.2 第 5.5 节)
 * 对应 asset 表 status 字段
 */
@Getter
public enum AssetStatus {

    DRAFT("draft", "草稿"),
    CONFIRMED("confirmed", "已确认"),
    DEPRECATED("deprecated", "已废弃");

    /** 数据库存储值 */
    private final String code;
    /** 描述 */
    private final String desc;

    AssetStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
