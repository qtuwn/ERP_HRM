package com.vthr.erp_hrm.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class SignedUrlService {

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateSignedUrl(String endpoint, String objectPath) {
        long expiresAt = Instant.now().plusSeconds(15 * 60).getEpochSecond(); // 15 minutes
        String dataToSign = objectPath + ":" + expiresAt;
        String signature = calculateHmac(dataToSign);
        return endpoint + "/" + objectPath + "?expires=" + expiresAt + "&signature=" + signature;
    }

    public boolean verifySignature(String objectPath, long expiresAt, String signature) {
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String dataToSign = objectPath + ":" + expiresAt;
        String expectedSignature = calculateHmac(dataToSign);
        return expectedSignature.equals(signature);
    }

    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }
}
