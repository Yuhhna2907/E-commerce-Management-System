# VNPay Advanced Features - Operations Runbook

## Overview

This runbook provides operational procedures for monitoring, troubleshooting, and maintaining the VNPay Advanced Features implementation. It covers transaction logging, retry logic, webhook processing, and refund operations.

## Table of Contents

1. [Monitoring Procedures](#monitoring-procedures)
2. [Common Issues and Solutions](#common-issues-and-solutions)
3. [Emergency Procedures](#emergency-procedures)
4. [Maintenance Tasks](#maintenance-tasks)
5. [Performance Optimization](#performance-optimization)
6. [Security Incident Response](#security-incident-response)

## Monitoring Procedures

### 1. Health Check Monitoring

#### Automated Health Checks

Monitor these endpoints every 30 seconds:

```bash
# Application health
curl -f https://yourdomain.com/actuator/health

# Webhook service health
curl -f https://yourdomain.com/api/payment/webhook/health

# Database connectivity
curl -f https://yourdomain.com/actuator/health/db
```

#### Expected Responses

```json
// Healthy response
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}

// Webhook health
{
  "status": "healthy",
  "service": "vnpay-webhook",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 2. Key Performance Indicators (KPIs)

#### Transaction Logging KPIs

```bash
# Monitor transaction creation rate (per minute)
grep "Payment operation started" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l

# Monitor database write latency
grep "Database operation metrics" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | awk '{print $NF}' | sed 's/ms//' | sort -n
```

**Thresholds:**
- Transaction creation rate: < 1000/minute (normal), > 2000/minute (alert)
- Database write latency: < 100ms (good), > 500ms (warning), > 1000ms (critical)

#### Retry Logic KPIs

```bash
# Monitor retry attempts
grep "Retry attempt" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l

# Monitor retry success rate
grep "Payment operation completed" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l
```

**Thresholds:**
- Retry rate: < 5% (normal), > 15% (warning), > 30% (critical)
- Retry success rate: > 90% (good), < 80% (warning), < 70% (critical)

#### Webhook Processing KPIs

```bash
# Monitor webhook processing duration
grep "Webhook processing metrics" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | awk '{print $6}' | sed 's/duration=//;s/ms,//' | sort -n

# Monitor signature validation failures
grep "Signature verification failed" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l
```

**Thresholds:**
- Webhook processing: < 2000ms (good), > 5000ms (warning), > 10000ms (critical)
- Signature failures: < 1% (normal), > 5% (warning), > 10% (critical)

#### Refund Operation KPIs

```bash
# Monitor refund success rate
grep "Refund operation completed" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l
grep "Refund operation failed" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l
```

**Thresholds:**
- Refund success rate: > 95% (good), < 90% (warning), < 85% (critical)

### 3. Alert Configuration

#### Critical Alerts (Immediate Response)

```yaml
# Example alert configuration (Prometheus/AlertManager format)
groups:
- name: vnpay-critical
  rules:
  - alert: VNPayWebhookDown
    expr: up{job="vnpay-webhook"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "VNPay webhook service is down"

  - alert: DatabaseWriteLatencyHigh
    expr: database_write_duration_ms > 1000
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "Database write latency is critically high"

  - alert: SignatureValidationFailureSpike
    expr: rate(signature_validation_failures[5m]) > 0.1
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "High rate of signature validation failures detected"
```

#### Warning Alerts (Monitor Closely)

```yaml
- name: vnpay-warning
  rules:
  - alert: RetryRateHigh
    expr: rate(payment_retry_attempts[5m]) > 0.15
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Payment retry rate is elevated"

  - alert: WebhookProcessingSlowdown
    expr: webhook_processing_duration_ms > 5000
    for: 3m
    labels:
      severity: warning
    annotations:
      summary: "Webhook processing is slower than expected"
```

## Common Issues and Solutions

### 1. Transaction Logging Issues

#### Issue: Transaction Records Not Being Created

**Symptoms:**
```bash
grep "Payment operation started" application.log | tail -10
# No corresponding "Transaction logged" messages
```

**Diagnosis:**
```bash
# Check database connectivity
grep "Database.*error" application.log | tail -5

# Check transaction logger errors
grep "PaymentTransactionLogger.*error" application.log | tail -5
```

**Solutions:**
1. **Database Connection Issue:**
   ```bash
   # Check database status
   mysql -u username -p -e "SELECT 1"
   
   # Restart application if needed
   sudo systemctl restart smartphone-management
   ```

2. **Transaction Rollback:**
   ```bash
   # Check for transaction rollback logs
   grep "Transaction rolled back" application.log | tail -5
   
   # Review application logic for transaction boundaries
   ```

#### Issue: Slow Transaction Logging

**Symptoms:**
```bash
grep "Slow database operation detected" application.log | tail -10
```

**Solutions:**
1. **Database Performance:**
   ```sql
   -- Check for missing indexes
   SHOW INDEX FROM payment_transactions;
   
   -- Analyze slow queries
   SHOW PROCESSLIST;
   ```

2. **Connection Pool Tuning:**
   ```properties
   # Increase connection pool size
   spring.datasource.hikari.maximum-pool-size=30
   spring.datasource.hikari.minimum-idle=10
   ```

### 2. Retry Logic Issues

#### Issue: Retry Logic Not Triggering

**Symptoms:**
```bash
# API failures without retry attempts
grep "VNPayAPIClient.*error" application.log | grep -v "Retry attempt"
```

**Diagnosis:**
```bash
# Check retry configuration
grep "retry" application.properties

# Verify exception types
grep "Non-retryable error" application.log | tail -5
```

**Solutions:**
1. **Configuration Issue:**
   ```properties
   # Verify retry configuration
   app.payment.retry.max-attempts=3
   app.payment.retry.initial-delay=1000
   ```

2. **Exception Handling:**
   ```java
   // Ensure correct exception types in @Retryable annotation
   @Retryable(retryFor = {SocketTimeoutException.class, ConnectException.class, HttpServerErrorException.class})
   ```

#### Issue: Retry Exhaustion

**Symptoms:**
```bash
grep "Retry attempts exhausted" application.log | tail -10
```

**Solutions:**
1. **Increase Retry Attempts (Temporary):**
   ```properties
   app.payment.retry.max-attempts=5
   app.payment.retry.max-delay=15000
   ```

2. **Check VNPay Service Status:**
   ```bash
   # Test VNPay API connectivity
   curl -I https://vnpay.vn/paymentv2/vpcpay.html
   ```

### 3. Webhook Processing Issues

#### Issue: Signature Validation Failures

**Symptoms:**
```bash
grep "Signature verification failed" application.log | tail -10
```

**Diagnosis:**
```bash
# Check for IP address patterns
grep "sourceIp" application.log | grep "Signature verification failed" | awk '{print $NF}' | sort | uniq -c

# Verify hash secret configuration
grep "vnpay.hashSecret" application.properties
```

**Solutions:**
1. **Hash Secret Mismatch:**
   ```bash
   # Verify hash secret with VNPay
   # Update configuration if needed
   export VNPAY_HASH_SECRET="correct_hash_secret"
   ```

2. **Malicious Requests:**
   ```bash
   # Block suspicious IP addresses
   iptables -A INPUT -s suspicious_ip -j DROP
   
   # Enable IP whitelist
   app.payment.webhook.ip-whitelist=vnpay_ip_1,vnpay_ip_2
   ```

#### Issue: Webhook Processing Timeout

**Symptoms:**
```bash
grep "Webhook processing timeout" application.log | tail -10
```

**Solutions:**
1. **Increase Timeout:**
   ```properties
   app.payment.webhook.timeout=60000
   ```

2. **Optimize Processing:**
   ```properties
   # Increase async pool size
   app.payment.webhook.async-pool-size=20
   ```

### 4. Refund Operation Issues

#### Issue: Refund API Failures

**Symptoms:**
```bash
grep "Refund operation failed" application.log | tail -10
```

**Diagnosis:**
```bash
# Check VNPay API responses
grep "VNPay.*response" application.log | grep "refund" | tail -5

# Check refund validation errors
grep "Refund validation" application.log | tail -5
```

**Solutions:**
1. **VNPay API Issues:**
   ```bash
   # Test VNPay refund API connectivity
   curl -I https://vnpay.vn/merchant_webapi/api/transaction
   ```

2. **Validation Errors:**
   ```sql
   -- Check order and payment status
   SELECT o.id, o.status, pt.status as payment_status 
   FROM orders o 
   JOIN payment_transactions pt ON o.id = pt.order_id 
   WHERE o.id = ?;
   ```

## Emergency Procedures

### 1. Service Outage Response

#### Complete Service Outage

1. **Immediate Actions (0-5 minutes):**
   ```bash
   # Check service status
   sudo systemctl status smartphone-management
   
   # Check recent logs
   tail -100 /var/log/smartphone-management/application.log
   
   # Restart service if needed
   sudo systemctl restart smartphone-management
   ```

2. **Escalation (5-15 minutes):**
   ```bash
   # If restart doesn't work, rollback to previous version
   sudo systemctl stop smartphone-management
   cp /opt/backups/smartphone-management-previous.jar /opt/smartphone-management.jar
   sudo systemctl start smartphone-management
   ```

3. **Communication (Within 15 minutes):**
   - Notify stakeholders via incident management system
   - Update status page
   - Prepare incident report

#### Partial Service Outage

1. **Disable Affected Features:**
   ```properties
   # Disable webhook processing
   app.payment.features.webhook-processing=false
   
   # Disable refund API
   app.payment.features.refund-api=false
   ```

2. **Monitor Recovery:**
   ```bash
   # Monitor error rates
   grep "ERROR" application.log | grep "$(date '+%Y-%m-%d %H:%M')" | wc -l
   ```

### 2. Security Incident Response

#### Suspected Attack on Webhook Endpoint

1. **Immediate Actions:**
   ```bash
   # Enable IP whitelist immediately
   app.payment.webhook.ip-whitelist=known_vnpay_ips_only
   
   # Monitor attack patterns
   grep "Signature verification failed" application.log | tail -50
   ```

2. **Block Malicious IPs:**
   ```bash
   # Extract attacking IPs
   grep "SECURITY WARNING" application.log | awk '{print $6}' | sort | uniq -c | sort -nr
   
   # Block top attacking IPs
   iptables -A INPUT -s attacking_ip -j DROP
   ```

3. **Incident Documentation:**
   - Document attack timeline
   - Preserve logs for analysis
   - Report to security team

### 3. Database Emergency Procedures

#### Database Connection Pool Exhaustion

1. **Immediate Relief:**
   ```properties
   # Increase pool size temporarily
   spring.datasource.hikari.maximum-pool-size=50
   spring.datasource.hikari.leak-detection-threshold=30000
   ```

2. **Identify Connection Leaks:**
   ```bash
   # Monitor connection pool metrics
   grep "HikariPool" application.log | tail -20
   
   # Check for long-running transactions
   mysql -e "SHOW PROCESSLIST;"
   ```

#### Database Performance Degradation

1. **Quick Fixes:**
   ```sql
   -- Kill long-running queries
   KILL QUERY process_id;
   
   -- Check for table locks
   SHOW OPEN TABLES WHERE In_use > 0;
   ```

2. **Performance Analysis:**
   ```sql
   -- Analyze slow queries
   SELECT * FROM information_schema.PROCESSLIST WHERE TIME > 30;
   
   -- Check index usage
   SHOW INDEX FROM payment_transactions;
   ```

## Maintenance Tasks

### Daily Tasks

1. **Log Review (10 minutes):**
   ```bash
   # Check error patterns
   grep "ERROR" application.log | grep "$(date '+%Y-%m-%d')" | head -20
   
   # Review security warnings
   grep "SECURITY WARNING" application.log | grep "$(date '+%Y-%m-%d')"
   
   # Check performance warnings
   grep "Slow.*operation detected" application.log | grep "$(date '+%Y-%m-%d')"
   ```

2. **Metrics Review (5 minutes):**
   ```bash
   # Transaction volume
   grep "Payment operation completed" application.log | grep "$(date '+%Y-%m-%d')" | wc -l
   
   # Webhook processing count
   grep "Webhook processing completed" application.log | grep "$(date '+%Y-%m-%d')" | wc -l
   
   # Refund operations
   grep "Refund operation" application.log | grep "$(date '+%Y-%m-%d')" | wc -l
   ```

### Weekly Tasks

1. **Performance Analysis (30 minutes):**
   ```bash
   # Generate weekly performance report
   ./scripts/generate-weekly-report.sh
   
   # Review database growth
   mysql -e "SELECT table_name, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)' FROM information_schema.tables WHERE table_schema = 'smart_phone' AND table_name IN ('payment_transactions', 'refund_transactions');"
   ```

2. **Configuration Review (15 minutes):**
   ```bash
   # Review configuration changes
   git log --oneline --since="1 week ago" -- application.properties
   
   # Validate current configuration
   grep -E "app\.payment\.|vnpay\." application.properties
   ```

### Monthly Tasks

1. **Security Audit (60 minutes):**
   ```bash
   # Review security logs
   grep "SECURITY WARNING" application.log | grep "$(date -d '1 month ago' '+%Y-%m')"
   
   # Check certificate expiration
   openssl x509 -in /path/to/certificate.crt -noout -dates
   
   # Review access patterns
   grep "sourceIp" application.log | awk '{print $NF}' | sort | uniq -c | sort -nr | head -20
   ```

2. **Capacity Planning (30 minutes):**
   ```bash
   # Analyze growth trends
   ./scripts/analyze-growth-trends.sh
   
   # Review resource utilization
   df -h
   free -h
   top -b -n 1 | head -20
   ```

## Performance Optimization

### Database Optimization

1. **Index Optimization:**
   ```sql
   -- Analyze index usage
   SELECT 
     table_name,
     index_name,
     cardinality,
     sub_part,
     packed,
     nullable,
     index_type
   FROM information_schema.statistics 
   WHERE table_schema = 'smart_phone' 
   AND table_name IN ('payment_transactions', 'refund_transactions');
   
   -- Add missing indexes if needed
   CREATE INDEX idx_payment_transactions_status_created 
   ON payment_transactions(status, created_at);
   ```

2. **Query Optimization:**
   ```sql
   -- Enable slow query log
   SET GLOBAL slow_query_log = 'ON';
   SET GLOBAL long_query_time = 2;
   
   -- Analyze slow queries
   SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
   ```

### Application Optimization

1. **Connection Pool Tuning:**
   ```properties
   # Optimize based on load patterns
   spring.datasource.hikari.maximum-pool-size=25
   spring.datasource.hikari.minimum-idle=5
   spring.datasource.hikari.idle-timeout=300000
   spring.datasource.hikari.max-lifetime=1200000
   ```

2. **Async Processing Optimization:**
   ```properties
   # Tune async thread pools
   app.payment.webhook.async-pool-size=15
   spring.task.execution.pool.core-size=10
   spring.task.execution.pool.max-size=30
   spring.task.execution.pool.queue-capacity=200
   ```

## Security Incident Response

### Incident Classification

#### Level 1 - Critical Security Incident
- Unauthorized access to payment data
- Successful signature validation bypass
- Data breach or exposure

**Response Time:** Immediate (< 15 minutes)

#### Level 2 - High Security Incident
- Multiple signature validation failures from same IP
- Suspicious webhook request patterns
- Potential brute force attacks

**Response Time:** < 1 hour

#### Level 3 - Medium Security Incident
- Unusual traffic patterns
- Configuration security warnings
- Minor security policy violations

**Response Time:** < 4 hours

### Incident Response Procedures

1. **Detection and Analysis:**
   ```bash
   # Collect evidence
   grep "SECURITY WARNING" application.log > security-incident-$(date +%Y%m%d-%H%M%S).log
   
   # Analyze attack patterns
   awk '{print $6}' security-incident-*.log | sort | uniq -c | sort -nr
   ```

2. **Containment:**
   ```bash
   # Block attacking IPs
   iptables -A INPUT -s attacking_ip -j DROP
   
   # Enable strict IP whitelist
   app.payment.webhook.ip-whitelist=trusted_ips_only
   ```

3. **Eradication and Recovery:**
   ```bash
   # Update security configurations
   # Patch vulnerabilities
   # Restore from clean backups if needed
   ```

4. **Post-Incident Activities:**
   - Document lessons learned
   - Update security procedures
   - Implement additional monitoring

## Contact Information

### Escalation Matrix

| Issue Type | Primary Contact | Secondary Contact | Escalation Time |
|------------|----------------|-------------------|-----------------|
| Service Outage | On-call Engineer | Engineering Manager | 15 minutes |
| Security Incident | Security Team | CISO | 30 minutes |
| Database Issues | DBA Team | Infrastructure Manager | 20 minutes |
| VNPay API Issues | Payment Team | External Relations | 1 hour |

### Emergency Contacts

- **On-call Engineer:** +1-xxx-xxx-xxxx
- **Engineering Manager:** +1-xxx-xxx-xxxx
- **Security Team:** security@company.com
- **VNPay Support:** vnpay-support@vnpay.vn

### Communication Channels

- **Incident Channel:** #incident-response
- **Payment Team:** #payment-team
- **Operations:** #ops-alerts
- **Status Updates:** #status-updates

---

*This runbook should be reviewed and updated quarterly to ensure accuracy and completeness.*