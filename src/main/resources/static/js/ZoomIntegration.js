/**
 * ZoomIntegration.js
 * Integrates zoom components into product detail page
 * 
 * Features:
 * - Device type detection (desktop vs mobile)
 * - Appropriate zoom component initialization
 * - Lazy loading of high-resolution images
 * - Performance optimization
 * 
 * Requirements: 1.1, 2.1, 13.1, 14.1
 */

class ZoomIntegration {
    constructor() {
        this.isDesktop = false;
        this.isMobile = false;
        this.desktopZoom = null;
        this.mobileZoom = null;
        this.highResImages = new Map();
        this.lazyLoadObserver = null;
        
        this.init();
    }
    
    /**
     * Initialize zoom integration
     * Requirement 13.1: Responsive design that adapts based on device type detection
     */
    init() {
        this.detectDeviceType();
        this.setupLazyLoading();
        this.initializeZoomComponents();
        this.attachResizeListener();
    }
    
    /**
     * Detect device type based on screen size and touch capability
     * Requirement 13.1: Device type detection
     */
    detectDeviceType() {
        const screenWidth = window.innerWidth;
        const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
        
        // Desktop: width >= 768px and no touch OR width >= 1024px
        this.isDesktop = (screenWidth >= 768 && !hasTouch) || screenWidth >= 1024;
        
        // Mobile: width < 768px OR has touch capability
        this.isMobile = screenWidth < 768 || (hasTouch && screenWidth < 1024);
        
        console.log(`Device detection: Desktop=${this.isDesktop}, Mobile=${this.isMobile}, Width=${screenWidth}, Touch=${hasTouch}`);
    }
    
