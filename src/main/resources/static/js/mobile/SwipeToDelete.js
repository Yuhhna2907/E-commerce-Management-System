/**
 * SwipeToDelete Component
 * 
 * Implements swipe-to-delete functionality for cart items on mobile devices.
 * Users can swipe left on cart items to reveal a delete button.
 * 
 * Features:
 * - Touch gesture detection (touchstart, touchmove, touchend)
 * - Left swipe direction detection only
 * - 100px threshold for revealing delete button
 * - Smooth CSS transitions with cubic-bezier easing
 * - State management (only one item swiped at a time)
 * - Outside tap listener to close swiped items
 * - Integration with existing modal system for confirmations
 * 
 * Requirements: 3.1, 3.2, 3.6, 3.9
 * Design System: Glassmorphism with smooth animations
 * Breakpoint: Mobile < 768px only
 */

class SwipeToDelete {
    /**
     * Initialize SwipeToDelete component
     * 
     * @param {HTMLElement} element - The swipeable cart item element
     * @param {Function} onDelete - Callback function when delete is confirmed
     */
    constructor(element, onDelete) {
        this.element = element;
        this.onDelete = onDelete;
        
        // Touch gesture state
        this.startX = 0;
        this.startY = 0;
        this.currentX = 0;
        this.currentY = 0;
        this.swiping = false;
        this.isOpen = false;
        
        // Configuration
        this.threshold = 100; // Requirement 3.2: 100px threshold
        this.maxSwipeDistance = 80; // Maximum swipe distance (delete button width)
        
        // DOM elements
        this.content = null;
        this.deleteButton = null;
        
        // Event handlers (bound to this context)
        this.handleTouchStart = this.handleTouchStart.bind(this);
        this.handleTouchMove = this.handleTouchMove.bind(this);
        this.handleTouchEnd = this.handleTouchEnd.bind(this);
        this.handleDeleteClick = this.handleDeleteClick.bind(this);
        this.handleOutsideClick = this.handleOutsideClick.bind(this);
        
        this.init();
    }

    /**
     * Initialize the component
     * Only activate on mobile viewports (< 768px)
     */
    init() {
        // Requirement 3.1: Only enable on mobile viewports
        if (!MobileUtils.isMobile()) {
            return;
        }
        
        // Find DOM elements
        this.content = this.element.querySelector('.cart-item__content');
        this.deleteButton = this.element.querySelector('.cart-item__delete');
        
        if (!this.content || !this.deleteButton) {
            console.warn('SwipeToDelete: Required elements not found');
            return;
        }
        
        this.setupEventListeners();
        this.setupAccessibility();
    }

    /**
     * Set up event listeners for touch gestures and interactions
     */
    setupEventListeners() {
        // Touch events on content area
        // Use passive listeners for better performance (Requirement 7.1)
        MobileUtils.addPassiveListener(this.content, 'touchstart', this.handleTouchStart, true);
        MobileUtils.addPassiveListener(this.content, 'touchmove', this.handleTouchMove, false); // Not passive - need preventDefault
        MobileUtils.addPassiveListener(this.content, 'touchend', this.handleTouchEnd, true);
        
        // Delete button click
        this.deleteButton.addEventListener('click', this.handleDeleteClick);
        
        // Outside click to close (Requirement 3.6)
        document.addEventListener('click', this.handleOutsideClick);
        
        // Viewport change listener
        this.viewportCleanup = MobileUtils.watchViewport(
            () => this.enable(),
            () => this.disable()
        );
    }

    /**
     * Set up accessibility features
     */
    setupAccessibility() {
        // Ensure delete button has proper ARIA label
        if (!this.deleteButton.getAttribute('aria-label')) {
            const productName = this.element.querySelector('.product-title')?.textContent || 'item';
            this.deleteButton.setAttribute('aria-label', `Delete ${productName}`);
        }
        
        // Add screen reader text if not present
        if (!this.deleteButton.querySelector('.sr-only')) {
            const srText = document.createElement('span');
            srText.className = 'sr-only';
            srText.textContent = 'Delete item';
            this.deleteButton.appendChild(srText);
        }
    }

    /**
     * Handle touch start event
     * 
     * @param {TouchEvent} e - Touch event
     */
    handleTouchStart(e) {
        if (this.swiping) return;
        
        const touch = MobileUtils.getTouchCoordinates(e);
        this.startX = touch.x;
        this.startY = touch.y;
        this.currentX = touch.x;
        this.currentY = touch.y;
        this.swiping = true;
        
        // Close any other open swipe items
        this.closeOtherSwipeItems();
        
        // Add swiping class for faster transitions during gesture
        this.element.classList.add('swiping');
    }

