package com.example.couponapi.controller;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
import com.example.couponapi.controller.dto.CouponIssueResponseDto;
import com.example.couponapi.service.CouponIssueRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CouponIssueController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PostMapping("/v1/issue")
    public CouponIssueResponseDto issueV1(@RequestBody CouponIssueRequestDto requestDto) {
        synchronized (this) {
            couponIssueRequestService.issueRequestV1(requestDto);
        }
        return new CouponIssueResponseDto(true, null);
    }

    @PostMapping("/v2/issue")
    public CouponIssueResponseDto issueV2(@RequestBody CouponIssueRequestDto requestDto) {
        couponIssueRequestService.issueRequestV2(requestDto);
        return new CouponIssueResponseDto(true, null);
    }

    @PostMapping("/v3/issue")
    public CouponIssueResponseDto issueV3(@RequestBody CouponIssueRequestDto requestDto) {
        couponIssueRequestService.issueRequestV1(requestDto);
        return new CouponIssueResponseDto(true, null);
    }

    @PostMapping("/v1/asyncIssue")
    public CouponIssueResponseDto asyncIssue(@RequestBody CouponIssueRequestDto requestDto) {
        couponIssueRequestService.asyncIssueRequest(requestDto);
        return new CouponIssueResponseDto(true, null);
    }

    @PostMapping("/v2/asyncIssue")
    public CouponIssueResponseDto asyncIssueV2(@RequestBody CouponIssueRequestDto requestDto) {
        couponIssueRequestService.asyncIssueRequestV2(requestDto);
        return new CouponIssueResponseDto(true, null);
    }
}
