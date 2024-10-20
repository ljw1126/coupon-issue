#!/bin/bash
HOST_IP=$(hostname -I | awk '{print $1}')

TARGET_HOST="http://$HOST_IP:8080" docker-compose down

TARGET_HOST="http://$HOST_IP:8080" docker-compose up -d --scale worker=3
