/**
 * RecommendationCarousel.js
 * Component hiển thị carousel sản phẩm "Khách Hàng Cũng Mua"
 * 
 * Features:
 * - Load recommendations từ API
 * - Carousel navigation với prev/next buttons
 * - Touch swipe support cho mobile
 * - Responsive grid (4 cards desktop, 2 cards mobile)
 * - Product cards với image, name, price, rating
 * 
 * Yêu cầu: 5.2, 5.7, 13.2
 */

class RecommendationCarousel {
    constructor(containerElement, productId, options = {}) {
        this.container = containerElement;
        this.productId = productId;
        this.recommendations = [];
        this.currentIndex = 0;
        this.cardsPerView = this.getCardsPerView();
        this.isLoading = false;
        this.touchStartX = 0;
        this.touchEndX = 0;
        
        // Options
        this.apiEndpoint = options.apiEndpoint || `/api/products/${productId}/recommendations`;
        this.autoScroll = options.autoScroll || false;
        this.autoScrollInterval = options.autoScrollInterval || 5000;
        this.showRating = options.showRating !== false;
        this.showPrice = options.showPrice !== false;
        
        this.init();
    }
    
    /**
     * Initialize carousel
     */
    async init() {
        try {
            // Verify container element exists
            if (!this.container) {
                throw new Error('Container element is null or undefined');
            }
            
            // Verify product ID is valid
            if (!this.productId || this.productId <= 0) {
                throw new Error('Invalid product ID: ' + this.productId);
            }
            
            this.createCarouselStructure();
            await this.loadRecommendations();
            this.attachEventListeners();
            
            if (this.autoScroll && this.recommendations.length > this.cardsPerView) {
                this.startAutoScroll();
            }
            
        } catch (error) {
            // Try to show error state if possible
            if (this.container) {
                this.container.innerHTML = `
                    <div class="alert alert-warning">
                        <i class="bi bi-exclamation-triangle"></i>
                        Không thể khởi tạo carousel gợi ý sản phẩm.
                    </div>
                `;
            }
            
            throw error; // Re-throw for caller to handle
        }
    }
    
    /**
     * Tạo cấu trúc HTML cho carousel
     */
    createCarouselStructure() {
        if (!this.container) {
            throw new Error('Container element is required for createCarouselStructure');
        }
        
        this.container.innerHTML = `
            <div class="recommendation-carousel">
                <div class="carousel-header">
                    <h3 class="carousel-title">Khách Hàng Cũng Mua</h3>
                </div>
                <div class="carousel-container">
                    <button class="carousel-btn carousel-prev" aria-label="Previous">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                    <div class="carousel-viewport">
                        <div class="carousel-track">
                            <!-- Product cards will be inserted here -->
                        </div>
                    </div>
                    <button class="carousel-btn carousel-next" aria-label="Next">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                </div>
                <div class="carousel-loading" style="display: none;">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Đang tải...</span>
                    </div>
                </div>
                <div class="carousel-error" style="display: none;">
                    <p class="text-muted">Không thể tải danh sách gợi ý</p>
                </div>
                <div class="carousel-empty" style="display: none;">
                    <p class="text-muted">Chưa có sản phẩm gợi ý</p>
                </div>
            </div>
        `;
        
        // Cache DOM elements with verification
        this.track = this.container.querySelector('.carousel-track');
        this.prevBtn = this.container.querySelector('.carousel-prev');
        this.nextBtn = this.container.querySelector('.carousel-next');
        this.loadingEl = this.container.querySelector('.carousel-loading');
        this.errorEl = this.container.querySelector('.carousel-error');
        this.emptyEl = this.container.querySelector('.carousel-empty');
        this.viewport = this.container.querySelector('.carousel-viewport');
        
        // Verify all required elements were created
        const requiredElements = {
            track: this.track,
            prevBtn: this.prevBtn,
            nextBtn: this.nextBtn,
            loadingEl: this.loadingEl,
            errorEl: this.errorEl,
            emptyEl: this.emptyEl,
            viewport: this.viewport
        };
        
        const missingElements = Object.entries(requiredElements)
            .filter(([name, element]) => !element)
            .map(([name]) => name);
        
        if (missingElements.length > 0) {
            throw new Error('Failed to create required DOM elements: ' + missingElements.join(', '));
        }
    }
    
