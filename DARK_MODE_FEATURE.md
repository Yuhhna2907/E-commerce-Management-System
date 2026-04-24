# ✨ Dark Mode Feature - Admin Dashboard

## 📋 Tổng Quan

Đã thêm tính năng **chuyển đổi chế độ sáng/tối (Dark/Light Mode)** vào trang Admin Dashboard để cải thiện trải nghiệm người dùng, đặc biệt khi làm việc trong môi trường ánh sáng yếu.

---

## 🎯 Vị Trí

Nút chuyển đổi được đặt ở **header phía trên bên phải**, ngay bên cạnh nút thông báo (notification bell), trước avatar admin.

```
[🔍 Search] ..................... [🔔 Notification] [🌙 Dark Mode] [👤 Admin Profile]
```

---

## 🎨 Tính Năng

### 1. **Toggle Button**
- **Icon Light Mode**: 🌙 Moon icon (bi-moon-stars-fill)
- **Icon Dark Mode**: ☀️ Sun icon (bi-sun-fill)
- **Animation**: Hover effect với scale + rotate
- **Tooltip**: "Chuyển đổi chế độ sáng/tối"

### 2. **Dark Mode Styling**
- **Background**: Dark gradient với subtle color accents
- **Glass Panels**: Darker glassmorphism với adjusted opacity
- **Text Colors**: 
  - Primary text: `#e2e8f0` (light slate)
  - Muted text: `#94a3b8` (slate gray)
- **Cards**: Dark glass cards với border adjustments
- **Sidebar**: Dark theme với hover states
- **Galaxy Box**: Maintains gradient với darker tones

### 3. **Persistence**
- **LocalStorage**: Theme preference được lưu vào `localStorage`
- **Key**: `adminTheme` (values: `'light'` hoặc `'dark'`)
- **Auto-load**: Theme được apply tự động khi reload page

---

## 💻 Implementation Details

### HTML Structure
```html
<!-- Nút Dark/Light Mode Toggle -->
<div class="theme-toggle-btn" id="themeToggle" style="cursor: pointer;" title="Chuyển đổi chế độ sáng/tối">
    <i class="bi bi-moon-stars-fill fs-5 text-dark" id="themeIcon"></i>
</div>
```

### CSS Classes
```css
/* Dark mode được apply bằng class trên body */
body.dark-mode {
    background-color: #0f172a;
    /* ... dark theme styles ... */
}
```

### JavaScript Logic
```javascript
// 1. Load saved theme từ localStorage
const currentTheme = localStorage.getItem('adminTheme') || 'light';

// 2. Apply theme on page load
if (currentTheme === 'dark') {
    body.classList.add('dark-mode');
    // Update icon
}

// 3. Toggle on click
themeToggle.addEventListener('click', function() {
    body.classList.toggle('dark-mode');
    // Update icon & save to localStorage
});
```

---

## 🎨 Color Palette

### Light Mode (Default)
- Background: `#f0f2f5` với gradient overlays
- Glass panels: `rgba(255, 255, 255, 0.65)`
- Text: `#1A1C20` (dark)
- Muted: `#8E8E93` (gray)

### Dark Mode
- Background: `#0f172a` (slate-900)
- Glass panels: `rgba(30, 41, 59, 0.7)` (slate-800)
- Text: `#e2e8f0` (slate-200)
- Muted: `#94a3b8` (slate-400)
- Borders: `rgba(71, 85, 105, 0.3)` (slate-600)

---

## ✅ Tested Components

Các component sau đã được style cho dark mode:

1. ✅ **Top Navigation Bar**
   - Search pill
   - Notification icon
   - Theme toggle button
   - User profile button

2. ✅ **Sidebar** (trong fragment)
   - Sidebar links
   - Active states
   - Hover effects
   - Section headings

3. ✅ **Galaxy Welcome Box**
   - Gradient background
   - Text colors
   - Stars animation

4. ✅ **Glass Cards & Panels**
   - KPI cards
   - Bento grid items
   - Chart containers
   - Drill-down links

5. ✅ **Text Elements**
   - `.text-dark` → light color
   - `.text-muted` → adjusted gray
   - Headings & paragraphs

---

## 🚀 User Experience

### Smooth Transitions
- Theme switch là instant (no page reload)
- Icon animation khi hover/click
- Smooth color transitions với CSS

### Accessibility
- Clear visual feedback
- Tooltip cho button
- High contrast trong cả 2 modes
- Icon thay đổi rõ ràng (moon ↔ sun)

### Persistence
- Theme được nhớ across sessions
- Không cần login lại
- Works với multiple tabs

---

## 📝 Notes

1. **LocalStorage Key**: `adminTheme`
2. **Default Theme**: Light mode
3. **Icon Library**: Bootstrap Icons
4. **Browser Support**: All modern browsers với localStorage support
5. **Performance**: No impact, chỉ toggle CSS class

---

## 🔮 Future Enhancements

Có thể mở rộng thêm:
- [ ] Auto dark mode based on system preference (`prefers-color-scheme`)
- [ ] Scheduled dark mode (e.g., 6PM - 6AM)
- [ ] Multiple theme options (light, dark, auto)
- [ ] Theme sync across all admin pages
- [ ] Custom color schemes

---

## 📊 Impact

### Benefits
- ✅ Giảm eye strain khi làm việc ban đêm
- ✅ Tiết kiệm pin cho OLED screens
- ✅ Modern UX feature
- ✅ Professional appearance
- ✅ User preference respect

### Technical
- ✅ Minimal code addition (~100 lines CSS + 30 lines JS)
- ✅ No external dependencies
- ✅ No performance impact
- ✅ Easy to maintain

---

**Ngày triển khai**: 20/04/2026  
**Version**: 1.0  
**Status**: ✅ Production Ready
