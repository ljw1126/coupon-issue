package com.example.couponapi.service;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.service.AsyncCouponIssueService;
import com.example.couponcore.service.AsyncCouponIssueServiceV2;
import com.example.couponcore.service.CouponIssueProcessor;
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
    private final AsyncCouponIssueService asyncCouponIssueService;
    private final AsyncCouponIssueServiceV2 asyncCouponIssueServiceV2;
    private final CouponIssueProcessor couponIssueProcessor;

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueProcessor.issue(requestDto.couponId(), requestDto.userId());
        printLog(requestDto);
    }

    public void issueRequestV2(CouponIssueRequestDto requestDto) {
        distributeLockExecutor.execute(() -> couponIssueProcessor.issue(requestDto.couponId(), requestDto.userId()),
                "lock_" + requestDto.couponId(), 10000, 10000);
        printLog(requestDto);
    }

    public void issueRequestV3(CouponIssueRequestDto requestDto) {
        couponIssueProcessor.issueWithLock(requestDto.couponId(), requestDto.userId());
        printLog(requestDto);
    }

    private void printLog(CouponIssueRequestDto requestDto) {
        if (logger.isInfoEnabled()) {
            logger.info("쿠폰 발급 완료. couponId : %s , userId : %s".formatted(requestDto.couponId(), requestDto.userId()));
        }
    }

    public void asyncIssueRequest(CouponIssueRequestDto requestDto) {
        asyncCouponIssueService.issue(requestDto.couponId(), requestDto.userId());
    }

    public void asyncIssueRequestV2(CouponIssueRequestDto requestDto) {
        asyncCouponIssueServiceV2.issue(requestDto.couponId(), requestDto.userId());
    }
}
