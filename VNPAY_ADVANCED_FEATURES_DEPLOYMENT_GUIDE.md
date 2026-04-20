# VNPay Advanced Features - Deployment Guide

## Overview

This document provides comprehensive deployment instructions for the VNPay Advanced Features implementation, including transaction logging, retry logic, webhook support, and refund API integration.

## Pre-Deployment Requirements

### 1. Environment Setup

#### Database Requirements
- MySQL 8.0+ or compatible database
- Ensure database has sufficient storage for transaction logs
- Verify database connection pooling is configured appropriately

#### Application Requirements
- Java 17+ runtime environment
- Spring Boot 3.x compatible environment
- Minimum 2GB RAM for production deployment
- SSL/TLS certificate for webhook endpoints

#### VNPay Configuration
- Valid VNPay merchant account with API access
- VNPay terminal code (vnp_TmnCode)
- VNPay hash secret (vnp_HashSecret)
- VNPay API URLs (production vs sandbox)
- Webhook endpoint registered with VNPay

### 2. Configuration Validation

Before deployment, verify all required configuration properties:

```properties
# Required VNPay Configuration
vnpay.tmnCode=YOUR_TERMINAL_CODE
vnpay.hashSecret=YOUR_HASH_SECRET
vnpay.url=https://vnpay.vn/paymentv2/vpcpay.html
vnpay.returnUrl=https://yourdomain.com/payment/vnpay-return
vnpay.apiUrl=https://vnpay.vn
vnpay.refundUrl=${vnpay.apiUrl}

# Payment Configuration
app.payment.retry.max-attempts=3
app.payment.retry.initial-delay=1000
app.payment.retry.multiplier=2.0
app.payment.retry.max-delay=10000

app.payment.webhook.async-pool-size=10
app.payment.webhook.timeout=30000
app.payment.webhook.ip-whitelist=

app.payment.refund.timeout=30000
app.payment.refund.max-amount-check=true
```

## Deployment Steps

### Phase 1: Database Migration

1. **Backup Current Database**
   ```bash
   mysqldump -u username -p database_name > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Run Database Migrations**
   - Execute payment_transactions table creation
   - Execute refund_transactions table creation
   - Verify indexes are created properly

3. **Verify Database Schema**
   ```sql
   DESCRIBE payment_transactions;
   DESCRIBE refund_transactions;
   SHOW INDEX FROM payment_transactions;
   SHOW INDEX FROM refund_transactions;
   ```

### Phase 2: Application Deployment

1. **Build Application**
   ```bash
   ./gradlew clean build -x test
   ```

2. **Deploy with Feature Flags (Recommended)**
   ```properties
   # Gradual rollout configuration
   app.payment.features.transaction-logging=true
   app.payment.features.retry-logic=true
   app.payment.features.webhook-processing=false  # Enable after testing
   app.payment.features.refund-api=false          # Enable after testing
   ```

3. **Start Application**
   ```bash
   java -jar -Dspring.profiles.active=production smartphone-management.jar
   ```

4. **Verify Application Startup**
   - Check logs for configuration validation messages
   - Verify all beans are initialized correctly
   - Test health endpoints

### Phase 3: Feature Activation

1. **Enable Transaction Logging**
   - Set `app.payment.features.transaction-logging=true`
   - Monitor database for transaction records
   - Verify logging performance impact

2. **Enable Retry Logic**
   - Set `app.payment.features.retry-logic=true`
   - Monitor retry metrics and logs
   - Test with simulated network failures

3. **Enable Webhook Processing**
   - Set `app.payment.features.webhook-processing=true`
   - Register webhook URL with VNPay: `https://yourdomain.com/api/payment/vnpay-ipn`
   - Test with VNPay sandbox transactions

4. **Enable Refund API**
   - Set `app.payment.features.refund-api=true`
   - Test refund operations in sandbox environment
   - Verify refund status tracking

## Post-Deployment Monitoring

### 1. Health Checks

Monitor these endpoints:
- `GET /api/payment/webhook/health` - Webhook service health
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics

### 2. Key Metrics to Monitor

#### Transaction Logging Metrics
- Transaction creation rate
- Database write latency
- Failed transaction logging count

#### Retry Logic Metrics
- Retry attempt count by error type
- Retry success rate
- Total retry duration

#### Webhook Processing Metrics
- Webhook processing duration
- Signature validation failure rate
- Duplicate webhook count
- Async thread pool utilization

#### Refund Operation Metrics
- Refund request count
- Refund success rate
- Refund processing duration

### 3. Log Monitoring

