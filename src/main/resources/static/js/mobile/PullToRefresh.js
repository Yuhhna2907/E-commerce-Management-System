/**
 * PullToRefresh Component
 * 
 * Implements pull-to-refresh gesture for mobile product lists.
 * Allows users to refresh content by pulling down when at the top of the page.
 * 
 * Requirements: 2.1, 2.3, 2.4, 2.8, 2.10, 7.1
 * 
 * @class PullToRefresh
 */
class PullToRefresh {
    /**
     * Create a PullToRefresh instance
     * @param {HTMLElement} element - The scrollable container element
     * @param {Function} onRefresh - Callback function to execute on refresh
     */
    constructor(element, onRefresh) {
        this.element = element;
        this.onRefresh = onRefresh;
        this.indicator = null;
        
        // Touch tracking
        this.startY = 0;
        this.currentY = 0;
        this.pulling = false;
        this.refreshing = false;
        
        // Configuration
        this.threshold = 80; // Minimum pull distance to trigger refresh (px)
        this.maxPullDistance = 120; // Maximum visual pull distance (px)
        
        // State
        this.isEnabled = window.innerWidth < 768;
        
        this.init();
    }

    /**
     * Initialize the component
     */
    init() {
        if (!this.isEnabled) return;
        
        this.createIndicator();
        this.attachEventListeners();
        this.setupViewportListener();
    }

    /**
     * Create the pull-to-refresh indicator element
     */
    createIndicator() {
        this.indicator = this.element.querySelector('.pull-to-refresh__indicator');
        
        if (!this.indicator) {
            console.warn('PullToRefresh: indicator element not found');
        }
    }

    /**
     * Attach touch event listeners
     * Uses passive listeners for performance (Requirement 7.1)
     */
    attachEventListeners() {
        // Passive for touchstart (no preventDefault needed)
        this.element.addEventListener('touchstart', this.handleTouchStart.bind(this), { 
            passive: true 
        });
        
        // Non-passive for touchmove (need preventDefault to prevent scroll)
        this.element.addEventListener('touchmove', this.handleTouchMove.bind(this), { 
            passive: false 
        });
        
        // Passive for touchend
        this.element.addEventListener('touchend', this.handleTouchEnd.bind(this), { 
            passive: true 
        });
    }

    /**
     * Setup viewport resize listener to enable/disable based on screen size
     */
    setupViewportListener() {
        window.addEventListener('resize', () => {
            const wasMobile = this.isEnabled;
            this.isEnabled = window.innerWidth < 768;
            
            // Clean up if switching from mobile to desktop
            if (wasMobile && !this.isEnabled) {
                this.reset();
            }
        });
    }

    /**
     * Handle touch start event
     * @param {TouchEvent} e - Touch event
     */
    handleTouchStart(e) {
        // Only activate if at the top of the scroll container (Requirement 2.1)
        if (this.element.scrollTop === 0 && !this.refreshing) {
            this.startY = e.touches[0].clientY;
            this.pulling = true;
        }
    }

    /**
     * Handle touch move event
     * @param {TouchEvent} e - Touch event
     */
    handleTouchMove(e) {
        if (!this.pulling || this.refreshing) return;
        
        this.currentY = e.touches[0].clientY;
        const pullDistance = this.currentY - this.startY;
        
        // Only handle downward pulls
        if (pullDistance > 0) {
            // Prevent default scroll behavior
            e.preventDefault();
            
            // Apply resistance curve for natural feel
            const resistance = 0.5;
            const adjustedDistance = pullDistance * resistance;
            const translateY = Math.min(adjustedDistance, this.maxPullDistance);
            
            // Update indicator position
            if (this.indicator) {
                this.indicator.style.transform = `translateY(${translateY}px)`;
                this.indicator.style.opacity = Math.min(translateY / this.threshold, 1);
            }
            
            // Update visual state based on threshold (Requirement 2.3)
            if (pullDistance >= this.threshold) {
                this.element.classList.add('pull-to-refresh--ready');
                if (this.indicator) {
                    const text = this.indicator.querySelector('span');
                    if (text) text.textContent = 'Release to refresh';
                }
            } else {
                this.element.classList.remove('pull-to-refresh--ready');
                if (this.indicator) {
                    const text = this.indicator.querySelector('span');
                    if (text) text.textContent = 'Pull to refresh';
                }
            }
        }
    }

