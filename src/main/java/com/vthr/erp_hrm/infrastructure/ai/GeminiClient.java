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
        String prompt =
                "Bạn là một chuyên gia tuyển dụng (ATS). Hãy phân tích CV dựa trên Job Description (JD) sau.\n" +
                "Yêu cầu: so sánh kỹ năng, kinh nghiệm; chấm điểm mức phù hợp.\n\n" +
                "JD_TEXT:\n" + (jobDescription == null ? "" : jobDescription) + "\n\n" +
                "REQUIRED_SKILLS (comma-separated):\n" + (requiredSkills == null ? "" : requiredSkills) + "\n\n" +
                "CV_TEXT:\n" + (cvText == null ? "" : cvText) + "\n\n" +
                "QUY TẮC TRẢ LỜI:\n" +
                "- CHỈ trả về JSON thuần (không markdown, không code block).\n" +
                "- matchedSkills và missingSkills phải là mảng string.\n" +
                "- score là số nguyên 0-100.\n" +
                "- suitability chỉ nhận một trong: HIGH | MEDIUM | LOW.\n" +
                "JSON schema:\n" +
                "{\"score\":0,\"matchedSkills\":[],\"missingSkills\":[],\"summary\":\"\",\"suitability\":\"HIGH\"}";

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
