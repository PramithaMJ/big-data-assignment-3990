# Kafka Order Processing Dashboard

A modern, real-time web dashboard for monitoring and interacting with the Kafka Order Processing System.

## Features

### Real-Time Monitoring

- **Live Statistics**: View total orders, running average, total revenue, and success rate
- **Auto-Refresh**: Dashboard updates every 5 seconds automatically
- **System Health**: Monitor all 8 containers (Kafka cluster, services) status
- **Performance Metrics**: Track throughput, latency, and consumer lag

### Order Management

- **Create Orders**: Interactive form to send orders to Kafka
- **Random Orders**: Generate test orders with one click
- **Batch Processing**: Send 10 orders simultaneously for load testing
- **Order History**: View recent orders in a sortable table

### Kafka Cluster Visualization

- **3-Broker Status**: Monitor all Kafka brokers (ports 9092-9094)
- **ZooKeeper Health**: Track ZooKeeper coordination service
- **Schema Registry**: Monitor Avro schema management
- **Kafka UI Access**: Quick link to Kafka UI management console

### Advanced Monitoring

- **Error Tracking**: View errors, retries, and DLQ counts
- **Topic Statistics**: Monitor messages in orders, retry, and DLQ topics
- **Service Health**: Check producer and consumer service status
- **Toast Notifications**: Real-time feedback for all actions

## Quick Start

### Access the Dashboard

Once all services are running, access the dashboard at:

```
http://localhost:3000
```

### Start the Complete System

```bash
# From project root
cd infrastructure/docker
docker-compose up -d

# Wait for all services to be healthy (~2-3 minutes)
docker-compose ps

# Access dashboard
open http://localhost:3000
```

## Dashboard Sections

### 1. System Overview

- **Total Orders**: Cumulative count of processed orders
- **Running Average**: Real-time average order price
- **Total Revenue**: Sum of all order amounts
- **Success Rate**: Processing success percentage

### 2. Create New Order

- **Order ID**: Unique identifier (e.g., ORD12345)
- **Product Name**: Item description (e.g., Laptop)
- **Price**: Order amount in USD

**Actions:**

- **Send Order**: Submit single order to Kafka
- **Random Order**: Auto-generate test data
- **Send 10 Orders**: Batch test for load simulation

### 3. Kafka Cluster Status

Monitor all infrastructure components:

- Kafka Broker 1 (Port 9092)
- Kafka Broker 2 (Port 9093)
- Kafka Broker 3 (Port 9094)
- ZooKeeper (Port 2181)
- Schema Registry (Port 8081)
- Kafka UI (Port 8080) - Click to open

### 4. Microservices Status

- **Producer Service**: Order creation endpoint (Port 8090)
- **Consumer Service**: Order processing & statistics (Port 8082)
- Direct links to health endpoints

### 5. Recent Orders

Live table showing:

- Timestamp
- Order ID
- Product name
- Price
- Status (Success/Pending/Error)

### 6. System Monitoring

- **Error Tracking**: Total errors, retries, DLQ messages
- **Performance**: Orders/sec, average latency, consumer lag
- **Topics Status**: Message counts in each Kafka topic

### 7. Quick Actions

- **Refresh Dashboard**: Manual data update
- **Clear Statistics**: Reset dashboard counters
- **Open Kafka UI**: External management console
- **Export Data**: Download JSON with all metrics

## Technical Architecture

### Frontend Stack

- **HTML5**: Semantic markup
- **CSS3**: Modern styling with gradients, animations
- **Vanilla JavaScript**: No frameworks, zero dependencies
- **Font Awesome 6.4.0**: Icon library (CDN)

### Backend Integration

- **Producer API**: `http://localhost:8090/api/orders`
- **Consumer Stats**: `http://localhost:8082/api/consumer/stats`
- **Health Checks**: Spring Boot Actuator endpoints
- **Schema Registry**: `http://localhost:8081/subjects`

### Containerization

- **Base Image**: `nginx:alpine` (lightweight)
- **Port**: 3000
- **Health Check**: `/health` endpoint
- **Auto-restart**: Yes

## File Structure

```
dashboard/
├── index.html          # Main HTML structure
├── styles.css          # Complete styling (800+ lines)
├── app.js             # Application logic (600+ lines)
├── nginx.conf         # Nginx server configuration
├── Dockerfile         # Multi-stage container build
└── README.md          # This file
```

## API Endpoints Used

### Producer Service (Port 8090)

```bash
# Create order
POST http://localhost:8090/api/orders?orderId=ORD123&product=Laptop&price=999.99

# Health check
GET http://localhost:8090/actuator/health
```

### Consumer Service (Port 8082)

```bash
# Get statistics
GET http://localhost:8082/api/consumer/stats

# Response format:
{
  "ordersProcessed": 150,
  "runningAverage": 349.99,
  "totalAmount": 52498.50,
  "detailedStats": {
    "minPrice": 10.00,
    "maxPrice": 1500.00
  }
}

# Health check
GET http://localhost:8082/actuator/health
```

### Schema Registry (Port 8081)

```bash
# List schemas
GET http://localhost:8081/subjects
```

## Demo Scenarios

### Scenario 1: Single Order Creation

1. Open dashboard at `http://localhost:3000`
2. Fill order form:
   - Order ID: `ORD12345`
   - Product: `Laptop`
   - Price: `999.99`
3. Click "Send Order"
4. Watch statistics update in real-time

### Scenario 2: Load Testing

1. Click "Send 10 Orders" button
2. Observe batch processing in order table
3. Monitor statistics increase
4. Check success rate remains 100%

### Scenario 3: System Monitoring

1. Scroll to "Kafka Cluster Status"
2. Verify all brokers are healthy (green)
3. Check "Microservices Status"
4. Confirm producer & consumer are running

### Scenario 4: Data Export

1. Process multiple orders
2. Click "Export Data" in Quick Actions
3. Download JSON file with complete metrics
4. Use for reporting or analysis

## Troubleshooting

### Dashboard Not Loading

```bash
# Check container status
docker ps | grep dashboard

# View logs
docker logs dashboard

# Restart dashboard
docker-compose restart dashboard
```

### Can't Create Orders

```bash
# Verify producer service is running
curl http://localhost:8090/actuator/health

# Check Docker network
docker network inspect docker_kafka-net
```

### Statistics Not Updating

```bash
# Check consumer service
curl http://localhost:8082/api/consumer/stats

# Verify consumer is processing
docker logs consumer-service -f
```

## Auto-Refresh Behavior

The dashboard automatically refreshes data every 5 seconds:

- Consumer statistics
- System health checks
- Service status
- Timestamp updates

**Disable auto-refresh**: Modify `app.js` line with `setInterval(refreshDashboard, 5000)`

## Metrics Explained

### Running Average

Calculated using **running average algorithm**:

```
newAverage = ((oldAverage × oldCount) + newPrice) / (oldCount + 1)
```

### Success Rate

```
successRate = (successfulOrders / totalOrders) × 100
```

### Consumer Lag

Difference between produced and consumed message offsets.
