/**
 * Mobile Utilities
 * 
 * Utility functions for mobile enhancements including:
 * - Debounce and throttle for performance
 * - Viewport detection and monitoring
 * - Touch gesture helpers
 * - Screen reader announcements
 * 
 * Requirements: 9.5, 9.6, 10.6
 */

const MobileUtils = {
    /**
     * Mobile breakpoint constant (matches CSS)
     */
    MOBILE_BREAKPOINT: 768,

    /**
     * Debounce function - delays execution until after wait time has elapsed
     * since the last call. Useful for resize events, search input, etc.
     * 
     * @param {Function} func - Function to debounce
     * @param {number} wait - Wait time in milliseconds
     * @param {boolean} immediate - Execute on leading edge instead of trailing
     * @returns {Function} Debounced function
     * 
     * @example
     * const debouncedResize = MobileUtils.debounce(() => {
     *     console.log('Window resized');
     * }, 300);
     * window.addEventListener('resize', debouncedResize);
     */
    debounce(func, wait, immediate = false) {
        let timeout;
        return function executedFunction(...args) {
            const context = this;
            const later = () => {
                timeout = null;
                if (!immediate) func.apply(context, args);
            };
            const callNow = immediate && !timeout;
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
            if (callNow) func.apply(context, args);
        };
    },

    /**
     * Throttle function - ensures function is called at most once per interval
     * Useful for scroll events, gesture tracking, etc.
     * 
     * @param {Function} func - Function to throttle
     * @param {number} limit - Minimum time between calls in milliseconds
     * @returns {Function} Throttled function
     * 
     * @example
     * const throttledScroll = MobileUtils.throttle(() => {
     *     console.log('Scrolling');
     * }, 16); // 60fps
     * window.addEventListener('scroll', throttledScroll);
     */
    throttle(func, limit) {
        let inThrottle;
        return function executedFunction(...args) {
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },

    /**
     * Check if current viewport is mobile (< 768px)
     * 
     * @returns {boolean} True if viewport is mobile
     * 
     * @example
     * if (MobileUtils.isMobile()) {
     *     // Initialize mobile features
     * }
     */
    isMobile() {
        return window.innerWidth < this.MOBILE_BREAKPOINT;
    },

    /**
     * Check if device supports touch events
     * 
     * @returns {boolean} True if touch is supported
     */
    isTouchDevice() {
        return (
            'ontouchstart' in window ||
            navigator.maxTouchPoints > 0 ||
            navigator.msMaxTouchPoints > 0
        );
    },

    /**
     * Watch for viewport changes and execute callbacks
     * Uses matchMedia API for efficient viewport monitoring
     * 
     * @param {Function} onMobile - Callback when entering mobile viewport
     * @param {Function} onDesktop - Callback when entering desktop viewport
     * @returns {Function} Cleanup function to remove listener
     * 
     * @example
     * const cleanup = MobileUtils.watchViewport(
     *     () => console.log('Switched to mobile'),
     *     () => console.log('Switched to desktop')
     * );
     * // Later: cleanup();
     */
    watchViewport(onMobile, onDesktop) {
        const mediaQuery = window.matchMedia(`(max-width: ${this.MOBILE_BREAKPOINT - 1}px)`);
        
        const handler = (e) => {
            if (e.matches && onMobile) {
                onMobile();
            } else if (!e.matches && onDesktop) {
                onDesktop();
            }
        };
        
        // Check initial state
        handler(mediaQuery);
        
        // Listen for changes
        if (mediaQuery.addEventListener) {
            mediaQuery.addEventListener('change', handler);
        } else {
            // Fallback for older browsers
            mediaQuery.addListener(handler);
        }
        
        // Return cleanup function
        return () => {
            if (mediaQuery.removeEventListener) {
                mediaQuery.removeEventListener('change', handler);
            } else {
                mediaQuery.removeListener(handler);
            }
        };
    },

    /**
     * Get touch coordinates from touch event
     * 
     * @param {TouchEvent} event - Touch event
     * @returns {Object} Object with x and y coordinates
     */
    getTouchCoordinates(event) {
        const touch = event.touches[0] || event.changedTouches[0];
        return {
            x: touch.clientX,
            y: touch.clientY
        };
    },

    /**
     * Calculate distance between two points
     * 
     * @param {Object} point1 - First point {x, y}
     * @param {Object} point2 - Second point {x, y}
     * @returns {number} Distance in pixels
     */
    calculateDistance(point1, point2) {
        const dx = point2.x - point1.x;
        const dy = point2.y - point1.y;
        return Math.sqrt(dx * dx + dy * dy);
    },

    /**
     * Detect swipe direction from touch events
     * 
     * @param {Object} startPoint - Starting point {x, y}
     * @param {Object} endPoint - Ending point {x, y}
     * @param {number} threshold - Minimum distance to consider a swipe (default: 50px)
     * @returns {string|null} Direction: 'left', 'right', 'up', 'down', or null
     */
    detectSwipeDirection(startPoint, endPoint, threshold = 50) {
        const dx = endPoint.x - startPoint.x;
        const dy = endPoint.y - startPoint.y;
        const absDx = Math.abs(dx);
        const absDy = Math.abs(dy);
        
        // Check if movement exceeds threshold
        if (absDx < threshold && absDy < threshold) {
            return null;
        }
        
        // Determine primary direction
        if (absDx > absDy) {
            return dx > 0 ? 'right' : 'left';
        } else {
            return dy > 0 ? 'down' : 'up';
        }
    },

    /**
     * Announce message to screen readers
     * Creates a live region for accessibility
     * 
     * @param {string} message - Message to announce
     * @param {string} priority - 'polite' or 'assertive' (default: 'polite')
     * 
     * @example
     * MobileUtils.announceToScreenReader('Item deleted from cart');
     */
    announceToScreenReader(message, priority = 'polite') {
        const announcement = document.createElement('div');
        announcement.setAttribute('role', 'status');
        announcement.setAttribute('aria-live', priority);
        announcement.setAttribute('aria-atomic', 'true');
        announcement.className = 'sr-only';
        announcement.textContent = message;
        
        document.body.appendChild(announcement);
        
        // Remove after announcement
        setTimeout(() => {
            document.body.removeChild(announcement);
        }, 1000);
    },

    /**
     * Request animation frame with fallback
     * 
     * @param {Function} callback - Function to execute on next frame
     * @returns {number} Request ID
     */
    requestAnimFrame(callback) {
        return (
            window.requestAnimationFrame ||
            window.webkitRequestAnimationFrame ||
            window.mozRequestAnimationFrame ||
            function(callback) {
                return window.setTimeout(callback, 1000 / 60);
            }
        )(callback);
    },

    /**
     * Cancel animation frame with fallback
     * 
     * @param {number} id - Request ID to cancel
     */
    cancelAnimFrame(id) {
        (
            window.cancelAnimationFrame ||
            window.webkitCancelAnimationFrame ||
            window.mozCancelAnimationFrame ||
            function(id) {
                window.clearTimeout(id);
            }
        )(id);
    },

    /**
     * Prevent body scroll (useful during modals or gestures)
     * 
     * @param {boolean} prevent - True to prevent scroll, false to restore
     */
    preventBodyScroll(prevent) {
        if (prevent) {
            document.body.style.overflow = 'hidden';
            document.body.style.position = 'fixed';
            document.body.style.width = '100%';
        } else {
            document.body.style.overflow = '';
            document.body.style.position = '';
            document.body.style.width = '';
        }
    },

    /**
     * Get scroll position
     * 
     * @returns {Object} Object with x and y scroll positions
     */
    getScrollPosition() {
        return {
            x: window.pageXOffset || document.documentElement.scrollLeft,
            y: window.pageYOffset || document.documentElement.scrollTop
        };
    },

    /**
     * Check if element is at top of scroll container
     * 
     * @param {HTMLElement} element - Element to check
     * @returns {boolean} True if at top
     */
    isAtScrollTop(element) {
        return element.scrollTop === 0;
    },

    /**
     * Check if element is at bottom of scroll container
     * 
     * @param {HTMLElement} element - Element to check
     * @returns {boolean} True if at bottom
     */
    isAtScrollBottom(element) {
        return element.scrollHeight - element.scrollTop === element.clientHeight;
    },

    /**
     * Add passive event listener for better scroll performance
     * 
     * @param {HTMLElement} element - Element to attach listener to
     * @param {string} event - Event name
     * @param {Function} handler - Event handler
     * @param {boolean} passive - Use passive listener (default: true)
     * @returns {Function} Cleanup function
     */
    addPassiveListener(element, event, handler, passive = true) {
        const options = { passive };
        element.addEventListener(event, handler, options);
        
        return () => {
            element.removeEventListener(event, handler, options);
        };
    },

    /**
     * Clamp value between min and max
     * 
     * @param {number} value - Value to clamp
     * @param {number} min - Minimum value
     * @param {number} max - Maximum value
     * @returns {number} Clamped value
     */
    clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    },

    /**
     * Linear interpolation between two values
     * 
     * @param {number} start - Start value
     * @param {number} end - End value
     * @param {number} t - Interpolation factor (0-1)
     * @returns {number} Interpolated value
     */
    lerp(start, end, t) {
        return start + (end - start) * t;
    },

    /**
     * Check if reduced motion is preferred
     * 
     * @returns {boolean} True if user prefers reduced motion
     */
    prefersReducedMotion() {
        const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
        return mediaQuery.matches;
    },

    /**
     * Get safe area insets for devices with notches
     * 
     * @returns {Object} Object with top, right, bottom, left insets
     */
    getSafeAreaInsets() {
        const style = getComputedStyle(document.documentElement);
        return {
            top: parseInt(style.getPropertyValue('--sat') || '0'),
            right: parseInt(style.getPropertyValue('--sar') || '0'),
            bottom: parseInt(style.getPropertyValue('--sab') || '0'),
            left: parseInt(style.getPropertyValue('--sal') || '0')
        };
    }
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = MobileUtils;
}
