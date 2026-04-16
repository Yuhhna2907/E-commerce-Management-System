/**
 * Unit Tests for SwipeToDelete Component
 * 
 * Tests the delete confirmation modal functionality and cart updates
 * Requirements: 3.4, 3.5, 10.5
 */

// Mock dependencies
const mockMobileUtils = {
    isMobile: () => true,
    getTouchCoordinates: (e) => ({ x: e.clientX || 0, y: e.clientY || 0 }),
    addPassiveListener: (element, event, handler, passive) => {
        element.addEventListener(event, handler, { passive });
        return () => element.removeEventListener(event, handler);
    },
    watchViewport: (onMobile, onDesktop) => {
        onMobile();
        return () => {};
    },
    announceToScreenReader: (message) => console.log('Screen reader:', message),
    clamp: (value, min, max) => Math.min(Math.max(value, min), max),
    lerp: (start, end, t) => start + (end - start) * t
};

// Mock global objects
global.MobileUtils = mockMobileUtils;
global.bootstrap = {
    Modal: class {
        constructor(element) {
            this.element = element;
        }
        show() {
            this.element.style.display = 'block';
        }
        hide() {
            this.element.style.display = 'none';
        }
        static getInstance(element) {
            return new this(element);
        }
    }
};

// Mock DOM
const { JSDOM } = require('jsdom');
const dom = new JSDOM(`
    <!DOCTYPE html>
    <html>
    <body>
        <div class="cart-item swipeable" data-id="123">
            <div class="cart-item__content">
                <img src="test.jpg" alt="Test Product">
                <div class="product-title">Test Product</div>
                <div class="product-specs">Red | 128GB | 8GB RAM</div>
            </div>
            <button class="cart-item__delete" aria-label="Delete Test Product">
                <i class="bi bi-trash"></i>
            </button>
        </div>
        
        <div id="confirmDeleteModal" style="display: none;">
            <div id="deleteProductName"></div>
            <div id="deleteProductImage"></div>
            <div id="deleteProductSpecs"></div>
            <form id="deleteForm">
                <button class="js-delete-btn">
                    <span class="js-delete-btn-text">Delete</span>
                </button>
            </form>
        </div>
        
        <div class="js-total-items">2</div>
        <div class="js-cart-total" data-raw-total="100000">100,000 ₫</div>
        <button class="js-checkout-btn">Checkout</button>
        <a class="js-checkout-link">Checkout Link</a>
    </body>
    </html>
`);

global.document = dom.window.document;
global.window = dom.window;
global.fetch = jest.fn();
global.Intl = {
    NumberFormat: class {
        constructor() {}
        format(number) {
            return number.toLocaleString('vi-VN');
        }
    }
};

// Load the SwipeToDelete component
const SwipeToDelete = require('../../../../../main/resources/static/js/mobile/SwipeToDelete.js');

