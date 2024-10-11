package com.example.couponcore.service;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponType;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;

import static com.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.example.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@Testcontainers
class AsyncCouponIssueServiceTest extends TestConfig {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3")
            .withDatabaseName("coupon")
            .withUsername("tester")
            .withPassword("1234")
            .withUrlParam("serverTimezone", "UTC")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true");

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    private static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.jpa.show-sql", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    AsyncCouponIssueService asyncCouponIssueService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Collection<String> redisKey = redisTemplate.keys("*");
        if (redisKey != null && !redisKey.isEmpty()) {
            redisTemplate.delete(redisKey);
        }
    }

    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 반환한다")
    @Test
    void issue() {
        long couponId = 1L;
        long userId = 1L;

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueService.issue(couponId, userId);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_EXIST);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 기한이 아니라면 예외를 반환한다")
    @Test
    void issue2() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueService.issue(coupon.getId(), 1L);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 가능 수량이 존재하지 않는다면 예외를 반환한다")
    @Test
    void issue3() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        IntStream.range(0, coupon.getTotalQuantity())
                .forEach(userId -> redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId)));

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueService.issue(coupon.getId(), 999L);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @DisplayName("쿠폰 발급 - 이미 쿠폰 발급된 유저라면 예외를 반환한다")
    @Test
    void issue4() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueService.issue(coupon.getId(), userId);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청을 기록한다")
    @Test
    void issue5() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        asyncCouponIssueService.issue(coupon.getId(), userId);

        Boolean isMember = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        assertThat(isMember).isTrue();
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청이 성공하면 쿠폰 발급 queue에 적재된다.")
    @Test
    void issue6() throws JsonProcessingException {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        asyncCouponIssueService.issue(coupon.getId(), userId);

        Boolean isMember = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        assertThat(isMember).isTrue();

        String savedIssueRequest = redisTemplate.opsForList().leftPop(getIssueRequestQueueKey());
        CouponIssueRequest couponIssueRequest = new CouponIssueRequest(coupon.getId(), userId);
        assertThat(objectMapper.writeValueAsString(couponIssueRequest)).isEqualTo(savedIssueRequest);
    }
}