    /**
     * Handle touch end event
     */
    handleTouchEnd() {
        if (!this.pulling) return;
        
        const pullDistance = this.currentY - this.startY;
        
        // Trigger refresh if threshold exceeded (Requirement 2.3, 2.4)
        if (pullDistance >= this.threshold && !this.refreshing) {
            this.refresh();
        } else {
            this.reset();
        }
        
        this.pulling = false;
    }

    /**
     * Execute the refresh action
     * Preserves current filter and search parameters (Requirement 2.10)
     */
    async refresh() {
        if (this.refreshing) return;
        
        this.refreshing = true;
        this.element.classList.add('pull-to-refresh--refreshing');
        this.element.classList.remove('pull-to-refresh--ready');
        
        // Update indicator to show loading state
        if (this.indicator) {
            this.indicator.style.transform = `translateY(${this.threshold}px)`;
            this.indicator.style.opacity = '1';
            const text = this.indicator.querySelector('span');
            if (text) text.textContent = 'Refreshing...';
        }
        
        try {
            // Preserve current URL parameters (filters, search, etc.) - Requirement 2.10
            const currentParams = this.getCurrentParameters();
            
            // Execute the refresh callback (Requirement 2.4)
            await this.onRefresh(currentParams);
            
            // Success feedback
            this.showSuccessFeedback();
        } catch (error) {
            console.error('PullToRefresh: Refresh failed', error);
            this.showErrorFeedback();
        } finally {
            // Reset after a brief delay for visual feedback
            setTimeout(() => {
                this.refreshing = false;
                this.reset();
            }, 300);
        }
    }

    /**
     * Get current URL parameters to preserve during refresh (Requirement 2.10)
     * @returns {URLSearchParams} Current URL parameters
     */
    getCurrentParameters() {
        return new URLSearchParams(window.location.search);
    }

    /**
     * Show success feedback
     */
    showSuccessFeedback() {
        if (this.indicator) {
            const text = this.indicator.querySelector('span');
            if (text) {
                text.textContent = 'Refreshed!';
                this.indicator.classList.add('pull-to-refresh__indicator--success');
            }
        }
    }

    /**
     * Show error feedback
     */
    showErrorFeedback() {
        if (this.indicator) {
            const text = this.indicator.querySelector('span');
            if (text) {
                text.textContent = 'Refresh failed';
                this.indicator.classList.add('pull-to-refresh__indicator--error');
            }
        }
    }

    /**
     * Reset the component to initial state
     */
    reset() {
        this.element.classList.remove(
            'pull-to-refresh--pulling',
            'pull-to-refresh--ready',
            'pull-to-refresh--refreshing'
        );
        
        if (this.indicator) {
            this.indicator.style.transform = '';
            this.indicator.style.opacity = '0';
            this.indicator.classList.remove(
                'pull-to-refresh__indicator--success',
                'pull-to-refresh__indicator--error'
            );
            
            const text = this.indicator.querySelector('span');
            if (text) text.textContent = 'Pull to refresh';
        }
        
        this.startY = 0;
        this.currentY = 0;
    }

    /**
     * Destroy the component and clean up event listeners
     */
    destroy() {
        this.element.removeEventListener('touchstart', this.handleTouchStart);
        this.element.removeEventListener('touchmove', this.handleTouchMove);
        this.element.removeEventListener('touchend', this.handleTouchEnd);
        this.reset();
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PullToRefresh;
}
