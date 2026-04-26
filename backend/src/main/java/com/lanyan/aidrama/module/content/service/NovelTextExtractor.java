package com.lanyan.aidrama.module.content.service;

import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Service
public class NovelTextExtractor {

    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of("txt", "md", "markdown");

    public String extract(byte[] bytes, String originalFileName) {
        String extension = getExtension(originalFileName);
        try {
            String parsed = switch (extension) {
                case "docx" -> extractDocx(bytes);
                case "doc" -> extractDoc(bytes);
                case "pdf" -> extractPdf(bytes);
                case "txt", "md", "markdown", "" -> extractPlainText(bytes);
                default -> throw new BusinessException(
                        ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(),
                        "当前仅支持 txt/md/doc/docx/pdf 小说解析");
            };
            if (parsed == null || parsed.isBlank()) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), "小说内容解析为空");
            }
            return parsed.replace("\u0000", "").strip();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("小说文本解析失败, fileName: {}", originalFileName, e);
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), "小说文件解析失败");
        }
    }

    private String extractPlainText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String getExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
