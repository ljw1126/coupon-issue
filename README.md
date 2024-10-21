# 선착순 쿠폰 발급 시스템

## Requirements
\- 쿠폰은 유효 기간 내에 발급이 가능합니다. <br/>
\- 선착순 쿠폰이므로 1인당 1개의 쿠폰 발급만 가능합니다. <br/>
\- 선착순 쿠폰의 최대 발급 수량을 설정할 수 있어야 합니다. <br/>


## Tech Stack

**Infra** 
<br/>
AWS EC2, RDS, ElasticCache, VPC, Subnet

**Server** 
<br/>
Java 17, Spring Boot 3.3.3, JPA, QueryDsl, Kafka

**Database**
<br/>
MySQL 8.0, H2, Redis

**Monitoring**
<br/>
AWS CloudWatch, Spring Actuator, Prometheus, Grafana

**Testing**
<br/>
JUnit5, Mockito, Locust, Apache Jmeter, k6, Testcontainers

**Etc** 
<br/>
Docker, Gradle 8.1, SonarQube, Git, Postman

## Architecture
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/flow-with-kafka.png?raw=true"/>
<br/>

**쿠폰 발급 요청 시나리오(coupon-api)**
<br/>
① 쿠폰 정책 조회(Coupon) 및 유효기간 확인 -- 로컬 캐시, Redis 캐시<br/>
② 쿠폰 중복 발급 요청 여부 확인 (SISMEMBER) <br/>
③ 수량 조회 (SCARD) 및 발급 가능 여부 검증<br/>
④ 요청 추가 (SADD)<br/>
⑤ 쿠폰 발급 메시지 전송 -- Kafka <br/>

**쿠폰 발급 처리 시나리오(coupon-consumer)** 
</br>
① 쿠폰 발급 메시지 소비 (@KafkaListener) <br/>
② 쿠폰 발급 이력 저장 (+중복 발급 확인) <br/>
③ 쿠폰 정책 조회(Coupon), 발급 수량 +1 <br/>
④ CouponIssueCompleteEvent 발행 (Redis와 로컬 캐시 갱신)<br/>

## 실행 방법
**버전 정보** 
docker : v27.2.1, docker-compose : v2.29.2, python3 : v3.12.3

**1. MySQL, Redis**
```shell
# 프로젝트 루트 경로 이동
$ docker-compose up -d
```

**2. Prometheus, Grafana**
```shell
$ cd monitoring
$ docker-compose up -d
```

**참고.** 모니터링 수집 대상 정보는 아래 설정 파일 기재하고, 설정 변경시 컨테이너 재시작
> 경로 : {프로젝트}/monitoring/promtheus/config/prometheus.yml
```text
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: "coupon-api"
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: [ '<ipv4주소>:8080' ]
  - job_name: "coupon-consumer"
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: [ '<ipv4주소>:8081' ]
```

**3. 부하테스트 실행**
> 경로 : {프로젝트}/load-test/
```shell
# locust
$ ./locust/run.sh

# k6
$ ./k6/run.sh

# apache jmeter, /jmeter 폴더에 *.jmx 열기
$ jmeter
```

**4. 프로젝트 빌드 & 실행**
```shell
# 프로젝트 루트 경로에서 
$ ./gradlew build -x test
```

coupon-api와 coupon-consumer 각 모듈의 /build/libs 이동 후 아래 명령어 실행
```shell
$ java -jar {name}.jar --spring.profiles.active={local|prod}

# 백그라운드 실행, 로그 기록
$ nohup java -jar {name}.jar --spring.profiles.active={local|prod} > app.log 2>&1 &
$ tail -f app.log
```
<br/>

**참고. 부하 테스트하기 전 주의사항**
<br/>
① coupons, coupon_issues 테이블 데이터 초기화 <br/>
② redis 캐시 초기화 (ex. Set, coupon::1 키 값) <br/>
③ coupon-api 서버 로컬 캐시 초기화 <br/>
④ 로컬 컴퓨터 한 대로 전체 실행할 경우 잡음 있을 수 있으니, 노트북이나 다른 컴퓨터에서 부하 테스트 실행 권장
<br/>

```shell
# redis
$ docker exec -uroot -it coupon-redis /bin/sh

$ redis-cli -h localhost -p 6380
redis-cli> flushall
redis-cli> keys *

# 쿠폰 정책 미리 캐싱(warm up)
redis-cli> set coupon::1 "{\"@class\":\"com.example.couponcore.repository.redis.dto.CouponRedisEntity\",\"id\":1,\"couponType\":\"FIRST_COME_FIRST_SERVED\",\"totalQuantity\":500,\"availableIssueQuantity\":true,\"dateIssueStart\":[2024,9,1,0,0],\"dateIssueEnd\":[2024,9,30,11,59,59]}"
```
<br/>

