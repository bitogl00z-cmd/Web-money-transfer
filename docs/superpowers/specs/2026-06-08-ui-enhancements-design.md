# UI Enhancements: Collapsible Sidebar, Dark/Light Mode, Menu Cleanup, Face+2FA for ≥10M

## Overview
Frontend-only changes (no backend modification needed). Add a collapsible sidebar toggle, a dark/light mode switcher, remove beneficiary/limit items, and re-enable face verification for transfers ≥10M VND (alongside existing PIN requirement).

## Changes

### 1. Collapsible Sidebar
- **Toggle button** in the navbar (desktop, next to the hamburger)
- State: `expanded` (default, 240px) or `collapsed` (60px, icons only)
- Persisted in `localStorage` key `sidebarCollapsed`
- CSS class `.sidebar.collapsed` + `.main-content.expanded`
- On collapse: sidebar width → 60px, text hidden (only icons via flex), main content margin-left → 60px
- Existing mobile behavior (sidebar slides off-screen) unchanged

### 2. Dark/Light Mode Toggle
- **Toggle button** in the navbar (a sun/moon icon)
- Switches CSS custom properties (`:root` ↔ `.light-mode`)
- Light theme variables:
  - `--bg: #f8fafc`, `--bg-elevated: #ffffff`, `--card: #ffffff`
  - `--text: #1e293b`, `--text-muted: #64748b`, `--border: #e2e8f0`
  - etc.
- Persisted in `localStorage` key `theme` (`"dark"` | `"light"`)
- Applied via class on `<html>` element

### 3. Remove Beneficiary Menu Item
- Delete `<a href="/beneficiaries">` from `layout.html` sidebar

### 4. Remove Limits Tab
- Delete `<button data-tab="limits">` and `<div id="tab-limits">` from `settings.html`

### 5. Face + PIN Verification for Transfers ≥10M VND
- In `transfer.html` submit handler, when `amount >= 10000000`:
  1. Prompt for PIN (existing `requirePin()`)
  2. Open camera via `openCamera()` (existing in `banking.js`)
  3. Call `POST /api/face/verify-for-transfer` with `{ faceImage: "<base64>" }`
  4. On success: include `x-pin-token` header AND `faceToken` in request body
  5. If face fails: show error, abort transfer
  6. For amounts < 10M: existing PIN-only flow unchanged

## Files Modified
| File | Change |
|---|---|
| `layout.html` | Add sidebar collapse toggle, theme toggle, remove beneficiaries link |
| `banking.css` | Add `.sidebar.collapsed`, `.light-mode` variables, `.collapsed` transitions |
| `banking.js` | Add `toggleTheme()`, `toggleCollapse()` functions, face flow logic |
| `settings.html` | Remove limits tab |
| `transfer.html` | Add face verification step for >= 10M |
