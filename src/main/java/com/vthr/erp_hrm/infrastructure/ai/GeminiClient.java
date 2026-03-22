package com.vthr.erp_hrm.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiClient {

    private final RestClient restClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent")
                .build();
    }

    public String analyzeCv(String jobDescription, String requiredSkills, String cvText) {
        String prompt = "You are an expert ATS (Applicant Tracking System) AI assistant. Evaluate the candidate's CV against the Job Description.\n" +
                "Job Description: " + jobDescription + "\n" +
                "Required Skills: " + requiredSkills + "\n" +
                "CV Text: " + cvText + "\n\n" +
                "Respond ONLY with a raw JSON object and nothing else. No markdown, no code blocks.\n" +
                "Schema: {\"score\": number 0-100, \"matchedSkills\": string, \"missingSkills\": string, \"summary\": string, \"discrepancy\": string}";

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );

            Map response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                    return parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
        }
        return null;
    }
}
