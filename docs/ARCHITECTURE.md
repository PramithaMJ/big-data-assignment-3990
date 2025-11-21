# -System Architecture Documentation

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Order Processing System                          │
│                                                                         │
│  ┌──────────────────┐              ┌─────────────────────────────────┐  │
│  │                  │              │     Kafka Cluster (3 Nodes)     │  │
│  │  Producer        │              │                                 │  │
│  │  Service         │   Produce    │  ┌────────┐  ┌────────┐         │  │
│  │  (REST API)      │─────────────►│  │ Kafka1 │  │ Kafka2 │         │  │
│  │                  │   Messages   │  │ :9092  │  │ :9093  │         │  │
│  │  Port: 8090      │              │  └────────┘  └────────┘         │  │
│  │                  │              │       ┌────────┐                │  │
│  └────────┬─────────┘              │       │ Kafka3 │                │  │
│           │                        │       │ :9094  │                │  │
│           │ Register               │       └────────┘                │  │
│           │ Schema                 │                                 │  │
│           │                        │   Coordination                  │  │
│           ▼                        │        ▲                        │  │
│  ┌──────────────────┐              │        │                        │  │
│  │                  │              │  ┌─────┴──────┐                 │  │
│  │  Schema          │◄─────────────┼──│ ZooKeeper  │                 │  │
│  │  Registry        │   Schema     │  │  :2181     │                 │  │
│  │                  │   Management │  └────────────┘                 │  │
│  │  Port: 8081      │              │                                 │  │
│  │                  │              └────────┬────────────────────────┘  │
│  └──────────────────┘                       │                           │
│                                             │ Consume                   │
│                                             │ Messages                  │
│                                             ▼                           │
│  ┌──────────────────┐              ┌────────────────┐                   │
│  │                  │              │    Topics      │                   │
│  │  Consumer        │◄─────────────│                │                   │
│  │  Service         │   Subscribe  │  • orders      │                   │
│  │  (Processing)    │              │  • orders-retry│                   │
│  │                  │              │  • orders-dlq  │                   │
│  │  Port: 8082      │              └────────────────┘                   │
│  │                  │                                                   │
│  │  - Aggregation   │                                                   │
│  │  - Statistics    │                                                   │
│  │  - Running Avg   │                                                   │
│  └──────────────────┘                                                   │
│                                                                         │
│  ┌──────────────────┐                                                   │
│  │   Kafka UI       │───► Visual Monitoring & Management                │
│  │   Port: 8080     │                                                   │
│  └──────────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────────┘

              All containers running on Docker Network: kafka-net
```

---

## Component Details

### 1. Producer Service

**Technology:** Spring Boot 3.3.5
**Port:** 8090
**Purpose:** REST API for order creation

**Responsibilities:**

- Accept HTTP POST requests with order data
- Validate order information
- Serialize orders to Avro format
- Publish messages to Kafka topics
- Register/validate schemas with Schema Registry

**Key Classes:**

```
com.pramithamj.kafka.producer.OrderProducer
  └── Sends messages to Kafka
com.pramithamj.kafka.controller.OrderController
  └── REST endpoints
com.pramithamj.kafka.config.KafkaProducerConfig
  └── Producer configuration
```

**Producer Configuration:**

- Serializer: Avro (KafkaAvroSerializer)
- Acks: `all` (ensures all replicas acknowledge)
- Retries: 3
- Idempotence: Enabled
- Compression: snappy

---

### 2. Consumer Service

**Technology:** Spring Boot 3.3.5
**Port:** 8082
**Purpose:** Process orders and calculate statistics

**Responsibilities:**

- Consume messages from Kafka topics
- Deserialize Avro messages
- Calculate running average of order prices
- Handle retry logic for failed messages
- Send failed messages to Dead Letter Queue
- Expose statistics via REST API

**Key Classes:**

```
com.pramithamj.kafka.consumer.OrderConsumer
  └── Main message consumer
com.pramithamj.kafka.aggregation.RunningAverageCalculator
  └── Calculates running average
com.pramithamj.kafka.retry.RetryHandler
  └── Handles retry logic
