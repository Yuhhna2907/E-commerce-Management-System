# Q&A Service Refactoring - Complete ✅

## Ngày: April 15, 2026

## Tổng Quan
Đã hoàn thành refactoring `ProductQuestionService.java` để loại bỏ Spring Security dependencies và sử dụng mock user approach với `userId = 1L`.

---

## Thay Đổi Chính

### 1. ✅ Loại Bỏ Spring Security Dependencies
- **Removed imports:**
  - `com.codegym.smartphonemanagement.exception.UnauthorizedException`
  - `org.springframework.security.core.Authentication`
  - `org.springframework.security.core.context.SecurityContextHolder`

### 2. ✅ Cập Nhật Method Signatures
Tất cả các methods đã được cập nhật để sử dụng `Long userId` thay vì `User user`:

#### **createQuestion()**
```java
// BEFORE
public QuestionResponseDTO createQuestion(Long productId, QuestionRequestDTO requestDTO, User user)

// AFTER
public QuestionResponseDTO createQuestion(Long productId, QuestionRequestDTO requestDTO, Long userId)
```

#### **answerQuestion()**
```java
// BEFORE
public AnswerResponseDTO answerQuestion(Long questionId, AnswerRequestDTO requestDTO, User user)

// AFTER
public AnswerResponseDTO answerQuestion(Long questionId, AnswerRequestDTO requestDTO, Long userId)
```

#### **voteAnswer()**
```java
// BEFORE
public AnswerResponseDTO voteAnswer(Long answerId, VoteRequestDTO requestDTO, User user)

// AFTER
public AnswerResponseDTO voteAnswer(Long answerId, VoteRequestDTO requestDTO, Long userId)
```

### 3. ✅ Mock User Logic
Tất cả các methods giờ sử dụng logic giả lập user:

```java
// Get mock user (default userId = 1L)
User user = userRepository.findById(userId != null ? userId : 1L)
        .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
```

**Default behavior:**
- Nếu `userId` là `null` → sử dụng `userId = 1L`
- Nếu `userId` được cung cấp → sử dụng giá trị đó
- Nếu user không tồn tại trong database → throw `IllegalArgumentException`

### 4. ✅ Removed Methods
- **getCurrentUser()** - Method này sử dụng `SecurityContextHolder` nên đã bị xóa hoàn toàn

### 5. ✅ Kept Methods (No Changes)
Các methods sau vẫn giữ nguyên vì không liên quan đến authentication:
- `hasAnswerPermission(User user)` - Vẫn hữu ích để check role
- `getQuestions()` - Read-only, không cần authentication
- `getQuestionById()` - Read-only, không cần authentication
- `convertToQuestionDTO()` - Helper method
- `convertToAnswerDTO()` - Helper method
- `getUserRoleDisplay()` - Helper method
- `countQuestionsByProductId()` - Read-only
- `countUnansweredQuestionsByProductId()` - Read-only

---

## Testing Instructions

### 1. Test với Mock User (userId = 1L)

#### Create Question
```java
QuestionRequestDTO request = new QuestionRequestDTO();
request.setQuestionText("Sản phẩm này có bảo hành không?");

// Sử dụng userId = 1L (mock user)
QuestionResponseDTO response = questionService.createQuestion(productId, request, 1L);
```

#### Answer Question
```java
AnswerRequestDTO request = new AnswerRequestDTO();
request.setAnswerText("Có, sản phẩm được bảo hành 12 tháng.");

// Sử dụng userId = 1L (mock user)
AnswerResponseDTO response = questionService.answerQuestion(questionId, request, 1L);
```

#### Vote Answer
```java
VoteRequestDTO request = new VoteRequestDTO();
request.setIsHelpful(true);

// Sử dụng userId = 1L (mock user)
AnswerResponseDTO response = questionService.voteAnswer(answerId, request, 1L);
```

### 2. Test với Null UserId (Default to 1L)
```java
// Tất cả sẽ default về userId = 1L
questionService.createQuestion(productId, request, null);
questionService.answerQuestion(questionId, request, null);
questionService.voteAnswer(answerId, request, null);
```

### 3. Test với Different UserIds
```java
// Test với user khác
questionService.createQuestion(productId, request, 2L);
questionService.answerQuestion(questionId, request, 3L);
questionService.voteAnswer(answerId, request, 4L);
```

---

## Controller Integration

Controllers cần được cập nhật để truyền `userId` thay vì `User` object:

