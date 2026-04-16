/**
 * Q&A Section Component
 * Handles product questions and answers display and interaction
 * 
 * @typedef {Object} QuestionData
 * @property {number} id
 * @property {string} questionText
 * @property {string} userName
 * @property {string} createdAt
 * @property {number} answerCount
 * @property {AnswerData[]} answers
 * 
 * @typedef {Object} AnswerData
 * @property {number} id
 * @property {string} answerText
 * @property {string} userName
 * @property {string} userRole
 * @property {string} createdAt
 * @property {number} voteCount
 * @property {boolean} userVoted
 */
class QASection {
    constructor(container, productId, options = {}) {
        this.container = container;
        this.productId = productId;
        this.options = {
            pageSize: options.pageSize || 10,
            sortBy: options.sortBy || 'recent',
            isAuthenticated: options.isAuthenticated || false,
            currentUserRole: options.currentUserRole || null,
            csrfToken: options.csrfToken || null
        };
        
        this.currentPage = 0;
        this.totalPages = 0;
        this.questions = [];
        
        this.init();
    }
    
    init() {
        this.render();
        this.loadQuestions();
    }
    
    render() {
        this.container.innerHTML = `
            <div class="qa-section">
                <div class="qa-header">
                    <h3 class="qa-title">Hỏi & Đáp</h3>
                    ${this.options.isAuthenticated ? `
                        <button class="btn btn-primary qa-ask-btn" id="qaAskBtn">
                            <i class="bi bi-question-circle me-2"></i>Đặt câu hỏi
                        </button>
                    ` : ''}
                </div>
                
                <div class="qa-filters">
                    <div class="qa-sort">
                        <label>Sắp xếp:</label>
                        <select class="form-select qa-sort-select" id="qaSortSelect">
                            <option value="recent">Mới nhất</option>
                            <option value="oldest">Cũ nhất</option>
                            <option value="most_answered">Nhiều câu trả lời nhất</option>
                        </select>
                    </div>
                </div>
                
                <div class="qa-content" id="qaContent">
                    <div class="qa-loading">
                        <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Đang tải...</span>
                        </div>
                    </div>
                </div>
                
                <div class="qa-pagination" id="qaPagination"></div>
            </div>
        `;
        
        // Append modals to body instead of container to avoid z-index issues
        const existingQuestionModal = document.getElementById('qaQuestionModal');
        const existingAnswerModal = document.getElementById('qaAnswerModal');
        
        if (!existingQuestionModal) {
            const questionModalDiv = document.createElement('div');
            questionModalDiv.innerHTML = this.renderQuestionModal();
            document.body.appendChild(questionModalDiv.firstElementChild);
        }
        
        if (!existingAnswerModal) {
            const answerModalDiv = document.createElement('div');
            answerModalDiv.innerHTML = this.renderAnswerModal();
            document.body.appendChild(answerModalDiv.firstElementChild);
        }
        
        this.attachEventListeners();
    }
    