describe('SwipeToDelete Component', () => {
    let swipeToDelete;
    let element;
    let mockOnDelete;

    beforeEach(() => {
        // Reset DOM
        element = document.querySelector('.swipeable');
        mockOnDelete = jest.fn();
        
        // Reset fetch mock
        fetch.mockClear();
        
        // Create new instance
        swipeToDelete = new SwipeToDelete(element, mockOnDelete);
    });

    afterEach(() => {
        if (swipeToDelete) {
            swipeToDelete.destroy();
        }
    });

    describe('Initialization', () => {
        test('should initialize with correct elements', () => {
            expect(swipeToDelete.element).toBe(element);
            expect(swipeToDelete.content).toBeTruthy();
            expect(swipeToDelete.deleteButton).toBeTruthy();
            expect(swipeToDelete.threshold).toBe(100);
        });

        test('should set up accessibility attributes', () => {
            const deleteButton = element.querySelector('.cart-item__delete');
            expect(deleteButton.getAttribute('aria-label')).toContain('Test Product');
        });
    });

    describe('Delete Confirmation Modal', () => {
        test('should populate modal with product information', async () => {
            // Mock modal elements
            const modal = document.getElementById('confirmDeleteModal');
            const deleteProductName = document.getElementById('deleteProductName');
            const deleteProductImage = document.getElementById('deleteProductImage');
            const deleteProductSpecs = document.getElementById('deleteProductSpecs');
            const deleteForm = document.getElementById('deleteForm');

            await swipeToDelete.confirmDelete();

            expect(deleteProductName.textContent).toBe('Test Product');
            expect(deleteProductImage.src).toContain('test.jpg');
            expect(deleteProductSpecs.textContent).toBe('Red | 128GB | 8GB RAM');
            expect(deleteForm.action).toContain('/user/cart/remove/123');
        });

        test('should show modal when confirmDelete is called', async () => {
            const modal = document.getElementById('confirmDeleteModal');
            
            await swipeToDelete.confirmDelete();
            
            expect(modal.style.display).toBe('block');
        });

        test('should close swipe state when modal opens', async () => {
            swipeToDelete.isOpen = true;
            element.classList.add('swiped');
            
            await swipeToDelete.confirmDelete();
            
            expect(swipeToDelete.isOpen).toBe(false);
            expect(element.classList.contains('swiped')).toBe(false);
        });
    });

    describe('Cart Updates After Deletion', () => {
        test('should update cart totals after successful deletion', async () => {
            // Mock successful API response
            fetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    success: true,
                    itemCount: 1,
                    total: 50000
                })
            });

            await swipeToDelete.updateCartTotals();

            expect(fetch).toHaveBeenCalledWith('/user/cart/api/totals');
            
            const totalItemsElement = document.querySelector('.js-total-items');
            const cartTotalElement = document.querySelector('.js-cart-total');
            
            expect(totalItemsElement.textContent).toBe('1');
            expect(cartTotalElement.textContent).toBe('50,000 ₫');
        });

        test('should dispatch cart:updated event', () => {
            const eventSpy = jest.spyOn(document, 'dispatchEvent');
            
            swipeToDelete.dispatchCartUpdatedEvent();
            
            expect(eventSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    type: 'cart:updated',
                    detail: expect.objectContaining({
                        source: 'swipe-to-delete'
                    })
                })
            );
        });

        test('should disable checkout when cart is empty', async () => {
            const checkoutBtn = document.querySelector('.js-checkout-btn');
            const checkoutLink = document.querySelector('.js-checkout-link');
            
            swipeToDelete.updateCheckoutButtonState(0);
            
            expect(checkoutBtn.disabled).toBe(true);
            expect(checkoutLink.style.pointerEvents).toBe('none');
        });
    });

    describe('Form Submission Handling', () => {
        test('should handle successful form submission', async () => {
            const form = document.getElementById('deleteForm');
            const deleteBtn = form.querySelector('.js-delete-btn');
            
            // Mock successful deletion
            fetch.mockResolvedValueOnce({ ok: true });
            fetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    success: true,
                    itemCount: 1,
                    total: 50000
                })
            });

            swipeToDelete.setupDeleteFormHandler(form, '123', 'Test Product');
            
            // Simulate form submission
            const submitEvent = new dom.window.Event('submit');
            form.dispatchEvent(submitEvent);

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 100));

            expect(fetch).toHaveBeenCalledWith('/user/cart/remove/123', expect.any(Object));
        });

        test('should show loading state during deletion', () => {
            const form = document.getElementById('deleteForm');
            const deleteBtn = form.querySelector('.js-delete-btn');
            const deleteBtnText = form.querySelector('.js-delete-btn-text');
            
            swipeToDelete.setupDeleteFormHandler(form, '123', 'Test Product');
            
            // Simulate form submission
            const submitEvent = new dom.window.Event('submit');
            form.dispatchEvent(submitEvent);
            
            expect(deleteBtn.disabled).toBe(true);
            expect(deleteBtnText.innerHTML).toContain('spinner-border');
        });
    });

    describe('Animation and UI Updates', () => {
        test('should animate item removal', () => {
            swipeToDelete.animateItemRemoval();
            
            expect(element.classList.contains('removing')).toBe(true);
            expect(element.style.opacity).toBe('0');
            expect(element.style.transform).toContain('translateX(-100%)');
        });

        test('should create toast notifications', () => {
            const initialToastCount = document.querySelectorAll('[style*="position: fixed"]').length;
            
            swipeToDelete.createSimpleToast('Test message', 'success');
            
            const toasts = document.querySelectorAll('[style*="position: fixed"]');
            expect(toasts.length).toBe(initialToastCount + 1);
        });
    });

    describe('Error Handling', () => {
        test('should handle API errors gracefully', async () => {
            fetch.mockRejectedValueOnce(new Error('Network error'));
            
            const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
            
            await swipeToDelete.updateCartTotals();
            
            expect(consoleSpy).toHaveBeenCalledWith('Error updating cart totals:', expect.any(Error));
            
            consoleSpy.mockRestore();
        });

        test('should show fallback confirmation when modal not found', async () => {
            // Remove modal from DOM
            const modal = document.getElementById('confirmDeleteModal');
            modal.remove();
            
            // Mock window.confirm
            global.confirm = jest.fn(() => true);
            
            await swipeToDelete.confirmDelete();
            
            expect(global.confirm).toHaveBeenCalled();
        });
    });

    describe('Integration Requirements', () => {
        test('should meet Requirement 3.4: Display confirmation modal', async () => {
            const modal = document.getElementById('confirmDeleteModal');
            
            await swipeToDelete.confirmDelete();
            
            // Modal should be visible
            expect(modal.style.display).toBe('block');
            
            // Modal should contain product information
            const productName = document.getElementById('deleteProductName');
            expect(productName.textContent).toBe('Test Product');
        });

        test('should meet Requirement 3.5: Remove item and update cart total', async () => {
            // Mock successful deletion and cart update
            fetch.mockResolvedValueOnce({ ok: true });
            fetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    success: true,
                    itemCount: 1,
                    total: 50000
                })
            });

            await swipeToDelete.handleSuccessfulDelete('123', 'Test Product');

            // Should animate removal
            expect(element.classList.contains('removing')).toBe(true);
            
            // Should update cart totals
            expect(fetch).toHaveBeenCalledWith('/user/cart/api/totals');
        });

        test('should meet Requirement 10.5: Use existing modal system', async () => {
            const modal = document.getElementById('confirmDeleteModal');
            
            await swipeToDelete.confirmDelete();
            
            // Should use Bootstrap Modal
            expect(modal.style.display).toBe('block');
            
            // Should populate existing modal elements
            const deleteForm = document.getElementById('deleteForm');
            expect(deleteForm.action).toContain('/user/cart/remove/123');
        });
    });
});

