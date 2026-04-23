package com.lanyan.aidrama.module.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点执行结果封装 (系分 5.2)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {

    /** 执行结果: success/fail/pending(待审核) */
    private String status;

    /** 输出数据，供后续步骤使用 */
    private String outputData;

    /** 错误信息，失败时填写 */
    private String errorMsg;

    /** 当前子步骤: submit/polling/download/upload_tos */
    private String subStep;

    /** 创建成功结果 */
    public static NodeResult success(String outputData) {
        return new NodeResult("success", outputData, null, null);
    }

    /** 创建失败结果 */
    public static NodeResult fail(String errorMsg, String subStep) {
        return new NodeResult("fail", null, errorMsg, subStep);
    }

    /** 创建待审核结果 */
    public static NodeResult pending(String outputData, String subStep) {
        return new NodeResult("pending", outputData, null, subStep);
    }
}