Monitor these log patterns:
```
# Success patterns
"Payment operation completed"
"Webhook processing completed"
"Refund operation completed"

# Warning patterns
"Retry attempt"
"Slow payment operation detected"
"Duplicate webhook detected"

# Error patterns
"Payment operation failed"
"Signature verification failed"
"Refund operation failed"
```

## Rollback Procedures

### Emergency Rollback

1. **Disable New Features**
   ```properties
   app.payment.features.webhook-processing=false
   app.payment.features.refund-api=false
   ```

2. **Revert to Previous Version**
   ```bash
   # Stop current application
   sudo systemctl stop smartphone-management
   
   # Deploy previous version
   cp smartphone-management-previous.jar smartphone-management.jar
   
   # Start application
   sudo systemctl start smartphone-management
   ```

3. **Database Rollback (if needed)**
   ```sql
   -- Only if database changes cause issues
   DROP TABLE IF EXISTS refund_transactions;
   DROP TABLE IF EXISTS payment_transactions;
   ```

### Gradual Rollback

1. **Disable Features One by One**
   - Start with newest features (refund API)
   - Monitor system stability after each change
   - Keep transaction logging as last to disable

2. **Monitor System Recovery**
   - Check error rates decrease
   - Verify payment processing returns to normal
   - Monitor database performance

## Security Considerations

### 1. Webhook Security

- **IP Whitelisting**: Configure VNPay IP addresses in `app.payment.webhook.ip-whitelist`
- **Signature Validation**: Always enabled, never disable in production
- **HTTPS Only**: Ensure webhook endpoint uses HTTPS with valid certificate

### 2. Configuration Security

- **Environment Variables**: Use environment variables for sensitive configuration
  ```bash
  export VNPAY_TMN_CODE="your_terminal_code"
  export VNPAY_HASH_SECRET="your_hash_secret"
  ```

- **Configuration Encryption**: Consider encrypting sensitive properties
- **Access Control**: Limit access to configuration files

### 3. Database Security

- **Connection Encryption**: Use SSL for database connections
- **Access Control**: Limit database user permissions
- **Audit Logging**: Enable database audit logging for payment tables

## Performance Optimization

### 1. Database Optimization

```sql
-- Optimize payment_transactions table
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at);
CREATE INDEX idx_payment_transactions_status_created ON payment_transactions(status, created_at);

-- Optimize refund_transactions table
CREATE INDEX idx_refund_transactions_created_at ON refund_transactions(created_at);
CREATE INDEX idx_refund_transactions_status_created ON refund_transactions(status, created_at);
```

### 2. Application Optimization

```properties
# Connection pool optimization
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Async processing optimization
app.payment.webhook.async-pool-size=20
spring.task.execution.pool.max-size=50
```

### 3. Monitoring Optimization

- Set up database query monitoring
- Configure JVM metrics collection
- Enable application performance monitoring (APM)

## Troubleshooting Guide

### Common Issues

1. **Webhook Signature Validation Failures**
   - Verify VNPay hash secret is correct
   - Check webhook URL registration with VNPay
   - Validate request parameter encoding

2. **Database Connection Issues**
   - Check connection pool configuration
   - Verify database server capacity
   - Monitor connection leak detection

3. **Retry Logic Not Working**
   - Verify retry configuration values
   - Check exception types being retried
   - Monitor retry attempt logs

4. **Refund API Failures**
   - Verify VNPay API credentials
   - Check refund request format
   - Monitor VNPay API response codes

### Log Analysis Commands

```bash
# Monitor payment operations
tail -f application.log | grep "Payment operation"

# Monitor webhook processing
tail -f application.log | grep "Webhook processing"

# Monitor retry attempts
tail -f application.log | grep "Retry attempt"

# Monitor errors
tail -f application.log | grep "ERROR"
```

## Support and Maintenance

### Regular Maintenance Tasks

1. **Weekly**
   - Review payment operation metrics
   - Check error rates and patterns
   - Verify webhook processing performance

2. **Monthly**
   - Analyze transaction logging storage usage
   - Review retry success rates
   - Update monitoring thresholds if needed

3. **Quarterly**
   - Performance optimization review
   - Security configuration audit
   - Disaster recovery testing

### Contact Information

- **Development Team**: [team-email@company.com]
- **Operations Team**: [ops-email@company.com]
- **VNPay Support**: [vnpay-support-contact]

## Appendix

### A. Configuration Reference

See `application.properties` for complete configuration reference.

### B. API Documentation

- Webhook endpoint: `POST /api/payment/vnpay-ipn`
- Refund endpoints: `POST /api/payment/refund`, `GET /api/payment/refund/{id}/status`

### C. Database Schema

See database migration scripts for complete schema definitions.