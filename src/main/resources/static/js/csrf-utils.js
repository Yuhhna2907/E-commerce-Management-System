/**
 * CSRF Utility Functions
 * Provides helper functions for including CSRF tokens in AJAX requests
 */

/**
 * Get CSRF token from cookie
 * @returns {string|null} CSRF token value or null if not found
 */
function getCsrfToken() {
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
 * Get CSRF token from meta tag (alternative method)
 * @returns {string|null} CSRF token value or null if not found
 */
function getCsrfTokenFromMeta() {
    const metaTag = document.querySelector('meta[name="_csrf"]');
    return metaTag ? metaTag.getAttribute('content') : null;
}

/**
 * Get CSRF header name from meta tag
 * @returns {string} CSRF header name, defaults to 'X-XSRF-TOKEN'
 */
function getCsrfHeaderName() {
    const metaTag = document.querySelector('meta[name="_csrf_header"]');
    return metaTag ? metaTag.getAttribute('content') : 'X-XSRF-TOKEN';
}

/**
 * Create fetch options with CSRF token included
 * @param {string} method - HTTP method (POST, PUT, DELETE, etc.)
 * @param {object} additionalOptions - Additional fetch options to merge
 * @returns {object} Fetch options with CSRF token header
 */
function createFetchOptionsWithCsrf(method = 'POST', additionalOptions = {}) {
    const token = getCsrfToken() || getCsrfTokenFromMeta();
    const headerName = getCsrfHeaderName();
    
    const options = {
        method: method,
        headers: {
            ...additionalOptions.headers
        },
        ...additionalOptions
    };
    
    if (token) {
        options.headers[headerName] = token;
    }
    
    return options;
}

/**
 * Perform a fetch request with CSRF token automatically included
 * @param {string} url - Request URL
 * @param {object} options - Fetch options
 * @returns {Promise} Fetch promise
 */
function fetchWithCsrf(url, options = {}) {
    const method = options.method || 'GET';
    
    // Only add CSRF token for state-changing methods
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method.toUpperCase())) {
        const token = getCsrfToken() || getCsrfTokenFromMeta();
        const headerName = getCsrfHeaderName();
        
        if (token) {
            options.headers = options.headers || {};
            options.headers[headerName] = token;
        }
    }
    
    return fetch(url, options);
}

/**
 * Add CSRF token to FormData
 * @param {FormData} formData - FormData object to add token to
 * @returns {FormData} FormData with CSRF token added
 */
function addCsrfToFormData(formData) {
    const token = getCsrfToken() || getCsrfTokenFromMeta();
    if (token) {
        formData.append('_csrf', token);
    }
    return formData;
}
