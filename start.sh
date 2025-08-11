#!/bin/bash
# Linux用户启动脚本

echo "🐧 Linux用户启动脚本"
echo "使用默认配置启动服务..."

# 设置环境变量
export DOCKER_BUILDKIT=1
export MAVEN_CACHE_PATH="/root/.m2"  # Linux默认使用容器内路径

# 检查.env文件是否存在
if [ ! -f .env ]; then
    echo "⚠️  .env文件不存在，从.env.example复制..."
    cp .env.example .env
    echo "✅ 请编辑.env文件填写你的配置"
fi

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动服务
echo "🚀 启动Docker Compose服务..."
docker-compose up -d --build

# 检查启动状态
if [ $? -eq 0 ]; then
    echo "✅ 服务启动成功！"
    echo ""
    echo "📝 查看服务状态:"
    docker-compose ps
    echo ""
    echo "📋 常用命令:"
    echo "  查看应用日志: docker-compose logs -f diary-server"
    echo "  查看数据库日志: docker-compose logs -f mysql"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart"
    echo ""
    echo "🌐 访问地址: http://localhost:7080"
else
    echo "❌ 服务启动失败，请检查错误信息"
    exit 1
fi