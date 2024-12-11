package com.example.couponapi.controller;

import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Stream;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    void couponIssueTest(String endPoint, String identifier, String summary, String tag) throws Exception {
        doNothing().when(couponIssueRequestService).issueRequestV1(any(CouponIssueRequestDto.class));

        CouponIssueRequestDto couponIssueRequestDto = new CouponIssueRequestDto(1L, 1L);

        mockMvc.perform(RestDocumentationRequestBuilders.post(endPoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponIssueRequestDto))
                ).andExpect(status().isOk())
                .andDo(document(identifier,
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        ResourceDocumentation.resource(
                                ResourceSnippetParameters.builder()
                                        .summary(summary)
                                        .tag(tag)
                                        .description("쿠폰 신청")
                                        .requestFields(
                                                PayloadDocumentation.fieldWithPath("userId").description("유저 아이디").type(JsonFieldType.NUMBER),
                                                PayloadDocumentation.fieldWithPath("couponId").description("쿠폰 아이디").type(JsonFieldType.NUMBER)
                                        )
                                        .responseFields(
                                                PayloadDocumentation.fieldWithPath("isSuccess").description("성공 여부").type(JsonFieldType.BOOLEAN)
                                        )
                                        .build()
                        )
                ));
    }

    static Stream<Arguments> couponIssueProvider() {
        return Stream.of(
                Arguments.arguments("/v1/issue", "issueV1", "synchronized 키워드 사용", "동기 방식"),
                Arguments.arguments("/v2/issue", "issueV2", "Redisson 분산락 사용", "동기 방식"),
                Arguments.arguments("/v3/issue", "issueV3", "MySQL 비관적 락 사용", "동기 방식"),
                Arguments.arguments("/v2/asyncIssue", "asyncIssueV2", "캐시(Redis + Caffeine)와 kafka 사용", "비동기 방식")
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
