package com.vthr.erp_hrm.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class CvParserService {

    public String extractText(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            String filename = filePath.toString().toLowerCase();
            if (filename.endsWith(".pdf")) {
                try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(filePath.toFile())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
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
