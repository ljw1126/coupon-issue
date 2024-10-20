import http from 'k6/http';
import {sleep, check} from 'k6';
import {randomIntBetween} from 'https://jslib.k6.io/k6-utils/1.1.0/index.js';

const maxVus = 100;

/*
 maxVus * 5 / 2 + maxVus * 10 + maxVus * 5 / 2 = 15 * maxVus
 15 * maxVus 초 동안 실행
 250ms delay (4번)
 총 15 * maxVus * 4 = 60 * maxVus 번 실행
*/
export const options = {
	scenarios : {
		couponAsyncIssue : {
			executor: 'per-vu-iterations',
	        vus: 100,         // 가상 사용자 수 (동시 요청)
     	    iterations: 50,   // 각 가상 사용자가 50번씩 요청
	        maxDuration: '15s' // 테스트 시간 제한
		},
		/*couponAsyncIssue: {
			executor: 'ramping-vus',
			startVUs: 0,
			stages: [
				{duration: '5s', target: maxVus},
				{duration: '10s', target: maxVus},
				{duration: '5s', target: 0},
			],
		}*/
	}
};

// https://grafana.com/docs/k6/latest/javascript-api/k6-http/response/
export default function () {
	const payload  = {
        couponId : 1,
		userId : randomIntBetween(1, 100000)
    };

	const params = {
		headers : {
			'Content-Type' : 'application/json'
		},
		timeout : '2s'
	};

    const url = 'http://localhost:8080/v2/asyncIssue';
    const resp = http.post(url, JSON.stringify(payload), params);
	const isSuccess = JSON.parse(resp.body)["isSuccess"];

    check(resp, {
        'is status 200': (r) => r.status === 200,
        'should have isSuccess true': () => isSuccess === true,
    });

	if(!isSuccess) {
		console.error(`fail request : ${resp.body}`);
	}

    sleep(0.25);
}
