// API Endpoints
const API_BASE = {
    producer: 'http://localhost:8090',
    consumer: 'http://localhost:8082',
    schemaRegistry: 'http://localhost:8081',
    kafkaUI: 'http://localhost:8080'
};

// State Management
let ordersHistory = [];
let statsData = {
    totalOrders: 0,
    runningAverage: 0,
    totalRevenue: 0,
    successRate: 100,
    errorCount: 0,
    retryCount: 0,
    dlqCount: 0
};

// Initialize Dashboard
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Kafka Order Processing Dashboard Initialized');
    
    // Initialize form submission
    document.getElementById('orderForm').addEventListener('submit', handleOrderSubmit);
    
    // Check system status immediately
    checkSystemStatus();
    
    // Start auto-refresh (every 5 seconds)
    setInterval(refreshDashboard, 5000);
    
    // Update timestamp
    updateTimestamp();
    setInterval(updateTimestamp, 1000);
    
    // Load initial data
    loadConsumerStats();
});

// Handle Order Submission
async function handleOrderSubmit(event) {
    event.preventDefault();
    
    const orderId = document.getElementById('orderId').value;
    const product = document.getElementById('product').value;
    const price = parseFloat(document.getElementById('price').value);
    
    try {
        showLoading(true);
        
        const response = await fetch(
            `${API_BASE.producer}/api/orders?orderId=${orderId}&product=${encodeURIComponent(product)}&price=${price}`,
            { method: 'POST' }
        );
        
        if (response.ok) {
            const data = await response.json();
            showToast('success', 'Order Created Successfully!', `Order ${orderId} sent to Kafka`);
            addOrderToTable(orderId, product, price, 'success');
            
            // Clear form
            document.getElementById('orderForm').reset();
            
            // Refresh stats
            setTimeout(loadConsumerStats, 1000);
        } else {
            throw new Error('Failed to create order');
        }
    } catch (error) {
        console.error('Error creating order:', error);
        showToast('error', 'Order Failed', error.message);
        addOrderToTable(orderId, product, price, 'error');
    } finally {
        showLoading(false);
    }
}

// Generate Random Order
function generateRandomOrder() {
    const products = ['Laptop', 'Mouse', 'Keyboard', 'Monitor', 'Headphones', 'Webcam', 'Speaker', 'Microphone'];
    const randomProduct = products[Math.floor(Math.random() * products.length)];
    const randomPrice = (Math.random() * 500 + 10).toFixed(2);
    const randomId = 'ORD' + Math.floor(Math.random() * 100000);
    
    document.getElementById('orderId').value = randomId;
    document.getElementById('product').value = randomProduct;
    document.getElementById('price').value = randomPrice;
    
    showToast('info', 'Random Order Generated', `${randomProduct} - $${randomPrice}`);
}

// Send Batch Orders
async function sendBatchOrders() {
    const products = ['Laptop', 'Mouse', 'Keyboard', 'Monitor', 'Headphones', 'Webcam', 'Speaker', 'Microphone', 'USB Cable', 'Router'];
    let successCount = 0;
    let failCount = 0;
    
    showToast('info', 'Sending Batch Orders', 'Creating 10 random orders...');
    showLoading(true);
    
    for (let i = 0; i < 10; i++) {
        const orderId = 'BATCH' + Date.now() + '-' + i;
        const product = products[Math.floor(Math.random() * products.length)];
        const price = (Math.random() * 500 + 10).toFixed(2);
        
        try {
            const response = await fetch(
                `${API_BASE.producer}/api/orders?orderId=${orderId}&product=${encodeURIComponent(product)}&price=${price}`,
                { method: 'POST' }
            );
            
            if (response.ok) {
                successCount++;
                addOrderToTable(orderId, product, price, 'success');
            } else {
                failCount++;
                addOrderToTable(orderId, product, price, 'error');
            }
        } catch (error) {
            failCount++;
            addOrderToTable(orderId, product, price, 'error');
        }
        
        // Small delay between requests
        await new Promise(resolve => setTimeout(resolve, 200));
    }
    
    showLoading(false);
    showToast('success', 'Batch Complete', `Success: ${successCount}, Failed: ${failCount}`);
    
    // Refresh stats after batch
    setTimeout(loadConsumerStats, 2000);
}

