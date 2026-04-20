# VNPay Refund API Documentation

## Overview

This document describes the VNPay refund API endpoints that enable full and partial refunds for completed payments. The API provides refund request creation and status tracking capabilities.

## Base URL

```
Production: https://yourdomain.com/api/payment
Staging: https://staging.yourdomain.com/api/payment
Development: http://localhost:8080/api/payment
```

## Authentication

All refund API endpoints require authentication. Include the appropriate authentication headers in your requests.

## Endpoints

### 1. Create Refund Request

Creates a new refund request for a completed payment.

#### Request

```http
POST /api/payment/refund
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "orderId": 12345,
  "refundAmount": 50000,
  "refundReason": "Customer requested refund"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | Long | Yes | The ID of the order to refund |
| `refundAmount` | BigDecimal | Yes | Amount to refund (in VND, without decimal) |
| `refundReason` | String | Yes | Reason for the refund (max 255 characters) |

#### Response

**Success Response (HTTP 200)**

```json
{
  "success": true,
  "data": {
    "refundRequestId": "ORD12345_REF1640995200000",
    "orderId": 12345,
    "refundAmount": 50000,
    "originalAmount": 100000,
    "refundType": "PARTIAL",
    "status": "PENDING",
    "reason": "Customer requested refund",
    "createdAt": "2024-01-01T10:00:00Z",
    "vnpayRefundTransactionNo": null
  },
  "message": "Refund request created successfully"
}
```

**Error Response (HTTP 400)**

```json
{
  "success": false,
  "error": "Refund Error",
  "message": "Refund amount exceeds the original payment amount",
  "orderId": 12345,
  "timestamp": "2024-01-01T10:00:00Z"
}
```

#### Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `INVALID_AMOUNT` | Refund amount is invalid or exceeds original amount |
| 404 | `ORDER_NOT_FOUND` | Order not found or not eligible for refund |
| 400 | `PAYMENT_NOT_COMPLETED` | Original payment is not in completed status |
| 400 | `ALREADY_REFUNDED` | Order has already been fully refunded |
| 500 | `VNPAY_API_ERROR` | Error communicating with VNPay API |
| 503 | `SERVICE_UNAVAILABLE` | Refund service is temporarily unavailable |

### 2. Check Refund Status

Retrieves the current status of a refund request.

#### Request

```http
GET /api/payment/refund/{refundRequestId}/status
Authorization: Bearer {token}
```

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `refundRequestId` | String | Yes | The refund request ID returned from create refund |

#### Response

**Success Response (HTTP 200)**

```json
{
  "success": true,
  "data": {
    "refundRequestId": "ORD12345_REF1640995200000",
    "orderId": 12345,
    "refundAmount": 50000,
    "status": "COMPLETED",
    "reason": "Customer requested refund",
    "createdAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:05:00Z",
    "vnpayRefundTransactionNo": "VNP123456789",
    "vnpayResponseCode": "00",
    "vnpayMessage": "Refund successful"
  },
  "message": "Refund status retrieved successfully"
}
```

**Error Response (HTTP 404)**

```json
{
  "success": false,
  "error": "Not Found",
  "message": "Refund request not found",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

## Data Models

### RefundTransaction

```json
{
  "id": 1,
  "orderId": 12345,
  "refundAmount": 50000,
  "originalTransactionNo": "VNP987654321",
  "refundRequestId": "ORD12345_REF1640995200000",
  "status": "COMPLETED",
  "reason": "Customer requested refund",
  "createdAt": "2024-01-01T10:00:00Z",
  "completedAt": "2024-01-01T10:05:00Z",
  "vnpayRefundTransactionNo": "VNP123456789",
  "responseCode": "00",
  "rawResponse": "{\"vnp_ResponseCode\":\"00\",\"vnp_Message\":\"Success\"}"
}
```

### Refund Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Refund request created, waiting for processing |
| `PROCESSING` | Refund is being processed by VNPay |
| `COMPLETED` | Refund completed successfully |
| `FAILED` | Refund failed |

### Refund Type Values

| Type | Description |
|------|-------------|
| `FULL` | Full refund (refund amount equals original amount) |
| `PARTIAL` | Partial refund (refund amount less than original amount) |

## Usage Examples

### Example 1: Full Refund

```bash
# Create full refund
curl -X POST https://yourdomain.com/api/payment/refund \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "orderId": 12345,
    "refundAmount": 100000,
    "refundReason": "Order cancelled by customer"
  }'

# Check refund status
curl -X GET https://yourdomain.com/api/payment/refund/ORD12345_REF1640995200000/status \
  -H "Authorization: Bearer your-token"
```

### Example 2: Partial Refund

```bash
# Create partial refund
curl -X POST https://yourdomain.com/api/payment/refund \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "orderId": 12345,
    "refundAmount": 30000,
    "refundReason": "Partial return - 1 item returned"
  }'
```

### Example 3: JavaScript/TypeScript

```typescript
interface RefundRequest {
  orderId: number;
  refundAmount: number;
  refundReason: string;
}

interface RefundResponse {
  success: boolean;
  data?: {
    refundRequestId: string;
    orderId: number;
    refundAmount: number;
    status: string;
    // ... other fields
  };
  message: string;
}

async function createRefund(request: RefundRequest): Promise<RefundResponse> {
  const response = await fetch('/api/payment/refund', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(request)
  });
  
  return await response.json();
}

async function checkRefundStatus(refundRequestId: string): Promise<RefundResponse> {
  const response = await fetch(`/api/payment/refund/${refundRequestId}/status`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
}
```

## Business Rules

### Refund Eligibility

1. **Order Status**: Order must have completed payment status
2. **Payment Method**: Only VNPay payments are eligible for API refunds
3. **Time Limit**: Refunds must be requested within VNPay's allowed timeframe
4. **Amount Validation**: Refund amount cannot exceed original payment amount
5. **Multiple Refunds**: Multiple partial refunds are allowed until full amount is refunded

### Refund Processing

1. **Validation**: System validates order eligibility and refund amount
2. **VNPay API Call**: Refund request is submitted to VNPay
3. **Status Tracking**: Refund status is tracked and updated automatically
4. **Order Update**: Order payment status is updated when refund completes
5. **Audit Trail**: All refund operations are logged for audit purposes

## Error Handling

### Retry Logic

The refund API includes automatic retry logic for transient failures:
- **Network Timeouts**: Automatically retried up to 3 times
- **VNPay Server Errors**: Retried with exponential backoff
- **Client Errors**: Not retried (400-level errors)

### Idempotency

Refund requests are idempotent based on the generated refund request ID. Duplicate requests with the same parameters will return the existing refund status.

## Rate Limiting

- **Rate Limit**: 100 requests per minute per authenticated user
- **Burst Limit**: 10 requests per second
- **Headers**: Rate limit information is included in response headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995260
```

## Monitoring and Logging

### Request Logging

All refund API requests are logged with:
- Request timestamp and duration
- User authentication details
- Request parameters (sensitive data masked)
- Response status and error details

### Metrics

Key metrics tracked:
- Refund request rate
- Refund success rate
- Average processing time
- VNPay API response times

## Security Considerations

### Authentication

- All endpoints require valid authentication token
- Tokens should be transmitted over HTTPS only
- Implement proper token expiration and refresh

### Authorization

- Users can only access refunds for orders they have permission to view
- Admin users can access all refund operations
- Audit logging tracks all access attempts

### Data Protection

- Sensitive payment data is masked in logs
- Refund reasons are stored securely
- PCI DSS compliance maintained for payment data

## Support

For technical support or questions about the refund API:

- **Documentation**: This document and inline API documentation
- **Support Email**: api-support@yourdomain.com
- **Developer Portal**: https://developer.yourdomain.com
- **Status Page**: https://status.yourdomain.com

## Changelog

### Version 1.0.0 (2024-01-01)
- Initial release of refund API
- Support for full and partial refunds
- VNPay integration with retry logic
- Comprehensive error handling and logging