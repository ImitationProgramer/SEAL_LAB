package com.seal.seal_lab.infra.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordAppenderTests {

    @Mock
    private RestOperations restOperations;

    @Mock
    private ILoggingEvent loggingEvent;

    @Test
    void doesNotSendWhenAppenderIsDisabled() {
        DiscordAppender appender = configuredAppender();
        appender.setEnabled(false);

        appender.doAppend(loggingEvent);

        verify(restOperations, never()).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    void doesNotSendWhenWebhookUrlIsBlank() {
        DiscordAppender appender = configuredAppender();
        appender.setWebhookUrl("   ");

        appender.doAppend(loggingEvent);

        verify(restOperations, never()).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    void truncatesLongDiscordMessageToConfiguredLimit() {
        DiscordAppender appender = configuredAppender();
        appender.setMaxContentLength(80);
        when(loggingEvent.getFormattedMessage()).thenReturn("A".repeat(200));

        appender.doAppend(loggingEvent);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restOperations).postForEntity(eq("https://discord.test/webhook"), bodyCaptor.capture(), eq(String.class));
        String content = bodyCaptor.getValue().get("content");

        assertThat(content).hasSize(80);
        assertThat(content).endsWith("...");
        assertThat(content).contains("SEAL_LAB Security Alert");
    }

    @Test
    void swallowsDiscordDeliveryFailure() {
        DiscordAppender appender = configuredAppender();
        when(restOperations.postForEntity(eq("https://discord.test/webhook"), any(), eq(String.class)))
                .thenThrow(new RestClientException("timeout"));

        assertThatCode(() -> appender.doAppend(loggingEvent)).doesNotThrowAnyException();
    }

    private DiscordAppender configuredAppender() {
        DiscordAppender appender = new DiscordAppender();
        appender.setWebhookUrl("https://discord.test/webhook");
        appender.setRestOperations(restOperations);
        appender.setContext(new ch.qos.logback.core.ContextBase());
        appender.start();

        lenient().when(loggingEvent.getLevel()).thenReturn(Level.WARN);
        lenient().when(loggingEvent.getLoggerName()).thenReturn("com.seal.seal_lab.test");
        lenient().when(loggingEvent.getFormattedMessage()).thenReturn("sample security event");

        return appender;
    }
}
