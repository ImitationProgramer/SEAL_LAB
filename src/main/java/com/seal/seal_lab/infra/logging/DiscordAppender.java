package com.seal.seal_lab.infra.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

public class DiscordAppender extends AppenderBase<ILoggingEvent> {

    private String webhookUrl; // logback-spring.xml에서 주입받을 변수
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        // 디스코드에 보낼 메시지 포맷팅
        String message = String.format("[%s] %s - %s",
                eventObject.getLevel(),
                eventObject.getLoggerName(),
                eventObject.getFormattedMessage());

        Map<String, String> body = new HashMap<>();
        body.put("content", "🚨 **SEAL_LAB 보안 알림**\n" + message);

        try {
            restTemplate.postForEntity(webhookUrl, body, String.class);
        } catch (Exception e) {
            // 로그 전송 실패 시 무한 루프 방지를 위해 표준 출력으로만 남김
            addError("Failed to send log to Discord: " + e.getMessage());
        }
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}