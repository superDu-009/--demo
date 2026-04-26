package com.lanyan.aidrama.module.storage.service;

import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.config.TosConfig;
import com.lanyan.aidrama.module.storage.dto.PresignResult;
import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TosServiceTest {

    private TosService tosService;

    @BeforeEach
    void setUp() {
        TosConfig tosConfig = new TosConfig();
        ReflectionTestUtils.setField(tosConfig, "accessKey", "ak");
        ReflectionTestUtils.setField(tosConfig, "secretKey", "sk");
        ReflectionTestUtils.setField(tosConfig, "endpoint", "tos-s3-cn-beijing.volces.com");
        ReflectionTestUtils.setField(tosConfig, "region", "cn-beijing");
        ReflectionTestUtils.setField(tosConfig, "bucket", "unit-test-bucket");
        tosService = new TosService(tosConfig);
    }

    @Test
    void generatePresignUrlShouldFollowCurrentContract() {
        PresignResult result = tosService.generatePresignUrlWithUser(
                "cover.png", "image/png", "project", "1001", 7L);

        assertNotNull(result.getUploadUrl());
        assertTrue(result.getUploadUrl().contains("unit-test-bucket"));
        assertEquals("users/7/project/1001/7_cover.png", result.getFileKey());
        assertEquals(3600, result.getExpireSeconds());
    }

    @Test
    void completeUploadShouldRejectForeignFileKeyBeforeRemoteCheck() {
        TosCompleteRequest req = new TosCompleteRequest();
        req.setFileKey("users/8/project/1001/8_cover.png");
        req.setSource("project");
        req.setUserId(7L);

        BusinessException ex = assertThrows(BusinessException.class, () -> tosService.completeUpload(req));
        assertEquals(40102, ex.getCode());
    }

    @Test
    void publicUrlShouldSupportEndpointWithScheme() {
        TosConfig tosConfig = new TosConfig();
        ReflectionTestUtils.setField(tosConfig, "accessKey", "ak");
        ReflectionTestUtils.setField(tosConfig, "secretKey", "sk");
        ReflectionTestUtils.setField(tosConfig, "endpoint", "https://tos-s3-cn-shanghai.volces.com");
        ReflectionTestUtils.setField(tosConfig, "region", "cn-shanghai");
        ReflectionTestUtils.setField(tosConfig, "bucket", "bucket-a");
        TosService service = new TosService(tosConfig);

        assertEquals(
                "https://bucket-a.tos-cn-shanghai.volces.com/users/1/avatar/0/1_a.png",
                service.buildPublicUrl("users/1/avatar/0/1_a.png"));
    }
}