com.pramithamj.kafka.dlq.DLQHandler
  └── Handles dead letter queue
com.pramithamj.kafka.controller.ConsumerController
  └── Statistics API endpoint
```

**Consumer Configuration:**

- Deserializer: Avro (KafkaAvroDeserializer)
- Group ID: `order-consumer-group`
- Auto offset reset: `earliest`
- Concurrency: 3 (one per partition)

---

### 3. Apache Kafka Cluster

**Technology:** Kafka 3.7.1 (Confluent Platform 7.6.0)
**Brokers:** 3 nodes
**Ports:** 9092, 9093, 9094

**Configuration:**

- **Replication Factor:** 3 (all data replicated to all brokers)
- **Min In-Sync Replicas:** 2 (at least 2 replicas must acknowledge)
- **Default Partitions:** 3 per topic
- **Log Retention:** Varies by topic (7 days for orders)

**Topics:**

```
1. orders
   - Partitions: 3
   - Replication Factor: 3
   - Retention: 7 days (604800000 ms)
   - Purpose: Main order processing

2. orders-retry
   - Partitions: 3
   - Replication Factor: 3
   - Retention: 1 day (86400000 ms)
   - Purpose: Retry failed messages

3. orders-dlq (Dead Letter Queue)
   - Partitions: 1
   - Replication Factor: 3
   - Retention: 30 days (2592000000 ms)
   - Purpose: Store permanently failed messages
```

**High Availability:**

- Can tolerate 2 broker failures
- Automatic leader election
- Data replicated across all brokers

---

### 4. ZooKeeper

**Technology:** Apache ZooKeeper 3.8.x
**Port:** 2181
**Purpose:** Cluster coordination

**Responsibilities:**

- Broker registration and discovery
- Leader election for partitions
- Configuration management
- Cluster metadata storage

---

### 5. Schema Registry

**Technology:** Confluent Schema Registry 7.6.0
**Port:** 8081
**Purpose:** Avro schema management

**Responsibilities:**

- Store Avro schemas
- Schema versioning
- Compatibility checking
- Schema evolution management

**Schema Evolution Rules:**

- Backward compatible by default
- Can add optional fields
- Cannot remove required fields

**Registered Schemas:**

```
orders-value: Order schema with fields:
  - orderId: string
  - product: string
  - price: double
  - timestamp: long
```

---

### 6. Kafka UI

**Technology:** Kafka UI (Provectus)
**Port:** 8080
**Purpose:** Visual monitoring and management

**Features:**

- View brokers status
- Monitor topics and partitions
- View messages in topics
- Monitor consumer groups and lag
- View schemas in Schema Registry
- Real-time cluster health

---

## Data Flow

### 1. Order Creation Flow

```
1. Client sends HTTP POST to Producer Service (port 8090)
   POST /api/orders?orderId=X&product=Y&price=Z

2. Producer validates the order data

3. Producer serializes order to Avro format
   - Uses Schema Registry for schema
   - Binary format for efficiency

4. Producer publishes message to Kafka
   - Topic: orders
   - Key: orderId (ensures ordering per order)
   - Partition: Determined by key hash
   - Acks: all (wait for all replicas)

5. Kafka stores message across 3 brokers
   - Leader broker receives message
   - Follower brokers replicate message
   - Acknowledgment sent after min.insync.replicas confirm

6. Producer returns success response to client
```

### 2. Order Processing Flow

```
1. Consumer Service subscribes to 'orders' topic
   - 3 consumer instances (one per partition)
   - Parallel processing for scalability

2. Consumer receives message from assigned partition

3. Consumer deserializes Avro message
   - Uses Schema Registry for schema
   - Converts binary to Java object

4. Consumer processes order:
   - Updates running average calculation
   - Increments order counter
   - Calculates total amount

5. Consumer commits offset to Kafka
   - Manual commit after successful processing
   - Ensures at-least-once delivery

6. On error:
   - Retry up to 3 times with exponential backoff
   - If still failing, send to orders-retry topic
   - After max retries, send to orders-dlq (Dead Letter Queue)
