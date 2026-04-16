# Recommendation Carousel Component Guide

## Overview

The **RecommendationCarousel** component displays a "Khách Hàng Cũng Mua" (Customers Also Bought) carousel on the product detail page. It loads product recommendations from the backend API and provides an interactive, responsive carousel interface.

## Features

✅ **API Integration**: Automatically loads recommendations from `/api/products/{productId}/recommendations`  
✅ **Responsive Design**: 4 cards on desktop, 3 on tablet, 2 on mobile  
✅ **Touch Support**: Swipe gestures for mobile navigation  
✅ **Keyboard Navigation**: Arrow keys for accessibility  
✅ **Auto-scroll**: Optional automatic carousel rotation  
✅ **Loading States**: Loading, error, and empty state handling  
✅ **Product Cards**: Display image, name, brand, price, rating, and stock status  

## Requirements Fulfilled

- **Yêu cầu 5.2**: Display "Customers Also Bought" section with 4-6 recommendations
- **Yêu cầu 5.7**: Carousel with horizontal scrolling and navigation
- **Yêu cầu 13.2**: Responsive design with touch swipe support

## File Structure

```
E-commerce-Management-System/
├── src/main/resources/
│   ├── static/
│   │   ├── js/
│   │   │   └── RecommendationCarousel.js       # Main component
│   │   ├── css/
│   │   │   └── recommendation-carousel.css     # Styling
│   │   └── test-recommendation-carousel.html   # Test page
│   └── templates/user/product/
│       └── detail.html                         # Integration
```

## Usage

### Basic Integration

```html
<!-- Add CSS -->
<link th:href="@{/css/recommendation-carousel.css}" rel="stylesheet">

<!-- Add container -->
<div id="recommendation-section"></div>

<!-- Add JavaScript -->
<script th:src="@{/js/RecommendationCarousel.js}"></script>
<script th:inline="javascript">
    const productId = /*[[${product.id}]]*/ 1;
    const container = document.getElementById('recommendation-section');
    
    const carousel = new RecommendationCarousel(container, productId, {
        showRating: true,
        showPrice: true,
        autoScroll: false
    });
</script>
```

### Configuration Options

```javascript
new RecommendationCarousel(container, productId, {
    // API endpoint (default: /api/products/{productId}/recommendations)
    apiEndpoint: '/api/products/1/recommendations',
    
    // Enable auto-scroll (default: false)
    autoScroll: true,
    
    // Auto-scroll interval in milliseconds (default: 5000)
    autoScrollInterval: 5000,
    
    // Show product rating (default: true)
    showRating: true,
    
    // Show product price (default: true)
    showPrice: true
});
```

## API Response Format

The component expects the following JSON response from the API:

```json
{
    "success": true,
    "productId": 1,
    "recommendations": [
        {
            "id": 2,
            "name": "iPhone 15 Pro Max",
            "brand": "Apple",
            "price": 29990000,
            "imageUrl": "/images/products/iphone-15-pro-max.jpg",
            "averageRating": 4.8,
            "totalReviews": 156,
            "stock": 50,
            "sold": 234
        }
    ],
    "count": 4,
    "hasRecommendations": true
}
```

## Responsive Breakpoints

| Screen Size | Cards Displayed | Navigation |
|-------------|----------------|------------|
| Desktop (≥1200px) | 4 cards | Buttons + Keyboard |
| Desktop (992-1199px) | 3 cards | Buttons + Keyboard |
| Tablet (768-991px) | 2 cards | Buttons + Touch |
| Mobile (<768px) | 2 cards | Buttons + Touch |

## Methods

### Public Methods

```javascript
// Refresh recommendations
carousel.refresh();

// Navigate to previous items
carousel.prev();

// Navigate to next items
carousel.next();

// Start auto-scroll
carousel.startAutoScroll();

// Stop auto-scroll
carousel.stopAutoScroll();

// Destroy carousel and cleanup
carousel.destroy();
```

## Events

The carousel handles the following events:

- **Click**: Navigation buttons
- **Touch**: Swipe gestures (left/right)
- **Keyboard**: Arrow keys (left/right)
- **Resize**: Automatic responsive adjustment
- **Hover**: Pause auto-scroll (if enabled)

## Styling Customization

### CSS Variables

You can customize the carousel appearance by overriding CSS variables:

```css
.recommendation-carousel {
    --carousel-gap: 16px;
    --card-border-radius: 8px;
    --card-hover-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    --button-size: 40px;
    --transition-duration: 0.4s;
}
```

