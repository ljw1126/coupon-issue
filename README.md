# 선착순 쿠폰 발급 시스템

## Requirements
- 쿠폰은 유효 기간 내에 발급이 가능합니다.
- 선착순 쿠폰이므로 1인당 1개의 쿠폰 발급만 가능합니다.
- 선착순 쿠폰의 최대 발급 수량을 설정할 수 있어야 합니다.


## Tech Stack

**Infra** 
AWS EC2, RDS, ElasticCache, VPC, Subnet

**Server** 
Java 17, Spring Boot 3.3.3, JPA, QueryDsl

**Database**
MySQL 8.0, H2, Redis

**Monitoring**
AWS CloudWatch, Spring Actuator, Prometheus, Grafana

**Etc** 
JUnit5, Locust, Gradle 8.1, Docker, SonarQube, Git, Postman

## Architecture
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/flow.png?raw=true"/>

**쿠폰 발급 요청 시나리오(coupon-api)**
① 쿠폰 정책 조회(Coupon) 및 유효기간 확인 -- 로컬 캐시, Redis 캐시
② 쿠폰 중복 발급 요청 여부 확인 (SISMEMBER) -- RedisScript ② ~ ⑤ 처리
③ 수량 조회 (SCARD) 및 발급 가능 여부 검증
④ 요청 추가 (SADD)
⑤ 쿠폰 발급 대기열 추가 (RPUSH)

**쿠폰 발급 처리 시나리오(coupon-consumer)**
 ① 대기열 큐 데이터 확인 -- 스프링 스케쥴러 사용
 ② 대기열 첫번째 데이터 조회 (LIndex)
 ③ 쿠폰 정책 조회(Coupon), 발급 수량 +1 
 ④ 쿠폰 중복 발급 이력 조회, 발급 이력 저장 
 ⑤ CouponIssueCompleteEvent 발행, Redis 글로벌 캐시와 로컬 캐시 갱신
 ⑥ 대기열 첫번째 데이터 삭제 (LPop) 

## Web Sequence Diagram
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/websequencediagrams/coupon-issue%20.png?raw=true"/>
<br/>
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/websequencediagrams/coupon-consumer.png?raw=true"/>

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

**3. Locust 실행**
> 경로 : {프로젝트}/load-test/
```shell
$ ./run.sh
```
**참고.** 부하 테스트시 DB 테이블 데이터, Redis 캐시 초기화 주의
```shell
$ docker exec -uroot -it coupon-redis /bin/sh

$ redis-cli -h localhost -p 6380
redis-cli> flushall
redis-cli> keys *

# 쿠폰 정책 미리 캐싱하여 warmup
redis-cli> set coupon::1 "{\"@class\":\"com.example.couponcore.repository.redis.dto.CouponRedisEntity\",\"id\":1,\"couponType\":\"FIRST_COME_FIRST_SERVED\",\"totalQuantity\":500,\"availableIssueQuantity\":true,\"dateIssueStart\":[2024,9,1,0,0],\"dateIssueEnd\":[2024,9,30,11,59,59]}"
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

## Load Test Result

**AWS 스펙** 
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/aws/spec.png?raw=true"/>
<br/>

**Load Test** 
\- 수행 시간 : 30s 
\- RPS : **5107.21** (*목표치 달성)
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/aws/load-test-result1.png?raw=true"/>
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/aws/load-test-result2.png?raw=true"/>
<br/>

**AWS CloudWatch**
모니터링 결과 **redis와 mysql은 더 많은 트래픽을 받을 수 있는 상태** 확인
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/aws/cloudwatch.png?raw=true"/>
<br/>

**Grafana**
모니터링 결과 coupon-api 서버 **scale-out시 추가 트래픽 처리 가능성** 확인
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/aws/grafana.png?raw=true"/>


## Reference 
- JUnit5 : https://junit.org/junit5/docs/current/user-guide/
- Locust : https://docs.locust.io/en/stable/
- docker-compose : https://docs.docker.com/compose/
- web sequence diagrams : https://www.websequencediagrams.com/examples.html
