var translations = {
    vi: {
        'nav.logout': '🚪 Đăng xuất',
        'nav.theme': 'Đổi màu nền',
        'nav.collapse': 'Thu gọn sidebar',
        'sidebar.dashboard': 'Tổng quan',
        'sidebar.transfer': 'Chuyển tiền',
        'sidebar.qr': 'Mã QR',
        'sidebar.history': 'Lịch sử',
        'sidebar.scheduled': 'Định kỳ',
        'sidebar.settings': 'Cài đặt',
        'sidebar.ai': 'Trợ lý AI',
        'sidebar.admin': 'Quản trị',
        'sidebar.logout': 'Đăng xuất',
    },
    en: {
        'nav.logout': '🚪 Logout',
        'nav.theme': 'Toggle theme',
        'nav.collapse': 'Collapse sidebar',
        'sidebar.dashboard': 'Dashboard',
        'sidebar.transfer': 'Transfer',
        'sidebar.qr': 'QR Code',
        'sidebar.history': 'History',
        'sidebar.scheduled': 'Scheduled',
        'sidebar.settings': 'Settings',
        'sidebar.ai': 'AI Assistant',
        'sidebar.admin': 'Admin',
        'sidebar.logout': 'Logout',
    }
};

function applyLanguage(lang) {
    lang = lang || 'vi';
    var t = translations[lang] || translations.vi;
    var els = document.querySelectorAll('[data-i18n]');
    for (var i = 0; i < els.length; i++) {
        var key = els[i].getAttribute('data-i18n');
        if (t[key]) els[i].textContent = t[key];
    }
}
