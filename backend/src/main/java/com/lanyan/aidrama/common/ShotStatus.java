package com.lanyan.aidrama.common;

/**
 * 分镜状态常量
 */
public final class ShotStatus {

    private ShotStatus() {}

    /** 待处理 */
    public static final int PENDING = 0;
    /** 生成中 */
    public static final int GENERATING = 1;
    /** 待审核 */
    public static final int REVIEWING = 2;
    /** 已通过 */
    public static final int APPROVED = 3;
    /** 已打回 */
    public static final int REJECTED = 4;
    /** 已完成 */
    public static final int COMPLETED = 5;
}
