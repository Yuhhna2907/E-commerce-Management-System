/**
 * CSRF Auto-Injection for Fetch API
 * Automatically adds CSRF token to all POST/PUT/DELETE/PATCH requests
 * 
 * Usage: Just include this script in your page, no code changes needed!
 */

(function() {
    'use strict';
    
    // Store original fetch
    const originalFetch = window.fetch;
    
    /**
     * Get CSRF token from meta tag or cookie
     */
    function getCsrfToken() {
        // Try meta tag first
        const metaTag = document.querySelector('meta[name="_csrf"]');
        if (metaTag) {
            return metaTag.getAttribute('content');
        }
        
        // Fallback to cookie
        const name = 'XSRF-TOKEN=';
        const decodedCookie = decodeURIComponent(document.cookie);
        const cookieArray = decodedCookie.split(';');
        
        for (let i = 0; i < cookieArray.length; i++) {
            let cookie = cookieArray[i].trim();
            if (cookie.indexOf(name) === 0) {
                return cookie.substring(name.length, cookie.length);
            }
        }
        return null;
    }
    
    /**
     * Get CSRF header name from meta tag
     */
    function getCsrfHeaderName() {
        const metaTag = document.querySelector('meta[name="_csrf_header"]');
        return metaTag ? metaTag.getAttribute('content') : 'X-XSRF-TOKEN';
    }
    
    /**
     * Override fetch to automatically include CSRF token
     */
    window.fetch = function(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        
        // Only add CSRF token for state-changing methods
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            const token = getCsrfToken();
            const headerName = getCsrfHeaderName();
            
            if (token) {
                // Initialize headers if not present
                options.headers = options.headers || {};
                
                // Add CSRF token header
                if (options.headers instanceof Headers) {
                    options.headers.set(headerName, token);
                } else {
                    options.headers[headerName] = token;
                }
            }
        }
        
        // Call original fetch
        return originalFetch(url, options);
    };
    
    console.log('[CSRF Auto] CSRF token auto-injection enabled for fetch API');
})();
