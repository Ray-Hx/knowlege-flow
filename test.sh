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
    echo "查看日志: docker compose logs -f [service-name]"
    echo "停止服务: docker compose down"
else
    echo -e "${YELLOW}=========================================="
    echo -e "  ⚠️  部分服务可能还在启动中"
    echo -e "==========================================${NC}"
    echo ""
    echo "请等待几分钟后运行以下命令检查状态："
    echo "  docker compose ps"
    echo ""
    echo "查看详细日志："
    echo "  docker compose logs -f"
fi

echo ""