```

### 3. Statistics Flow

```
1. Client sends HTTP GET to Consumer Service (port 8082)
   GET /api/consumer/stats

2. Consumer returns current statistics:
   {
     "ordersProcessed": 100,
     "totalAmount": 12500.50,
     "runningAverage": 125.00,
     "detailedStats": "..."
   }

3. Statistics updated in real-time as messages processed
```

---

## Network Architecture

### Docker Network: kafka-net (Bridge Mode)

```
┌────────────────────────────────────────────────────────────┐
│  Docker Network: kafka-net (172.18.0.0/16)                 │
│                                                            │
│  ┌─────────────────────────────────────────────────────-─┐ │
│  │  ZooKeeper (zookeeper)                                │ │
│  │  Internal: zookeeper:2181                             │ │
│  │  External: localhost:2181                             │ │
│  └────────────────────────────────────────────────────-──┘ │
│                                                            │
│  ┌─────────────────────────────────────────────────────-─┐ │
│  │  Kafka1 (kafka1)                                      │ │
│  │  Internal: kafka1:19092                               │ │
│  │  External: localhost:9092                             │ │
│  └─────────────────────────────────────────────────────-─┘ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Kafka2 (kafka2)                                     │  │
│  │  Internal: kafka2:19093                              │  │
│  │  External: localhost:9093                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Kafka3 (kafka3)                                     │  │
│  │  Internal: kafka3:19094                              │  │
│  │  External: localhost:9094                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Schema Registry (schema-registry)                   │  │
│  │  Internal: schema-registry:8081                      │  │
│  │  External: localhost:8081                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Producer Service (producer-service)                 │  │
│  │  Internal: producer-service:8090                     │  │
│  │  External: localhost:8090                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Consumer Service (consumer-service)                 │  │
│  │  Internal: consumer-service:8082                     │  │
│  │  External: localhost:8082                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Kafka UI (kafka-ui)                                 │  │
│  │  Internal: kafka-ui:8080                             │  │
│  │  External: localhost:8080                            │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘

Internal Communication: Services use hostnames (kafka1:19092)
External Access: Host uses localhost with mapped ports
```

---

## Container Dependencies

```
Startup Order:

1. Network Creation
   docker_kafka-net

2. ZooKeeper
   └── Coordinates cluster

3. Kafka Brokers (Parallel)
   ├── kafka1 (depends on: zookeeper)
   ├── kafka2 (depends on: zookeeper)
   └── kafka3 (depends on: zookeeper)

4. Schema Registry
   └── depends on: kafka1, kafka2, kafka3

5. Application Services (Parallel)
   ├── producer-service (depends on: schema-registry)
   ├── consumer-service (depends on: producer-service)
   └── kafka-ui (depends on: schema-registry)

Health Check Chain:
  ZooKeeper healthy → Kafka healthy → Schema Registry healthy → Apps healthy
```

---

## Data Persistence

### Docker Volumes

```
1. zookeeper-data
   - Stores: ZooKeeper data files
   - Path: /var/lib/zookeeper/data
   - Purpose: Cluster state and metadata

2. zookeeper-logs
   - Stores: ZooKeeper transaction logs
   - Path: /var/lib/zookeeper/log
   - Purpose: Transaction history

3. kafka1-data
   - Stores: Kafka broker 1 log segments
   - Path: /var/lib/kafka/data
   - Purpose: Message data for broker 1

4. kafka2-data
   - Stores: Kafka broker 2 log segments
   - Path: /var/lib/kafka/data
   - Purpose: Message data for broker 2

5. kafka3-data
   - Stores: Kafka broker 3 log segments
   - Path: /var/lib/kafka/data
   - Purpose: Message data for broker 3
```

**Persistence Benefits:**

- Data survives container restarts
- No data loss on container recreation
- Can backup volumes for disaster recovery

---

## Security Architecture

### Container Security

```
1. Non-root Execution
   - Producer: runs as user 'spring' (UID 1000)
   - Consumer: runs as user 'spring' (UID 1000)
   - Minimal privileges