    renderQuestionModal() {
        return `
            <div class="modal fade" id="qaQuestionModal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Đặt câu hỏi</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <form id="qaQuestionForm">
                                <div class="mb-3">
                                    <label class="form-label">Câu hỏi của bạn</label>
                                    <textarea class="form-control" id="qaQuestionText" rows="4" 
                                        placeholder="Nhập câu hỏi về sản phẩm..." required></textarea>
                                </div>
                                <div class="alert alert-info">
                                    <i class="bi bi-info-circle me-2"></i>
                                    Câu hỏi của bạn sẽ được hiển thị công khai và có thể được trả lời bởi người bán hoặc người dùng khác.
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                            <button type="button" class="btn btn-primary" id="qaSubmitQuestion">Gửi câu hỏi</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    renderAnswerModal() {
        return `
            <div class="modal fade" id="qaAnswerModal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Trả lời câu hỏi</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label fw-bold">Câu hỏi:</label>
                                <p class="text-muted" id="qaAnswerQuestionText"></p>
                            </div>
                            <form id="qaAnswerForm">
                                <input type="hidden" id="qaAnswerQuestionId">
                                <div class="mb-3">
                                    <label class="form-label">Câu trả lời của bạn</label>
                                    <textarea class="form-control" id="qaAnswerText" rows="4" 
                                        placeholder="Nhập câu trả lời..." required></textarea>
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                            <button type="button" class="btn btn-primary" id="qaSubmitAnswer">Gửi trả lời</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    attachEventListeners() {
        // Ask question button
        const askBtn = document.getElementById('qaAskBtn');
        if (askBtn) {
            askBtn.addEventListener('click', () => this.showQuestionModal());
        }
        
        // Submit question
        const submitQuestionBtn = document.getElementById('qaSubmitQuestion');
        if (submitQuestionBtn) {
            submitQuestionBtn.addEventListener('click', () => this.submitQuestion());
        }
        
        // Submit answer
        const submitAnswerBtn = document.getElementById('qaSubmitAnswer');
        if (submitAnswerBtn) {
            submitAnswerBtn.addEventListener('click', () => this.submitAnswer());
        }
        
        // Sort change
        const sortSelect = document.getElementById('qaSortSelect');
        if (sortSelect) {
            sortSelect.value = this.options.sortBy;
            sortSelect.addEventListener('change', (e) => {
                this.options.sortBy = e.target.value;
                this.currentPage = 0;
                this.loadQuestions();
            });
        }
    }
    
    async loadQuestions() {
        const content = document.getElementById('qaContent');
        if (!content) return;
        
        content.innerHTML = `
            <div class="qa-loading">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
            </div>
        `;
        
        try {
            const url = `/qa/products/${this.productId}/questions?page=${this.currentPage}&size=${this.options.pageSize}&sort=${this.options.sortBy}`;
            const response = await fetch(url);
            const responseText = await response.text();
            
            let data;
            try {
                data = JSON.parse(responseText);
            } catch (parseError) {
                console.error('JSON parse error:', parseError);
                this.showError('Server trả về dữ liệu không hợp lệ');
                return;
            }
            
            if (data.success) {
                this.questions = data.questions || [];
                this.totalPages = data.totalPages || 0;
                this.renderQuestions();
            } else {
                console.error('Failed to load questions:', data.message);
                this.showError(data.message || 'Không thể tải câu hỏi');
            }
        } catch (error) {
            console.error('Error loading questions:', error);
            this.showError('Đã xảy ra lỗi khi tải câu hỏi: ' + error.message);
        }
    }
    
    renderQuestions() {
        const content = document.getElementById('qaContent');
        if (!content) return;
        
        if (this.questions.length === 0) {
            content.innerHTML = `
                <div class="qa-empty">
                    <i class="bi bi-chat-dots qa-empty-icon"></i>
                    <p class="qa-empty-text">Chưa có câu hỏi nào. Hãy là người đầu tiên đặt câu hỏi!</p>
                </div>
            `;
            document.getElementById('qaPagination').innerHTML = '';
            return;
        }
        
        try {
            const questionsHtml = this.questions.map(q => this.renderQuestion(q)).join('');
            content.innerHTML = questionsHtml;
            this.renderPagination();
            this.attachQuestionEventListeners();
        } catch (error) {
            console.error('ERROR in renderQuestions:', error);
        }
    }
    
    renderQuestion(question) {
        const hasAnswers = question.answers && question.answers.length > 0;
        const userName = question.userName || 'Anonymous';
        const createdAt = question.createdAt || new Date().toISOString();
        const answerCount = question.answerCount || 0;
        const answers = question.answers || [];
        
        return `
            <div class="qa-item" data-question-id="${question.id}">
                <div class="qa-question">
                    <div class="qa-question-header">
                        <div class="qa-user-info">
                            <i class="bi bi-person-circle qa-user-icon"></i>
                            <span class="qa-username">${this.escapeHtml(userName)}</span>
                            <span class="qa-date">${this.formatDate(createdAt)}</span>
                        </div>
                    </div>
                    <div class="qa-question-text">${this.escapeHtml(question.questionText)}</div>
                    <div class="qa-question-actions">
                        ${this.options.isAuthenticated ? `
                            <button class="btn btn-sm btn-outline-primary qa-answer-btn" data-question-id="${question.id}">
                                <i class="bi bi-reply me-1"></i>Trả lời
                            </button>
                        ` : ''}
                        <span class="qa-answer-count">
                            <i class="bi bi-chat-left-text me-1"></i>
                            ${answerCount} câu trả lời
                        </span>
                    </div>
                </div>
                
                ${hasAnswers ? `
                    <div class="qa-answers">
                        ${answers.map(a => this.renderAnswer(a)).join('')}
                    </div>
                ` : ''}
            </div>
        `;
    }
    
    renderAnswer(answer) {
        const isSellerAnswer = answer.userRole === 'SELLER' || answer.userRole === 'ADMIN';
        const userName = answer.userName || 'Anonymous';
        const createdAt = answer.createdAt || new Date().toISOString();
        const userRole = answer.userRole || 'USER';
        const voteCount = answer.voteCount || 0;
        const userVoted = answer.userVoted || false;
        
        return `
            <div class="qa-answer ${isSellerAnswer ? 'qa-answer-seller' : ''}" data-answer-id="${answer.id}">
                <div class="qa-answer-header">
                    <div class="qa-user-info">
                        <i class="bi bi-person-circle qa-user-icon"></i>
                        <span class="qa-username">${this.escapeHtml(userName)}</span>
                        ${isSellerAnswer ? '<span class="qa-seller-badge">Người bán</span>' : ''}
                        <span class="qa-date">${this.formatDate(createdAt)}</span>
                    </div>
                </div>
                <div class="qa-answer-text">${this.escapeHtml(answer.answerText)}</div>
                <div class="qa-answer-actions">
                    ${this.options.isAuthenticated ? `
                        <button class="btn btn-sm qa-vote-btn ${userVoted ? 'voted' : ''}" 
                            data-answer-id="${answer.id}">
                            <i class="bi bi-hand-thumbs-up me-1"></i>
                            Hữu ích (${voteCount})
                        </button>
                    ` : `
                        <span class="qa-vote-count">
                            <i class="bi bi-hand-thumbs-up me-1"></i>
                            ${voteCount} người thấy hữu ích
                        </span>
                    `}
                </div>
            </div>
        `;
    }
    
    renderPagination() {
        const pagination = document.getElementById('qaPagination');
        
        if (this.totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        
        let html = '<nav><ul class="pagination justify-content-center">';
        
        // Previous button
        html += `
            <li class="page-item ${this.currentPage === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${this.currentPage - 1}">Trước</a>
            </li>
        `;
        
        // Page numbers
        for (let i = 0; i < this.totalPages; i++) {
            html += `
                <li class="page-item ${i === this.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>
            `;
        }
        
        // Next button
        html += `
            <li class="page-item ${this.currentPage === this.totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${this.currentPage + 1}">Sau</a>
            </li>
        `;
        
        html += '</ul></nav>';
        pagination.innerHTML = html;
        
        // Attach pagination event listeners
        pagination.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(e.target.dataset.page);
                if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
                    this.currentPage = page;
                    this.loadQuestions();
                }
            });
        });
    }
    
    attachQuestionEventListeners() {
        // Answer buttons
        document.querySelectorAll('.qa-answer-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const questionId = e.currentTarget.dataset.questionId;
                this.showAnswerModal(questionId);
            });
        });
        
        // Vote buttons
        document.querySelectorAll('.qa-vote-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const answerId = e.currentTarget.dataset.answerId;
                // Fire and forget - no need to await
                this.voteAnswer(answerId).catch(err => {
                    console.error('Vote failed:', err);
                });
            });
        });
    }
    
    showQuestionModal() {
        if (typeof bootstrap === 'undefined') {
            console.error('Bootstrap is not loaded');
            alert('Bootstrap chưa được load. Vui lòng refresh trang.');
            return;
        }
        
        const modalElement = document.getElementById('qaQuestionModal');
        if (!modalElement) {
            console.error('Modal element not found');
            alert('Không tìm thấy modal element');
            return;
        }
        
        const textArea = document.getElementById('qaQuestionText');
        if (textArea) {
            textArea.value = '';
        }
        
        try {
            const modal = new bootstrap.Modal(modalElement, {
                backdrop: true,
                keyboard: true,
                focus: true
            });
            modal.show();
            
            modalElement.addEventListener('shown.bs.modal', function () {
                if (textArea) {
                    textArea.focus();
                }
            }, { once: true });
        } catch (error) {
            console.error('Error showing modal:', error);
            alert('Lỗi khi mở modal: ' + error.message);
        }
    }
    
    showAnswerModal(questionId) {
        const question = this.questions.find(q => q.id === questionId);
        if (!question) return;
        
        // Check if bootstrap is available
        if (typeof bootstrap === 'undefined') {
            console.error('Bootstrap is not loaded');
            return;
        }
        
        document.getElementById('qaAnswerQuestionId').value = questionId;
        document.getElementById('qaAnswerQuestionText').textContent = question.questionText;
        document.getElementById('qaAnswerText').value = '';
        
        const modal = new bootstrap.Modal(document.getElementById('qaAnswerModal'));
        modal.show();
    }
    
    async submitQuestion() {
        const questionText = document.getElementById('qaQuestionText').value.trim();
        
        if (!questionText) {
            this.showToast('Vui lòng nhập câu hỏi', 'warning');
            return;
        }
        
        try {
            const response = await fetch(`/qa/products/${this.productId}/questions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(this.options.csrfToken && { 'X-CSRF-TOKEN': this.options.csrfToken })
                },
                body: JSON.stringify({ questionText })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showToast('Câu hỏi đã được gửi thành công', 'success');
                
                // Check if bootstrap is available
                if (typeof bootstrap !== 'undefined') {
                    const modalInstance = bootstrap.Modal.getInstance(document.getElementById('qaQuestionModal'));
                    if (modalInstance) {
                        modalInstance.hide();
                    }
                }
                
                this.currentPage = 0;
                await this.loadQuestions();
            } else {
                this.showToast(data.message || 'Không thể gửi câu hỏi', 'error');
            }
        } catch (error) {
            console.error('Error submitting question:', error);
            this.showToast('Đã xảy ra lỗi khi gửi câu hỏi', 'error');
        }
    }
    
    async submitAnswer() {
        const questionId = document.getElementById('qaAnswerQuestionId').value;
        const answerText = document.getElementById('qaAnswerText').value.trim();
        
        if (!answerText) {
            this.showToast('Vui lòng nhập câu trả lời', 'warning');
            return;
        }
        
        try {
            const response = await fetch(`/qa/questions/${questionId}/answers`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(this.options.csrfToken && { 'X-CSRF-TOKEN': this.options.csrfToken })
                },
                body: JSON.stringify({ answerText })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showToast('Câu trả lời đã được gửi thành công', 'success');
                
                // Check if bootstrap is available
                if (typeof bootstrap !== 'undefined') {
                    const modalInstance = bootstrap.Modal.getInstance(document.getElementById('qaAnswerModal'));
                    if (modalInstance) {
                        modalInstance.hide();
                    }
                }
                
                await this.loadQuestions();
            } else {
                this.showToast(data.message || 'Không thể gửi câu trả lời', 'error');
            }
        } catch (error) {
            console.error('Error submitting answer:', error);
            this.showToast('Đã xảy ra lỗi khi gửi câu trả lời', 'error');
        }
    }
    
    async voteAnswer(answerId) {
        try {
            const response = await fetch(`/qa/answers/${answerId}/vote`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(this.options.csrfToken && { 'X-CSRF-TOKEN': this.options.csrfToken })
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showToast(data.message || 'Đã vote thành công', 'success');
                this.loadQuestions();
            } else {
                this.showToast(data.message || 'Không thể vote', 'error');
            }
        } catch (error) {
            console.error('Error voting answer:', error);
            this.showToast('Đã xảy ra lỗi khi vote', 'error');
        }
    }
    
    showError(message) {
        const content = document.getElementById('qaContent');
        content.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle me-2"></i>${message}
            </div>
        `;
    }
    
    showToast(message, type = 'info') {
        // Check if bootstrap is available
        if (typeof bootstrap === 'undefined') {
            console.error('Bootstrap is not loaded');
            alert(message); // Fallback to alert
            return;
        }
        
        // Create toast element
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'warning'} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        // Add to container
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(container);
        }
        
        container.appendChild(toast);
        
        // Show toast
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove after hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
    
    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now - date;
        
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);
        
        if (minutes < 1) return 'Vừa xong';
        if (minutes < 60) return `${minutes} phút trước`;
        if (hours < 24) return `${hours} giờ trước`;
        if (days < 7) return `${days} ngày trước`;
        
        return date.toLocaleDateString('vi-VN');
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Make QASection available globally
window.QASection = QASection;
