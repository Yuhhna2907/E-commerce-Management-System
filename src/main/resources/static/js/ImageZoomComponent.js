/**
 * ImageZoomComponent.js
 * Desktop image zoom component with hover magnifier effect
 * 
 * Features:
 * - Hover detection and magnifier positioning
 * - Zoom calculation (2x-4x)
 * - Background positioning for zoomed image
 * - Boundary detection
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */

class ImageZoomComponent {
    /**
     * Constructor
     * @param {HTMLImageElement} imageElement - The product image element to zoom
     * @param {Object} options - Configuration options
     * @param {number} options.zoomLevel - Zoom level (2x-4x), default 2.5
     * @param {number} options.magnifierSize - Size of magnifier in pixels, default 150
     */
    constructor(imageElement, options = {}) {
        if (!imageElement || !(imageElement instanceof HTMLImageElement)) {
            throw new Error('ImageZoomComponent requires a valid image element');
        }
        
        this.image = imageElement;
        this.zoomLevel = this.validateZoomLevel(options.zoomLevel || 2.5);
        this.magnifierSize = options.magnifierSize || 150;
        this.magnifier = null;
        this.isActive = false;
        
        this.init();
    }
    
    /**
     * Validate zoom level is within bounds (2x-4x)
     * Requirement 1.5: Maintain zoom level between 2x and 4x
     */
    validateZoomLevel(level) {
        const MIN_ZOOM = 2;
        const MAX_ZOOM = 4;
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, level));
    }
    
    /**
     * Initialize the component
     */
    init() {
        // Wait for image to load before initializing
        if (this.image.complete) {
            this.setup();
        } else {
            this.image.addEventListener('load', () => this.setup());
        }
    }
    
    /**
     * Setup the magnifier and event listeners
     */
    setup() {
        this.createMagnifier();
        this.attachEventListeners();
    }
    
    /**
     * Create the magnifier element
     * Requirement 1.1: Display magnifier showing zoomed portion
     */
    createMagnifier() {
        this.magnifier = document.createElement('div');
        this.magnifier.className = 'image-magnifier';
        
        // Style the magnifier
        this.magnifier.style.cssText = `
            position: absolute;
            width: ${this.magnifierSize}px;
            height: ${this.magnifierSize}px;
            border: 3px solid rgba(255, 255, 255, 0.8);
            border-radius: 50%;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 0 0 1px rgba(0, 0, 0, 0.1);
            pointer-events: none;
            display: none;
            background-repeat: no-repeat;
            background-size: ${this.image.width * this.zoomLevel}px ${this.image.height * this.zoomLevel}px;
            background-image: url('${this.image.src}');
            z-index: 1000;
            transition: opacity 0.1s ease;
        `;
        
        document.body.appendChild(this.magnifier);
    }
    
    /**
     * Attach event listeners for hover detection
     * Requirement 1.1, 1.2: Hover detection and magnifier positioning
     */
    attachEventListeners() {
        // Show magnifier on mouse enter
        // Requirement 1.3: Display zoom effect within 100ms
        this.image.addEventListener('mouseenter', () => {
            this.isActive = true;
            this.magnifier.style.display = 'block';
            this.magnifier.style.opacity = '1';
            // Add zooming class for cursor change
            this.image.parentElement.classList.add('zooming');
        });
        
        // Update magnifier position on mouse move
        // Requirement 1.2: Update magnifier position to track cursor
        this.image.addEventListener('mousemove', (e) => {
            if (this.isActive) {
                this.updateMagnifierPosition(e);
            }
        });
        
        // Hide magnifier on mouse leave
        // Requirement 1.4: Hide magnifier when cursor leaves image boundaries
        this.image.addEventListener('mouseleave', () => {
            this.isActive = false;
            this.magnifier.style.display = 'none';
            this.magnifier.style.opacity = '0';
            // Remove zooming class
            this.image.parentElement.classList.remove('zooming');
        });
    }
    
    /**
     * Update magnifier position based on cursor location
     * Requirement 1.2: Update magnifier position to track cursor
     * Requirement 1.4: Implement boundary detection
     * @param {MouseEvent} e - Mouse event
     */
    updateMagnifierPosition(e) {
        const rect = this.image.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        
        // Boundary detection - hide if cursor is outside image
        // Requirement 1.4: Hide magnifier when cursor moves outside boundaries
        if (x < 0 || y < 0 || x > rect.width || y > rect.height) {
            this.magnifier.style.display = 'none';
            return;
        }
        
        // Show magnifier if it was hidden due to boundary
        if (this.magnifier.style.display === 'none') {
            this.magnifier.style.display = 'block';
        }
        
        // Position the magnifier centered on cursor
        // Requirement 1.2: Magnifier follows cursor position
        const magnifierX = e.clientX - this.magnifierSize / 2;
        const magnifierY = e.clientY - this.magnifierSize / 2;
        
        this.magnifier.style.left = `${magnifierX}px`;
        this.magnifier.style.top = `${magnifierY}px`;
        
        // Calculate background position for zoomed image
        // Requirement 1.3: Implement zoom calculation
        this.updateBackgroundPosition(x, y);
    }
    
    /**
     * Update background position to show correct zoomed portion
     * Requirement 1.3: Implement zoom calculation (2x-4x)
     * @param {number} x - X coordinate relative to image
     * @param {number} y - Y coordinate relative to image
     */
    updateBackgroundPosition(x, y) {
        // Calculate the background position to center the zoomed area on cursor
        // Formula: -(cursor_position * zoom_level - magnifier_radius)
        const bgX = -((x * this.zoomLevel) - this.magnifierSize / 2);
        const bgY = -((y * this.zoomLevel) - this.magnifierSize / 2);
        
        this.magnifier.style.backgroundPosition = `${bgX}px ${bgY}px`;
    }
    
    /**
     * Update zoom level dynamically
     * Requirement 1.5: Maintain zoom level between 2x and 4x
     * @param {number} newZoomLevel - New zoom level (2-4)
     */
    setZoomLevel(newZoomLevel) {
        this.zoomLevel = this.validateZoomLevel(newZoomLevel);
        
        // Update background size with new zoom level
        if (this.magnifier) {
            this.magnifier.style.backgroundSize = 
                `${this.image.width * this.zoomLevel}px ${this.image.height * this.zoomLevel}px`;
        }
    }
    
    /**
     * Update magnifier size dynamically
     * @param {number} newSize - New magnifier size in pixels
     */
    setMagnifierSize(newSize) {
        this.magnifierSize = newSize;
        
        if (this.magnifier) {
            this.magnifier.style.width = `${newSize}px`;
            this.magnifier.style.height = `${newSize}px`;
        }
    }
    
    /**
     * Destroy the component and clean up
     */
    destroy() {
        if (this.magnifier && this.magnifier.parentNode) {
            this.magnifier.parentNode.removeChild(this.magnifier);
        }
        
        this.isActive = false;
        this.magnifier = null;
    }
    
    /**
     * Check if component is currently active
     * @returns {boolean}
     */
    isZoomActive() {
        return this.isActive;
    }
}

// Auto-initialize on desktop devices (controlled by ZoomIntegration.js)
// Removed auto-initialization to prevent conflicts with ZoomIntegration.js
// The ZoomIntegration.js will handle proper device detection and initialization

// Re-initialize on window resize if crossing desktop/mobile threshold
// Removed resize handler to prevent conflicts with ZoomIntegration.js
// The ZoomIntegration.js will handle responsive behavior
