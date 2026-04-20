/**
 * Enhanced Image Upload with Progress Tracking
 * Features:
 * - Progress bar
 * - Drag & drop
 * - Image preview
 * - Reordering
 * - Alt text editing
 */

class EnhancedImageUpload {
    constructor(options) {
        this.reviewId = options.reviewId;
        this.maxFiles = options.maxFiles || 5;
        this.maxFileSize = options.maxFileSize || 5 * 1024 * 1024; // 5MB
        this.allowedTypes = options.allowedTypes || ['image/jpeg', 'image/png', 'image/webp'];
        
        this.uploadContainer = document.getElementById(options.containerId);
        this.fileInput = document.getElementById(options.fileInputId);
        this.progressContainer = document.getElementById(options.progressContainerId);
        this.previewContainer = document.getElementById(options.previewContainerId);
        
        this.currentSessionId = null;
        this.progressInterval = null;
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
        this.setupDragAndDrop();
        this.loadExistingImages();
    }
    
    setupEventListeners() {
        // File input change
        this.fileInput.addEventListener('change', (e) => {
            this.handleFileSelect(e.target.files);
        });
        
        // Upload button click
        const uploadBtn = document.getElementById('upload-btn');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', () => {
                this.fileInput.click();
            });
        }
    }
    
    setupDragAndDrop() {
        const dropZone = document.getElementById('drop-zone');
        if (!dropZone) return;
        
        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('drag-over');
        });
        
        dropZone.addEventListener('dragleave', () => {
            dropZone.classList.remove('drag-over');
        });
        
        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');
            this.handleFileSelect(e.dataTransfer.files);
        });
    }
    
    handleFileSelect(files) {
        const fileArray = Array.from(files);
        
        // Validate files
        const validFiles = this.validateFiles(fileArray);
        if (validFiles.length === 0) return;
        
        // Check total count
        const currentCount = this.getCurrentImageCount();
        if (currentCount + validFiles.length > this.maxFiles) {
            this.showError(`Chỉ được upload tối đa ${this.maxFiles} ảnh. Hiện tại có ${currentCount} ảnh.`);
            return;
        }
        
        // Start upload
        this.uploadFiles(validFiles);
    }
    
    validateFiles(files) {
        const validFiles = [];
        
        for (const file of files) {
            // Check file type
            if (!this.allowedTypes.includes(file.type)) {
                this.showError(`File ${file.name} không đúng định dạng. Chỉ chấp nhận JPG, PNG, WebP.`);
                continue;
            }
            
            // Check file size
            if (file.size > this.maxFileSize) {
                this.showError(`File ${file.name} quá lớn. Tối đa ${this.maxFileSize / 1024 / 1024}MB.`);
                continue;
            }
            
            validFiles.push(file);
        }
        
        return validFiles;
    }
    
    async uploadFiles(files) {
        const formData = new FormData();
        files.forEach(file => {
            formData.append('images', file);
        });
        
        try {
            this.showProgress();
            
            const response = await fetch(`/user/reviews/${this.reviewId}/images/enhanced`, {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.currentSessionId = result.sessionId;
                this.startProgressTracking();
                this.showSuccess(result.message);
            } else {
                this.hideProgress();
                this.showError(result.message);
            }
            
        } catch (error) {
            this.hideProgress();
            this.showError('Lỗi kết nối. Vui lòng thử lại.');
            console.error('Upload error:', error);
        }
    }
    
    startProgressTracking() {
        if (!this.currentSessionId) return;
        
        this.progressInterval = setInterval(async () => {
            try {
                const response = await fetch(`/user/reviews/upload-progress/${this.currentSessionId}`);
                const result = await response.json();
                
                if (result.success) {
                    const progress = result.progress;
                    this.updateProgress(progress);
                    
                    if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
                        this.stopProgressTracking();
                        this.hideProgress();
                        
                        if (progress.status === 'COMPLETED') {
                            this.loadExistingImages(); // Refresh image list
                        }
                    }
                }
            } catch (error) {
                console.error('Progress tracking error:', error);
                this.stopProgressTracking();
                this.hideProgress();
            }
        }, 1000);
    }
    
    stopProgressTracking() {
        if (this.progressInterval) {
            clearInterval(this.progressInterval);
            this.progressInterval = null;
        }
    }
    
    updateProgress(progress) {
        const progressBar = document.getElementById('upload-progress-bar');
        const progressText = document.getElementById('upload-progress-text');
        const currentFile = document.getElementById('current-file');
        
        if (progressBar) {
            progressBar.style.width = `${progress.progressPercentage}%`;
        }
        
        if (progressText) {
            progressText.textContent = `${progress.processedFiles}/${progress.totalFiles} files (${Math.round(progress.progressPercentage)}%)`;
        }
        
        if (currentFile && progress.currentFileName) {
            currentFile.textContent = `Đang xử lý: ${progress.currentFileName}`;
        }
    }
    
    showProgress() {
        if (this.progressContainer) {
            this.progressContainer.style.display = 'block';
        }
    }
    
    hideProgress() {
        if (this.progressContainer) {
            this.progressContainer.style.display = 'none';
        }
    }
    
    async loadExistingImages() {
        try {
            const response = await fetch(`/user/reviews/${this.reviewId}/images`);
            const result = await response.json();
            
            if (result.success) {
                this.renderImagePreviews(result.images);
            }
        } catch (error) {
            console.error('Error loading images:', error);
        }
    }
    
    renderImagePreviews(images) {
        if (!this.previewContainer) return;
        
        this.previewContainer.innerHTML = '';
        
        images.forEach((image, index) => {
            const imageElement = this.createImagePreview(image, index);
            this.previewContainer.appendChild(imageElement);
        });
        
        this.setupSortable();
    }
    
    createImagePreview(image, index) {
        const div = document.createElement('div');
        div.className = 'image-preview';
        div.dataset.imageId = image.id;
        
        div.innerHTML = `
            <div class="image-wrapper">
                <img src="${image.thumbnailUrl || image.imageUrl}" alt="${image.altText || ''}" loading="lazy">
                <div class="image-overlay">
                    <button type="button" class="btn-edit" onclick="editAltText(${image.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button type="button" class="btn-delete" onclick="deleteImage(${image.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="image-info">
                <small>${image.width}x${image.height} • ${this.formatFileSize(image.fileSize)}</small>
                <input type="text" class="alt-text-input" value="${image.altText || ''}" 
                       placeholder="Mô tả ảnh..." onblur="updateAltText(${image.id}, this.value)">
            </div>
        `;
        
        return div;
    }
    
    setupSortable() {
        // Simple drag and drop reordering
        let draggedElement = null;
        
        const previews = this.previewContainer.querySelectorAll('.image-preview');
        
        previews.forEach(preview => {
            preview.draggable = true;
            
            preview.addEventListener('dragstart', (e) => {
                draggedElement = preview;
                preview.classList.add('dragging');
            });
            
            preview.addEventListener('dragend', () => {
                preview.classList.remove('dragging');
                draggedElement = null;
            });
            
            preview.addEventListener('dragover', (e) => {
                e.preventDefault();
            });
            
            preview.addEventListener('drop', (e) => {
                e.preventDefault();
                if (draggedElement && draggedElement !== preview) {
                    const rect = preview.getBoundingClientRect();
                    const midpoint = rect.left + rect.width / 2;
                    
                    if (e.clientX < midpoint) {
                        preview.parentNode.insertBefore(draggedElement, preview);
                    } else {
                        preview.parentNode.insertBefore(draggedElement, preview.nextSibling);
                    }
                    
                    this.saveImageOrder();
                }
            });
        });
    }
    
    async saveImageOrder() {
        const imageIds = Array.from(this.previewContainer.querySelectorAll('.image-preview'))
            .map(preview => parseInt(preview.dataset.imageId));
        
        try {
            const response = await fetch(`/user/reviews/${this.reviewId}/images/reorder`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(imageIds)
            });
            
            const result = await response.json();
            if (result.success) {
                this.showSuccess('Đã sắp xếp lại thứ tự ảnh');
            }
        } catch (error) {
            console.error('Error saving image order:', error);
            this.showError('Không thể lưu thứ tự ảnh');
        }
    }
    
    getCurrentImageCount() {
        return this.previewContainer ? this.previewContainer.children.length : 0;
    }
    
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    showSuccess(message) {
        this.showNotification(message, 'success');
    }
    
    showError(message) {
        this.showNotification(message, 'error');
    }
    
    showNotification(message, type) {
        // Create or update notification element
        let notification = document.getElementById('upload-notification');
        if (!notification) {
            notification = document.createElement('div');
            notification.id = 'upload-notification';
            document.body.appendChild(notification);
        }
        
        notification.className = `notification ${type}`;
        notification.textContent = message;
        notification.style.display = 'block';
        
        // Auto hide after 3 seconds
        setTimeout(() => {
            notification.style.display = 'none';
        }, 3000);
    }
}

