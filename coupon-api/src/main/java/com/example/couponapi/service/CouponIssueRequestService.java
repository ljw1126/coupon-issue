package com.example.couponapi.service;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponIssueRequestService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor distributeLockExecutor;

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        logger.info("쿠폰 발급 완료. couponId : %s , userId : %s".formatted(requestDto.couponId(), requestDto.userId()));
    }

    public void issueRequestV2(CouponIssueRequestDto requestDto) {
        distributeLockExecutor.execute(() -> couponIssueService.issue(requestDto.couponId(), requestDto.userId()),
                "lock_" + requestDto.couponId(), 10000, 10000);
        logger.info("쿠폰 발급 완료. couponId : %s , userId : %s".formatted(requestDto.couponId(), requestDto.userId()));
    }
}