    /**
     * Handle touch move event
     * 
     * @param {TouchEvent} e - Touch event
     */
    handleTouchMove(e) {
        if (!this.swiping) return;
        
        const touch = MobileUtils.getTouchCoordinates(e);
        this.currentX = touch.x;
        this.currentY = touch.y;
        
        const deltaX = this.currentX - this.startX;
        const deltaY = this.currentY - this.startY;
        
        // Check if this is a horizontal swipe (more horizontal than vertical movement)
        const isHorizontalSwipe = Math.abs(deltaX) > Math.abs(deltaY);
        
        // Requirement 3.2: Only allow left swipes (negative X delta)
        if (isHorizontalSwipe && deltaX < 0) {
            // Prevent default scrolling behavior
            e.preventDefault();
            
            // Calculate swipe distance, clamped to maximum
            const swipeDistance = MobileUtils.clamp(Math.abs(deltaX), 0, this.maxSwipeDistance);
            
            // Apply transform to content
            this.updateContentTransform(-swipeDistance);
            
            // Update delete button visibility based on swipe progress
            this.updateDeleteButtonVisibility(swipeDistance / this.maxSwipeDistance);
        }
    }

    /**
     * Handle touch end event
     * 
     * @param {TouchEvent} e - Touch event
     */
    handleTouchEnd(e) {
        if (!this.swiping) return;
        
        this.swiping = false;
        this.element.classList.remove('swiping');
        
        const deltaX = this.currentX - this.startX;
        const swipeDistance = Math.abs(deltaX);
        
        // Requirement 3.2: Check if swipe distance exceeds threshold (100px)
        if (deltaX < 0 && swipeDistance >= this.threshold) {
            // Open delete button
            this.open();
        } else {
            // Close/reset position
            this.close();
        }
    }

    /**
     * Handle delete button click
     * 
     * @param {Event} e - Click event
     */
    handleDeleteClick(e) {
        e.preventDefault();
        e.stopPropagation();
        
        // Trigger delete confirmation
        this.confirmDelete();
    }

    /**
     * Handle outside click to close swiped items
     * 
     * @param {Event} e - Click event
     */
    handleOutsideClick(e) {
        // Requirement 3.6: Close on outside tap
        if (!this.element.contains(e.target) && this.isOpen) {
            this.close();
        }
    }

    /**
     * Open the swipe-to-delete state (reveal delete button)
     */
    open() {
        this.isOpen = true;
        this.element.classList.add('swiped');
        
        // Update transforms
        this.updateContentTransform(-this.maxSwipeDistance);
        this.updateDeleteButtonVisibility(1);
        
        // Announce to screen readers
        MobileUtils.announceToScreenReader('Delete button revealed');
    }

    /**
     * Close the swipe-to-delete state (hide delete button)
     */
    close() {
        this.isOpen = false;
        this.element.classList.remove('swiped');
        
        // Reset transforms
        this.updateContentTransform(0);
        this.updateDeleteButtonVisibility(0);
    }

    /**
     * Update content transform position
     * 
     * @param {number} translateX - X translation value in pixels
     */
    updateContentTransform(translateX) {
        // Use GPU-accelerated transform for smooth performance
        this.content.style.transform = `translateX(${translateX}px)`;
    }

    /**
     * Update delete button visibility and position
     * 
     * @param {number} progress - Progress from 0 to 1
     */
    updateDeleteButtonVisibility(progress) {
        const opacity = MobileUtils.clamp(progress, 0, 1);
        const translateX = MobileUtils.lerp(20, 0, progress); // Slide in from right
        
        this.deleteButton.style.opacity = opacity;
        this.deleteButton.style.transform = `translateX(${translateX}px)`;
    }

    /**
     * Close any other open swipe items to maintain state isolation
     * Requirement 3.9: Only one item swiped at a time
     */
    closeOtherSwipeItems() {
        document.querySelectorAll('.swipeable.swiped').forEach(item => {
            if (item !== this.element) {
                const instance = item.swipeToDeleteInstance;
                if (instance && instance.close) {
                    instance.close();
                }
            }
        });
    }

