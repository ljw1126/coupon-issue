from locust import task, FastHttpUser
import random

class CouponIssueV1(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0

    @task
    def issue(self):
        payload = {
            "userId" : random.randint(1, 1000000),
            "couponId" : 1
        }
        with self.rest("POST", "/v1/asyncIssue", json = payload):
            pass
