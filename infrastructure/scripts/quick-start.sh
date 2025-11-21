#!/bin/bash

###############################################################################
# Quick Start Script for Kafka Order System
# Description: One-command setup and start
###############################################################################

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║   Kafka Order Processing System - Quick Start             ║
║   Spring Boot 3.3.5 | Kafka 3.7 | Confluent 7.6.0         ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${YELLOW}Step 1/4: Checking Docker...${NC}"
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED} Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi
echo -e "${GREEN} Docker is running${NC}"

echo ""
echo -e "${YELLOW}Step 2/4: Building services...${NC}"
cd "$SCRIPT_DIR/../docker"
docker compose build

echo ""
echo -e "${YELLOW}Step 3/4: Starting infrastructure (Kafka cluster)...${NC}"
docker compose up -d zookeeper kafka1 kafka2 kafka3 schema-registry kafka-ui

echo ""
echo -e "${YELLOW}Waiting for Kafka cluster to be ready (60 seconds)...${NC}"
sleep 60

echo ""
echo -e "${YELLOW}Step 4/4: Starting application services...${NC}"
docker compose up -d producer-service consumer-service

echo ""
echo -e "${YELLOW}Waiting for applications to start (30 seconds)...${NC}"
sleep 30

echo ""
echo -e "${GREEN} System started successfully!${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}🌐 Service URLs:${NC}"
echo ""
echo "  • Kafka UI:         http://localhost:8080"
echo "  • Schema Registry:  http://localhost:8081"
echo "  • Producer Service: http://localhost:8090"
echo "  • Consumer Service: http://localhost:8082"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Open Kafka UI: http://localhost:8080"
echo "  2. Create topics: ./infrastructure/scripts/manage.sh create-topics"
echo "  3. View logs: ./infrastructure/scripts/manage.sh logs"
echo ""
echo -e "${GREEN}To stop: ./infrastructure/scripts/manage.sh stop${NC}"
echo ""
