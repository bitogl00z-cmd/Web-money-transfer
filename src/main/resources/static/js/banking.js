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
    toast.innerHTML = '<span>' + (icons[type] || '') + '</span> ' + message;
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
        '<button onclick="closeModal()" class="btn btn-outline">Hủy</button>' +
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
        var data = await res.json();
        return { ok: res.ok, status: res.status, data: data };
    } catch (e) {
        return { ok: false, status: 0, data: { error: 'Network error' } };
    }
}

function toggleSidebar() {
    document.querySelector('.sidebar').classList.toggle('open');
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
});
