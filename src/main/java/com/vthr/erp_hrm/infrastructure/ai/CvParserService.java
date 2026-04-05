package com.vthr.erp_hrm.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.vthr.erp_hrm.infrastructure.storage.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvParserService {

    private final FileService fileService;

    public String extractText(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            String filename = filePath.toString().toLowerCase();
            if (filename.endsWith(".pdf")) {
                return fileService.extractTextFromPdf(filePath.toString());
            } else if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse CV: {}", filePath, e);
        }
        return "";
    }
}
