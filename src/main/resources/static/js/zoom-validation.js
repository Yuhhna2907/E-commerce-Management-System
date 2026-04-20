/**
 * Zoom Integration Validation Script
 * Validates that all zoom components are properly integrated
 */

(function() {
    'use strict';
    
    const validation = {
        results: [],
        
        log(test, passed, message) {
            this.results.push({ test, passed, message });
            console.log(`${passed ? '✅' : '❌'} ${test}: ${message}`);
        },
        
        validateComponents() {
            console.log('🔍 Validating Zoom Integration...\n');
            
            // Check if classes are available
            this.log(
                'ImageZoomComponent Class',
                typeof ImageZoomComponent === 'function',
                typeof ImageZoomComponent === 'function' ? 'Available' : 'Not loaded'
            );
            
            this.log(
                'MobileImageZoom Class',
                typeof MobileImageZoom === 'function',
                typeof MobileImageZoom === 'function' ? 'Available' : 'Not loaded'
            );
            
            this.log(
                'ZoomIntegration Class',
                typeof ZoomIntegration === 'function',
                typeof ZoomIntegration === 'function' ? 'Available' : 'Not loaded'
            );
            
            // Check if integration is initialized
            this.log(
                'ZoomIntegration Instance',
                window.zoomIntegration instanceof ZoomIntegration,
                window.zoomIntegration ? 'Initialized' : 'Not initialized'
            );
            
            // Check DOM elements
            const productImg = document.querySelector('.product-img');
            this.log(
                'Product Image Element',
                !!productImg,
                productImg ? 'Found' : 'Not found'
            );
            
            const imageParallax = document.querySelector('.detail-image-parallax');
            this.log(
                'Image Container Element',
                !!imageParallax,
                imageParallax ? 'Found' : 'Not found'
            );
            
            // Check device detection
            if (window.zoomIntegration) {
                const state = window.zoomIntegration.getZoomState();
                this.log(
                    'Device Detection',
                    state.isDesktop || state.isMobile,
                    `Desktop: ${state.isDesktop}, Mobile: ${state.isMobile}`
                );
                
                // Check component initialization
                if (state.isDesktop) {
                    this.log(
                        'Desktop Zoom Component',
                        !!window.zoomIntegration.desktopZoom,
                        window.zoomIntegration.desktopZoom ? 'Initialized' : 'Not initialized'
                    );
                }
                
                if (state.isMobile) {
                    this.log(
                        'Mobile Zoom Component',
                        !!window.zoomIntegration.mobileZoom,
                        window.zoomIntegration.mobileZoom ? 'Initialized' : 'Not initialized'
                    );
                }
            }
            
            // Check CSS classes
            if (productImg && productImg.parentElement) {
                const hasDesktopClass = productImg.parentElement.classList.contains('desktop-zoom-enabled');
                const hasMobileClass = productImg.parentElement.classList.contains('mobile-zoom-enabled');
                
                this.log(
                    'CSS Classes Applied',
                    hasDesktopClass || hasMobileClass,
                    `Desktop: ${hasDesktopClass}, Mobile: ${hasMobileClass}`
                );
            }
            
            // Check lazy loading setup
            if (window.zoomIntegration) {
                this.log(
                    'Lazy Loading Observer',
                    !!window.zoomIntegration.lazyLoadObserver,
                    window.zoomIntegration.lazyLoadObserver ? 'Active' : 'Not active'
                );
                
                this.log(
                    'High-Res Image Mapping',
                    window.zoomIntegration.highResImages.size > 0,
                    `${window.zoomIntegration.highResImages.size} images mapped`
                );
            }
            
            // Summary
            const passed = this.results.filter(r => r.passed).length;
            const total = this.results.length;
            const percentage = Math.round((passed / total) * 100);
            
            console.log(`\n📊 Validation Summary: ${passed}/${total} tests passed (${percentage}%)`);
            
            if (percentage === 100) {
                console.log('🎉 All validations passed! Zoom integration is working correctly.');
            } else if (percentage >= 80) {
                console.log('⚠️ Most validations passed. Check failed tests above.');
            } else {
                console.log('❌ Multiple validations failed. Check implementation.');
            }
            
            return { passed, total, percentage, results: this.results };
        },
        
        testZoomFunctionality() {
            console.log('\n🧪 Testing Zoom Functionality...\n');
            
            if (!window.zoomIntegration) {
                console.log('❌ Cannot test: ZoomIntegration not available');
                return;
            }
            
            const state = window.zoomIntegration.getZoomState();
            
            // Test desktop zoom
            if (state.isDesktop && window.zoomIntegration.desktopZoom) {
                const desktopZoom = window.zoomIntegration.desktopZoom;
                
                // Test zoom level validation
                const originalZoom = desktopZoom.zoomLevel;
                desktopZoom.setZoomLevel(10); // Should be clamped to 4
                this.log(
                    'Desktop Zoom Level Clamping',
                    desktopZoom.zoomLevel === 4,
                    `Set to 10, clamped to ${desktopZoom.zoomLevel}`
                );
                
                desktopZoom.setZoomLevel(1); // Should be clamped to 2
                this.log(
                    'Desktop Zoom Level Minimum',
                    desktopZoom.zoomLevel === 2,
                    `Set to 1, clamped to ${desktopZoom.zoomLevel}`
                );
                
                // Restore original zoom
                desktopZoom.setZoomLevel(originalZoom);
            }
            
            // Test mobile zoom
            if (state.isMobile && window.zoomIntegration.mobileZoom) {
                const mobileZoom = window.zoomIntegration.mobileZoom;
                
                // Test zoom limits
                const originalZoom = mobileZoom.currentZoom;
                mobileZoom.setZoom(10); // Should be clamped to 5
                this.log(
                    'Mobile Zoom Level Maximum',
                    mobileZoom.currentZoom === 5,
                    `Set to 10, clamped to ${mobileZoom.currentZoom}`
                );
                
                mobileZoom.setZoom(0.5); // Should be clamped to 1
                this.log(
                    'Mobile Zoom Level Minimum',
                    mobileZoom.currentZoom === 1,
                    `Set to 0.5, clamped to ${mobileZoom.currentZoom}`
                );
                
                // Restore original zoom
                mobileZoom.setZoom(originalZoom);
            }
            
            console.log('✅ Functionality tests completed');
        },
        
        simulateResponsiveChange() {
            console.log('\n📱 Simulating Responsive Behavior...\n');
            
            if (!window.zoomIntegration) {
                console.log('❌ Cannot test: ZoomIntegration not available');
                return;
            }
            
            const originalWidth = window.innerWidth;
            
            // Simulate mobile width
            Object.defineProperty(window, 'innerWidth', {
                writable: true,
                configurable: true,
                value: 600
            });
            
            window.zoomIntegration.detectDeviceType();
            const mobileState = window.zoomIntegration.getZoomState();
            
            this.log(
                'Mobile Detection Simulation',
                mobileState.isMobile,
                `Width: 600px, Mobile: ${mobileState.isMobile}`
            );
            
            // Simulate desktop width
            Object.defineProperty(window, 'innerWidth', {
                writable: true,
                configurable: true,
                value: 1200
            });
            
            window.zoomIntegration.detectDeviceType();
            const desktopState = window.zoomIntegration.getZoomState();
            
            this.log(
                'Desktop Detection Simulation',
                desktopState.isDesktop,
                `Width: 1200px, Desktop: ${desktopState.isDesktop}`
            );
            
            // Restore original width
            Object.defineProperty(window, 'innerWidth', {
                writable: true,
                configurable: true,
                value: originalWidth
            });
            
            window.zoomIntegration.detectDeviceType();
            console.log('✅ Responsive simulation completed');
        }
    };
    
    // Auto-run validation when page loads
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(() => validation.validateComponents(), 500);
        });
    } else {
        setTimeout(() => validation.validateComponents(), 500);
    }
    
    // Expose validation functions globally for manual testing
    window.zoomValidation = {
        validate: () => validation.validateComponents(),
        testFunctionality: () => validation.testZoomFunctionality(),
        testResponsive: () => validation.simulateResponsiveChange(),
        runAll: () => {
            validation.validateComponents();
            validation.testZoomFunctionality();
            validation.simulateResponsiveChange();
        }
    };
    
})();