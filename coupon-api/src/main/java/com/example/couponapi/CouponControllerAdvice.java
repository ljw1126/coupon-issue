package com.example.couponapi;

import com.example.couponapi.controller.dto.CouponIssueResponseDto;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CouponControllerAdvice {

    @ExceptionHandler(CouponIssueException.class)
    public CouponIssueResponseDto couponIssueException(CouponIssueException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return new CouponIssueResponseDto(false, errorCode.message);
    }
}
