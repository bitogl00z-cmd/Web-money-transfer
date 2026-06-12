function escapeHtml(str) {
    if (!str) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

function formatVND(amount) {
    var num = Number(amount);
    if (isNaN(num)) return '0₫';
    return num.toLocaleString('vi-VN') + '₫';
}

function showToast(message, type) {
    type = type || 'success';
    var container = document.getElementById('toastContainer');
    if (!container) {
        var div = document.createElement('div');
        div.id = 'toastContainer';
        div.className = 'toast-container';
        document.body.appendChild(div);
        container = div;
    }
    var toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    var icons = { success: '✓', error: '✕', info: 'ℹ' };
    toast.innerHTML = '<span>' + (icons[type] || '') + '</span> ';
    toast.appendChild(document.createTextNode(message));
    container.appendChild(toast);
    setTimeout(function() { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(function() { toast.remove(); }, 300); }, 3000);
}

function openModal(html) {
    closeModal();
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'activeModal';
    var modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = html;
    overlay.appendChild(modal);
    document.body.appendChild(overlay);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) closeModal(); });
    return overlay;
}

function closeModal() {
    var existing = document.getElementById('activeModal');
    if (existing) { existing.remove(); }
}

function openCamera(onCapture) {
    var html = '<div class="modal-title">📸 Xác thực gương mặt</div>' +
        '<div class="modal-desc">Vui lòng nhìn thẳng vào camera để chụp ảnh xác thực</div>' +
        '<div class="camera-container">' +
        '<video id="faceVideo" autoplay playsinline></video>' +
        '<div class="camera-overlay"></div>' +
        '</div>' +
        '<div class="modal-actions">' +
        '<button id="cancelFaceBtn" class="btn btn-outline">Hủy</button>' +
        '<button id="captureBtn" class="btn btn-primary">Chụp ảnh</button>' +
        '</div>';
    openModal(html);

    var video = document.getElementById('faceVideo');
    var stream = null;

    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: 320, height: 240 } })
        .then(function(s) {
            stream = s;
            video.srcObject = s;
        })
        .catch(function() {
            showToast('Không thể truy cập camera', 'error');
            closeModal();
        });

    document.getElementById('cancelFaceBtn').addEventListener('click', function() {
        if (stream) { stream.getTracks().forEach(function(t) { t.stop(); }); }
        closeModal();
        if (onCapture) onCapture(null);
    });

    document.getElementById('captureBtn').addEventListener('click', function() {
        var canvas = document.createElement('canvas');
        canvas.width = video.videoWidth || 320;
        canvas.height = video.videoHeight || 240;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0);
        var base64 = canvas.toDataURL('image/jpeg', 0.8).split(',')[1];

        if (stream) { stream.getTracks().forEach(function(t) { t.stop(); }); }
        closeModal();

        if (onCapture) onCapture(base64);
    });
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatDateShort(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN');
}

async function apiFetch(url, options) {
    options = options || {};
    options.credentials = 'include';
    options.headers = options.headers || {};
    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    try {
        var res = await fetch(url, options);
        if (res.status === 401 && !url.includes('/api/auth/refresh-token') && !url.includes('/api/auth/login')) {
            var refreshRes = await fetch('/api/auth/refresh-token', { method: 'POST', credentials: 'include' });
            if (refreshRes.ok) {
                res = await fetch(url, options);
            } else {
                window.location.href = '/login';
                return { ok: false, status: 401, data: { error: 'Session expired' } };
            }
        }
        var data = await res.json();
        return { ok: res.ok, status: res.status, data: data };
    } catch (e) {
        return { ok: false, status: 0, data: { error: 'Network error' } };
    }
}

function toggleSidebar() {
    document.querySelector('.sidebar').classList.toggle('open');
}

function toggleCollapse() {
    var sidebar = document.querySelector('.sidebar');
    var main = document.querySelector('.main-content');
    sidebar.classList.toggle('collapsed');
    main.classList.toggle('expanded');
    localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed') ? 'true' : 'false');
}