    /**
     * Load recommendations từ API
     */
    async loadRecommendations() {
        this.showLoading();
        
        try {
            const response = await fetch(this.apiEndpoint);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            
            // Enhanced API response validation
            if (!this.validateApiResponse(data)) {
                this.showError();
                return;
            }
            
            if (data.success && data.recommendations && data.recommendations.length > 0) {
                // Sanitize and filter product data
                this.recommendations = this.sanitizeProductData(data.recommendations);
                
                if (this.recommendations.length > 0) {
                    this.renderProducts();
                    this.updateNavigationButtons();
                    this.hideLoading();
                } else {
                    this.showEmpty();
                }
            } else {
                this.showEmpty();
            }
            
        } catch (error) {
            this.showError();
        }
    }
    
    /**
     * Validate API response format
     */
    validateApiResponse(data) {
        if (!data) {
            return false;
        }
        
        if (typeof data !== 'object') {
            return false;
        }
        
        if (!data.hasOwnProperty('success')) {
            return false;
        }
        
        if (!data.hasOwnProperty('recommendations')) {
            return false;
        }
        
        if (!Array.isArray(data.recommendations)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Sanitize and filter product data
     */
    sanitizeProductData(products) {
        const sanitized = products.filter(product => {
            // Check required fields
            if (!product.id) {
                return false;
            }
            
            if (!product.name || typeof product.name !== 'string') {
                return false;
            }
            
            if (product.price === null || product.price === undefined) {
                return false;
            }
            
            // Check if product is active (if field exists)
            if (product.hasOwnProperty('active') && !product.active) {
                return false;
            }
            
            // Sanitize image URL
            if (!product.imageUrl || typeof product.imageUrl !== 'string') {
                return null; // Skip products without valid image
            }
            
            // Sanitize brand
            if (!product.brand) {
                product.brand = '';
            }
            
            // Sanitize rating fields
            if (typeof product.averageRating !== 'number') {
                product.averageRating = 0;
            }
            
            if (typeof product.totalReviews !== 'number') {
                product.totalReviews = 0;
            }
            
            // Sanitize stock
            if (typeof product.stock !== 'number') {
                product.stock = 0;
            }
            
            return true;
        });
        
        return sanitized;
    }
    
    /**
     * Render product cards
     */
    renderProducts() {
        this.track.innerHTML = '';
        
        this.recommendations.forEach((product, index) => {
            const card = this.createProductCard(product, index);
            this.track.appendChild(card);
        });
    }
    
    /**
     * Tạo product card HTML
     */
    createProductCard(product, index) {
        const card = document.createElement('div');
        card.className = 'recommendation-card';
        card.dataset.index = index;
        
        // Format price
        const formattedPrice = this.formatPrice(product.price);
        
        // Generate rating stars
        const ratingHTML = this.showRating ? this.generateRatingStars(product.averageRating || 0) : '';
        
        // Product image URL
        const imageUrl = product.imageUrl;
        
        // Stock status
        const stockStatus = product.stock > 0 ? 'in-stock' : 'out-of-stock';
        const stockText = product.stock > 0 ? 'Còn hàng' : 'Hết hàng';
        
        card.innerHTML = `
            <a href="/user/products/${product.id}" class="card-link">
                <div class="card-image-wrapper">
                    <img src="${imageUrl}" 
                         alt="${this.escapeHtml(product.name)}" 
                         class="card-image"
                         loading="lazy">
                    <span class="stock-badge ${stockStatus}">${stockText}</span>
                </div>
                <div class="card-body">
                    <h5 class="card-title" title="${this.escapeHtml(product.name)}">
                        ${this.escapeHtml(product.name)}
                    </h5>
                    <p class="card-brand">${this.escapeHtml(product.brand || '')}</p>
                    ${this.showRating ? `
                        <div class="card-rating">
                            ${ratingHTML}
                            <span class="rating-count">(${product.totalReviews || 0})</span>
                        </div>
                    ` : ''}
                    ${this.showPrice ? `
                        <div class="card-price">
                            <span class="price-value">${formattedPrice}</span>
                        </div>
                    ` : ''}
                    ${product.sold ? `
                        <p class="card-sold">Đã bán: ${product.sold}</p>
                    ` : ''}
                </div>
            </a>
        `;
        
        return card;
    }
    
    /**
     * Generate rating stars HTML
     */
    generateRatingStars(rating) {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
        
        let starsHTML = '';
        
        // Full stars
        for (let i = 0; i < fullStars; i++) {
            starsHTML += '<i class="bi bi-star-fill text-warning"></i>';
        }
        
        // Half star
        if (hasHalfStar) {
            starsHTML += '<i class="bi bi-star-half text-warning"></i>';
        }
        
        // Empty stars
        for (let i = 0; i < emptyStars; i++) {
            starsHTML += '<i class="bi bi-star text-warning"></i>';
        }
        
        return `<span class="stars">${starsHTML}</span><span class="rating-value">${rating.toFixed(1)}</span>`;
    }
    
    /**
     * Format price
     */
    formatPrice(price) {
        if (!price) return '0₫';
        
        // Convert to number if string
        const numPrice = typeof price === 'string' ? parseFloat(price) : price;
        
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(numPrice);
    }
    
    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    /**
     * Attach event listeners
     */
    attachEventListeners() {
        // Navigation buttons
        this.prevBtn.addEventListener('click', () => this.prev());
        this.nextBtn.addEventListener('click', () => this.next());
        
        // Touch swipe support
        this.viewport.addEventListener('touchstart', (e) => this.handleTouchStart(e), { passive: true });
        this.viewport.addEventListener('touchend', (e) => this.handleTouchEnd(e), { passive: true });
        
        // Keyboard navigation
        this.container.addEventListener('keydown', (e) => this.handleKeyboard(e));
        
        // Responsive resize
        window.addEventListener('resize', () => this.handleResize());
        
        // Pause auto-scroll on hover
        if (this.autoScroll) {
            this.container.addEventListener('mouseenter', () => this.stopAutoScroll());
            this.container.addEventListener('mouseleave', () => this.startAutoScroll());
        }
    }
    
    /**
     * Navigate to previous items
     */
    prev() {
        if (this.currentIndex > 0) {
            this.currentIndex--;
            this.updateCarousel();
        }
    }
    
    /**
     * Navigate to next items
     */
    next() {
        const maxIndex = Math.max(0, this.recommendations.length - this.cardsPerView);
        if (this.currentIndex < maxIndex) {
            this.currentIndex++;
            this.updateCarousel();
        }
    }
    
    /**
     * Update carousel position
     */
    updateCarousel() {
        const cardWidth = this.track.querySelector('.recommendation-card')?.offsetWidth || 0;
        const gap = 16; // Gap between cards
        const offset = -(this.currentIndex * (cardWidth + gap));
        
        this.track.style.transform = `translateX(${offset}px)`;
        this.updateNavigationButtons();
    }
    
    /**
     * Update navigation button states
     */
    updateNavigationButtons() {
        const maxIndex = Math.max(0, this.recommendations.length - this.cardsPerView);
        
        // Disable prev button at start
        this.prevBtn.disabled = this.currentIndex === 0;
        this.prevBtn.style.opacity = this.currentIndex === 0 ? '0.5' : '1';
        
        // Disable next button at end
        this.nextBtn.disabled = this.currentIndex >= maxIndex;
        this.nextBtn.style.opacity = this.currentIndex >= maxIndex ? '0.5' : '1';
        
        // Hide buttons if not enough items
        if (this.recommendations.length <= this.cardsPerView) {
            this.prevBtn.style.display = 'none';
            this.nextBtn.style.display = 'none';
        } else {
            this.prevBtn.style.display = 'flex';
            this.nextBtn.style.display = 'flex';
        }
    }
    
    /**
     * Handle touch start
     */
    handleTouchStart(e) {
        this.touchStartX = e.changedTouches[0].screenX;
    }
    
    /**
     * Handle touch end
     */
    handleTouchEnd(e) {
        this.touchEndX = e.changedTouches[0].screenX;
        this.handleSwipe();
    }
    
    /**
     * Handle swipe gesture
     */
    handleSwipe() {
        const swipeThreshold = 50;
        const diff = this.touchStartX - this.touchEndX;
        
        if (Math.abs(diff) > swipeThreshold) {
            if (diff > 0) {
                // Swipe left - next
                this.next();
            } else {
                // Swipe right - prev
                this.prev();
            }
        }
    }
    
    /**
     * Handle keyboard navigation
     */
    handleKeyboard(e) {
        if (e.key === 'ArrowLeft') {
            e.preventDefault();
            this.prev();
        } else if (e.key === 'ArrowRight') {
            e.preventDefault();
            this.next();
        }
    }
    
    /**
     * Handle window resize
     */
    handleResize() {
        const newCardsPerView = this.getCardsPerView();
        
        if (newCardsPerView !== this.cardsPerView) {
            this.cardsPerView = newCardsPerView;
            this.currentIndex = 0;
            this.updateCarousel();
        }
    }
    
    /**
     * Get number of cards per view based on screen size
     */
    getCardsPerView() {
        const width = window.innerWidth;
        
        if (width >= 1200) {
            return 4; // Desktop large
        } else if (width >= 992) {
            return 3; // Desktop
        } else if (width >= 768) {
            return 2; // Tablet
        } else {
            return 2; // Mobile (changed from 1 to 2 per requirements)
        }
    }
    
    /**
     * Start auto-scroll
     */
    startAutoScroll() {
        if (this.autoScrollTimer) {
            clearInterval(this.autoScrollTimer);
        }
        
        this.autoScrollTimer = setInterval(() => {
            const maxIndex = Math.max(0, this.recommendations.length - this.cardsPerView);
            
            if (this.currentIndex >= maxIndex) {
                this.currentIndex = 0;
            } else {
                this.currentIndex++;
            }
            
            this.updateCarousel();
        }, this.autoScrollInterval);
    }
    
    /**
     * Stop auto-scroll
     */
    stopAutoScroll() {
        if (this.autoScrollTimer) {
            clearInterval(this.autoScrollTimer);
            this.autoScrollTimer = null;
        }
    }
    
    /**
     * Show loading state
     */
    showLoading() {
        this.isLoading = true;
        
        // Verify elements exist before manipulating
        if (this.loadingEl) {
            this.loadingEl.style.display = 'flex';
        }
        
        if (this.errorEl) {
            this.errorEl.style.display = 'none';
        }
        
        if (this.emptyEl) {
            this.emptyEl.style.display = 'none';
        }
        
        if (this.track) {
            this.track.style.display = 'none';
        }
        
        if (this.prevBtn) {
            this.prevBtn.style.display = 'none';
        }
        
        if (this.nextBtn) {
            this.nextBtn.style.display = 'none';
        }
    }
    
    /**
     * Hide loading state
     */
    hideLoading() {
        this.isLoading = false;
        this.loadingEl.style.display = 'none';
        this.track.style.display = 'flex';
        
        // Enhanced DOM element verification and wrapper display logic
        const wrapper = document.getElementById('recommendation-section-wrapper');
        
        if (wrapper) {
            // Show the wrapper when we have recommendations
            wrapper.style.display = 'block';
            wrapper.classList.add('recommendation-visible');
            wrapper.classList.remove('recommendation-hidden');
            
            // Force a reflow to ensure the change takes effect
            wrapper.offsetHeight;
            
            // Verify the change took effect
            setTimeout(() => {
                const finalDisplay = window.getComputedStyle(wrapper).display;
                
                if (finalDisplay === 'none') {
                    // Try alternative approaches
                    wrapper.style.setProperty('display', 'block', 'important');
                    wrapper.style.visibility = 'visible';
                    wrapper.style.opacity = '1';
                }
            }, 100);
        }
    }
    
    /**
     * Show error state
     */
    showError() {
        this.isLoading = false;
        this.loadingEl.style.display = 'none';
        this.errorEl.style.display = 'block';
        this.emptyEl.style.display = 'none';
        this.track.style.display = 'none';
        this.prevBtn.style.display = 'none';
        this.nextBtn.style.display = 'none';
        
        // Enhanced wrapper handling for error state
        const wrapper = document.getElementById('recommendation-section-wrapper');
        
        if (wrapper) {
            // Hide the wrapper on error but add error class for debugging
            wrapper.style.display = 'none';
            wrapper.classList.add('recommendation-error');
            wrapper.classList.remove('recommendation-visible', 'recommendation-hidden');
        }
    }
    
    /**
     * Show empty state
     */
    showEmpty() {
        this.isLoading = false;
        this.loadingEl.style.display = 'none';
        this.errorEl.style.display = 'none';
        this.emptyEl.style.display = 'block';
        this.track.style.display = 'none';
        this.prevBtn.style.display = 'none';
        this.nextBtn.style.display = 'none';
        
        // Enhanced wrapper handling for empty state
        const wrapper = document.getElementById('recommendation-section-wrapper');
        
        if (wrapper) {
            // Hide the wrapper when empty but add empty class for debugging
            wrapper.style.display = 'none';
            wrapper.classList.add('recommendation-empty');
            wrapper.classList.remove('recommendation-visible', 'recommendation-error');
        }
    }
    
    /**
     * Refresh recommendations
     */
    async refresh() {
        this.currentIndex = 0;
        await this.loadRecommendations();
    }
    
    /**
     * Destroy carousel
     */
    destroy() {
        this.stopAutoScroll();
        
        // Remove event listeners
        window.removeEventListener('resize', this.handleResize);
        
        // Clear container
        this.container.innerHTML = '';
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = RecommendationCarousel;
}