    /**
     * Show delete confirmation modal
     * Uses existing modal system for consistency
     * Requirement 3.4: Display confirmation modal when delete button is tapped
     * Requirement 3.5: Remove cart item and update cart total when confirmed
     * Requirement 10.5: Use existing modal system for delete confirmations
     */
    async confirmDelete() {
        try {
            // Get product information for confirmation
            const productName = this.element.querySelector('.product-title')?.textContent || 'this item';
            const productImage = this.element.querySelector('img')?.src || '';
            const productSpecs = this.element.querySelector('.product-specs')?.textContent || '';
            const itemId = this.element.getAttribute('data-id');
            
            // Use existing modal system (Requirement 10.5)
            const modal = document.getElementById('confirmDeleteModal');
            if (modal) {
                // Update modal content with product information
                const deleteProductName = modal.querySelector('#deleteProductName');
                const deleteProductImage = modal.querySelector('#deleteProductImage');
                const deleteProductSpecs = modal.querySelector('#deleteProductSpecs');
                const deleteForm = modal.querySelector('#deleteForm');
                
                if (deleteProductName) deleteProductName.textContent = productName;
                if (deleteProductImage) deleteProductImage.src = productImage;
                if (deleteProductSpecs) deleteProductSpecs.textContent = productSpecs;
                if (deleteForm) deleteForm.action = `/user/cart/remove/${itemId}`;
                
                // Set up form submission handler for cart updates
                this.setupDeleteFormHandler(deleteForm, itemId, productName);
                
                // Show modal using Bootstrap (Requirement 3.4)
                const bsModal = new bootstrap.Modal(modal);
                bsModal.show();
                
                // Close swipe state when modal opens
                this.close();
            } else {
                // Fallback: direct confirmation
                const confirmed = await this.showFallbackConfirmation(productName);
                if (confirmed) {
                    await this.handleDirectDelete(itemId, productName);
                }
            }
        } catch (error) {
            console.error('Error showing delete confirmation:', error);
            
            // Fallback confirmation
            const confirmed = confirm('Are you sure you want to delete this item?');
            if (confirmed) {
                await this.handleDirectDelete(itemId, productName);
            }
        }
    }

    /**
     * Set up delete form submission handler
     * Handles cart updates and event dispatching after successful deletion
     * 
     * @param {HTMLFormElement} form - The delete form element
     * @param {string} itemId - Cart item ID
     * @param {string} productName - Product name for notifications
     */
    setupDeleteFormHandler(form, itemId, productName) {
        if (!form) return;
        
        // Remove any existing handlers to prevent duplicates
        const newForm = form.cloneNode(true);
        form.parentNode.replaceChild(newForm, form);
        
        newForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const deleteBtn = newForm.querySelector('.js-delete-btn');
            const deleteBtnText = newForm.querySelector('.js-delete-btn-text');
            
            try {
                // Show loading state
                if (deleteBtn && deleteBtnText) {
                    deleteBtn.disabled = true;
                    deleteBtnText.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang xóa...';
                }
                
                // Perform delete request
                const response = await fetch(newForm.action, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    }
                });
                
