package com.vthr.erp_hrm.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Async
    public void sendInterviewInvitation(String toEmail, String candidateName, String jobTitle, String interviewTimeStr, String location) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Lịch Phỏng Vấn: " + jobTitle + " - VTHR Solutions");
            message.setText("Xin chào " + candidateName + ",\n\n" +
                    "Chúng tôi rất vui được mời bạn tham gia phỏng vấn cho vị trí " + jobTitle + ".\n" +
                    "Thời gian: " + interviewTimeStr + "\n" +
                    "Địa điểm/Link: " + location + "\n\n" +
                    "Vui lòng phản hồi email này nếu bạn hoặc HR cần thay đổi lịch hẹn.\n\n" +
                    "Trân trọng,\nBộ phận Tuyển dụng VTHR");
                    
            mailSender.send(message);
            log.info("Interview email dispatched asynchronously to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to route mail dispatch explicitly to {}", toEmail, e);
        }
    }
}
