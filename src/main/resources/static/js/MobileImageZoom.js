/**
 * MobileImageZoom.js
 * Mobile image zoom component with touch gesture support
 * Supports pinch-to-zoom (1x-5x), double-tap toggle (3x), and pan functionality
 */

class MobileImageZoom {
    constructor(imageElement, options = {}) {
        this.image = imageElement;
        this.container = imageElement.parentElement;
        this.minZoom = 1;
        this.maxZoom = options.maxZoom || 5;
        this.currentZoom = 1;
        this.doubleTapZoom = options.doubleTapZoom || 3;
        this.transitionDuration = options.transitionDuration || 200;
        
        // Pan state
        this.isPanning = false;
        this.startX = 0;
        this.startY = 0;
        this.translateX = 0;
        this.translateY = 0;
        this.lastTranslateX = 0;
        this.lastTranslateY = 0;
        
        // Pinch state
        this.isPinching = false;
        this.initialDistance = 0;
        this.initialZoom = 1;
        
        // Double-tap state
        this.lastTapTime = 0;
        this.tapTimeout = null;
        
        this.init();
    }
    
    init() {
        this.setupContainer();
        this.attachGestureListeners();
    }
    
    setupContainer() {
        // Setup container styles
        this.container.style.cssText = `
            overflow: hidden;
            touch-action: none;
            position: relative;
            user-select: none;
            -webkit-user-select: none;
        `;
        
        // Setup image styles
        this.image.style.cssText = `
            transition: transform ${this.transitionDuration}ms ease;
            transform-origin: center center;
            max-width: 100%;
            height: auto;
            display: block;
        `;
        
        // Create zoom indicator
        this.zoomIndicator = document.createElement('div');
        this.zoomIndicator.className = 'zoom-indicator';
        this.container.appendChild(this.zoomIndicator);
        
        // Prevent default image drag
        this.image.addEventListener('dragstart', (e) => e.preventDefault());
    }
    
    attachGestureListeners() {
        // Touch start
        this.container.addEventListener('touchstart', (e) => {
            this.handleTouchStart(e);
        }, { passive: false });
        
        // Touch move
        this.container.addEventListener('touchmove', (e) => {
            this.handleTouchMove(e);
        }, { passive: false });
        
        // Touch end
        this.container.addEventListener('touchend', (e) => {
            this.handleTouchEnd(e);
        }, { passive: false });
        
        // Touch cancel
        this.container.addEventListener('touchcancel', (e) => {
            this.handleTouchEnd(e);
        }, { passive: false });
    }
    
    handleTouchStart(e) {
        const touches = e.touches;
        
        if (touches.length === 2) {
            // Pinch gesture
            e.preventDefault();
            this.startPinch(touches);
        } else if (touches.length === 1) {
            // Single touch - check for double-tap or pan
            const currentTime = Date.now();
            const tapLength = currentTime - this.lastTapTime;
            
            if (tapLength < 300 && tapLength > 0) {
                // Double-tap detected
                e.preventDefault();
                this.handleDoubleTap(touches[0]);
            } else {
                // Potential pan or single tap
                if (this.currentZoom > 1) {
                    e.preventDefault();
                    this.startPan(touches[0]);
                }
            }
            
            this.lastTapTime = currentTime;
        }
    }
    
    handleTouchMove(e) {
        const touches = e.touches;
        
        if (touches.length === 2 && this.isPinching) {
            // Pinch zoom
            e.preventDefault();
            this.updatePinch(touches);
        } else if (touches.length === 1 && this.isPanning && this.currentZoom > 1) {
            // Pan
            e.preventDefault();
            this.updatePan(touches[0]);
        }
    }
    
    handleTouchEnd(e) {
        if (this.isPinching) {
            this.endPinch();
        }
        
        if (this.isPanning) {
            this.endPan();
        }
    }
    
    // Pinch-to-zoom implementation
    startPinch(touches) {
        this.isPinching = true;
        this.initialDistance = this.getDistance(touches[0], touches[1]);
        this.initialZoom = this.currentZoom;
        
        // Disable transition for smooth pinching
        this.image.style.transition = 'none';
    }
    
    updatePinch(touches) {
        const currentDistance = this.getDistance(touches[0], touches[1]);
        const scale = currentDistance / this.initialDistance;
        const newZoom = this.initialZoom * scale;
        
        // Calculate center point of pinch
        const centerX = (touches[0].clientX + touches[1].clientX) / 2;
        const centerY = (touches[0].clientY + touches[1].clientY) / 2;
        
        this.setZoom(newZoom, { x: centerX, y: centerY });
    }
    
    endPinch() {
        this.isPinching = false;
        
        // Re-enable transition
        this.image.style.transition = `transform ${this.transitionDuration}ms ease`;
        
        // Constrain zoom to limits
        if (this.currentZoom < this.minZoom) {
            this.setZoom(this.minZoom);
        } else if (this.currentZoom > this.maxZoom) {
            this.setZoom(this.maxZoom);
        }
        
        // Reset pan if zoomed out completely
        if (this.currentZoom === this.minZoom) {
            this.resetPan();
        }
    }
    
