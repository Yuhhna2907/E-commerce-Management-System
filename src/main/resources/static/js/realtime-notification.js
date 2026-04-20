let stompClient = null;

function connectNotificationWebSocket() {
    const username = document.getElementById("currentUsername")?.value;
    if (!username) return; // User not logged in

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    // Disable logging for production
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('Connected to Notifications WebSocket');
        
        // Subscribe to user-specific notifications
        stompClient.subscribe('/topic/user/' + username + '/notifications', function (notificationMsg) {
            const notification = JSON.parse(notificationMsg.body);
            showNotificationToast(notification);
            updateNotificationBadgeCount(notification.unreadCount);
            prependNotificationToList(notification);
        });
        
    }, function(error) {
        console.error('STOMP Error:', error);
        // Attempt to reconnect after 5s
        setTimeout(connectNotificationWebSocket, 5000);
    });
}

function showNotificationToast(notification) {
    const toastHtml = `
        <div class="toast align-items-center text-white bg-warning border-0" role="alert" aria-live="assertive" aria-atomic="true" style="margin-bottom: 10px; border-radius: 12px; box-shadow: 0 10px 25px rgba(245, 158, 11, 0.4);">
          <div class="d-flex">
            <div class="toast-body fw-bold d-flex align-items-center gap-2">
              <i class="bi bi-bell-fill"></i>
              ${notification.message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
          </div>
        </div>
    `;
    const toastContainer = document.getElementById("toastPlacement");
    if(toastContainer) {
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        const newToastEl = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(newToastEl, {delay: 5000});
        toast.show();
        
        newToastEl.addEventListener('hidden.bs.toast', () => {
            newToastEl.remove();
        });
        
        if (notification.relatedUrl) {
           newToastEl.style.cursor = 'pointer';
           newToastEl.onclick = function(e) {
               if(!e.target.classList.contains('btn-close')) {
                   window.location.href = notification.relatedUrl;
               }
           }
        }
    }
}

function updateNotificationBadgeCount(count) {
    const badge = document.getElementById("notificationBadge");
    if(!badge) return;
    
    if (count > 0) {
        badge.style.display = 'inline-block';
        badge.innerText = count > 99 ? '99+' : count;
    } else {
        badge.style.display = 'none';
        badge.innerText = '0';
    }
}

function prependNotificationToList(notification) {
    const list = document.getElementById("notificationList");
    const noNotif = document.getElementById("noNotificationText");
    if(noNotif) noNotif.remove();
    
    const timeText = "Vừa xong";
    
    const itemHtml = `
        <a href="${notification.relatedUrl || '#'}" class="text-decoration-none border-bottom p-3 d-flex flex-column gap-1 notification-item" style="transition: background 0.3s; color: #0f172a; background: rgba(245, 158, 11, 0.05);" onclick="markNotificationAsRead(${notification.id}, this)">
            <div class="fw-bold" style="font-size: 0.9rem;">${notification.message}</div>
            <div class="text-muted d-flex align-items-center gap-1" style="font-size: 0.75rem;">
                <i class="bi bi-clock"></i> ${timeText} 
                <span class="badge bg-warning ms-auto">Mới</span>
            </div>
        </a>
    `;
    
    list.insertAdjacentHTML('afterbegin', itemHtml);
}

function markAllNotificationsAsRead() {
    fetch('/user/notifications/mark-all-read', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(res => {
        if(res.ok) {
            updateNotificationBadgeCount(0);
            document.querySelectorAll('.notification-item').forEach(el => {
                el.style.background = '#fff';
                const badge = el.querySelector('.badge');
                if(badge) badge.remove();
            });
        }
    });
}

function markNotificationAsRead(id, triggerElement) {
    fetch('/user/notifications/' + id + '/read', {
        method: 'POST'
    }).then(() => {
        // UI update
        if(triggerElement) {
            triggerElement.style.background = '#fff';
            const badge = triggerElement.querySelector('.badge');
            if(badge) badge.remove();
        }
        
        let currentCount = parseInt(document.getElementById("notificationBadge").innerText);
        if(!isNaN(currentCount) && currentCount > 0) {
            updateNotificationBadgeCount(currentCount - 1);
        }
    });
}

// Initial fetch on page load
function fetchInitialNotifications() {
    const username = document.getElementById("currentUsername")?.value;
    
    // Bypass browser cache totally
    fetch('/user/notifications/all-json', { cache: 'no-store' })
        .then(async res => {
            if (!res.ok) {
                let text = await res.text();
                throw new Error("HTTP " + res.status + " : " + (text.substring(0, 50)));
            }
            return res.json();
        })
        .then(data => {
            let unreadCount = data.filter(n => !(n.read || n.isRead)).length;
            updateNotificationBadgeCount(unreadCount);
            
            const list = document.getElementById("notificationList");
            if(data.length > 0) {
                const noNotif = document.getElementById("noNotificationText");
                if(noNotif) noNotif.remove();
                
                let html = '';
                data.forEach(notification => {
                    const isRead = notification.read || notification.isRead;
                    const bgStr = isRead ? '#fff' : 'rgba(245, 158, 11, 0.05)';
                    const badgeStr = !isRead ? '<span class="badge bg-warning ms-auto">Mới</span>' : '';
                    
                    // Format date safely
                    let timeStr = "Vừa xong";
                    if (notification.createdAt) {
                        let dateObj;
                        if (Array.isArray(notification.createdAt)) { // Jackson serializes LocalDateTime to array
                            const [y, m, d, h, min] = notification.createdAt;
                            dateObj = new Date(y, m - 1, d, h || 0, min || 0);
                        } else {
                            dateObj = new Date(notification.createdAt);
                        }
                        if (!isNaN(dateObj.getTime())) {
                            timeStr = dateObj.toLocaleDateString('vi-VN') + " " + dateObj.toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
                        }
                    }
                    
                    html += `
                        <a href="${notification.relatedUrl || '#'}" class="text-decoration-none border-bottom p-3 d-flex flex-column gap-1 notification-item" style="transition: background 0.3s; color: #0f172a; background: ${bgStr};" onclick="markNotificationAsRead(${notification.id}, this)">
                            <div class="${isRead ? '' : 'fw-bold'}" style="font-size: 0.9rem;">${notification.message}</div>
                            <div class="text-muted d-flex align-items-center gap-1" style="font-size: 0.75rem;">
                                <i class="bi bi-clock"></i> ${timeStr} 
                                ${badgeStr}
                            </div>
                        </a>
                    `;
                });
                
                list.innerHTML = html;
            } else {
                const list = document.getElementById("notificationList");
                list.innerHTML = '<div class="p-4 text-center text-muted fw-bold" id="noNotificationText">Không có thông báo mới (0 kết quả)</div>';
            }
        })
        .catch(err => {
            console.log('Error fetching notifications: ', err);
            const list = document.getElementById("notificationList");
            if (list) list.innerHTML = '<div class="p-4 text-center text-danger fw-bold">Lỗi tải dữ liệu: ' + err.message + '</div>';
        });
}

document.addEventListener("DOMContentLoaded", function() {
    fetchInitialNotifications();
    connectNotificationWebSocket();
});
