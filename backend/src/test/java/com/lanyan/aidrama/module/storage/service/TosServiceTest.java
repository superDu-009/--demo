package com.lanyan.aidrama.module.storage.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.lanyan.aidrama.module.storage.dto.PresignResult;
import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import com.lanyan.aidrama.common.exception.TosException;
import com.lanyan.aidrama.module.storage.service.TosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TosService单元测试
 * 注意：测试前需要在application-test.yml中填写真实TOS配置
 */
@SpringBootTest
public class TosServiceTest {

    @Autowired
    private TosService tosService;

    /**
     * 测试生成预签名上传URL功能
     * 验证新filekey生成规则是否符合要求
     */
    @Test
    public void testGeneratePresignUrl() {
        // 测试生成预签名URL（前端上传的图片）
        PresignResult result = tosService.generatePresignUrl("111.jpeg", "image/jpeg", "backend", 1L);
        System.out.println(JSONUtil.toJsonStr(result));
        tosService.uploadFromBytes(FileUtil.readBytes("/Users/mac/Downloads/111.jpeg"), result.getFileKey());
    }

    /**
     * 测试字节上传 + 上传完成校验 + 删除功能
     */
    @Test
    public void testUploadAndVerifyFlow() {
        // 1. 测试字节上传
        byte[] testData = "Hello Volcengine TOS".getBytes();
        String fileKey = "test/unit_test_hello.txt";
        String publicUrl = tosService.uploadFromBytes(testData, fileKey);
        assertNotNull(publicUrl);
        System.out.println("字节上传成功，公开访问URL: " + publicUrl);

        // 2. 测试上传完成校验
        TosCompleteRequest req = new TosCompleteRequest();
        req.setFileKey(fileKey);
        req.setBusinessId(1L);
        req.setFileSize((long) testData.length);
        req.setOriginalName("hello.txt");
        String verifiedUrl = tosService.completeUpload(req);
        assertNotNull(verifiedUrl);
        assertEquals(publicUrl, verifiedUrl);
        System.out.println("上传完成校验通过");

        // 3. 测试删除文件
        tosService.deleteFile(fileKey);
        System.out.println("文件删除成功");

        // 4. 测试删除后校验应该抛出异常
        assertThrows(TosException.class, () -> tosService.completeUpload(req), "删除后校验应该失败");
        System.out.println("删除后校验失败验证通过");
    }
}