// Load Consumer Statistics
async function loadConsumerStats() {
    try {
        const response = await fetch(`${API_BASE.consumer}/api/consumer/stats`);
        
        if (response.ok) {
            const data = await response.json();
            
            // Update stats
            statsData.totalOrders = data.ordersProcessed || 0;
            statsData.runningAverage = data.runningAverage || 0;
            statsData.totalRevenue = data.totalAmount || 0;
            
            // Calculate success rate
            if (data.ordersProcessed > 0) {
                const successfulOrders = data.ordersProcessed - (data.errorCount || 0);
                statsData.successRate = ((successfulOrders / data.ordersProcessed) * 100).toFixed(2);
            }
            
            // Update UI
            updateStatsUI();
        }
    } catch (error) {
        console.error('Error loading consumer stats:', error);
    }
}

// Update Statistics UI
function updateStatsUI() {
    document.getElementById('totalOrders').textContent = statsData.totalOrders;
    document.getElementById('runningAverage').textContent = statsData.runningAverage.toFixed(2);
    document.getElementById('totalRevenue').textContent = statsData.totalRevenue.toFixed(2);
    document.getElementById('successRate').textContent = statsData.successRate;
    document.getElementById('errorCount').textContent = statsData.errorCount;
    document.getElementById('retryCount').textContent = statsData.retryCount;
    document.getElementById('dlqCount').textContent = statsData.dlqCount;
    
    // Simulate other metrics (in real app, get from Kafka metrics)
    document.getElementById('throughput').textContent = Math.floor(Math.random() * 100);
    document.getElementById('avgLatency').textContent = Math.floor(Math.random() * 50);
    document.getElementById('consumerLag').textContent = Math.floor(Math.random() * 5);
    document.getElementById('ordersTopicCount').textContent = statsData.totalOrders;
    document.getElementById('retryTopicCount').textContent = statsData.retryCount;
    document.getElementById('dlqTopicCount').textContent = statsData.dlqCount;
}

// Add Order to Table
function addOrderToTable(orderId, product, price, status) {
    const tableBody = document.getElementById('ordersTableBody');
    
    // Remove "no data" row if it exists
    const noDataRow = tableBody.querySelector('.no-data');
    if (noDataRow) {
        noDataRow.remove();
    }
    
    const now = new Date();
    const timeStr = now.toLocaleTimeString();
    
    const row = document.createElement('tr');
    row.innerHTML = `
        <td>${timeStr}</td>
        <td><strong>${orderId}</strong></td>
        <td>${product}</td>
        <td>$${parseFloat(price).toFixed(2)}</td>
        <td><span class="order-status ${status}">${status.toUpperCase()}</span></td>
    `;
    
    // Add to beginning of table
    tableBody.insertBefore(row, tableBody.firstChild);
    
    // Keep only last 20 orders
    if (tableBody.children.length > 20) {
        tableBody.removeChild(tableBody.lastChild);
    }
    
    // Add to history
    ordersHistory.unshift({
        time: timeStr,
        orderId,
        product,
        price,
        status,
        timestamp: now
    });
}

// Check System Status
async function checkSystemStatus() {
    let allHealthy = true;
    
    // Check Producer Service
    try {
        const response = await fetch(`${API_BASE.producer}/actuator/health`, { method: 'GET' });
        updateServiceStatus('producerService', response.ok);
        if (!response.ok) allHealthy = false;
    } catch (error) {
        updateServiceStatus('producerService', false);
        allHealthy = false;
    }
    
    // Check Consumer Service
    try {
        const response = await fetch(`${API_BASE.consumer}/actuator/health`, { method: 'GET' });
        updateServiceStatus('consumerService', response.ok);
        if (!response.ok) allHealthy = false;
    } catch (error) {
        updateServiceStatus('consumerService', false);
        allHealthy = false;
    }
    
    // Check Schema Registry
    try {
        const response = await fetch(`${API_BASE.schemaRegistry}/subjects`, { method: 'GET' });
        updateClusterStatus('schemaRegistry', response.ok);
        if (!response.ok) allHealthy = false;
    } catch (error) {
        updateClusterStatus('schemaRegistry', false);
        allHealthy = false;
    }
    
    // Simulate other services (in real app, implement actual health checks)
    updateClusterStatus('broker1', true);
    updateClusterStatus('broker2', true);
    updateClusterStatus('broker3', true);
    updateClusterStatus('zookeeper', true);
    updateClusterStatus('kafkaUI', true);
    
    // Update overall system status
    const statusDot = document.getElementById('systemStatus');
    const statusText = document.getElementById('systemStatusText');
    
    if (allHealthy) {
        statusDot.classList.add('online');
        statusDot.classList.remove('offline');
        statusText.textContent = 'All Systems Operational';
    } else {
        statusDot.classList.add('offline');
        statusDot.classList.remove('online');
        statusText.textContent = 'Some Services Down';
    }
}

