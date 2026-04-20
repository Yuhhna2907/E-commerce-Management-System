# File Upload Exception Handling Implementation

## Overview
This document describes the exception handling implementation for file upload operations in the Review Images Upload System.

## Components Implemented

### 1. InvalidFileException (Already Existed)
**Location:** `src/main/java/com/codegym/smartphonemanagement/exception/InvalidFileException.java`

**Purpose:** Custom exception thrown when file validation fails

**Features:**
- Extends `RuntimeException`
- Supports message-only constructor
- Supports message + cause constructor

**Usage Examples:**
```java
throw new InvalidFileException("File không hợp lệ");
throw new InvalidFileException("Lỗi xử lý file", ioException);
```

### 2. FileUploadExceptionHandler (NEW)
**Location:** `src/main/java/com/codegym/smartphonemanagement/exception/FileUploadExceptionHandler.java`

**Purpose:** Global exception handler for file upload operations using `@ControllerAdvice`

**Handles Three Exception Types:**

#### a) InvalidFileException
- **HTTP Status:** 400 BAD_REQUEST
- **Triggered When:** File validation fails (size, type, dimensions, etc.)
- **Response Format:**
```json
{
  "timestamp": "2026-04-14T10:30:00",
  "status": 400,
  "error": "Invalid File",
  "message": "Kích thước file vượt quá 5MB"
}
```

#### b) MaxUploadSizeExceededException
- **HTTP Status:** 413 PAYLOAD_TOO_LARGE
- **Triggered When:** File size exceeds Spring's configured limit
- **Response Format:**
```json
{
  "timestamp": "2026-04-14T10:30:00",
  "status": 413,
  "error": "File Too Large",
  "message": "Kích thước file vượt quá giới hạn cho phép. Vui lòng chọn file nhỏ hơn 5MB."
}
```

#### c) IOException
- **HTTP Status:** 500 INTERNAL_SERVER_ERROR
- **Triggered When:** File I/O operations fail
- **Response Format:**
```json
{
  "timestamp": "2026-04-14T10:30:00",
  "status": 500,
  "error": "File Processing Error",
  "message": "Lỗi khi xử lý file. Vui lòng thử lại sau."
}
```

## Configuration

### Application Properties
**Location:** `src/main/resources/application.properties`

```properties
# FILE UPLOAD CONFIGURATION
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=25MB
spring.servlet.multipart.file-size-threshold=2KB
app.upload.review-images-dir=uploads/review-images
app.upload.max-file-size=5242880
```

### Key Settings:
- **max-file-size:** 5MB per file
- **max-request-size:** 25MB total (allows 5 files × 5MB)
- **file-size-threshold:** 2KB (files larger than this are written to disk)

## Integration with ReviewImageService

The `ReviewImageService` throws `InvalidFileException` for various validation failures:

### Validation Checks:
1. **File Count:** Max 5 images per review
2. **File Size:** Max 5MB per file
3. **File Type:** Only JPEG, PNG, WebP
4. **MIME Type:** Validates Content-Type header
5. **File Signature:** Validates magic bytes (security)
6. **Executable Check:** Blocks .exe, .bat, .sh, .dll, .so
7. **Image Dimensions:** 100×100 to 4096×4096 pixels
8. **Path Traversal:** Blocks "..", "/", "\" in filenames

### Example Service Code:
```java
if (file.getSize() > maxFileSize) {
    throw new InvalidFileException(
        String.format("Kích thước file '%s' vượt quá giới hạn %d MB", 
            file.getOriginalFilename(), maxFileSize / 1024 / 1024)
    );
}
```

## Integration with ReviewImageController

The controller has local exception handling that works alongside the global handler:

```java
@PostMapping("/{reviewId}/images")
@ResponseBody
public ResponseEntity<Map<String, Object>> uploadImages(
        @PathVariable Long reviewId,
        @RequestParam("images") List<MultipartFile> files) {
    try {
        List<ReviewImage> uploadedImages = reviewImageService.uploadReviewImages(reviewId, files);
        // ... success response
    } catch (InvalidFileException e) {
        // Local handling (optional)
        response.put("success", false);
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
```

**Note:** The global `FileUploadExceptionHandler` serves as a fallback for any unhandled exceptions.

## Testing

### Unit Tests
**Location:** `src/test/java/com/codegym/smartphonemanagement/exception/FileUploadExceptionHandlerTest.java`

**Test Coverage:**
- ✅ `testHandleInvalidFileException()` - Validates 400 response
- ✅ `testHandleMaxUploadSizeExceededException()` - Validates 413 response
- ✅ `testHandleIOException()` - Validates 500 response
- ✅ `testInvalidFileExceptionWithCause()` - Validates exception chaining

### Running Tests:
```bash
./gradlew test --tests "com.codegym.smartphonemanagement.exception.FileUploadExceptionHandlerTest"
```

## Error Flow Diagram

```
User Upload Request
       ↓
ReviewImageController
       ↓
ReviewImageService.uploadReviewImages()
       ↓
Validation Checks
       ↓
   [FAIL] → throw InvalidFileException
       ↓
FileUploadExceptionHandler.handleInvalidFileException()
       ↓
Return 400 BAD_REQUEST with error details
```

## Security Considerations

### 1. File Type Validation
- Validates both extension and MIME type
- Checks file signature (magic bytes)
- Blocks executable files

### 2. Size Limits
- Per-file limit: 5MB
- Total request limit: 25MB
- Prevents DoS attacks

### 3. Path Traversal Prevention
- Blocks "..", "/", "\" in filenames
- Generates unique filenames with UUID

### 4. Error Message Safety
- Does not expose internal paths
- Provides user-friendly Vietnamese messages
- Logs detailed errors server-side only

## Requirements Satisfied

This implementation satisfies the following requirements from the spec:

- **Yêu cầu 3.7:** Error handling for file upload failures
- **Yêu cầu 12.4:** Security validation and error handling

### Acceptance Criteria Met:
✅ Custom `InvalidFileException` created  
✅ `FileUploadExceptionHandler` with `@ControllerAdvice` implemented  
✅ Handles `InvalidFileException` → 400 BAD_REQUEST  
✅ Handles `MaxUploadSizeExceededException` → 413 PAYLOAD_TOO_LARGE  
✅ Handles `IOException` → 500 INTERNAL_SERVER_ERROR  
✅ Provides specific error messages in Vietnamese  
✅ Logs errors appropriately  
✅ Unit tests created and passing  

## Future Enhancements

1. **Internationalization (i18n):** Support multiple languages
2. **Rate Limiting:** Prevent abuse of upload endpoints
3. **Virus Scanning:** Integrate with antivirus service
4. **Image Optimization:** Auto-resize/compress large images
5. **CDN Integration:** Upload to cloud storage (S3, Azure Blob)

## Maintenance Notes

- Exception messages are in Vietnamese for user-facing errors
- Log messages include English for developer debugging
- All exceptions are logged with appropriate levels (WARN for validation, ERROR for system errors)
- Response format is consistent across all exception types

## Related Files

- `InvalidFileException.java` - Custom exception
- `FileUploadExceptionHandler.java` - Global exception handler
- `ReviewImageService.java` - Service that throws exceptions
- `ReviewImageController.java` - Controller with local exception handling
- `application.properties` - Multipart configuration
- `FileUploadExceptionHandlerTest.java` - Unit tests

---

**Implementation Date:** April 14, 2026  
**Task:** 2.5 Tạo exception handlers cho file upload  
**Status:** ✅ Completed
