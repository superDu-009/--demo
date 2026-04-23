package com.lanyan.aidrama.common;

/**
 * 内容状态常量 (分集/分场)
 */
public final class ContentStatus {

    private ContentStatus() {}

    /** 待处理 */
    public static final int PENDING = 0;
    /** 进行中 */
    public static final int IN_PROGRESS = 1;
    /** 已完成 */
    public static final int COMPLETED = 2;
}