2. Minimal Base Images
   - Build: maven:3.9.6-eclipse-temurin-17
   - Runtime: eclipse-temurin:17-jre-jammy
   - Only JRE included, no development tools

3. Health Checks
   - All services have health endpoints
   - Docker monitors and restarts unhealthy containers
   - Prevents cascading failures
```

### Network Security

```
1. Isolated Network
   - All containers on private bridge network
   - No direct internet access required

2. Port Exposure
   - Only necessary ports exposed to host
   - Internal ports remain isolated

3. No Hardcoded Secrets
   - Configuration via environment variables
   - Sensitive data in .env file (not committed)
```

---

## Scalability Architecture

### Horizontal Scaling

```
Producer Service:
  - Stateless design
  - Can run multiple instances behind load balancer
  - Each instance independently publishes to Kafka

Consumer Service:
  - Consumer group coordination
  - Can add more consumers (up to partition count)
  - Automatic rebalancing

Kafka Cluster:
  - Add more brokers for increased throughput
  - Add more partitions for more parallel consumers
  - Replication ensures no single point of failure
```

### Current Configuration

```
Partitions: 3
  → Max 3 parallel consumers per consumer group
  → Can process 3 orders simultaneously

Brokers: 3
  → Can handle failure of 2 brokers
  → Load distributed across all brokers

Replication Factor: 3
  → Every message stored on all 3 brokers
  → No data loss even if 2 brokers fail
```

---

## Message Flow Diagrams

### Normal Flow

```
Client
  │
  │ HTTP POST
  ▼
Producer Service
  │
  │ Avro Serialize
  ▼
Schema Registry ◄─── Fetch Schema
  │
  │ Validated Message
  ▼
Kafka (orders topic)
  │
  │ Replicate to RF=3
  ├──► Broker 1 (Leader)
  ├──► Broker 2 (Follower)
  └──► Broker 3 (Follower)
  │
  │ Pull Messages
  ▼
Consumer Service
  │
  │ Avro Deserialize
  ▼
Process Order
  │
  ├─► Update Statistics
  ├─► Calculate Running Average
  └─► Commit Offset
```

### Error Handling Flow

```
Consumer Service
  │
  │ Processing Failed
  ▼
Retry Handler
  │
  ├─► Attempt 1 (immediate)
  ├─► Attempt 2 (2 sec backoff)
  └─► Attempt 3 (4 sec backoff)
  │
  │ Still Failing
  ▼
orders-retry Topic
  │
  │ Delayed reprocessing
  ▼
Retry Consumer
  │
  ├─► Max 5 retry attempts
  │
  │ All retries exhausted
  ▼
orders-dlq Topic
  │
  │ Manual investigation required
  ▼
DLQ Handler
  │
  └─► Log for analysis
```

---

## Technology Stack

### Backend Services

```
Language: Java 17 LTS
Framework: Spring Boot 3.3.5
Build Tool: Maven 3.9.6

Key Dependencies:
  - Spring Kafka 3.2.4
  - Apache Kafka Clients 3.7.1
  - Avro 1.11.3
  - Confluent Avro Serializer 7.6.0
  - Spring Boot Actuator (health checks)
  - Lombok (boilerplate reduction)
```

### Kafka Ecosystem

```
Message Broker: Apache Kafka 3.7.1
Confluent Platform: 7.6.0
Coordination: Apache ZooKeeper 3.8.x
Schema Registry: Confluent Schema Registry 7.6.0
Monitoring: Kafka UI (Provectus)
```

### Containerization

```
Container Runtime: Docker 24.x
Orchestration: Docker Compose V2
Base Images:
  - Build: maven:3.9.6-eclipse-temurin-17
  - Runtime: eclipse-temurin:17-jre-jammy
  - Kafka: confluentinc/cp-kafka:7.6.0
  - ZooKeeper: confluentinc/cp-zookeeper:7.6.0
  - Schema Registry: confluentinc/cp-schema-registry:7.6.0
```

### Serialization

```
Format: Apache Avro 1.11.3
Benefits:
  - Compact binary format (smaller than JSON)
  - Schema evolution support
  - Type safety
  - Fast serialization/deserialization
```