function toggleTheme() {
    var html = document.documentElement;
    var isLight = html.classList.toggle('light-mode');
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
    var btn = document.getElementById('themeToggleBtn');
    if (btn) btn.textContent = isLight ? '🌙' : '☀️';
}

function showNotificationBox(title, messageHtml, type) {
    var container = document.getElementById('notificationContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'notificationContainer';
        container.style.cssText = 'position:fixed;bottom:20px;left:240px;z-index:10000;display:flex;flex-direction:column;gap:8px;max-width:380px;';
        document.body.appendChild(container);
    }
    var box = document.createElement('div');
    box.style.cssText = 'background:#fff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.15);padding:16px;animation:slideIn 0.3s ease;border-left:4px solid ' + (type === 'success' ? '#10b981' : '#ef4444') + ';';
    var titleDiv = document.createElement('div');
    titleDiv.style.cssText = 'font-weight:600;font-size:0.95rem;margin-bottom:4px;';
    titleDiv.textContent = title;
    var msgDiv = document.createElement('div');
    msgDiv.style.cssText = 'font-size:0.85rem;color:#666;';
    msgDiv.innerHTML = messageHtml;
    box.appendChild(titleDiv);
    box.appendChild(msgDiv);
    container.appendChild(box);
    setTimeout(function() { box.style.opacity = '0'; box.style.transition = 'opacity 0.3s'; setTimeout(function() { box.remove(); }, 300); }, 8000);
}

var styleSheet = document.createElement('style');
styleSheet.textContent = '@keyframes slideIn { from { transform:translateX(-100%);opacity:0; } to { transform:translateX(0);opacity:1; } }';
document.head.appendChild(styleSheet);

var stompClient = null;
function connectWebSocket(userId) {
    if (!userId || stompClient && stompClient.connected) return;
    var socket = new SockJS('/ws/notifications');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function() {
        stompClient.subscribe('/topic/notifications/' + userId, function(msg) {
            var data = JSON.parse(msg.body);
            var safeMsg = escapeHtml(data.message || '');
            var extra = data.transactionCode ? '<br><small style="color:#999;">GD: ' + escapeHtml(data.transactionCode) + '</small>' : '';
            var bal = data.balance ? '<br><strong style="color:#059669;">' + escapeHtml(String(data.balance)) + '</strong>' : '';
            var isSuccess = data.type === 'SCHEDULED_PAYMENT';
            showNotificationBox(escapeHtml(data.title || ''), safeMsg + bal + extra, isSuccess ? 'success' : 'error');
        });
    });
}

document.addEventListener('DOMContentLoaded', function() {
    var sidebar = document.querySelector('.sidebar');
    var main = document.querySelector('.main-content');
    if (main && sidebar) {
        main.addEventListener('click', function() {
            if (window.innerWidth <= 768 && sidebar.classList.contains('open')) {
                sidebar.classList.remove('open');
            }
        });
    }
    // Restore sidebar collapsed state
    if (localStorage.getItem('sidebarCollapsed') === 'true') {
        document.querySelector('.sidebar')?.classList.add('collapsed');
        document.querySelector('.main-content')?.classList.add('expanded');
    }
    // Restore theme
    if (localStorage.getItem('theme') === 'light') {
        document.documentElement.classList.add('light-mode');
        var btn = document.getElementById('themeToggleBtn');
        if (btn) btn.textContent = '🌙';
    }
});

function renderAvatar(containerEl, imgEl, placeholderEl, avatarUrl, name) {
    if (avatarUrl && avatarUrl.trim() !== '') {
        imgEl.src = avatarUrl;
        imgEl.style.display = '';
        if (placeholderEl) placeholderEl.style.display = 'none';
    } else {
        imgEl.style.display = 'none';
        if (placeholderEl) {
            placeholderEl.style.display = '';
            placeholderEl.textContent = (name || 'U').charAt(0).toUpperCase();
        }
    }
}
