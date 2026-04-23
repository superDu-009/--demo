package com.lanyan.aidrama.module.workflow.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.common.ContentStatus;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.module.storage.service.TosService;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导入节点执行器 (系分 5.2)
 * 职责：读取小说文件 TOS 路径，通过 Doubao AI 拆分章节，创建 episode 记录
 * 幂等保证：创建 episode 前先查 project_id + title 是否已存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportNodeExecutor implements NodeExecutor {

    private final ProjectMapper projectMapper;
    private final EpisodeMapper episodeMapper;
    private final DoubaoClient doubaoClient;
    private final TosService tosService;
    private final ObjectMapper objectMapper;

    @Override
    public String getStepType() {
        return "import";
    }

    @Override
    public NodeResult execute(Long projectId, Long episodeId, StepConfig config) {
        log.info("开始执行导入节点, projectId: {}", projectId);

        // 获取项目信息，读取小说文件路径
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return NodeResult.fail("项目不存在", "submit");
        }
        if (project.getNovelTosPath() == null || project.getNovelTosPath().isBlank()) {
            return NodeResult.fail("项目未绑定小说文件路径", "submit");
        }

        // 从 TOS 下载小说内容
        // 注意：novel_tos_path 是 TOS 的 key，需要获取公网 URL 来下载
        String novelContent = downloadNovelContent(project.getNovelTosPath());
        if (novelContent == null || novelContent.isBlank()) {
            return NodeResult.fail("小说内容为空，请确认文件已上传", "submit");
        }

        // 调用 Doubao AI 解析小说章节
        String jsonResponse = doubaoClient.parseNovelChapters(novelContent);
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return NodeResult.fail("AI 章节解析失败，返回为空", "submit");
        }

        // 解析 AI 返回的 JSON
        List<Map<String, Object>> chapters;
        try {
            chapters = objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 AI 返回的章节 JSON 失败, response: {}", jsonResponse, e);
            return NodeResult.fail("解析 AI 章节 JSON 失败: " + e.getMessage(), "submit");
        }

        if (chapters.isEmpty()) {
            return NodeResult.fail("小说章节解析结果为空", "submit");
        }

        // 幂等创建 episode：先查 project_id + title 是否已存在
        List<Long> createdEpisodeIds = new ArrayList<>();
        for (Map<String, Object> chapter : chapters) {
            String title = (String) chapter.get("title");
            String content = (String) chapter.get("content");
            Integer sortOrder = chapter.get("sortOrder") != null ? ((Number) chapter.get("sortOrder")).intValue() : createdEpisodeIds.size();

            // 幂等检查
            LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Episode::getProjectId, projectId)
                   .eq(Episode::getTitle, title);
            if (episodeMapper.selectOne(wrapper) != null) {
                log.info("分集已存在，跳过, title: {}", title);
                continue;
            }

            Episode episode = new Episode();
            episode.setProjectId(projectId);
            episode.setTitle(title);
            episode.setContent(content);
            episode.setSortOrder(sortOrder);
            episode.setStatus(ContentStatus.PENDING);
            episodeMapper.insert(episode);

            createdEpisodeIds.add(episode.getId());
            log.info("创建分集成功, episodeId: {}, title: {}", episode.getId(), title);
        }

        // 保存已创建的 episode ID 列表到 output_data
        String outputData;
        try {
            outputData = objectMapper.writeValueAsString(Map.of("episodeIds", createdEpisodeIds));
        } catch (JsonProcessingException e) {
            return NodeResult.fail("序列化 output_data 失败", "upload_tos");
        }

        log.info("导入节点执行完成, projectId: {}, 创建分集数: {}", projectId, createdEpisodeIds.size());
        return NodeResult.success(outputData);
    }

    /**
     * 从 TOS 路径下载小说内容
     */
    private String downloadNovelContent(String novelTosPath) {
        log.info("下载小说内容, novelTosPath: {}", novelTosPath);
        return tosService.downloadAsString(novelTosPath);
    }
}
