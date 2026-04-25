package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目创建请求 DTO (系分 v1.2 第 7.2 节)
 */
@Data
@Schema(description = "创建项目请求参数")
public class ProjectCreateRequest {

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "小说文件信息")
    @Valid
    private NovelFile novelFile;

    @Schema(description = "画面比例: 16:9 / 9:16", defaultValue = "16:9")
    private String ratio;

    @Schema(description = "清晰度: 720P / 1080P", defaultValue = "1080P")
    private String definition;

    @Schema(description = "风格: 2D次元风/日漫风/国漫风/古风/现代写实/自定义")
    private String style;

    @Schema(description = "风格描述（自定义风格时使用）")
    private String styleDesc;

    /**
     * 小说文件嵌套对象
     */
    @Data
    @Schema(description = "小说文件信息")
    public static class NovelFile {
        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "TOS文件Key")
        private String fileKey;

        @Schema(description = "文件大小(字节)")
        private Long fileSize;
    }
}
