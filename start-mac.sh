#!/bin/bash
# Mac用户专用启动脚本

echo "🍎 Mac用户启动脚本"
echo "启用BuildKit并使用Mac优化的Dockerfile..."

# 设置环境变量
export DOCKER_BUILDKIT=1
export MAVEN_CACHE_PATH="$HOME/.m2"  # 使用Mac用户的Maven缓存目录

# 检查.env文件是否存在
if [ ! -f .env ]; then
    echo "⚠️  .env文件不存在，从.env.example复制..."
    cp .env.example .env
    echo "✅ 请编辑.env文件填写你的配置"
fi

# 启动服务
echo "🚀 启动Docker Compose服务..."
docker compose up -d --build

echo "📝 查看服务状态:"
docker compose ps

echo "📋 查看应用日志:"
echo "docker compose logs -f diary-server"