### Example Controller Method
```java
@PostMapping("/products/{productId}/questions")
public ResponseEntity<QuestionResponseDTO> createQuestion(
        @PathVariable Long productId,
        @RequestBody QuestionRequestDTO request) {
    
    // Mock user với ID = 1L
    Long userId = 1L;
    
    QuestionResponseDTO response = questionService.createQuestion(productId, request, userId);
    return ResponseEntity.ok(response);
}
```

---

## Compilation Status
✅ **No compilation errors**
- Verified with `getDiagnostics` tool
- All imports resolved correctly
- All method signatures updated consistently

---

## Next Steps

### ~~1. Update Controllers~~ ✅ DONE
- ~~[ ] `ProductQuestionController.java` - Update all endpoints to pass `userId` instead of `User`~~
- ~~[ ] Remove Spring Security dependencies from controllers~~
- ~~[ ] Update request mappings if needed~~

### 2. Update DTOs (if needed)
- [ ] Verify all DTOs are compatible with new service signatures
- [ ] Update DTO documentation

### 3. Integration Testing
- [ ] Test create question flow
- [ ] Test answer question flow
- [ ] Test vote answer flow
- [ ] Test with different user IDs
- [ ] Test with null userId (should default to 1L)

### 4. Frontend Integration
- [ ] Update JavaScript to pass userId in API calls
- [ ] Update QASection.js component
- [ ] Test all Q&A features in browser

---

## Files Modified
1. `E-commerce-Management-System/src/main/java/com/codegym/smartphonemanagement/service/qa/ProductQuestionService.java`
2. `E-commerce-Management-System/src/main/java/com/codegym/smartphonemanagement/exception/QAExceptionHandler.java`
3. `E-commerce-Management-System/src/main/java/com/codegym/smartphonemanagement/controller/user/ProductQuestionController.java`

## Files Created
1. `E-commerce-Management-System/QA_SERVICE_REFACTORING_COMPLETE.md` (this file)

---

## Controller Refactoring

### ✅ Updated ProductQuestionController
All controller methods now pass `Long userId` instead of `User` object:

#### **Changes Made:**

1. **createQuestion()** - Now passes `getCurrentUserId()` (returns 1L) instead of `getCurrentUser()`
2. **answerQuestion()** - Now passes `getCurrentUserId()` (returns 1L) instead of `getCurrentUser()`
3. **voteAnswer()** - Now passes `getCurrentUserId()` (returns 1L) instead of `getCurrentUser()`

#### **Removed:**
- `getCurrentUser()` method - No longer needed
- `UserRepository` dependency - No longer needed
- `User` import - No longer needed

#### **Kept:**
- `getCurrentUserId()` method - Returns hardcoded `HARDCODED_USER_ID = 1L`
- `UnauthorizedException` import - Still used in exception handling

### ✅ Simplified Controller Logic
```java
// BEFORE
User currentUser = getCurrentUser();
if (currentUser == null) {
    throw new UnauthorizedException("...");
}
questionService.createQuestion(productId, requestDTO, currentUser);

// AFTER
Long currentUserId = getCurrentUserId(); // Returns 1L
questionService.createQuestion(productId, requestDTO, currentUserId);
```

---

## Exception Handler Refactoring

### ✅ Removed Spring Security Exception Handlers
Vì đang giả lập user nên đã loại bỏ các exception handlers sau:

1. **UnauthorizedException** - Không cần vì không có authentication check
2. **AccessDeniedException** - Không cần vì không có role-based access control
3. **AuthenticationException** - Không cần vì không có Spring Security

### ✅ Kept Exception Handlers (Still Useful)
Các exception handlers sau vẫn được giữ lại:

1. **MethodArgumentNotValidException** - Validation cho @Valid annotations
2. **IllegalArgumentException** - Business logic errors (e.g., user không tồn tại, sản phẩm không tồn tại)
3. **ResourceNotFoundException** - Khi không tìm thấy question/answer
4. **BadRequestException** - General bad requests
5. **Exception** - Catch-all cho unexpected errors

---

## Summary
✅ Đã hoàn thành refactoring ProductQuestionService
✅ Đã hoàn thành refactoring QAExceptionHandler
✅ Đã hoàn thành refactoring ProductQuestionController
✅ Loại bỏ tất cả Spring Security dependencies
✅ Sử dụng mock user approach với userId = 1L
✅ Không có compilation errors
✅ Tất cả methods đã được cập nhật nhất quán

**Status: READY FOR TESTING** 🚀
