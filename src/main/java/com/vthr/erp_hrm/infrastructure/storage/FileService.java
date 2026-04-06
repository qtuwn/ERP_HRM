package com.vthr.erp_hrm.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class FileService {

    /**
     * Extract raw text from a PDF file using PDFBox.
     * - Returns empty string for blank/unsupported/encrypted PDFs.
     */
    public String extractTextFromPdf(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path) || Files.isDirectory(path)) {
                return "";
            }

            File pdfFile = path.toFile();
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                if (document.isEncrypted()) {
                    // Without password, we cannot extract. Avoid crashing worker.
                    log.warn("PDF is encrypted/password-protected: {}", filePath);
                    return "";
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                if (text == null) return "";
                String trimmed = text.trim();
                if (trimmed.isBlank()) {
                    log.warn("PDF extracted text is blank: {}", filePath);
                    return "";
                }
                return trimmed;
            }
        } catch (InvalidPasswordException e) {
            log.warn("PDF password required: {}", filePath);
            return "";
        } catch (Exception e) {
            log.error("Failed to extract PDF text: {}", filePath, e);
            return "";
        }
    }
}

