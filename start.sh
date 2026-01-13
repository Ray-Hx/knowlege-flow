#!/bin/bash

# ============================================
# KnowledgeFlow Docker 启动脚本
# ============================================

set -e

echo "=========================================="
echo "  KnowledgeFlow 基础设施启动脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行，请先启动 Docker Desktop${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker 运行正常${NC}"
echo ""

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env 文件不存在，从模板复制...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✅ 已创建 .env 文件，请根据需要修改配置${NC}"
fi

# 停止并删除旧容器
echo -e "${YELLOW}🔄 停止旧容器...${NC}"
docker-compose down 2>/dev/null || true

# 拉取最新镜像
echo ""
echo -e "${YELLOW}📥 拉取 Docker 镜像...${NC}"
docker-compose pull

# 启动服务
echo ""
echo -e "${YELLOW}🚀 启动服务...${NC}"
docker-compose up -d

# 等待服务启动
echo ""
echo -e "${YELLOW}⏳ 等待服务启动（30秒）...${NC}"
sleep 30

# 检查服务健康状态
echo ""
echo -e "${YELLOW}🔍 检查服务状态...${NC}"
echo ""

SERVICES=("mysql" "redis" "elasticsearch" "neo4j" "qdrant" "minio" "rabbitmq")
ALL_HEALTHY=true

for service in "${SERVICES[@]}"; do
    container_name="knowledge-flow-${service}"
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")
    
    if [ "$status" = "healthy" ]; then
        echo -e "  ${GREEN}✅ $service: 健康${NC}"
    elif [ "$status" = "no-healthcheck" ]; then
        # 某些服务可能没有健康检查，检查运行状态
        if docker ps | grep -q "$container_name"; then
            echo -e "  ${GREEN}✅ $service: 运行中${NC}"
        else
            echo -e "  ${RED}❌ $service: 未运行${NC}"
            ALL_HEALTHY=false
        fi
    else
        echo -e "  ${YELLOW}⚠️  $service: $status${NC}"
        ALL_HEALTHY=false
    fi
done

echo ""

if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${GREEN}=========================================="
    echo -e "  ✅ 所有服务启动成功！"
    echo -e "==========================================${NC}"
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
    echo "查看日志: docker-compose logs -f [service-name]"
    echo "停止服务: docker-compose down"
else
    echo -e "${YELLOW}=========================================="
    echo -e "  ⚠️  部分服务可能还在启动中"
    echo -e "==========================================${NC}"
    echo ""
    echo "请等待几分钟后运行以下命令检查状态："
    echo "  docker-compose ps"
    echo ""
    echo "查看详细日志："
    echo "  docker-compose logs -f"
fi

echo ""