```shell
# MySQL
$ docker exec -uroot -it coupon-mysql /bin/bash
$ mysql -uroot -p1234
$ use coupon;
```

```sql
CREATE TABLE `coupon`.`coupons`
(
    `id`                   BIGINT(20) NOT NULL AUTO_INCREMENT,
    `title`                VARCHAR(255) NOT NULL COMMENT '쿠폰명',
    `coupon_type`          VARCHAR(255) NOT NULL COMMENT '쿠폰 타입 (선착순 쿠폰, ..)',
    `total_quantity`       INT NULL COMMENT '쿠폰 발급 최대 수량',
    `issued_quantity`      INT          NOT NULL COMMENT '발급된 쿠폰 수량',
    `discount_amount`      INT          NOT NULL COMMENT '할인 금액',
    `min_available_amount` INT          NOT NULL COMMENT '최소 사용 금액',
    `date_issue_start`     datetime(6) NOT NULL COMMENT '발급 시작 일시',
    `date_issue_end`       datetime(6) NOT NULL COMMENT '발급 종료 일시',
    `date_created`         datetime(6) NOT NULL COMMENT '생성 일시',
    `date_updated`         datetime(6) NOT NULL COMMENT '수정 일시',
PRIMARY KEY (`id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COMMENT '쿠폰 정책';

CREATE TABLE `coupon`.`coupon_issues`
(
    `id`           BIGINT(20) NOT NULL AUTO_INCREMENT,
    `coupon_id`    BIGINT(20) NOT NULL COMMENT '쿠폰 ID',
    `user_id`      BIGINT(20) NOT NULL COMMENT '유저 ID',
    `date_issued`  datetime(6) NOT NULL COMMENT '발급 일시',
    `date_used`    datetime(6) NULL COMMENT '사용 일시',
    `date_created` datetime(6) NOT NULL COMMENT '생성 일시',
    `date_updated` datetime(6) NOT NULL COMMENT '수정 일시',
PRIMARY KEY (`id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COMMENT '쿠폰 발급 내역';

INSERT INTO `coupon`.`coupons`
(
    `id`,
    `title`,
    `coupon_type`,
    `total_quantity`,
    `issued_quantity`,
    `discount_amount`,
    `min_available_amount`,
    `date_issue_start`,
    `date_issue_end`,
    `date_created`,
    `date_updated`
) VALUES (
    1,
    '선착순 쿠폰',
    'FIRST_COME_FIRST_SERVED',
    500,
    0,
    10000,
    50000,
    '2024-10-01 00:00:00.000000',
    '2024-10-31 11:59:59.000000',
    '2024-08-20 04:47:50.000000',
    '2024-08-20 04:47:50.000000'
);
```


## Load Test Result

**참고. 노션**
<br/>
\- kafka 도입 전 내용으로 AWS 인프라 아키텍처 구성과 문제 해결 과정, 그리고 부하 테스트 및 모니터링 결과를 정리 <br/>
\- **결과**: Redis와 로컬 캐시 사용하여 **RPS 5.4배, 평균 응답 시간 26.7배 성능 개선**<br/>
\- **링크**:  <a href="https://www.notion.so/105791faa0988018921dff51b06f6117?pvs=4" title="동시성과 트래픽을 고려한 선착순 쿠폰 발급 시스템">동시성과 트래픽을 고려한 선착순 쿠폰 발급 시스템</a>
<br/>


**Locust 결과**
<br/>
① Kafka 도입 후 (12862.79 RPS) - 이전 대비 **RPS 6.97% 성능 향상**
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/load-test/locust1.png?raw=true"/>

② Kafka 도입 전 (12024.69 RPS)
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/load-test/before-kafka.png?raw=true"/>
<br/>

**Apache Jmeter 결과**
<br/>
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/load-test/apache-jmeter.png?raw=true"/>


**k6 결과**
<br/>
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/load-test/k6.png?raw=true"/>
<br/>


쿠폰 수량 5000개 설정 후 부하 테스트시 정상 처리된 결과(issued_quantity) 확인 
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/load-test/database.png?raw=true"/>
<br/>

## Reference
- JUnit5 : https://junit.org/junit5/docs/current/user-guide/
- Locust : https://docs.locust.io/en/stable/
- Apahce Jmeter : https://jmeter.apache.org/usermanual/index.html
- Grafana k6 : https://grafana.com/docs/k6/latest/
- docker-compose : https://docs.docker.com/compose/
- Spring cloud stream : https://spring.io/projects/spring-cloud-stream
- Testcontainers : https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- web sequence diagrams : https://www.websequencediagrams.com/examples.html
