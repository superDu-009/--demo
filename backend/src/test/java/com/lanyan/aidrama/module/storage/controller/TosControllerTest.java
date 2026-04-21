package com.lanyan.aidrama.module.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.module.storage.dto.PresignResult;
import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import com.lanyan.aidrama.module.storage.dto.TosPresignRequest;
import com.lanyan.aidrama.common.exception.TosException;
import com.lanyan.aidrama.module.storage.service.TosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TosController单元测试
 * 验证presign和complete接口的参数校验、正常返回、异常处理
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TosService tosService;

    private TosPresignRequest validPresignRequest;
    private TosCompleteRequest validCompleteRequest;

    @BeforeEach
    void setUp() {
        validPresignRequest = new TosPresignRequest();
        validPresignRequest.setFileName("test.png");
        validPresignRequest.setContentType("image/png");
        validPresignRequest.setSource(TosPresignRequest.UploadSource.frontend);
        validPresignRequest.setBusinessId(1L);

        validCompleteRequest = new TosCompleteRequest();
        validCompleteRequest.setFileKey("test/unit_hello.txt");
        validCompleteRequest.setBusinessId(1L);
        validCompleteRequest.setFileSize(19L);
        validCompleteRequest.setOriginalName("hello.txt");
    }

    @Test
    void testPresign_success() throws Exception {
        mockMvc.perform(post("/api/tos/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPresignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.fileKey").isNotEmpty())
                .andExpect(jsonPath("$.data.expireSeconds").value(3600))
                .andDo(result -> {
                    PresignResult presignResult = objectMapper.readValue(
                            objectMapper.readTree(result.getResponse().getContentAsString())
                                    .get("data").toString(),
                            PresignResult.class
                    );
                    assertNotNull(presignResult.getUploadUrl());
                    assertTrue(presignResult.getFileKey().contains("frontend"));
                    assertTrue(presignResult.getFileKey().contains("1/"));
                });
    }

    @Test
    void testPresign_missingFileName() throws Exception {
        validPresignRequest.setFileName(null);
        mockMvc.perform(post("/api/tos/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPresignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testPresign_missingContentType() throws Exception {
        validPresignRequest.setContentType(null);
        mockMvc.perform(post("/api/tos/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPresignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testPresign_missingSource() throws Exception {
        validPresignRequest.setSource(null);
        mockMvc.perform(post("/api/tos/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPresignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testPresign_missingBusinessId() throws Exception {
        validPresignRequest.setBusinessId(null);
        mockMvc.perform(post("/api/tos/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPresignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testComplete_success() throws Exception {
        // 1. 先上传一个测试文件到TOS
        byte[] testData = "Hello TOS Test".getBytes();
        String fileKey = "test/complete_unit_test.txt";
        String publicUrl = tosService.uploadFromBytes(testData, fileKey);
        assertNotNull(publicUrl);

        // 2. 构建complete请求
        TosCompleteRequest req = new TosCompleteRequest();
        req.setFileKey(fileKey);
        req.setBusinessId(1L);
        req.setFileSize((long) testData.length);
        req.setOriginalName("complete_test.txt");

        // 3. 调用complete接口，校验应通过
        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andDo(result -> {
                    String url = objectMapper.readTree(result.getResponse().getContentAsString())
                            .get("data").asText();
                    assertNotNull(url);
                    assertTrue(url.startsWith("http"));
                });

        // 4. 清理测试文件
        tosService.deleteFile(fileKey);
    }

    @Test
    void testComplete_fileNotExists() throws Exception {
        // 请求一个不存在的fileKey
        validCompleteRequest.setFileKey("test/non_existent_file_key_12345.txt");

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(51102))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testComplete_fileSizeMismatch() throws Exception {
        // 先上传文件
        byte[] testData = "Test data".getBytes();
        String fileKey = "test/size_mismatch_test.txt";
        tosService.uploadFromBytes(testData, fileKey);

        // 故意传一个错误的文件大小
        TosCompleteRequest req = new TosCompleteRequest();
        req.setFileKey(fileKey);
        req.setBusinessId(1L);
        req.setFileSize(999999L); // 错误的大小
        req.setOriginalName("mismatch.txt");

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(51102));

        // 清理
        tosService.deleteFile(fileKey);
    }

    @Test
    void testComplete_missingFileKey() throws Exception {
        validCompleteRequest.setFileKey(null);

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testComplete_missingBusinessId() throws Exception {
        validCompleteRequest.setBusinessId(null);

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testComplete_missingFileSize() throws Exception {
        validCompleteRequest.setFileSize(null);

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void testComplete_missingOriginalName() throws Exception {
        validCompleteRequest.setOriginalName(null);

        mockMvc.perform(post("/api/tos/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002));
    }
}
