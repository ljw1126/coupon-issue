#!/bin/bash
./gradlew clean build sonar \
  -Dsonar.projectKey=coupon-issue \
  -Dsonar.projectName='coupon-issue' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=sqp_b3284e53531aee466880b262fcb3ea2c4c98ffcc
