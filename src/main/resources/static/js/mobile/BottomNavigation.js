/**
 * BottomNavigation.js
 * 
 * Mobile-only bottom navigation bar component with cart badge and active state management.
 * Automatically shows/hides based on viewport width (< 768px = mobile).
 * 
 * Features:
 * - Active page highlighting with aria-current
 * - Dynamic cart badge updates
 * - Responsive viewport handling
 * - Event-driven cart count updates
 * 
 * Requirements: 1.5, 1.8, 9.1, 9.2
 */

class BottomNavigation {
  constructor() {
    this.nav = null;
    this.cartBadge = null;
    this.mediaQuery = window.matchMedia('(max-width: 767px)');
    this.resizeHandler = null;
    
    this.init();
  }

  /**
   * Initialize the bottom navigation component
   * Only activates on mobile viewports (< 768px)
   */
  init() {
    // Only initialize if we're on mobile viewport
    if (!this.mediaQuery.matches) {
      return;
    }
    
    this.nav = document.querySelector('.bottom-nav');
    
    if (!this.nav) {
      console.warn('BottomNavigation: .bottom-nav element not found');
      return;
    }
    
    this.cartBadge = this.nav.querySelector('.bottom-nav__badge');
    
    // Set active navigation item based on current page
    this.setActiveItem();
    
    // Update cart badge with current count
    this.updateCartBadge();
    
    // Setup event listeners
    this.setupEventListeners();
  }

  /**
   * Set the active navigation item based on current URL path
   * Adds aria-current="page" to the matching navigation item
   * 
   * Requirement 1.5: Display active navigation state for current page
   */
  setActiveItem() {
    const currentPath = window.location.pathname;
    const items = this.nav.querySelectorAll('.bottom-nav__item');
    
    items.forEach(item => {
      const href = item.getAttribute('href');
      
      // Match exact path or special case for home/products
      const isActive = currentPath === href || 
                      (currentPath === '/user/products' && href === '/') ||
                      (currentPath === '/' && href === '/');
      
      if (isActive) {
        item.setAttribute('aria-current', 'page');
      } else {
        item.removeAttribute('aria-current');
      }
    });
  }

  /**
   * Update the cart badge with current cart item count
   * Fetches count from server API endpoint
   * 
   * Requirement 1.8: Display cart badge when cart contains items
   */
  async updateCartBadge() {
    if (!this.cartBadge) {
      return;
    }
    
    try {
      const response = await fetch('/api/cart/count');
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      
      // Only show badge if count > 0
      if (data.count > 0) {
        this.cartBadge.setAttribute('data-count', data.count);
        this.cartBadge.style.display = 'inline-block';
      } else {
        this.cartBadge.removeAttribute('data-count');
        this.cartBadge.style.display = 'none';
      }
    } catch (error) {
      console.error('Failed to fetch cart count:', error);
      // Fallback: try to get count from localStorage or hide badge
      this.cartBadge.style.display = 'none';
    }
  }

  /**
   * Setup event listeners for cart updates and viewport changes
   * 
   * Requirements:
   * - 1.8: Listen for cart:updated events
   * - 9.1, 9.2: Handle viewport resize to toggle visibility
   */
  setupEventListeners() {
    // Listen for custom cart:updated events
    document.addEventListener('cart:updated', () => {
      this.updateCartBadge();
    });
    
    // Handle viewport resize with matchMedia API (more efficient than resize event)
    // Requirement 9.1, 9.2: Toggle visibility based on viewport width
    this.resizeHandler = (e) => {
      if (e.matches) {
        // Mobile viewport (< 768px) - show navigation
        if (this.nav) {
          this.nav.style.display = 'flex';
        }
      } else {
        // Desktop viewport (>= 768px) - hide navigation
        if (this.nav) {
          this.nav.style.display = 'none';
        }
      }
    };
    
    // Use matchMedia listener for better performance
    if (this.mediaQuery.addEventListener) {
      this.mediaQuery.addEventListener('change', this.resizeHandler);
    } else {
      // Fallback for older browsers
      this.mediaQuery.addListener(this.resizeHandler);
    }
  }

  /**
   * Cleanup method to remove event listeners
   * Call this when destroying the component
   */
  destroy() {
    if (this.resizeHandler) {
      if (this.mediaQuery.removeEventListener) {
        this.mediaQuery.removeEventListener('change', this.resizeHandler);
      } else {
        // Fallback for older browsers
        this.mediaQuery.removeListener(this.resizeHandler);
      }
    }
    
    // Remove cart:updated listener
    document.removeEventListener('cart:updated', this.updateCartBadge);
  }
}

// Auto-initialize on DOM ready if on mobile viewport
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    if (window.matchMedia('(max-width: 767px)').matches) {
      window.bottomNavigation = new BottomNavigation();
    }
  });
} else {
  // DOM already loaded
  if (window.matchMedia('(max-width: 767px)').matches) {
    window.bottomNavigation = new BottomNavigation();
  }
}
