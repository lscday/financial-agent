#!/usr/bin/env bash
#
# ECS 部署脚本
# 用法: ./deploy.sh <镜像名> [镜像仓库地址]
# 示例: ./deploy.sh financial-agent:1.0.0
#
# 前置: 服务器已安装 Docker，安全组放行 8080

set -euo pipefail

IMAGE_NAME="${1:-financial-agent:latest}"
CONTAINER_NAME="financial-agent"
PORT="${PORT:-8080}"

# 需通过环境变量传入的密钥
: "${ALIYUN_ACCESS_KEY_ID:?需要设置 ALIYUN_ACCESS_KEY_ID}"
: "${ALIYUN_ACCESS_KEY_SECRET:?需要设置 ALIYUN_ACCESS_KEY_SECRET}"
: "${ALIYUN_ES_HOST:?需要设置 ALIYUN_ES_HOST}"
: "${ALIYUN_ES_USERNAME:?需要设置 ALIYUN_ES_USERNAME}"
: "${ALIYUN_ES_PASSWORD:?需要设置 ALIYUN_ES_PASSWORD}"
: "${DEEPSEEK_API_KEY:?需要设置 DEEPSEEK_API_KEY}"
: "${TONGYI_API_KEY:?需要设置 TONGYI_API_KEY}"

echo "=== 停止旧容器 ==="
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

echo "=== 启动新容器 ${IMAGE_NAME} ==="
docker run -d --name "${CONTAINER_NAME}" \
  -p "${PORT}:8080" \
  --restart=always \
  -e ALIYUN_ACCESS_KEY_ID="${ALIYUN_ACCESS_KEY_ID}" \
  -e ALIYUN_ACCESS_KEY_SECRET="${ALIYUN_ACCESS_KEY_SECRET}" \
  -e ALIYUN_ES_HOST="${ALIYUN_ES_HOST}" \
  -e ALIYUN_ES_USERNAME="${ALIYUN_ES_USERNAME}" \
  -e ALIYUN_ES_PASSWORD="${ALIYUN_ES_PASSWORD}" \
  -e DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY}" \
  -e TONGYI_API_KEY="${TONGYI_API_KEY}" \
  -e SPRING_PROFILES_ACTIVE=aliyun \
  "${IMAGE_NAME}"

echo "=== 等待健康检查 ==="
sleep 15
docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Status}}"

echo "=== 部署完成，访问 http://<ECS公网IP>:${PORT} ==="
