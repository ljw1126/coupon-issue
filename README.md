# 선착순 쿠폰 발급 시스템

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_coupon-issue&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ljw1126_coupon-issue)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_coupon-issue&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=ljw1126_coupon-issue)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_coupon-issue&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ljw1126_coupon-issue)

## 프로젝트 목표
선착순 쿠폰 발급 시스템을 개발하며 동시성 제어, 부하 테스트 및 모니터링, 그리고 성능 개선 경험을 쌓는다. 

- Docker Compose를 활용해 부하 테스트 및 모니터링 환경 구축
- 테스트 후 지표 기반으로 병목 구간 분석 및 성능 개선
- 분산락, 로컬 캐시, Kafka를 단계적으로 도입하며 동시성 문제 해결 및 시스템 확장
- 부가적으로 Testcontainers 및 다양한 오픈소스 도구 활용
<br/>

## 요구사항
1. 쿠폰은 유효 기간동안 발급이 가능하다.
2. 쿠폰은 1인당 1장만 지급한다.
3. 쿠폰의 최대 발급 수량을 초과해서는 안된다.
<br/>

## 개발 스택

>- Infra: AWS EC2, RDS, ElasticCache, VPC, Subnet <br/>
>- Server: JDK 17, Spring Boot 3.x, JPA, QueryDsl, Kafka <br/>
>- Database: MySQL 8.0, H2, Redis <br/>
>- Monitoring: AWS CloudWatch, Spring Actuator, Prometheus, Grafana <br/>
>- Testing: JUnit5, Mockito, Locust, Apache JMeter, k6, Testcontainers <br/>
>- Etc: Docker, Gradle 8.1, SonarCloud, Git, Postman, Spring REST Docs, Swagger-UI
<br/>

## 아키텍처
<img src="https://github.com/ljw1126/user-content/blob/master/coupon-issue/diagram.jpg?raw=true" alt="다이어그램" style="float: left"/>
<br/>


## 트러블 슈팅 및 성능 개선 과정
| 이슈                 | 해결 방법                           | 개선 효과                                                                       |
|--------------------|---------------------------------|-----------------------------------------------------------------------------|
| 동시성 이슈             | Redis 분산락 도입                    | 동시 요청 처리 시 데이터 정합성 유지                                                       |
| RPS 성능 한계          | Redis Lua Script 활용하여 원자적 연산 수행 | - 네트워크 비용 감소 및 동시성 제어 <br/> - RPS **5.4**배 향상 <br/> - 평균 응답 시간 **26.7**배 단축 |
| Redis 부하 증가        | 로컬 캐시(Caffeine) 도입                        | Redis 부하 감소 및 RPS **1.33**배 향상                                              |
| 트래픽 증가로 인한 Redis 부하 | Kafka 도입하여 비동기 이벤트 처리           | Redis 부하 감소 및 RPS **6.97**% 향상                                              |
