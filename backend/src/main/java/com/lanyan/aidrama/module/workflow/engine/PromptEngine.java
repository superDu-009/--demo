package com.lanyan.aidrama.module.workflow.engine;

import com.lanyan.aidrama.entity.Scene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提示词生成引擎 (系分 3.9)
 * 职责：根据分场+资产信息，生成 AI 生图/视频提示词（中英文）
 */
@Slf4j
@Component
public class PromptEngine {

    /**
     * 根据分场和资产信息生成中文提示词
     * TODO: 后续通过强模型（AI）翻译和优化
     * @param scene 分场信息
     * @param assets 关联资产列表
     * @return 中文提示词
     */
    public String generatePrompt(Scene scene, List<?> assets) {
        log.info("生成分镜提示词, sceneTitle: {}, assetCount: {}",
                scene != null ? scene.getTitle() : "unknown",
                assets != null ? assets.size() : 0);

        // 当前阶段：简化实现，直接使用分场标题和描述
        if (scene == null) {
            return "默认分镜描述";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(scene.getTitle());
        if (scene.getContent() != null) {
            prompt.append("，").append(scene.getContent());
        }

        // 简单拼接资产名称
        if (assets != null && !assets.isEmpty()) {
            prompt.append("，包含资产：");
            for (int i = 0; i < assets.size(); i++) {
                if (i > 0) prompt.append("、");
                // 简化实现，后续使用 Asset 的 getName()
                prompt.append("asset").append(i + 1);
            }
        }

        return prompt.toString();
    }

    /**
     * 将中文提示词翻译为英文（供 AI 模型使用）
     * TODO: 后续通过强模型翻译
     * @param promptCn 中文提示词
     * @return 英文提示词
     */
    public String translatePrompt(String promptCn) {
        log.info("翻译提示词");
        // 当前阶段：直接返回中文+占位符，后续接入翻译模型
        return "[EN] " + promptCn;
    }
}
