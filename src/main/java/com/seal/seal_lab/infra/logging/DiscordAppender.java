package com.seal.seal_lab.infra.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class DiscordAppender extends AppenderBase<ILoggingEvent> {

    private static final int DISCORD_CONTENT_HARD_LIMIT = 2000;
    private static final String CONTENT_PREFIX = "SEAL_LAB Security Alert\n";

    private String webhookUrl;
    private boolean enabled = true;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 2000;
    private int maxContentLength = 1500;
    private RestOperations restOperations;

    @Override
    public void start() {
        if (restOperations == null) {
            restOperations = buildRestOperations();
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        if (restOperations == null) {
            restOperations = buildRestOperations();
        }

        String message = String.format("[%s] %s - %s",
                eventObject.getLevel(),
                eventObject.getLoggerName(),
                eventObject.getFormattedMessage());

        Map<String, String> body = new HashMap<>();
        body.put("content", toDiscordContent(message));

        try {
            restOperations.postForEntity(webhookUrl, body, String.class);
        } catch (Exception e) {
            addError("Failed to send log to Discord: " + e.getMessage());
        }
    }

    private RestOperations buildRestOperations() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    private String toDiscordContent(String message) {
        int payloadLimit = Math.min(DISCORD_CONTENT_HARD_LIMIT, Math.max(32, maxContentLength));
        String content = CONTENT_PREFIX + message;
        if (content.length() <= payloadLimit) {
            return content;
        }
        return content.substring(0, payloadLimit - 3) + "...";
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    void setRestOperations(RestOperations restOperations) {
        this.restOperations = restOperations;
    }
}