// Update Service Status
function updateServiceStatus(serviceId, isHealthy) {
    const serviceCard = document.getElementById(serviceId);
    const statusSpan = serviceCard.querySelector('.service-status');
    
    if (isHealthy) {
        statusSpan.textContent = 'Healthy';
        statusSpan.classList.remove('checking', 'unhealthy');
        statusSpan.classList.add('healthy');
    } else {
        statusSpan.textContent = 'Unhealthy';
        statusSpan.classList.remove('checking', 'healthy');
        statusSpan.classList.add('unhealthy');
    }
}

// Update Cluster Status
function updateClusterStatus(clusterId, isHealthy) {
    const clusterCard = document.getElementById(clusterId);
    const statusSpan = clusterCard.querySelector('.cluster-status');
    
    if (isHealthy) {
        clusterCard.classList.add('healthy');
        clusterCard.classList.remove('unhealthy');
        statusSpan.textContent = 'Healthy';
        statusSpan.classList.remove('checking', 'unhealthy');
        statusSpan.classList.add('healthy');
    } else {
        clusterCard.classList.add('unhealthy');
        clusterCard.classList.remove('healthy');
        statusSpan.textContent = 'Unhealthy';
        statusSpan.classList.remove('checking', 'healthy');
        statusSpan.classList.add('unhealthy');
    }
}

// Refresh Dashboard
function refreshDashboard() {
    loadConsumerStats();
    checkSystemStatus();
    updateTimestamp();
}

// Clear Statistics
function clearStats() {
    if (confirm('Are you sure you want to clear all statistics? This will not affect actual Kafka data.')) {
        statsData = {
            totalOrders: 0,
            runningAverage: 0,
            totalRevenue: 0,
            successRate: 100,
            errorCount: 0,
            retryCount: 0,
            dlqCount: 0
        };
        
        ordersHistory = [];
        
        // Clear table
        const tableBody = document.getElementById('ordersTableBody');
        tableBody.innerHTML = '<tr><td colspan="5" class="no-data">No orders yet. Create your first order above!</td></tr>';
        
        updateStatsUI();
        showToast('info', 'Statistics Cleared', 'Dashboard has been reset');
    }
}

// Export Data
function exportData() {
    const exportData = {
        timestamp: new Date().toISOString(),
        stats: statsData,
        orders: ordersHistory
    };
    
    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `kafka-dashboard-export-${Date.now()}.json`;
    link.click();
    
    showToast('success', 'Data Exported', 'Downloaded JSON file');
}

// Show Toast Notification
function showToast(type, title, message) {
    const container = document.getElementById('toastContainer');
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'error' ? 'fa-exclamation-circle' : 
                 'fa-info-circle';
    
    toast.innerHTML = `
        <i class="fas ${icon}"></i>
        <div>
            <strong>${title}</strong>
            <p style="margin: 0; font-size: 0.9rem; color: #666;">${message}</p>
        </div>
    `;
    
    container.appendChild(toast);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}

// Show Loading State
function showLoading(show) {
    const submitBtn = document.querySelector('button[type="submit"]');
    if (show) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="loading"></span> Sending...';
    } else {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fas fa-paper-plane"></i> Send Order';
    }
}

// Update Timestamp
function updateTimestamp() {
    const now = new Date();
    document.getElementById('lastUpdate').textContent = now.toLocaleTimeString();
}

// Keyboard Shortcuts
document.addEventListener('keydown', (event) => {
    // Ctrl/Cmd + R: Refresh Dashboard
    if ((event.ctrlKey || event.metaKey) && event.key === 'r') {
        event.preventDefault();
        refreshDashboard();
        showToast('info', 'Dashboard Refreshed', 'Data updated successfully');
    }
    
    // Ctrl/Cmd + G: Generate Random Order
    if ((event.ctrlKey || event.metaKey) && event.key === 'g') {
        event.preventDefault();
        generateRandomOrder();
    }
});

// Console Banner
console.log(`
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║     🚀 Kafka Order Processing Dashboard                  ║
║                                                           ║
║     Version: 1.0.0                                        ║
║     Spring Boot: 3.3.5                                    ║
║     Apache Kafka: 3.7.1                                   ║
║     Confluent Platform: 7.6.0                             ║
║                                                           ║
║     Keyboard Shortcuts:                                   ║
║     • Ctrl/Cmd + R: Refresh Dashboard                     ║
║     • Ctrl/Cmd + G: Generate Random Order                 ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
`);