// Property-based test for swipe threshold
describe('SwipeToDelete Property Tests', () => {
    test('Property: Delete button reveals if and only if swipe distance >= 100px', () => {
        const element = document.querySelector('.swipeable');
        const swipeToDelete = new SwipeToDelete(element);
        
        // Test various swipe distances
        const testCases = [
            { distance: 50, shouldReveal: false },
            { distance: 99, shouldReveal: false },
            { distance: 100, shouldReveal: true },
            { distance: 150, shouldReveal: true },
            { distance: 200, shouldReveal: true }
        ];
        
        testCases.forEach(({ distance, shouldReveal }) => {
            // Simulate swipe
            swipeToDelete.startX = 200;
            swipeToDelete.currentX = 200 - distance; // Left swipe
            swipeToDelete.swiping = true;
            
            // Simulate touch end
            swipeToDelete.handleTouchEnd({});
            
            if (shouldReveal) {
                expect(swipeToDelete.isOpen).toBe(true);
                expect(element.classList.contains('swiped')).toBe(true);
            } else {
                expect(swipeToDelete.isOpen).toBe(false);
                expect(element.classList.contains('swiped')).toBe(false);
            }
            
            // Reset for next test
            swipeToDelete.close();
        });
        
        swipeToDelete.destroy();
    });
});