package com.example.couponcore.service.event;

import com.example.couponcore.CouponCoreConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.example.couponcore.utils.MyAssertions.assertCouponIssueEvent;

@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.config.name=application-core"})
@SpringBootTest(classes = CouponCoreConfiguration.class)
class CouponIssuePublisherTest {

    @MockBean
    private RedissonClient redissonClient; // redis connection fail when @ComponentScan

    @Autowired
    CouponIssuePublisher sut; // system under test: 테스트 대상 시스템

    @Autowired
    OutputDestination outputDestination;

    @DisplayName("CouponIssueEvent 객체를 publish 하면 메시지가 발행된다")
    @Test
    void couponIssueEvent() throws JsonProcessingException {
        long couponId = 1L;
        long userId = 1L;
        CouponIssueEvent event = new CouponIssueEvent(couponId, userId);

        sut.publish(event);

        final Message<byte[]> result = outputDestination.receive(1000, "coupon-issue-0");
        assertCouponIssueEvent(result, couponId, userId);
    }
}
