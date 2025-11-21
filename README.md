# Kafka Order Processing System

A comprehensive, production-ready Kafka-based order processing system with Avro serialization, retry mechanisms, Dead Letter Queue (DLQ), and real-time running average aggregation.

## 📋 Table of Contents

- [Architecture](#architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## 🏗️ Architecture

The system consists of three main components:

1. **Producer Service** (Port 8090)
   - Generates and publishes order events to Kafka
   - Uses Avro serialization with Schema Registry
   - REST API for order creation and batch operations

2. **Consumer Service** (Port 8082)
   - Consumes orders from Kafka topics
   - Implements retry logic for failed messages
   - Handles Dead Letter Queue for permanently failed messages
   - Calculates running average of order amounts

3. **Kafka Infrastructure**
   - 3-broker Kafka cluster with Zookeeper
   - Schema Registry for Avro schema management (Port 8081)
   - Kafka UI for monitoring and management

### Data Flow

```
Producer → orders topic (3 partitions) → Consumer
                                           ↓ (on failure)
                                    orders-retry topic
                                           ↓ (max retries reached)
                                    orders-dlq topic
```

## ✨ Features

### Producer Service
- ✅ Avro-based serialization with Schema Registry
- ✅ Idempotent producer configuration
- ✅ REST API for order creation
- ✅ Batch order generation
- ✅ Partition-specific message publishing
- ✅ Asynchronous message sending with callbacks
- ✅ Comprehensive error handling

### Consumer Service
- ✅ Avro deserialization
- ✅ Manual offset commit for reliability
- ✅ Concurrent processing (3 threads for main topic)
- ✅ Exponential backoff retry mechanism
- ✅ Dead Letter Queue for failed messages
- ✅ Real-time running average calculation
- ✅ Thread-safe aggregation
- ✅ REST API for statistics and monitoring

### Reliability Features
- ✅ At-least-once delivery guarantee
- ✅ Exactly-once semantics (idempotent producer)
- ✅ Replication factor of 3
- ✅ Min in-sync replicas: 2
- ✅ Automatic retry on failure (up to 3 attempts)
- ✅ DLQ for permanently failed messages
- ✅ Manual offset management

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.5.7**
- **Apache Kafka 3.x** (Confluent Platform 7.5.0)
- **Apache Avro 1.11.3**
- **Confluent Schema Registry 7.5.0**
- **Docker & Docker Compose**
- **Maven**
- **Lombok**

## 📁 Project Structure

```
kafka-order-system/
│
├── producer-service/
│   ├── src/main/java/com/pramithamj/kafka/
│   │   ├── config/
│   │   │   └── KafkaProducerConfig.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── producer/
│   │   │   └── OrderProducer.java
│   │   └── ProducerServiceApplication.java
│   ├── src/main/resources/
│   │   ├── avro/
│   │   │   └── order.avsc
│   │   └── application.properties
│   └── pom.xml
│
├── consumer-service/
│   ├── src/main/java/com/pramithamj/kafka/
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   └── KafkaProducerConfig.java
│   │   ├── consumer/
│   │   │   └── OrderConsumer.java
│   │   ├── aggregation/
│   │   │   └── RunningAverageCalculator.java
│   │   ├── retry/
│   │   │   └── RetryHandler.java
│   │   ├── dlq/
│   │   │   └── DLQHandler.java
│   │   ├── controller/
│   │   │   └── ConsumerController.java
│   │   └── ConsumerServiceApplication.java
│   ├── src/main/resources/
│   │   ├── avro/
│   │   │   └── order.avsc
│   │   └── application.properties
│   └── pom.xml
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml
│   │   └── .env
│   └── scripts/
│       ├── create-topics.sh
│       ├── check-cluster.sh
│       └── seed-data.sh
│
└── docs/
    └── README.md
```

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- At least 4GB of available RAM for Docker

## 🚀 Quick Start

### 1. Start Kafka Infrastructure

```bash
cd infrastructure/docker
docker-compose up -d
```

Wait for all services to be healthy (~30 seconds):

```bash
cd ../scripts
chmod +x *.sh
./check-cluster.sh
```

### 2. Create Kafka Topics

```bash
./create-topics.sh
```

This creates:
- `orders` (3 partitions, RF=3)
- `orders-retry` (3 partitions, RF=3)
- `orders-dlq` (1 partition, RF=3)

### 3. Build Services

**Producer Service:**
```bash
cd ../../producer-service
mvn clean package
```

**Consumer Service:**
```bash
cd ../consumer-service
mvn clean package
```

### 4. Start Services

**Terminal 1 - Producer:**
```bash
cd producer-service
mvn spring-boot:run
```

**Terminal 2 - Consumer:**
```bash
cd consumer-service
mvn spring-boot:run
```

### 5. Send Test Orders

```bash
# Single order
curl -X POST http://localhost:8090/api/orders

# Batch of 50 orders
curl -X POST "http://localhost:8090/api/orders/batch?count=50"
```

### 6. Check Statistics

```bash
# Consumer statistics
curl http://localhost:8082/api/consumer/stats

# Running average
curl http://localhost:8082/api/consumer/average
```

## ⚙️ Configuration

### Producer Service (application.properties)

```properties
spring.application.name=producer-service
server.port=8090

spring.kafka.bootstrap-servers=localhost:9092,localhost:9093,localhost:9094
spring.kafka.properties.schema.registry.url=http://localhost:8081

kafka.topic.orders=orders
```

### Consumer Service (application.properties)

```properties
spring.application.name=consumer-service
server.port=8082

spring.kafka.bootstrap-servers=localhost:9092,localhost:9093,localhost:9094
spring.kafka.properties.schema.registry.url=http://localhost:8081

spring.kafka.consumer.group-id=order-consumer-group
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.max-poll-interval-ms=300000

kafka.topic.orders=orders
kafka.topic.orders-retry=orders-retry
kafka.topic.orders-dlq=orders-dlq

kafka.retry.max-attempts=3
```

### Environment Variables (.env)

```properties
CONFLUENT_VERSION=7.5.0
REPLICATION_FACTOR=3
MIN_ISR=2
KAFKA1_PORT=9092
KAFKA2_PORT=9093
KAFKA3_PORT=9094
SCHEMA_REGISTRY_PORT=8081
KAFKA_UI_PORT=8080
```

## 📡 API Documentation

### Producer Service (Port 8090)

#### Create Single Order
```bash
POST /api/orders
Content-Type: application/json

{
  "orderId": "ORD-12345",
  "customerId": "CUST-5678",
  "productId": "PROD-001",
  "quantity": 2,
  "price": 99.99,
  "totalAmount": 199.98,
  "orderStatus": "PENDING",
  "timestamp": 1700000000000,
  "paymentMethod": "CREDIT_CARD"
}
```

Response:
```json
{
  "success": true,
  "orderId": "ORD-12345",
  "message": "Order sent to Kafka successfully"
}
```

#### Create Batch Orders
```bash
POST /api/orders/batch?count=10
```

Response:
```json
{
  "success": true,
  "count": 10,
  "orderIds": ["ORD-ABC123", "ORD-DEF456", ...],
  "message": "10 orders sent to Kafka successfully"
}
```

#### Send to Specific Partition
```bash
POST /api/orders/partition/0
```

#### Health Check
```bash
GET /api/orders/health
```

### Consumer Service (Port 8082)

#### Get Statistics
```bash
GET /api/consumer/stats
```

Response:
```json
{
  "ordersProcessed": 150,
  "totalAmount": 15234.56,
  "runningAverage": 101.56,
  "detailedStats": "Processed: 150 | Errors: 5 | Success Rate: 96.67% | ..."
}
```

#### Get Running Average
```bash
GET /api/consumer/average
```

Response:
```json
{
  "currentAverage": 101.56,
  "orderCount": 150,
  "totalAmount": 15234.56,
  "statistics": "Orders Processed: 150 | Total Amount: $15234.56 | Running Average: $101.56"
}
```

#### Reset Statistics
```bash
POST /api/consumer/stats/reset
```

#### Health Check
```bash
GET /api/consumer/health
```

## 📊 Monitoring

### Kafka UI
Access the web interface at: **http://localhost:8080**

Features:
- View topics, partitions, and messages
- Monitor consumer groups and lag
- Inspect Schema Registry schemas
- View broker metrics

### Schema Registry
Access at: **http://localhost:8081**

View registered schemas:
```bash
curl http://localhost:8081/subjects
```

### Application Logs

Both services provide detailed logging:
- INFO level for normal operations
- DEBUG level for detailed processing
- WARN/ERROR for issues and failures

## 🧪 Testing

### Test Producer
```bash
# Generate 100 random orders
curl -X POST "http://localhost:8090/api/orders/batch?count=100"
```

### Test Consumer
Watch consumer logs to see processing in real-time:
```bash
# Consumer terminal will show:
# - Order processing
# - Running average updates
# - Retry attempts
# - DLQ entries
```

### Test Retry Mechanism

The system automatically simulates ~5% failure rate for testing. Check logs for:
- Failed order processing
- Retry attempts with exponential backoff
- Final DLQ entries after max retries

### Test Running Average

```bash
# Send orders and check average
curl -X POST "http://localhost:8090/api/orders/batch?count=20"
sleep 5
curl http://localhost:8082/api/consumer/average
```

### Verify Topics

```bash
cd infrastructure/scripts
./check-cluster.sh
```

## 🔧 Troubleshooting

### Issue: Services can't connect to Kafka

**Check if Kafka is running:**
```bash
cd infrastructure/scripts
./check-cluster.sh
```

**Restart Kafka:**
```bash
cd infrastructure/docker
docker-compose restart
```

### Issue: Schema Registry errors

**Check Schema Registry:**
```bash
curl http://localhost:8081/subjects
```

**Restart Schema Registry:**
```bash
docker restart schema-registry
```

### Issue: Consumer not processing messages

**Check consumer group:**
```bash
docker exec kafka1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-consumer-group
```

**Reset consumer offsets (if needed):**
```bash
docker exec kafka1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-consumer-group \
  --topic orders \
  --reset-offsets \
  --to-earliest \
  --execute
```

### Issue: Build failures

**Clean and rebuild:**
```bash
mvn clean install -DskipTests
```

**Check Java version:**
```bash
java -version  # Should be Java 17+
mvn -version
```

### Common Port Conflicts

If ports are already in use:

1. **Producer (8090):** Change `server.port` in producer `application.properties`
2. **Consumer (8082):** Change `server.port` in consumer `application.properties`
3. **Schema Registry (8081):** Change `SCHEMA_REGISTRY_PORT` in `.env`
4. **Kafka UI (8080):** Change `KAFKA_UI_PORT` in `.env`

## 📈 Performance Tuning

### Producer Optimization

```properties
# Increase batch size for throughput
spring.kafka.producer.batch-size=32768

# Increase linger time
spring.kafka.producer.linger-ms=20

# Increase buffer memory
spring.kafka.producer.buffer-memory=67108864
```

### Consumer Optimization

```properties
# Increase concurrent consumers
spring.kafka.consumer.concurrency=5

# Adjust poll records
spring.kafka.consumer.max-poll-records=1000

# Tune fetch settings
spring.kafka.consumer.fetch-min-bytes=1024
spring.kafka.consumer.fetch-max-wait-ms=500
```

## 🛡️ Production Considerations

1. **Security:** Enable SSL/TLS and SASL authentication
2. **Monitoring:** Integrate with Prometheus and Grafana
3. **Alerting:** Set up alerts for DLQ entries and consumer lag
4. **Backup:** Configure topic retention and backup strategies
5. **Scaling:** Increase partitions and consumer instances for higher throughput
6. **Error Handling:** Implement proper error tracking and recovery procedures

## 📝 License

This project is for educational purposes.

## 👥 Contributors

- Pramitha Jayasooriya

## 🙏 Acknowledgments

- Apache Kafka community
- Confluent Platform
- Spring Boot team
