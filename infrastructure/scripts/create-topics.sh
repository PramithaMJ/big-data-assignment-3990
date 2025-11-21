#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Creating Kafka topics for order processing system...${NC}\n"

# Wait for Kafka to be ready
echo "Waiting for Kafka cluster to be ready..."
sleep 10

# Create orders topic (main topic)
echo -e "\n${YELLOW}Creating 'orders' topic...${NC}"
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --if-not-exists

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ 'orders' topic created successfully${NC}"
else
  echo -e "${RED}✗ Failed to create 'orders' topic${NC}"
fi

# Create retry topic
echo -e "\n${YELLOW}Creating 'orders-retry' topic...${NC}"
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders-retry \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=86400000 \
  --if-not-exists

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ 'orders-retry' topic created successfully${NC}"
else
  echo -e "${RED}✗ Failed to create 'orders-retry' topic${NC}"
fi

# Create DLQ topic
echo -e "\n${YELLOW}Creating 'orders-dlq' topic...${NC}"
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders-dlq \
  --partitions 1 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=2592000000 \
  --if-not-exists

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ 'orders-dlq' topic created successfully${NC}"
else
  echo -e "${RED}✗ Failed to create 'orders-dlq' topic${NC}"
fi

# List all topics
echo -e "\n${YELLOW}Listing all topics:${NC}"
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094

# Describe topics
echo -e "\n${YELLOW}Topic Details:${NC}"
docker exec kafka1 kafka-topics --describe --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --topic orders
docker exec kafka1 kafka-topics --describe --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --topic orders-retry
docker exec kafka1 kafka-topics --describe --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --topic orders-dlq

echo -e "\n${GREEN}✓ Topic creation complete!${NC}"
