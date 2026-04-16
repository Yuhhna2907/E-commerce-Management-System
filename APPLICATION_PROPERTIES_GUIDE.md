# Application Properties Configuration Guide

This document explains the configuration properties added for the Product Detail UX Enhancements feature.

## File Upload Configuration

```properties
# Enable multipart file upload
spring.servlet.multipart.enabled=true

# Maximum file size per upload (5MB)
spring.servlet.multipart.max-file-size=5MB

# Maximum request size (25MB for multiple files)
spring.servlet.multipart.max-request-size=25MB

# File size threshold for writing to disk
spring.servlet.multipart.file-size-threshold=2KB

# Custom upload directory for review images
app.upload.review-images-dir=uploads/review-images

# Maximum file size in bytes (5MB = 5,242,880 bytes)
app.upload.max-file-size=5242880
```

**Usage:**
- Review images will be stored in `uploads/review-images/` directory
- Maximum 5MB per image file
- Supports JPEG, PNG, and WebP formats

## Email Configuration (SMTP)

```properties
# Gmail SMTP server configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587

# Email credentials (use environment variables for security)
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}

# SMTP authentication and TLS settings
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Connection timeouts (5 seconds)
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

**Setup Instructions:**

1. **For Gmail:**
   - Enable 2-factor authentication on your Google account
   - Generate an App Password: https://myaccount.google.com/apppasswords
   - Set environment variables:
     ```bash
     export MAIL_USERNAME=your-email@gmail.com
     export MAIL_PASSWORD=your-16-char-app-password
     ```

2. **For Other SMTP Providers:**
   - Update `spring.mail.host` and `spring.mail.port`
   - Adjust TLS/SSL settings as needed

## Email Retry Configuration

```properties
# Maximum retry attempts for failed email sends
app.email.retry.max-attempts=3

# Initial delay before first retry (1 second)
app.email.retry.initial-delay=1000

# Exponential backoff multiplier
app.email.retry.multiplier=2.0

# Maximum delay between retries (10 seconds)
app.email.retry.max-delay=10000
```

**Retry Strategy:**
- Attempt 1: Immediate
- Attempt 2: After 1 second
- Attempt 3: After 2 seconds (1s × 2.0)
- Attempt 4: After 4 seconds (2s × 2.0)

## Caching Configuration

```properties
# Cache type (simple in-memory, can be changed to Redis)
spring.cache.type=simple

# Cache names for different data types
spring.cache.cache-names=productRecommendations,coPurchasePatterns

# Caffeine cache specifications
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=24h
```

**Cache Details:**
- **productRecommendations**: Stores computed product recommendations
- **coPurchasePatterns**: Stores co-purchase frequency data
- **TTL**: 24 hours (refreshed daily by scheduled job)
- **Max Size**: 1000 entries per cache

**Upgrading to Redis:**
```properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## Async Execution Configuration

```properties
# Thread pool for async tasks
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
spring.task.execution.thread-name-prefix=async-task-
```

**Usage:**
- Email notifications are sent asynchronously
- Core pool: 5 threads always available
- Max pool: Up to 10 threads during high load
- Queue: 100 tasks can wait before rejection

## Scheduling Configuration

```properties
# Thread pool for scheduled tasks
spring.task.scheduling.pool.size=5
spring.task.scheduling.thread-name-prefix=scheduled-task-

# Cron expression for recommendation rebuild (2:00 AM daily)
app.scheduling.recommendation-rebuild-cron=0 0 2 * * ?
```

**Scheduled Jobs:**
- **Recommendation Rebuild**: Runs at 2:00 AM daily
- Recalculates co-purchase patterns from order history
- Updates recommendation cache

**Cron Expression Format:**
```
0 0 2 * * ?
│ │ │ │ │ │
│ │ │ │ │ └─ Day of week (? = any)
│ │ │ │ └─── Month (*)
│ │ │ └───── Day of month (*)
│ │ └─────── Hour (2 = 2 AM)
│ └───────── Minute (0)
└─────────── Second (0)
```

## Static Resources Configuration

```properties
# Serve static files from classpath and uploads directory
spring.web.resources.static-locations=classpath:/static/,file:uploads/

# URL pattern for static resources
spring.mvc.static-path-pattern=/**

# Enable static resource mappings
spring.web.resources.add-mappings=true
```

**Access URLs:**
- Uploaded review images: `http://localhost:8080/uploads/review-images/filename.jpg`
- Static CSS/JS: `http://localhost:8080/css/style.css`

## Environment-Specific Configuration

For production, create `application-prod.properties`:

```properties
# Production email settings
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# Production upload directory
app.upload.review-images-dir=/var/app/uploads/review-images

# Redis cache for production
spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# Larger thread pools for production
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=20
```

Activate with: `spring.profiles.active=prod`

## Security Considerations

1. **Never commit credentials** to version control
2. Use environment variables for sensitive data
3. Validate uploaded files thoroughly (done in ReviewImageService)
4. Set appropriate file permissions on upload directory:
   ```bash
   mkdir -p uploads/review-images
   chmod 755 uploads/review-images
   ```
5. Consider using cloud storage (S3, Azure Blob) for production

## Monitoring

Monitor these metrics in production:
- Cache hit/miss ratio
- Email send success/failure rate
- Async task queue size
- File upload success rate
- Scheduled job execution time

## Troubleshooting

**Email not sending:**
- Check SMTP credentials
- Verify firewall allows port 587
- Check application logs for retry attempts

**Cache not working:**
- Verify `@EnableCaching` is present
- Check cache names match in code
- Monitor cache statistics

**File upload fails:**
- Check directory permissions
- Verify disk space available
- Check file size limits

**Scheduled job not running:**
- Verify `@EnableScheduling` is present
- Check cron expression syntax
- Review application logs at scheduled time