// Global functions for inline event handlers
async function deleteImage(imageId) {
    if (!confirm('Bạn có chắc muốn xóa ảnh này?')) return;
    
    try {
        const response = await fetch(`/user/reviews/images/${imageId}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        if (result.success) {
            // Remove from DOM
            const imageElement = document.querySelector(`[data-image-id="${imageId}"]`);
            if (imageElement) {
                imageElement.remove();
            }
            
            // Show success message
            window.imageUpload?.showSuccess('Đã xóa ảnh thành công');
        } else {
            window.imageUpload?.showError(result.message);
        }
    } catch (error) {
        console.error('Error deleting image:', error);
        window.imageUpload?.showError('Không thể xóa ảnh');
    }
}

async function updateAltText(imageId, altText) {
    try {
        const response = await fetch(`/user/reviews/${window.imageUpload.reviewId}/images/${imageId}/alt-text`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ altText })
        });
        
        const result = await response.json();
        if (!result.success) {
            window.imageUpload?.showError(result.message);
        }
    } catch (error) {
        console.error('Error updating alt text:', error);
    }
}

function editAltText(imageId) {
    const imageElement = document.querySelector(`[data-image-id="${imageId}"]`);
    if (imageElement) {
        const input = imageElement.querySelector('.alt-text-input');
        if (input) {
            input.focus();
            input.select();
        }
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Check if we're on a page that needs image upload
    const reviewIdElement = document.querySelector('[data-review-id]');
    if (reviewIdElement) {
        window.imageUpload = new EnhancedImageUpload({
            reviewId: reviewIdElement.dataset.reviewId,
            containerId: 'image-upload-container',
            fileInputId: 'image-files',
            progressContainerId: 'upload-progress',
            previewContainerId: 'image-previews'
        });
    }
});