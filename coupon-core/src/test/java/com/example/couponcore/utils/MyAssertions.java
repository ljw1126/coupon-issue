package com.example.couponcore.utils;

import com.example.couponcore.service.event.CouponIssuePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

public class MyAssertions {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    private MyAssertions() {
    }

    public static void assertCouponIssueEvent(Message<byte[]> result, long couponId, long userId) throws JsonProcessingException {
        final String payload = new String(result.getPayload());
        final JsonNode jsonNode = objectMapper.readTree(payload);

        assertThat(jsonNode.get("type").asText()).isEqualTo("CouponIssue");
        assertThat(jsonNode.get("couponId").asLong()).isEqualTo(couponId);
        assertThat(jsonNode.get("userId").asLong()).isEqualTo(userId);

        MessageHeaders headers = result.getHeaders();
        Long messageKey = headers.get(CouponIssuePublisher.MESSAGE_KEY, Long.class);
        assertThat(messageKey).isNotNull();
        assertThat(messageKey).isEqualTo(couponId);
    }
}
