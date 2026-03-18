package com.vthr.erp_hrm.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.repository.AIEvaluationRepository;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiScreeningWorker {

    private final AiQueueService queueService;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final AIEvaluationRepository evaluationRepository;
    private final CvParserService cvParserService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        String applicationIdStr = queueService.popApplication();
        if (applicationIdStr == null) return;

        UUID applicationId = UUID.fromString(applicationIdStr);
        log.info("Processing AI screening for application: {}", applicationId);

        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) return;

        try {
            application.setAiStatus("PROCESSING");
            applicationRepository.save(application);

            Job job = jobRepository.findById(application.getJobId()).orElse(null);
            if (job == null) throw new RuntimeException("Job not found");

            Path cvPath = Paths.get(uploadDir).resolve(application.getCvUrl()).normalize();
            String cvText = cvParserService.extractText(cvPath);

            if (cvText == null || cvText.isBlank()) {
                throw new RuntimeException("CV text extraction failed or empty");
            }

            application.setCvText(cvText);
            applicationRepository.save(application);

            String jsonResponse = geminiClient.analyzeCv(job.getDescription(), job.getRequiredSkills(), cvText);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new RuntimeException("Gemini returned empty response");
            }

            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
            }

            JsonNode node = objectMapper.readTree(jsonResponse);
            
            AIEvaluation eval = AIEvaluation.builder()
                    .applicationId(applicationId)
                    .score(node.path("score").asInt(0))
                    .matchedSkills(node.path("matchedSkills").asText(""))
                    .missingSkills(node.path("missingSkills").asText(""))
                    .summary(node.path("summary").asText(""))
                    .discrepancy(node.path("discrepancy").asText(""))
                    .build();

            evaluationRepository.save(eval);

            application.setAiStatus("DONE");
            applicationRepository.save(application);

            log.info("AI screening completed for application: {}. Score: {}", applicationId, eval.getScore());

        } catch (Exception e) {
            log.error("AI screening failed for application: {}", applicationId, e);
            application.setAiStatus("FAILED");
            applicationRepository.save(application);
        }
    }
}