                if (response.ok) {
                    // Requirement 3.5: Remove cart item and update cart total
                    await this.handleSuccessfulDelete(itemId, productName);
                    
                    // Close modal
                    const modal = bootstrap.Modal.getInstance(document.getElementById('confirmDeleteModal'));
                    if (modal) {
                        modal.hide();
                    }
                } else {
                    throw new Error('Delete request failed');
                }
                
            } catch (error) {
                console.error('Error deleting cart item:', error);
                
                // Show error message
                this.showErrorMessage('Không thể xóa sản phẩm. Vui lòng thử lại.');
                
                // Reset button state
                if (deleteBtn && deleteBtnText) {
                    deleteBtn.disabled = false;
                    deleteBtnText.innerHTML = 'Xóa ngay';
                }
            }
        });
    }

    /**
     * Handle successful cart item deletion
     * Updates UI, cart totals, and dispatches events
     * 
     * @param {string} itemId - Cart item ID
     * @param {string} productName - Product name for notifications
     */
    async handleSuccessfulDelete(itemId, productName) {
        try {
            // Remove cart item from DOM with animation
            this.animateItemRemoval();
            
            // Update cart totals and item count
            await this.updateCartTotals();
            
            // Dispatch cart updated event for bottom nav badge (Requirement: Update bottom nav badge)
            this.dispatchCartUpdatedEvent();
            
            // Show success notification
            this.showSuccessMessage(`Đã xóa "${productName}" khỏi giỏ hàng`);
            
            // Check if cart is now empty and redirect if needed
            setTimeout(() => {
                const remainingItems = document.querySelectorAll('.cart-item:not(.removing)');
                if (remainingItems.length === 0) {
                    window.location.reload(); // Reload to show empty cart state
                }
            }, 500);
            
        } catch (error) {
            console.error('Error handling successful delete:', error);
            // Fallback: reload page to ensure consistency
            window.location.reload();
        }
    }

    /**
     * Handle direct delete (without modal)
     * 
     * @param {string} itemId - Cart item ID
     * @param {string} productName - Product name for notifications
     */
    async handleDirectDelete(itemId, productName) {
        try {
            const response = await fetch(`/user/cart/remove/${itemId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                }
            });
            
            if (response.ok) {
                await this.handleSuccessfulDelete(itemId, productName);
            } else {
                throw new Error('Delete request failed');
            }
        } catch (error) {
            console.error('Error in direct delete:', error);
            this.showErrorMessage('Không thể xóa sản phẩm. Vui lòng thử lại.');
        }
    }

    /**
     * Animate cart item removal
     */
    animateItemRemoval() {
        this.element.classList.add('removing');
        this.element.style.transition = 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)';
        this.element.style.opacity = '0';
        this.element.style.transform = 'translateX(-100%) scale(0.8)';
        this.element.style.height = '0';
        this.element.style.marginBottom = '0';
        this.element.style.paddingTop = '0';
        this.element.style.paddingBottom = '0';
    }

    /**
     * Update cart totals in the UI
     */
    async updateCartTotals() {
        try {
            // Get updated cart data
            const response = await fetch('/user/cart/api/totals');
            if (response.ok) {
                const data = await response.json();
                
                // Update total items count
                const totalItemsElement = document.querySelector('.js-total-items');
                if (totalItemsElement && data.itemCount !== undefined) {
                    totalItemsElement.textContent = data.itemCount;
                }
                
                // Update cart total
                const cartTotalElement = document.querySelector('.js-cart-total');
                if (cartTotalElement && data.total !== undefined) {
                    cartTotalElement.textContent = new Intl.NumberFormat('vi-VN').format(data.total) + ' ₫';
                    cartTotalElement.setAttribute('data-raw-total', data.total);
                }
                
                // Update checkout button state
                this.updateCheckoutButtonState(data.itemCount);
                
            }
        } catch (error) {
            console.error('Error updating cart totals:', error);
            // Fallback: reload page after a delay
            setTimeout(() => window.location.reload(), 1000);
        }
    }

    /**
     * Update checkout button state based on cart contents
     * 
     * @param {number} itemCount - Number of items in cart
     */
    updateCheckoutButtonState(itemCount) {
        const checkoutBtn = document.querySelector('.js-checkout-btn');
        const checkoutLink = document.querySelector('.js-checkout-link');
        
        if (itemCount === 0) {
            if (checkoutBtn) checkoutBtn.disabled = true;
            if (checkoutLink) {
                checkoutLink.style.pointerEvents = 'none';
                checkoutLink.style.cursor = 'not-allowed';
            }
        }
    }

    /**
     * Dispatch cart updated event for other components (like bottom navigation)
     */
    dispatchCartUpdatedEvent() {
        // Dispatch custom event for bottom nav badge update
        const event = new CustomEvent('cart:updated', {
            bubbles: true,
            detail: {
                source: 'swipe-to-delete',
                timestamp: Date.now()
            }
        });
        document.dispatchEvent(event);
    }

    /**
     * Show success message using existing toast system
     * 
     * @param {string} message - Success message to display
     */
    showSuccessMessage(message) {
        if (typeof showToast === 'function') {
            showToast(message, 'success');
        } else {
            // Fallback: create simple toast
            this.createSimpleToast(message, 'success');
        }
    }

    /**
     * Show error message using existing toast system
     * 
     * @param {string} message - Error message to display
     */
    showErrorMessage(message) {
        if (typeof showToast === 'function') {
            showToast(message, 'error');
        } else {
            // Fallback: create simple toast
            this.createSimpleToast(message, 'error');
        }
    }

    /**
     * Create simple toast notification as fallback
     * 
     * @param {string} message - Message to display
     * @param {string} type - Toast type ('success' or 'error')
     */
    createSimpleToast(message, type) {
        const toast = document.createElement('div');
        toast.style.cssText = `
            position: fixed;
            bottom: 30px;
            right: 30px;
            background: ${type === 'success' ? '#10b981' : '#ef4444'};
            color: white;
            padding: 16px 24px;
            border-radius: 12px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            z-index: 9999;
            font-weight: 600;
            transform: translateX(400px);
            transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
        `;
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        setTimeout(() => toast.style.transform = 'translateX(0)', 10);
        setTimeout(() => {
            toast.style.transform = 'translateX(400px)';
            setTimeout(() => toast.remove(), 400);
        }, 3000);
    }

    /**
     * Show fallback confirmation dialog
     * 
     * @param {string} productName - Name of the product to delete
     * @returns {Promise<boolean>} True if confirmed
     */
    showFallbackConfirmation(productName) {
        return new Promise((resolve) => {
            // Create simple confirmation toast
            const message = `Delete "${productName}" from cart?`;
            
            // Use existing toast system if available
            if (typeof showConfirmToast === 'function') {
                showConfirmToast(
                    message,
                    'trash3-fill',
                    '#ef4444',
                    () => resolve(true)
                );
                
                // Auto-resolve to false after 8 seconds if no action
                setTimeout(() => resolve(false), 8000);
            } else {
                // Browser confirm as last resort
                resolve(confirm(message));
            }
        });
    }

    /**
     * Enable swipe-to-delete functionality
     */
    enable() {
        if (this.content && this.deleteButton) {
            this.content.style.pointerEvents = 'auto';
            this.deleteButton.style.display = 'flex';
        }
    }

    /**
     * Disable swipe-to-delete functionality
     */
    disable() {
        this.close();
        if (this.content && this.deleteButton) {
            this.content.style.pointerEvents = 'auto';
            this.deleteButton.style.display = 'none';
        }
    }

    /**
     * Destroy the component and clean up event listeners
     */
    destroy() {
        // Remove event listeners
        if (this.content) {
            this.content.removeEventListener('touchstart', this.handleTouchStart);
            this.content.removeEventListener('touchmove', this.handleTouchMove);
            this.content.removeEventListener('touchend', this.handleTouchEnd);
        }
        
        if (this.deleteButton) {
            this.deleteButton.removeEventListener('click', this.handleDeleteClick);
        }
        
        document.removeEventListener('click', this.handleOutsideClick);
        
        // Clean up viewport watcher
        if (this.viewportCleanup) {
            this.viewportCleanup();
        }
        
        // Remove instance reference
        if (this.element) {
            delete this.element.swipeToDeleteInstance;
        }
    }

    /**
     * Static method to initialize SwipeToDelete on all cart items
     * 
     * @param {string} selector - CSS selector for cart items (default: '.swipeable')
     * @param {Function} onDelete - Optional global delete handler
     */
    static initializeAll(selector = '.swipeable', onDelete = null) {
        // Only initialize on mobile
        if (!MobileUtils.isMobile()) {
            return [];
        }
        
        const elements = document.querySelectorAll(selector);
        const instances = [];
        
        elements.forEach(element => {
            // Skip if already initialized
            if (element.swipeToDeleteInstance) {
                return;
            }
            
            // Create delete handler for this specific item
            const itemDeleteHandler = onDelete || (() => {
                // Default handler: trigger existing delete button
                const deleteBtn = element.querySelector('.js-btn-remove');
                if (deleteBtn) {
                    deleteBtn.click();
                }
            });
            
            // Create instance
            const instance = new SwipeToDelete(element, itemDeleteHandler);
            
            // Store reference on element for later access
            element.swipeToDeleteInstance = instance;
            
            instances.push(instance);
        });
        
        return instances;
    }

    /**
     * Static method to destroy all SwipeToDelete instances
     * 
     * @param {string} selector - CSS selector for cart items (default: '.swipeable')
     */
    static destroyAll(selector = '.swipeable') {
        const elements = document.querySelectorAll(selector);
        
        elements.forEach(element => {
            if (element.swipeToDeleteInstance) {
                element.swipeToDeleteInstance.destroy();
            }
        });
    }
}

// Auto-initialize when DOM is ready (if not already initialized)
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        // Only auto-initialize if we're on a page with cart items
        if (document.querySelector('.swipeable') && MobileUtils.isMobile()) {
            SwipeToDelete.initializeAll();
        }
    });
} else {
    // DOM already ready
    if (document.querySelector('.swipeable') && MobileUtils.isMobile()) {
        SwipeToDelete.initializeAll();
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SwipeToDelete;
}

// Global access
window.SwipeToDelete = SwipeToDelete;