    /**
     * Setup lazy loading for high-resolution images
     * Requirement 14.1: Lazy load high-resolution images
     */
    setupLazyLoading() {
        // Create intersection observer for lazy loading
        if ('IntersectionObserver' in window) {
            this.lazyLoadObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        this.loadHighResImage(entry.target);
                        this.lazyLoadObserver.unobserve(entry.target);
                    }
                });
            }, {
                rootMargin: '50px 0px', // Start loading 50px before image comes into view
                threshold: 0.1
            });
        }
        
        // Prepare high-resolution image URLs
        this.prepareHighResImages();
    }
    
    /**
     * Prepare high-resolution image URLs for lazy loading
     * Requirement 14.1: Performance optimization with lazy loading
     */
    prepareHighResImages() {
        const productImage = document.querySelector('.product-img');
        if (!productImage) return;
        
        const currentSrc = productImage.src;
        
        // Generate high-resolution variants
        // Assuming the backend provides different resolutions
        const highResSrc = this.generateHighResUrl(currentSrc);
        
        if (highResSrc !== currentSrc) {
            this.highResImages.set(productImage, highResSrc);
            
            // Start observing the image for lazy loading
            if (this.lazyLoadObserver) {
                this.lazyLoadObserver.observe(productImage);
            }
        }
    }
    
    /**
     * Generate high-resolution image URL
     * @param {string} originalUrl - Original image URL
     * @returns {string} High-resolution image URL
     */
    generateHighResUrl(originalUrl) {
        // If URL already contains size parameters, replace them
        if (originalUrl.includes('w=') || originalUrl.includes('h=')) {
            return originalUrl.replace(/[?&]w=\d+/g, '').replace(/[?&]h=\d+/g, '') + 
                   (originalUrl.includes('?') ? '&' : '?') + 'w=1200&h=1200&q=90';
        }
        
        // For static images, try to find a high-res variant
        if (originalUrl.includes('/images/') || originalUrl.includes('/uploads/')) {
            const extension = originalUrl.split('.').pop();
            const baseName = originalUrl.replace(`.${extension}`, '');
            return `${baseName}_hd.${extension}`;
        }
        
        // Fallback: return original URL
        return originalUrl;
    }
    
    /**
     * Load high-resolution image
     * Requirement 14.1: Lazy load high-resolution images
     * @param {HTMLImageElement} imageElement - Image element to upgrade
     */
    loadHighResImage(imageElement) {
        const highResSrc = this.highResImages.get(imageElement);
        if (!highResSrc) return;
        
        // Create a new image to preload
        const highResImage = new Image();
        
        highResImage.onload = () => {
            // Smoothly transition to high-res image
            imageElement.style.transition = 'opacity 0.3s ease';
            imageElement.style.opacity = '0.7';
            
            setTimeout(() => {
                imageElement.src = highResSrc;
                imageElement.style.opacity = '1';
                
                // Update zoom component background if active
                this.updateZoomBackground(imageElement, highResSrc);
                
                console.log('High-resolution image loaded:', highResSrc);
            }, 150);
        };
        
        highResImage.onerror = () => {
            console.warn('Failed to load high-resolution image:', highResSrc);
        };
        
        // Start loading
        highResImage.src = highResSrc;
    }
    
    /**
     * Update zoom component background with high-res image
     * @param {HTMLImageElement} imageElement - Image element
     * @param {string} highResSrc - High-resolution image URL
     */
    updateZoomBackground(imageElement, highResSrc) {
        if (this.desktopZoom && this.desktopZoom.magnifier) {
            this.desktopZoom.magnifier.style.backgroundImage = `url('${highResSrc}')`;
        }
    }
    
    /**
     * Initialize appropriate zoom components based on device type
     * Requirement 1.1, 2.1: Initialize appropriate zoom component
     */
    initializeZoomComponents() {
        const productImage = document.querySelector('.product-img');
        if (!productImage) {
            console.warn('Product image not found for zoom initialization');
            return;
        }
        
        // Clean up existing zoom components
        this.destroyZoomComponents();
        
        if (this.isDesktop) {
            this.initializeDesktopZoom(productImage);
        }
        
        if (this.isMobile) {
            this.initializeMobileZoom(productImage);
        }
    }
    
    /**
     * Initialize desktop zoom component
     * Requirement 1.1: Desktop hover magnifier functionality
     * @param {HTMLImageElement} productImage - Product image element
     */
    initializeDesktopZoom(productImage) {
        try {
            // Check if ImageZoomComponent is available
            if (typeof ImageZoomComponent === 'undefined') {
                console.warn('ImageZoomComponent not loaded');
                return;
            }
            
            this.desktopZoom = new ImageZoomComponent(productImage, {
                zoomLevel: 2.5, // Reduced from 3x to 2.5x for better UX
                magnifierSize: 150 // Reduced from 200px to 150px for less intrusive display
            });
            
            console.log('Desktop zoom component initialized');
            
            // Add desktop-specific styling
            productImage.parentElement.classList.add('desktop-zoom-enabled');
            
        } catch (error) {
            console.error('Failed to initialize desktop zoom:', error);
        }
    }
    
    /**
     * Initialize mobile zoom component
     * Requirement 2.1: Mobile pinch-to-zoom and double-tap functionality
     * @param {HTMLImageElement} productImage - Product image element
     */
    initializeMobileZoom(productImage) {
        try {
            // Check if MobileImageZoom is available
            if (typeof MobileImageZoom === 'undefined') {
                console.warn('MobileImageZoom not loaded');
                return;
            }
            
            this.mobileZoom = new MobileImageZoom(productImage, {
                maxZoom: 5, // 1x-5x as per requirement 2.5
                doubleTapZoom: 3,
                transitionDuration: 200 // Smooth transitions per requirement 2.6
            });
            
            console.log('Mobile zoom component initialized');
            
            // Add mobile-specific styling
            productImage.parentElement.classList.add('mobile-zoom-enabled');
            
        } catch (error) {
            console.error('Failed to initialize mobile zoom:', error);
        }
    }
    
    /**
     * Destroy existing zoom components
     */
    destroyZoomComponents() {
        if (this.desktopZoom) {
            this.desktopZoom.destroy();
            this.desktopZoom = null;
        }
        
        if (this.mobileZoom) {
            this.mobileZoom.destroy();
            this.mobileZoom = null;
        }
        
        // Remove zoom-specific classes
        const productImage = document.querySelector('.product-img');
        if (productImage && productImage.parentElement) {
            productImage.parentElement.classList.remove('desktop-zoom-enabled', 'mobile-zoom-enabled');
        }
    }
    
    /**
     * Attach resize listener for responsive behavior
     * Requirement 13.1: Responsive design adaptation
     */
    attachResizeListener() {
        let resizeTimer;
        
        window.addEventListener('resize', () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(() => {
                const oldIsDesktop = this.isDesktop;
                const oldIsMobile = this.isMobile;
                
                // Re-detect device type
                this.detectDeviceType();
                
                // Re-initialize if device type changed
                if (oldIsDesktop !== this.isDesktop || oldIsMobile !== this.isMobile) {
                    console.log('Device type changed, re-initializing zoom components');
                    this.initializeZoomComponents();
                }
            }, 250);
        });
    }
    
    /**
     * Update zoom components when image changes (e.g., gallery navigation)
     * @param {HTMLImageElement} newImage - New image element
     */
    updateImage(newImage) {
        if (!newImage) return;
        
        // Update high-res image mapping
        const highResSrc = this.generateHighResUrl(newImage.src);
        if (highResSrc !== newImage.src) {
            this.highResImages.set(newImage, highResSrc);
            
            // Start lazy loading for new image
            if (this.lazyLoadObserver) {
                this.lazyLoadObserver.observe(newImage);
            }
        }
        
        // Update zoom components
        if (this.desktopZoom) {
            this.desktopZoom.destroy();
            this.desktopZoom = new ImageZoomComponent(newImage, {
                zoomLevel: 2.5, // Reduced from 3x to 2.5x for better UX
                magnifierSize: 150 // Reduced from 200px to 150px for less intrusive display
            });
        }
        
        if (this.mobileZoom) {
            this.mobileZoom.destroy();
            this.mobileZoom = new MobileImageZoom(newImage, {
                maxZoom: 5,
                doubleTapZoom: 3,
                transitionDuration: 200
            });
        }
    }
    
    /**
     * Get current zoom state
     * @returns {Object} Current zoom state
     */
    getZoomState() {
        return {
            isDesktop: this.isDesktop,
            isMobile: this.isMobile,
            desktopZoomActive: this.desktopZoom ? this.desktopZoom.isZoomActive() : false,
            mobileZoomLevel: this.mobileZoom ? this.mobileZoom.getZoomLevel() : 1
        };
    }
    
    /**
     * Destroy the integration and clean up
     */
    destroy() {
        this.destroyZoomComponents();
        
        if (this.lazyLoadObserver) {
            this.lazyLoadObserver.disconnect();
            this.lazyLoadObserver = null;
        }
        
        this.highResImages.clear();
    }
}

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    // Wait a bit for other components to initialize
    setTimeout(() => {
        window.zoomIntegration = new ZoomIntegration();
        console.log('ZoomIntegration initialized');
    }, 100);
});

// Export for use in modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ZoomIntegration;
}