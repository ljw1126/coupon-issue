package com.example.couponconsumer.component;

import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.example.couponcore.service.CouponIssueService;
import com.example.couponcore.util.CouponRedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@EnableScheduling
@Component
public class CouponIssueListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final RedisRepository redisRepository;
    private final CouponIssueService couponIssueService;
    private final ObjectMapper objectMapper;
    private final String issueRequestQueueKey = CouponRedisUtils.getIssueRequestQueueKey();

    @Scheduled(fixedDelay = 1000L)
    public void issue() throws JsonProcessingException {
        logger.info("listen.....");
        while (existCouponIssueTarget()) {
            CouponIssueRequest target = getIssueTarget();
            logger.info("발급 시작 target : %s".formatted(target));
            couponIssueService.issueWithLock(target.couponId(), target.userId());
            logger.info("발급 완료 target : %s".formatted(target));
            removeIssueTarget();
        }
    }

    private boolean existCouponIssueTarget() {
        return redisRepository.lSize(issueRequestQueueKey) > 0;
    }

    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        return objectMapper.readValue(redisRepository.lIndex(issueRequestQueueKey, 0L), CouponIssueRequest.class);
    }

    private void removeIssueTarget() {
        redisRepository.lPop(issueRequestQueueKey);
    }
}
