package com.example.couponapi.controller;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
import com.example.couponapi.service.CouponIssueRequestService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("restDocs")
@WebMvcTest(value = CouponIssueController.class)
@AutoConfigureRestDocs
class CouponIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponIssueRequestService couponIssueRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @MethodSource("couponIssueProvider")
    void couponIssueTest(String endPoint, String identifier) throws Exception {
        doNothing().when(couponIssueRequestService).issueRequestV1(any(CouponIssueRequestDto.class));

        CouponIssueRequestDto couponIssueRequestDto = new CouponIssueRequestDto(1L, 1L);

        mockMvc.perform(post(endPoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponIssueRequestDto))
                ).andExpect(status().isOk())
                .andDo(document(identifier,
                        requestFields(
                                fieldWithPath("userId").description("유저 아이디").type(JsonFieldType.NUMBER),
                                fieldWithPath("couponId").description("쿠폰 아이디").type(JsonFieldType.NUMBER)
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부").type(JsonFieldType.BOOLEAN)
                        )));
    }

    static Stream<Arguments> couponIssueProvider() {
        return Stream.of(
                Arguments.arguments("/v1/issue", "issueV1"),
                Arguments.arguments("/v2/issue", "issueV2"),
                Arguments.arguments("/v3/issue", "issueV3"),
                Arguments.arguments("/v2/asyncIssue", "asyncIssueV2")
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        }
    }

}
