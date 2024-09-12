package com.example.couponapi.service;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
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

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        logger.info("쿠폰 발급 완료. couponId : %s , userId : %s".formatted(requestDto.couponId(), requestDto.userId()));
    }
}
