/**
 * ReviewImageUpload.js
 * Component for uploading review images with client-side validation
 * 
 * Features:
 * - File selection with drag & drop support
 * - Client-side validation (type, size, count)
 * - Preview thumbnails with remove buttons
 * - Error display
 * - Integration with review form submission
 */

class ReviewImageUpload {
    constructor(formElement, options = {}) {
        this.form = formElement;
        this.maxFiles = options.maxFiles || 5;
        this.maxFileSize = options.maxFileSize || 5 * 1024 * 1024; // 5MB
        this.allowedTypes = options.allowedTypes || ['image/jpeg', 'image/png', 'image/webp'];
        this.allowedExtensions = options.allowedExtensions || ['jpg', 'jpeg', 'png', 'webp'];
        this.selectedFiles = [];
        this.init();
    }
    
    init() {
        this.createUploadUI();
        this.attachEventListeners();
    }
    
    createUploadUI() {
        const uploadHTML = `
            <div class="review-image-upload">
                <label class="upload-label">
                    <input type="file" 
                           class="upload-input" 
                           accept="image/jpeg,image/png,image/webp" 
                           multiple 
                           style="display: none;">
                    <div class="upload-button">
                        <i class="bi bi-camera"></i>
                        <span>Thêm ảnh (${this.maxFiles} ảnh tối đa)</span>
                    </div>
                </label>
                <div class="image-preview-container"></div>
                <div class="upload-error" style="display: none;"></div>
            </div>
        `;
        
        this.form.insertAdjacentHTML('beforeend', uploadHTML);
        this.fileInput = this.form.querySelector('.upload-input');
        this.previewContainer = this.form.querySelector('.image-preview-container');
        this.errorContainer = this.form.querySelector('.upload-error');
    }
    
    attachEventListeners() {
        // File input change
        this.fileInput.addEventListener('change', (e) => {
            this.handleFileSelect(e.target.files);
            // Reset input to allow selecting same file again
            e.target.value = '';
        });
        
        // Drag and drop support
        const uploadLabel = this.form.querySelector('.upload-label');
        
        uploadLabel.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadLabel.classList.add('drag-over');
        });
        
        uploadLabel.addEventListener('dragleave', () => {
            uploadLabel.classList.remove('drag-over');
        });
        
        uploadLabel.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadLabel.classList.remove('drag-over');
            this.handleFileSelect(e.dataTransfer.files);
        });
    }
    
    handleFileSelect(files) {
        this.clearError();
        
        // Validate số lượng file
        if (this.selectedFiles.length + files.length > this.maxFiles) {
            this.showError(`Chỉ được chọn tối đa ${this.maxFiles} ảnh`);
            return;
        }
        
        Array.from(files).forEach(file => {
            // Validate file
            const validation = this.validateFile(file);
            if (!validation.valid) {
                this.showError(validation.error);
                return;
            }
            
            this.selectedFiles.push(file);
            this.createPreview(file);
        });
        
        // Update button text
        this.updateButtonText();
    }
    
    validateFile(file) {
        // Check type
        if (!this.allowedTypes.includes(file.type)) {
            return {
                valid: false,
                error: `File ${file.name}: Định dạng không hợp lệ. Chỉ chấp nhận JPG, PNG, WebP`
            };
        }
        
        // Check extension
        const extension = this.getFileExtension(file.name).toLowerCase();
        if (!this.allowedExtensions.includes(extension)) {
            return {
                valid: false,
                error: `File ${file.name}: Phần mở rộng không hợp lệ`
            };
        }
        
        // Check size
        if (file.size > this.maxFileSize) {
            const maxSizeMB = (this.maxFileSize / 1024 / 1024).toFixed(0);
            return {
                valid: false,
                error: `File ${file.name}: Kích thước vượt quá ${maxSizeMB}MB`
            };
        }
        
        // Check for duplicate
        if (this.selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
            return {
                valid: false,
                error: `File ${file.name}: Đã được chọn`
            };
        }
        
        return { valid: true };
    }
    
    getFileExtension(filename) {
        const lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex === -1) {
            return '';
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    createPreview(file) {
        const reader = new FileReader();
        
        reader.onload = (e) => {
            const previewId = `preview-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
            const previewHTML = `
                <div class="image-preview" id="${previewId}" data-filename="${file.name}">
                    <img src="${e.target.result}" alt="Preview">
                    <button type="button" class="remove-image" title="Xóa ảnh">
                        <i class="bi bi-x-circle-fill"></i>
                    </button>
                    <div class="image-info">
                        <span class="file-size">${this.formatFileSize(file.size)}</span>
                    </div>
                </div>
            `;
            
            this.previewContainer.insertAdjacentHTML('beforeend', previewHTML);
            
            // Attach remove listener
            const preview = document.getElementById(previewId);
            preview.querySelector('.remove-image').addEventListener('click', () => {
                this.removeFile(file.name);
            });
        };
        
        reader.onerror = () => {
            this.showError(`Không thể đọc file: ${file.name}`);
        };
        
        reader.readAsDataURL(file);
    }
    
    removeFile(filename) {
        this.selectedFiles = this.selectedFiles.filter(f => f.name !== filename);
        const preview = this.previewContainer.querySelector(`[data-filename="${filename}"]`);
        if (preview) {
            preview.remove();
        }
        this.updateButtonText();
        this.clearError();
    }
    
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
    
    updateButtonText() {
        const button = this.form.querySelector('.upload-button span');
        const remaining = this.maxFiles - this.selectedFiles.length;
        if (remaining > 0) {
            button.textContent = `Thêm ảnh (còn ${remaining}/${this.maxFiles})`;
        } else {
            button.textContent = `Đã đủ ${this.maxFiles} ảnh`;
        }
        
        // Disable input if max reached
        this.fileInput.disabled = remaining === 0;
        const uploadButton = this.form.querySelector('.upload-button');
        if (remaining === 0) {
            uploadButton.classList.add('disabled');
        } else {
            uploadButton.classList.remove('disabled');
        }
    }
    
    showError(message) {
        this.errorContainer.textContent = message;
        this.errorContainer.style.display = 'block';
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            this.clearError();
        }, 5000);
    }
    
    clearError() {
        this.errorContainer.textContent = '';
        this.errorContainer.style.display = 'none';
    }
    
    getFiles() {
        return this.selectedFiles;
    }
    
    hasFiles() {
        return this.selectedFiles.length > 0;
    }
    
    getFileCount() {
        return this.selectedFiles.length;
    }
    
    reset() {
        this.selectedFiles = [];
        this.previewContainer.innerHTML = '';
        this.fileInput.value = '';
        this.fileInput.disabled = false;
        this.clearError();
        this.updateButtonText();
    }
    
    destroy() {
        // Clean up event listeners and DOM elements
        const uploadSection = this.form.querySelector('.review-image-upload');
        if (uploadSection) {
            uploadSection.remove();
        }
        this.selectedFiles = [];
    }
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ReviewImageUpload;
}
