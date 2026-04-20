# Q&A Section Integration Guide

## Overview

The QASection.js component provides a complete Q&A system for product detail pages with the following features:

- Load and display questions with sorting (recent/helpful)
- Question submission modal (requires authentication)
- Answer submission modal (SELLER/ADMIN only)
- Vote buttons with active state tracking
- Pagination support
- Responsive design for mobile and desktop

## Files Created

1. **JavaScript Component**: `/static/js/QASection.js`
2. **CSS Styles**: `/static/css/qa-section.css`

## Integration Steps

### 1. Add CSS to Product Detail Page

Add the CSS file to your product detail template (`detail.html`):

```html
<link rel="stylesheet" th:href="@{/css/qa-section.css}">
```

### 2. Add JavaScript to Product Detail Page

Add the JavaScript file before the closing `</body>` tag:

```html
<script th:src="@{/js/QASection.js}"></script>
```

### 3. Add Container Element

Add a container element where the Q&A section will be rendered:

```html
<div id="qa-section-container"></div>
```

### 4. Initialize the Component

Initialize the QASection component with the required configuration:

```html
<script th:inline="javascript">
    document.addEventListener('DOMContentLoaded', function() {
        const container = document.getElementById('qa-section-container');
        const productId = /*[[${product.id}]]*/ 0;
        
        // Get authentication status from Thymeleaf
        const isAuthenticated = /*[[${#authorization.expression('isAuthenticated()')}]]*/ false;
        
        // Get user role (if authenticated)
        let userRole = null;
        /*[# th:if="${#authorization.expression('hasRole(\'SELLER\')')}"]*/
            userRole = 'SELLER';
        /*[/]*/
        /*[# th:if="${#authorization.expression('hasRole(\'ADMIN\')')}"]*/
            userRole = 'ADMIN';
        /*[/]*/
        
        // Get CSRF token
        const csrfToken = /*[[${_csrf.token}]]*/ null;
        
        // Initialize Q&A Section
        const qaSection = new QASection(container, productId, {
            pageSize: 10,
            sortBy: 'recent',
            isAuthenticated: isAuthenticated,
            currentUserRole: userRole,
            csrfToken: csrfToken
        });
    });
</script>
```

## Configuration Options

The QASection constructor accepts the following options:

```javascript
{
    pageSize: 10,              // Number of questions per page (default: 10)
    sortBy: 'recent',          // Initial sort order: 'recent' or 'helpful' (default: 'recent')
    isAuthenticated: false,    // Whether user is logged in (default: false)
    currentUserRole: null,     // User role: 'SELLER', 'ADMIN', or null (default: null)
    csrfToken: null           // CSRF token for POST requests (default: null)
}
```

## API Endpoints Used

The component interacts with the following API endpoints:

1. **GET** `/api/products/{productId}/questions`
   - Query params: `sortBy`, `page`, `size`
   - Returns: Paginated list of questions with answers

2. **POST** `/api/products/{productId}/questions`
   - Body: `{ questionText: string }`
   - Requires: Authentication
   - Returns: Created question

3. **POST** `/api/products/questions/{questionId}/answers`
   - Body: `{ answerText: string }`
   - Requires: SELLER or ADMIN role
   - Returns: Created answer

4. **POST** `/api/products/answers/{answerId}/vote`
   - Body: `{ isHelpful: boolean }`
   - Requires: Authentication
   - Returns: Updated answer with vote counts

## Features

### 1. Question List Display

- Shows questions with user info and timestamps
- Displays answers with role badges (Seller/Admin)
- Shows vote counts for each answer
- Empty state when no questions exist
- Error state with retry option

### 2. Sorting

- **Recent**: Sort by creation date (newest first)
- **Helpful**: Sort by total helpful votes (most helpful first)

### 3. Question Submission

- Modal dialog with textarea (10-500 characters)
- Character counter
- Client-side validation
- Success/error feedback
- Requires authentication

### 4. Answer Submission

- Modal dialog with textarea (10-1000 characters)
- Character counter
- Client-side validation
- Success/error feedback
- Only available to SELLER and ADMIN roles

### 5. Voting System

- Helpful/Not Helpful buttons
- Active state tracking (shows user's current vote)
- Real-time vote count updates
- One vote per user per answer
- Requires authentication

### 6. Pagination

- Shows page numbers with prev/next buttons
- Smooth scroll to top on page change
- Hides when only one page exists

## Responsive Design

The component is fully responsive:

- **Desktop (≥768px)**: Full layout with side-by-side buttons
- **Tablet (768px)**: Adjusted spacing and button sizes
- **Mobile (<576px)**: Stacked layout, full-width buttons

## Security Features

- XSS prevention through HTML escaping
- CSRF token support for all POST requests
- Role-based access control for answers
- Authentication checks for all user actions

## Accessibility

- Semantic HTML structure
- ARIA labels for interactive elements
- Keyboard navigation support
- Focus indicators for all interactive elements
- Screen reader friendly

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Dependencies

- **Bootstrap 5**: For modals, buttons, and form components
- **Bootstrap Icons**: For UI icons
- **Fetch API**: For AJAX requests (native browser support)

## Example Usage in detail.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <!-- Other head content -->
    <link rel="stylesheet" th:href="@{/css/qa-section.css}">
</head>
<body>
    <!-- Product details content -->
    
    <!-- Q&A Section Container -->
    <div class="container mt-5">
        <div id="qa-section-container"></div>
    </div>
    
    <!-- Scripts -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script th:src="@{/js/QASection.js}"></script>
    
    <script th:inline="javascript">
        document.addEventListener('DOMContentLoaded', function() {
            const container = document.getElementById('qa-section-container');
            const productId = /*[[${product.id}]]*/ 0;
            const isAuthenticated = /*[[${#authorization.expression('isAuthenticated()')}]]*/ false;
            
            let userRole = null;
            /*[# th:if="${#authorization.expression('hasRole(\'SELLER\')')}"]*/
                userRole = 'SELLER';
            /*[/]*/
            /*[# th:if="${#authorization.expression('hasRole(\'ADMIN\')')}"]*/
                userRole = 'ADMIN';
            /*[/]*/
            
            const csrfToken = /*[[${_csrf.token}]]*/ null;
            
            new QASection(container, productId, {
                pageSize: 10,
                sortBy: 'recent',
                isAuthenticated: isAuthenticated,
                currentUserRole: userRole,
                csrfToken: csrfToken
            });
        });
    </script>
</body>
</html>
```

## Troubleshooting

### Questions not loading

1. Check browser console for errors
2. Verify API endpoint is accessible
3. Check product ID is valid
4. Verify backend controller is running

### Cannot submit questions/answers

1. Verify user is authenticated
2. Check CSRF token is present
3. Verify user has correct role (for answers)
4. Check backend validation rules

### Votes not updating

1. Verify user is authenticated
2. Check CSRF token is present
3. Verify answer ID is valid
4. Check network tab for API errors

## Next Steps

After integration, you should:

1. Test all features with different user roles
2. Verify responsive design on mobile devices
3. Test pagination with large datasets
4. Verify email notifications are sent (backend)
5. Add analytics tracking if needed

## Related Tasks

- Task 8.5: Add CSS styling for Q&A section (completed as part of this task)
- Task 7.4: EmailNotificationService (backend - should be completed)
- Task 8.1: ProductQuestionController (backend - should be completed)
