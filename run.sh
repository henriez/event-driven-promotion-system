#!/bin/bash

set -a
source .env
set +a

mkdir -p logs

echo "Starting microservices in background..."

echo "Starting MS Gateway..."
(cd ms-gateway && ./mvnw clean -e spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5000" > ../logs/gateway.log 2>&1) &

echo "Starting MS Promotion..."
(cd ms-promotion && ./mvnw clean -e spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5001" > ../logs/promotion.log 2>&1) &

echo "Starting MS Ranking..."
(cd ms-ranking && ./mvnw clean -e spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5002" > ../logs/ranking.log 2>&1) &

echo "Starting MS Notification..."
(cd ms-notification && ./mvnw clean -e spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5003" > ../logs/notification.log 2>&1) &

echo "All services started. Logs are being written to the logs/ directory."
echo "Press [CTRL+C] to stop all background processes."

trap 'kill $(jobs -p)' EXIT
wait