    // Double-tap implementation
    handleDoubleTap(touch) {
        if (this.currentZoom === this.minZoom) {
            // Zoom in to doubleTapZoom level
            const rect = this.image.getBoundingClientRect();
            const x = touch.clientX;
            const y = touch.clientY;
            
            this.setZoom(this.doubleTapZoom, { x, y });
        } else {
            // Zoom out to minimum
            this.setZoom(this.minZoom);
            this.resetPan();
        }
    }
    
    // Pan implementation
    startPan(touch) {
        this.isPanning = true;
        this.startX = touch.clientX - this.lastTranslateX;
        this.startY = touch.clientY - this.lastTranslateY;
        
        // Disable transition for smooth panning
        this.image.style.transition = 'none';
    }
    
    updatePan(touch) {
        this.translateX = touch.clientX - this.startX;
        this.translateY = touch.clientY - this.startY;
        
        // Constrain pan to image boundaries
        const constraints = this.getPanConstraints();
        this.translateX = Math.max(constraints.minX, Math.min(constraints.maxX, this.translateX));
        this.translateY = Math.max(constraints.minY, Math.min(constraints.maxY, this.translateY));
        
        this.applyTransform();
    }
    
    endPan() {
        this.isPanning = false;
        this.lastTranslateX = this.translateX;
        this.lastTranslateY = this.translateY;
        
        // Re-enable transition
        this.image.style.transition = `transform ${this.transitionDuration}ms ease`;
    }
    
    resetPan() {
        this.translateX = 0;
        this.translateY = 0;
        this.lastTranslateX = 0;
        this.lastTranslateY = 0;
        this.applyTransform();
    }
    
    // Zoom control
    setZoom(zoom, focalPoint = null) {
        const oldZoom = this.currentZoom;
        this.currentZoom = Math.max(this.minZoom, Math.min(this.maxZoom, zoom));
        
        if (focalPoint) {
            // Adjust pan to zoom towards focal point
            const rect = this.image.getBoundingClientRect();
            const offsetX = focalPoint.x - rect.left - rect.width / 2;
            const offsetY = focalPoint.y - rect.top - rect.height / 2;
            
            const zoomRatio = this.currentZoom / oldZoom;
            this.translateX = focalPoint.x - (focalPoint.x - this.translateX) * zoomRatio;
            this.translateY = focalPoint.y - (focalPoint.y - this.translateY) * zoomRatio;
            
            this.lastTranslateX = this.translateX;
            this.lastTranslateY = this.translateY;
        }
        
        this.applyTransform();
    }
    
    applyTransform() {
        const transform = `translate(${this.translateX}px, ${this.translateY}px) scale(${this.currentZoom})`;
        this.image.style.transform = transform;
        
        // Update zoom indicator
        this.updateZoomIndicator();
    }
    
    updateZoomIndicator() {
        if (this.zoomIndicator) {
            if (this.currentZoom > 1) {
                this.zoomIndicator.textContent = `${Math.round(this.currentZoom * 100)}%`;
                this.zoomIndicator.classList.add('active');
                
                // Auto-hide after 1.5 seconds
                clearTimeout(this.indicatorTimeout);
                this.indicatorTimeout = setTimeout(() => {
                    this.zoomIndicator.classList.remove('active');
                }, 1500);
            } else {
                this.zoomIndicator.classList.remove('active');
            }
        }
    }
    
    // Helper methods
    getDistance(touch1, touch2) {
        const dx = touch1.clientX - touch2.clientX;
        const dy = touch1.clientY - touch2.clientY;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    getPanConstraints() {
        const rect = this.image.getBoundingClientRect();
        const containerRect = this.container.getBoundingClientRect();
        
        // Calculate how much the image extends beyond container
        const imageWidth = rect.width;
        const imageHeight = rect.height;
        const containerWidth = containerRect.width;
        const containerHeight = containerRect.height;
        
        // Maximum pan distance (half of overflow on each side)
        const maxPanX = Math.max(0, (imageWidth - containerWidth) / 2);
        const maxPanY = Math.max(0, (imageHeight - containerHeight) / 2);
        
        return {
            minX: -maxPanX,
            maxX: maxPanX,
            minY: -maxPanY,
            maxY: maxPanY
        };
    }
    
    // Public API
    reset() {
        this.currentZoom = this.minZoom;
        this.resetPan();
    }
    
    zoomIn() {
        const newZoom = Math.min(this.maxZoom, this.currentZoom + 0.5);
        this.setZoom(newZoom);
    }
    
    zoomOut() {
        const newZoom = Math.max(this.minZoom, this.currentZoom - 0.5);
        this.setZoom(newZoom);
        
        if (newZoom === this.minZoom) {
            this.resetPan();
        }
    }
    
    getZoomLevel() {
        return this.currentZoom;
    }
    
    destroy() {
        // Remove event listeners and reset styles
        this.container.style.cssText = '';
        this.image.style.cssText = '';
        
        // Remove zoom indicator
        if (this.zoomIndicator && this.zoomIndicator.parentNode) {
            this.zoomIndicator.parentNode.removeChild(this.zoomIndicator);
        }
        
        // Clear timeouts
        clearTimeout(this.indicatorTimeout);
        
        this.reset();
    }
}

// Auto-initialize on mobile devices (controlled by ZoomIntegration.js)
// Removed auto-initialization to prevent conflicts with ZoomIntegration.js
// The ZoomIntegration.js will handle proper device detection and initialization

// Export for use in modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = MobileImageZoom;
}
