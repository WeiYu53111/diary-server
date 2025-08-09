#!/bin/bash

# 一次性数据迁移脚本
# 使用方法: ./migrate-once.sh [试运行]

set -e

echo "=== 一次性数据迁移工具 ==="

# 检查参数
DRY_RUN="false"
if [ "$1" = "试运行" ] || [ "$1" = "dry-run" ] || [ "$1" = "--dry-run" ]; then
    DRY_RUN="true"
    echo "模式: 试运行（不会实际插入数据库）"
else
    echo "模式: 正式迁移"
    echo "如需试运行，请使用: $0 dry-run"
fi

echo

# 读取配置（可以通过环境变量覆盖）
DB_URL=${DB_URL:-"jdbc:mysql://localhost:3306/diary_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true"}
DB_USERNAME=${DB_USERNAME:-"root"}
DB_PASSWORD=${DB_PASSWORD:-"123456"}
JSON_STORAGE_PATH=${JSON_STORAGE_PATH:-"diary-server/diary/"}

echo "配置信息:"
echo "  数据库URL: $DB_URL"
echo "  用户名: $DB_USERNAME"
echo "  JSON路径: $JSON_STORAGE_PATH"
echo

# 确认执行（正式迁移时）
if [ "$DRY_RUN" = "false" ]; then
    echo "警告: 即将开始正式数据迁移！"
    echo "这将会向数据库插入数据，请确保已经备份了重要数据。"
    read -p "确认继续？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "迁移已取消"
        exit 0
    fi
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境"
    exit 1
fi

# 编译项目
echo "编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 执行迁移
echo "开始执行一次性迁移..."
echo "时间: $(date)"
echo

mvn exec:java -Dexec.mainClass="wy.diary.server.migration.SimpleMigrationTool" \
    -DDB_URL="$DB_URL" \
    -DDB_USERNAME="$DB_USERNAME" \
    -DDB_PASSWORD="$DB_PASSWORD" \
    -DJSON_STORAGE_PATH="$JSON_STORAGE_PATH" \
    -DDRY_RUN="$DRY_RUN" \
    -q

if [ $? -eq 0 ]; then
    echo
    echo "迁移执行完成！"
    if [ "$DRY_RUN" = "true" ]; then
        echo "这是试运行，如需正式迁移请运行: $0"
    fi
else
    echo "迁移执行失败！"
    exit 1
fi 