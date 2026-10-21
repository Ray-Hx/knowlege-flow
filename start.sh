#!/bin/bash

# ============================================
# KnowledgeFlow Docker 启动脚本
# ============================================

set -e

# 启动哪一组服务，默认全套。可选: core / rag / search / graph / storage / full
# 用法: ./start.sh          全套
#       ./start.sh rag      核心 + Qdrant
#       ./start.sh core     仅核心 (MySQL + Redis + RabbitMQ)
PROFILE="${1:-full}"

if [ "$PROFILE" = "core" ]; then
    COMPOSE_ARGS=""
else
    COMPOSE_ARGS="--profile $PROFILE"
fi

echo "=========================================="
echo "  KnowledgeFlow 基础设施启动脚本"
echo "=========================================="
echo "  启动分组: $PROFILE"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    printf "${RED}❌ Docker 未运行，请先启动 Docker Desktop${NC}\n"
    exit 1
fi

printf "${GREEN}✅ Docker 运行正常${NC}\n"
echo ""

# 检查 .env 文件
if [ ! -f .env ]; then
    printf "${YELLOW}⚠️  .env 文件不存在，从模板复制...${NC}\n"
    cp .env.example .env
    printf "${GREEN}✅ 已创建 .env 文件，请根据需要修改配置${NC}\n"
fi

# 停止并删除旧容器
printf "${YELLOW}🔄 停止旧容器...${NC}\n"
docker compose $COMPOSE_ARGS down 2>/dev/null || true

# 拉取最新镜像
echo ""
printf "${YELLOW}📥 拉取 Docker 镜像...${NC}\n"
# docker compose pull

# 启动服务
echo ""
printf "${YELLOW}🚀 启动服务...${NC}\n"
docker compose $COMPOSE_ARGS up -d

# 等待服务启动
echo ""
printf "${YELLOW}⏳ 等待服务启动（30秒）...${NC}\n"
sleep 30

# 检查服务健康状态
echo ""
printf "${YELLOW}🔍 检查服务状态...${NC}\n"
echo ""

SERVICES=("mysql" "redis" "elasticsearch" "neo4j" "qdrant" "minio" "rabbitmq")
ALL_HEALTHY=true

for service in "${SERVICES[@]}"; do
    container_name="knowledge-flow-${service}"
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")
    
    if [ "$status" = "healthy" ]; then
        printf "  ${GREEN}✅ $service: 健康${NC}\n"
    elif [ "$status" = "no-healthcheck" ]; then
        # 某些服务可能没有健康检查，检查运行状态
        if docker ps | grep -q "$container_name"; then
            printf "  ${GREEN}✅ $service: 运行中${NC}\n"
        else
            printf "  ${RED}❌ $service: 未运行${NC}\n"
            ALL_HEALTHY=false
        fi
    else
        printf "  ${YELLOW}⚠️  $service: $status${NC}\n"
        ALL_HEALTHY=false
    fi
done

echo ""

if [ "$ALL_HEALTHY" = true ]; then
    printf "${GREEN}==========================================\n"
    printf "  ✅ 所有服务启动成功！\n"
    printf "==========================================${NC}\n"
    echo ""
    echo "服务访问地址："
    echo "  MySQL:          localhost:3306"
    echo "  Redis:          localhost:6379"
    echo "  Elasticsearch:  http://localhost:9200"
    echo "  Neo4j Browser:  http://localhost:7474"
    echo "  Neo4j Bolt:     bolt://localhost:7687"
    echo "  Qdrant:         http://localhost:6333"
    echo "  MinIO API:      http://localhost:9000"
    echo "  MinIO Console:  http://localhost:9001"
    echo "  RabbitMQ:       http://localhost:15672"
    echo ""
    echo "查看日志: docker compose logs -f [service-name]"
    echo "停止服务: docker compose down"
else
    printf "${YELLOW}==========================================\n"
    printf "  ⚠️  部分服务可能还在启动中\n"
    printf "==========================================${NC}\n"
    echo ""
    echo "请等待几分钟后运行以下命令检查状态："
    echo "  docker compose ps"
    echo ""
    echo "查看详细日志："
    echo "  docker compose logs -f"
fi

echo ""
