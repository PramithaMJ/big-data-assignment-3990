#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Seeding test data to Kafka...${NC}\n"

# Configuration
PRODUCER_JAR="../../producer-service/target/producer-service-1.0-SNAPSHOT.jar"
NUM_MESSAGES=${1:-50}

if [ ! -f "$PRODUCER_JAR" ]; then
  echo -e "${YELLOW}Producer JAR not found at $PRODUCER_JAR${NC}"
  echo -e "${YELLOW}Please build the producer service first:${NC}"
  echo -e "  cd producer-service"
  echo -e "  mvn clean package"
  exit 1
fi

echo -e "${GREEN}Starting producer to send $NUM_MESSAGES messages...${NC}"
java -jar "$PRODUCER_JAR" --message-count=$NUM_MESSAGES

echo -e "\n${GREEN}✓ Data seeding complete!${NC}"
echo -e "${YELLOW}Check Kafka UI at http://localhost:8082 to view messages${NC}"
