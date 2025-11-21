# Step-by-Step System Startup & Demo Guide

## 📋 Table of Contents

1. [Prerequisites Check](#prerequisites-check)
2. [System Startup (Step by Step)](#system-startup-step-by-step)
3. [Topic Creation (Manual)](#topic-creation-manual)
4. [Demo Part 1: Send Single Order](#demo-part-1-send-single-order)
5. [Demo Part 2: Send Multiple Orders](#demo-part-2-send-multiple-orders)
6. [Demo Part 3: Monitor with Kafka UI](#demo-part-3-monitor-with-kafka-ui)
7. [Demo Part 4: Check Statistics](#demo-part-4-check-statistics)
8. [Demo Part 5: View Logs](#demo-part-5-view-logs)
9. [Demo Part 6: Topic Details](#demo-part-6-topic-details)
10. [Demo Part 7: Consumer Groups](#demo-part-7-consumer-groups)
11. [System Shutdown](#system-shutdown)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites Check

### Step 1: Verify Docker is Running

```bash
# Check if Docker is running
docker --version

# Expected output:
# Docker version 24.x.x, build xxxxxxx
```

```bash
# Check if Docker Compose is available
docker compose version

# Expected output:
# Docker Compose version v2.x.x
```

### Step 2: Navigate to Project Directory

### Step 3: Ensure Clean State

```bash
# Stop any running containers
cd infrastructure/docker
docker compose down -v

# This removes:
# - All containers
# - All volumes (fresh start)
# - Network
```

## System Startup (Step by Step)

### Step 1: Start ZooKeeper

```bash
cd infrastructure/docker
docker compose up -d zookeeper
```

**Expected Output:**

```
[+] Running 2/2
 ✔ Network docker_kafka-net  Created
 ✔ Container zookeeper       Started
```

**Wait 5-10 seconds for ZooKeeper to be ready**

```bash
# Verify ZooKeeper is healthy
docker ps --filter name=zookeeper

# Expected Status: Up X seconds (healthy)
```

### Step 2: Start Kafka Brokers

```bash
# Start all 3 Kafka brokers
docker compose up -d kafka1 kafka2 kafka3
```

**Expected Output:**

```
[+] Running 3/3
 ✔ Container kafka1  Started
 ✔ Container kafka2  Started
 ✔ Container kafka3  Started
```

**Wait 20-30 seconds for Kafka cluster to form**

```bash
# Verify all Kafka brokers are healthy
docker ps --filter name=kafka

# Expected: All 3 brokers showing (healthy)
```

### Step 3: Start Schema Registry

```bash
docker compose up -d schema-registry
```

**Expected Output:**

```
[+] Running 1/1
 ✔ Container schema-registry  Started
```

**Wait 10-15 seconds for Schema Registry to be ready**

```bash
# Verify Schema Registry is healthy
docker ps --filter name=schema-registry

# Expected Status: Up X seconds (healthy)
```

### Step 4: Start Producer Service

```bash
docker compose up -d producer-service
```

**Expected Output:**

```
[+] Running 1/1
 ✔ Container producer-service  Started
```

**Wait 10-15 seconds for service to start**

```bash
# Check if producer is healthy
docker ps --filter name=producer-service

# Expected Status: Up X seconds (healthy)
```

### Step 5: Start Consumer Service

```bash
docker compose up -d consumer-service
```

**Expected Output:**

```
[+] Running 1/1
 ✔ Container consumer-service  Started
```

**Wait 10-15 seconds for service to start**

```bash
# Check if consumer is healthy
docker ps --filter name=consumer-service

# Expected Status: Up X seconds (healthy)
```

### Step 6: Start Kafka UI

```bash
docker compose up -d kafka-ui
```

**Expected Output:**

```
[+] Running 1/1
 ✔ Container kafka-ui  Started
```

### Step 7: Verify All Containers

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**Expected Output:**

```
NAMES                STATUS                   PORTS
consumer-service     Up X minutes (healthy)   0.0.0.0:8082->8082/tcp
producer-service     Up X minutes (healthy)   0.0.0.0:8090->8090/tcp
kafka-ui             Up X minutes (healthy)   0.0.0.0:8080->8080/tcp
schema-registry      Up X minutes (healthy)   0.0.0.0:8081->8081/tcp
kafka1               Up X minutes (healthy)   0.0.0.0:9092->9092/tcp
kafka2               Up X minutes (healthy)   0.0.0.0:9093->9093/tcp
kafka3               Up X minutes (healthy)   0.0.0.0:9094->9094/tcp
zookeeper            Up X minutes (healthy)   0.0.0.0:2181->2181/tcp
```

## Topic Creation (Manual)

### Step 1: Create 'orders' Topic

```bash
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --if-not-exists
```

**Expected Output:**

```
Created topic orders.
```

### Step 2: Create 'orders-retry' Topic

```bash
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders-retry \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=86400000 \
  --if-not-exists
```

**Expected Output:**

```
Created topic orders-retry.
```

### Step 3: Create 'orders-dlq' Topic

```bash
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 \
  --topic orders-dlq \
  --partitions 1 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=2592000000 \
  --if-not-exists
```

**Expected Output:**

```
Created topic orders-dlq.
```

### Step 4: List All Topics

```bash
docker exec kafka1 kafka-topics --list \
  --bootstrap-server kafka1:19092
```

**Expected Output:**

```
__consumer_offsets
_schemas
orders
orders-dlq
orders-retry
```

---

## Demo Part 1: Send Single Order

### Step 1: Check Producer Service Health

```bash
curl http://localhost:8090/actuator/health
```

**Expected Output:**

```json
{"status":"UP"}
```

### Step 2: Send Your First Order

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO001&product=Laptop&price=999.99"
```

**Expected Output:**

```json
{
  "orderId": "2658",
  "success": true,
  "message": "Order sent to Kafka successfully"
}
```

### Step 3: Verify Order was Processed

```bash
# Check consumer logs
docker logs consumer-service --tail 10
```

**Expected Output (look for):**

```
 Received order: orderId=DEMO001, product=Laptop, price=999.99
 Running average updated: $999.99
```

### Step 4: Check Statistics

```bash
curl -s http://localhost:8082/api/consumer/stats | python3 -m json.tool
```

**Expected Output:**

```json
{
    "ordersProcessed": 1,
    "totalAmount": 999.99,
    "runningAverage": 999.99,
    "detailedStats": "Processed: 1 | Errors: 0 | Success Rate: 100.00% | Total Amount: $999.99 | Running Average: $999.99"
}
```

---

## Demo Part 2: Send Multiple Orders

### Step 1: Send Order 2

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO002&product=Mouse&price=29.99"
```

### Step 2: Send Order 3

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO003&product=Keyboard&price=79.99"
```

### Step 3: Send Order 4

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO004&product=Monitor&price=299.99"
```

### Step 4: Send Order 5

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO005&product=Headphones&price=149.99"
```

### Step 5: Send Order 6

```bash
curl -X POST "http://localhost:8090/api/orders?orderId=DEMO006&product=Webcam&price=89.99"
```

### Step 6: Check Updated Statistics

```bash
curl -s http://localhost:8082/api/consumer/stats | python3 -m json.tool
```

**Expected Output:**

```json
{
    "ordersProcessed": 6,
    "totalAmount": 1649.94,
    "runningAverage": 274.99,
    "detailedStats": "Processed: 6 | Errors: 0 | Success Rate: 100.00% | Total Amount: $1649.94 | Running Average: $274.99"
}
```

**All 6 orders processed! Running average calculated!**

---

## Demo Part 3: Monitor with Kafka UI

### Step 1: Open Kafka UI in Browser

```
URL: http://localhost:8080
```

### Step 2: View Brokers

1. Click on **"Brokers"** tab in left menu
2. You should see **3 brokers**:
   - kafka1 (ID: 1)
   - kafka2 (ID: 2)
   - kafka3 (ID: 3)
3. All should show status: **"Online"**

### Step 3: View Topics

1. Click on **"Topics"** tab
2. You should see:
   - **orders** (3 partitions, RF: 3)
   - **orders-retry** (3 partitions, RF: 3)
   - **orders-dlq** (1 partition, RF: 3)
   - **__consumer_offsets** (system topic)
   - **_schemas** (schema registry topic)

### Step 4: View Messages in 'orders' Topic

1. Click on **"orders"** topic
2. Click on **"Messages"** tab
3. You should see your 6 orders distributed across 3 partitions
4. Click on any message to see details in Avro format

### Step 5: View Consumer Groups

1. Click on **"Consumers"** tab
2. You should see:
   - **order-consumer-group** (3 consumers)
   - **order-consumer-group-retry**
   - **order-consumer-group-dlq**
3. Consumer lag should be **0** (real-time processing)

---

## Demo Part 4: Check Statistics

### Endpoint 1: Consumer Health

```bash
curl http://localhost:8082/actuator/health
```

**Expected Output:**

```json
{"status":"UP"}
```

### Endpoint 2: Consumer Statistics

```bash
curl -s http://localhost:8082/api/consumer/stats | python3 -m json.tool
```

**Expected Output:**

```json
{
    "ordersProcessed": 6,
    "totalAmount": 1649.94,
    "runningAverage": 274.99,
    "detailedStats": "Processed: 6 | Errors: 0 | Success Rate: 100.00% | Total Amount: $1649.94 | Running Average: $274.99"
}
```

### Endpoint 3: Schema Registry Subjects

```bash
curl -s http://localhost:8081/subjects | python3 -m json.tool
```

**Expected Output:**

```json
[
    "orders-value"
]
```

### Endpoint 4: Get Schema Details

```bash
curl -s http://localhost:8081/subjects/orders-value/versions/1 | python3 -m json.tool
```

**Expected Output:**

```json
{
    "subject": "orders-value",
    "version": 1,
    "id": 1,
    "schema": "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.pramithamj.kafka.model\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"product\",\"type\":\"string\"},{\"name\":\"price\",\"type\":\"double\"},{\"name\":\"timestamp\",\"type\":\"long\"}]}"
}
```

---

## Demo Part 5: View Logs

### View Producer Service Logs

```bash
docker logs producer-service --tail 20
```

**Look for:**

```
Started ProducerServiceApplication
Tomcat started on port 8090
```

### View Consumer Service Logs

```bash
docker logs consumer-service --tail 30
```

**Look for:**

```
Started ConsumerServiceApplication
 Received order: orderId=DEMO001, product=Laptop, price=999.99
 Received order: orderId=DEMO002, product=Mouse, price=29.99
```

### View Kafka Broker Logs

```bash
docker logs kafka1 --tail 20
```

**Look for:**

```
[KafkaServer id=1] started
```

### View Consumer Logs with Grep (Only Order Messages)

```bash
docker logs consumer-service 2>&1 | grep "Received order"
```

**Expected Output:**

```
 Received order: orderId=DEMO001, product=Laptop, price=999.99
 Received order: orderId=DEMO002, product=Mouse, price=29.99
 Received order: orderId=DEMO003, product=Keyboard, price=79.99
 Received order: orderId=DEMO004, product=Monitor, price=299.99
 Received order: orderId=DEMO005, product=Headphones, price=149.99
 Received order: orderId=DEMO006, product=Webcam, price=89.99
```

### Follow Consumer Logs in Real-Time

```bash
# Open in separate terminal
docker logs -f consumer-service
```

Then send a new order and watch it being processed live!

---

## Demo Part 6: Topic Details

### List All Topics

```bash
docker exec kafka1 kafka-topics --list \
  --bootstrap-server kafka1:19092
```

### Describe 'orders' Topic

```bash
docker exec kafka1 kafka-topics --describe \
  --bootstrap-server kafka1:19092 \
  --topic orders
```

**Expected Output:**

```
Topic: orders    TopicId: xxxxxxxxxx    PartitionCount: 3    ReplicationFactor: 3    Configs: min.insync.replicas=2,retention.ms=604800000
    Topic: orders    Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1,2,3
    Topic: orders    Partition: 1    Leader: 2    Replicas: 2,3,1    Isr: 2,3,1
    Topic: orders    Partition: 2    Leader: 3    Replicas: 3,1,2    Isr: 3,1,2
```

**Explanation:**

- **3 partitions** for parallel processing
- **Replication Factor 3** - every partition replicated to all brokers
- **Isr (In-Sync Replicas)**: All 3 replicas are in sync
- **Leader**: Different partition leaders on different brokers (load balanced)

### Describe 'orders-retry' Topic

```bash
docker exec kafka1 kafka-topics --describe \
  --bootstrap-server kafka1:19092 \
  --topic orders-retry
```

### Describe 'orders-dlq' Topic

```bash
docker exec kafka1 kafka-topics --describe \
  --bootstrap-server kafka1:19092 \
  --topic orders-dlq
```

### Get Topic Configuration

```bash
docker exec kafka1 kafka-configs --describe \
  --bootstrap-server kafka1:19092 \
  --entity-type topics \
  --entity-name orders
```

---

## Demo Part 7: Consumer Groups

### List All Consumer Groups

```bash
docker exec kafka1 kafka-consumer-groups --list \
  --bootstrap-server kafka1:19092
```

**Expected Output:**

```
order-consumer-group
order-consumer-group-retry
order-consumer-group-dlq
```

### Describe Main Consumer Group

```bash
docker exec kafka1 kafka-consumer-groups --describe \
  --bootstrap-server kafka1:19092 \
  --group order-consumer-group
```

**Expected Output:**

```
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG  CONSUMER-ID                                     HOST            CLIENT-ID
order-consumer-group orders          0          2               2               0    consumer-order-consumer-group-1-xxxxx          /172.18.0.7     consumer-order-consumer-group-1
order-consumer-group orders          1          2               2               0    consumer-order-consumer-group-2-xxxxx          /172.18.0.7     consumer-order-consumer-group-2
order-consumer-group orders          2          2               2               0    consumer-order-consumer-group-3-xxxxx          /172.18.0.7     consumer-order-consumer-group-3
```

**Explanation:**

- **LAG = 0**: Consumer is processing in real-time, no backlog
- **3 consumers**: One per partition (optimal for parallel processing)
- **CURRENT-OFFSET = LOG-END-OFFSET**: All messages processed

### View Consumer Group State

```bash
docker exec kafka1 kafka-consumer-groups --describe \
  --bootstrap-server kafka1:19092 \
  --group order-consumer-group \
  --state
```

**Expected Output:**

```
GROUP                COORDINATOR (ID)   ASSIGNMENT-STRATEGY  STATE    #MEMBERS
order-consumer-group kafka2 (2)         range                Stable   3
```

- **State: Stable** means no rebalancing, all consumers active
- **#MEMBERS: 3** means 3 consumer instances

---

## Demo Commands

### Count Messages in Topic

```bash
docker exec kafka1 kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list kafka1:19092 \
  --topic orders
```

### View Messages from Beginning

```bash
docker exec kafka1 kafka-console-consumer \
  --bootstrap-server kafka1:19092 \
  --topic orders \
  --from-beginning \
  --max-messages 5
```

**Note:** Messages are in Avro format, so output will be binary

### Check Broker Status

```bash
docker exec kafka1 kafka-broker-api-versions \
  --bootstrap-server kafka1:19092
```

### View Cluster Metadata

```bash
docker exec kafka1 kafka-metadata \
  --bootstrap-server kafka1:19092 \
  --describe --entity-type brokers
```

---

## System Shutdown

### Step 1: Stop Application Services

```bash
cd infrastructure/docker
docker compose stop producer-service consumer-service kafka-ui
```

### Step 2: Stop Infrastructure

```bash
docker compose stop schema-registry kafka1 kafka2 kafka3 zookeeper
```

### Step 3: Remove All Containers (Keep Data)

```bash
docker compose down
```

### Step 4: Remove All Containers AND Data

```bash
# WARNING: This deletes all data in volumes!
docker compose down -v
```

## Quick Reference - All Endpoints

### Producer Service

```
Health Check:  http://localhost:8090/actuator/health
Create Order:  POST http://localhost:8090/api/orders?orderId=X&product=Y&price=Z
```

### Consumer Service

```
Health Check:  http://localhost:8082/actuator/health
Statistics:    http://localhost:8082/api/consumer/stats
```

### Schema Registry

```
List Subjects: http://localhost:8081/subjects
Get Schema:    http://localhost:8081/subjects/orders-value/versions/1
```

### Kafka UI

```
Dashboard:     http://localhost:8080
```

### Kafka Brokers

```
Broker 1:      localhost:9092
Broker 2:      localhost:9093
Broker 3:      localhost:9094
```

### ZooKeeper

```
ZooKeeper:     localhost:2181
```