### Custom Styles

```css
/* Custom card styling */
.recommendation-card {
    border: 2px solid #e0e0e0;
}

.recommendation-card:hover {
    border-color: #007bff;
    transform: translateY(-8px);
}

/* Custom button styling */
.carousel-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
}
```

## Testing

### Manual Testing

1. Open the test page: `http://localhost:8080/test-recommendation-carousel.html`
2. Test different product IDs
3. Test navigation buttons
4. Test touch swipe on mobile
5. Test responsive behavior by resizing window
6. Check browser console for errors

### Test Checklist

- [ ] Carousel loads recommendations successfully
- [ ] Navigation buttons work (prev/next)
- [ ] Touch swipe works on mobile
- [ ] Keyboard navigation works (arrow keys)
- [ ] Responsive design adapts to screen size
- [ ] Loading state displays correctly
- [ ] Error state displays when API fails
- [ ] Empty state displays when no recommendations
- [ ] Product cards display all information
- [ ] Links navigate to product detail pages
- [ ] Auto-scroll works (if enabled)
- [ ] Hover pauses auto-scroll

## Troubleshooting

### Carousel doesn't load

**Problem**: Carousel container is empty  
**Solution**: Check browser console for errors, verify API endpoint is correct

### API returns 404

**Problem**: Recommendation endpoint not found  
**Solution**: Verify RecommendationController is running and endpoint is `/api/products/{id}/recommendations`

### Images don't display

**Problem**: Product images show broken image icon  
**Solution**: Check `imageUrl` in API response, verify image files exist in `/images/products/`

### Navigation buttons don't work

**Problem**: Clicking prev/next has no effect  
**Solution**: Check if there are enough recommendations to navigate (need more than cardsPerView)

### Touch swipe doesn't work

**Problem**: Swipe gestures not detected  
**Solution**: Verify touch events are not blocked by other elements, check z-index

## Performance Optimization

### Lazy Loading

Images use `loading="lazy"` attribute for better performance:

```javascript
<img src="${imageUrl}" loading="lazy" />
```

### Caching

The backend API uses caching for recommendations:

```java
@Cacheable(value = "productRecommendations", key = "#productId")
public List<Product> getRecommendations(Long productId) {
    // ...
}
```

### Debouncing

Window resize events are debounced to prevent excessive recalculations.

## Accessibility

### ARIA Labels

```html
<button class="carousel-btn carousel-prev" aria-label="Previous">
<button class="carousel-btn carousel-next" aria-label="Next">
```

### Keyboard Navigation

- **Arrow Left**: Navigate to previous items
- **Arrow Right**: Navigate to next items
- **Tab**: Focus navigation buttons
- **Enter/Space**: Activate focused button

### Screen Reader Support

- Product cards have descriptive alt text
- Navigation buttons have aria-labels
- Loading/error states are announced

## Browser Support

| Browser | Version | Support |
|---------|---------|---------|
| Chrome | 90+ | ✅ Full |
| Firefox | 88+ | ✅ Full |
| Safari | 14+ | ✅ Full |
| Edge | 90+ | ✅ Full |
| Mobile Safari | 14+ | ✅ Full |
| Chrome Mobile | 90+ | ✅ Full |

## Future Enhancements

- [ ] Infinite scroll/loop navigation
- [ ] Thumbnail indicators
- [ ] Vertical carousel option
- [ ] Drag-to-scroll on desktop
- [ ] Animation presets
- [ ] A/B testing integration
- [ ] Analytics tracking
- [ ] Personalized recommendations

## Related Documentation

- [Design Document](.kiro/specs/product-detail-ux-enhancements/design.md)
- [Requirements Document](.kiro/specs/product-detail-ux-enhancements/requirements.md)
- [Tasks Document](.kiro/specs/product-detail-ux-enhancements/tasks.md)
- [RecommendationService](src/main/java/com/codegym/smartphonemanagement/service/recommendation/RecommendationService.java)
- [RecommendationController](src/main/java/com/codegym/smartphonemanagement/controller/user/RecommendationController.java)

## Support

For issues or questions:
1. Check browser console for errors
2. Review API response in Network tab
3. Test with the test page
4. Check this documentation
5. Review the design document

---

**Status**: ✅ Implemented  
**Task**: 4.7 Triển khai RecommendationCarousel.js component  
**Requirements**: 5.2, 5.7, 13.2  
**Last Updated**: 2026-04-15
