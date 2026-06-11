# Dark VCB Banking UI Redesign

## Overview
Redesign the existing Spring Boot money-transfer web app UI to a dark-themed interface inspired by Vietcombank (VCB) internet banking layout, while maintaining all existing functionality.

## Design Decisions
- **Dark mode** as primary theme (slate-900/#0f172a base)
- **VCB-style**: sidebar navigation with icon + text, gradient stat cards, clean information hierarchy
- **No balance column** in admin accounts table (already done)
- **No "Số tài khoản" stat card** on user dashboard (already done)
- Keep all existing pages: login, register, dashboard, transfer, history, beneficiaries, scheduled, settings, admin

## Color Palette
| Token | Color | Usage |
|-------|-------|-------|
| `--bg` | `#0f172a` | Page background |
| `--bg-elevated` | `#1a2332` | Secondary background (main content) |
| `--card` | `#1e293b` | Card/sidebar background |
| `--card-hover` | `#334155` | Card hover state |
| `--text` | `#f1f5f9` | Primary text |
| `--text-muted` | `#94a3b8` | Secondary text |
| `--text-dim` | `#64748b` | Placeholder/label text |
| `--border` | `#1e293b` | Subtle borders |
| `--border-light` | `#334155` | Visible borders |
| `--primary` | `#3b82f6` | Primary accent |
| `--primary-glow` | `rgba(59,130,246,0.15)` | Glow/hover effect |
| `--success` | `#10b981` | Success |
| `--danger` | `#ef4444` | Danger |
| `--warning` | `#f59e0b` | Warning |
| `--info` | `#3b82f6` | Info |

## Layout Structure (unchanged)
- Fixed top navbar (64px height)
- Fixed left sidebar (240px width)
- Main content with margin-left: 240px, margin-top: 64px

## Components to Redesign

### Navbar
- Gradient background: `linear-gradient(135deg, #0c1427, #1e293b)` (subtler)
- Logo: White text with blue accent icon
- User info: Light text with subtle background badge
- Bottom border glow: `1px solid #1e293b` + `box-shadow: 0 1px 12px rgba(59,130,246,0.1)`

### Sidebar
- Background: `#0f172a`, right border: `1px solid #1e293b`
- Items: Padding 12px 24px, text `#94a3b8`, icon size 1.1rem
- Hover: Background `rgba(59,130,246,0.08)`, text `#f1f5f9`
- Active: Background `rgba(59,130,246,0.12)`, text `#3b82f6`, right border 3px solid `#3b82f6`
- Icon with subtle glow on active

### Stat Cards
- Background: gradient (tinted dark), border `1px solid #1e293b`
- Border radius: 12px, padding: 20px 24px
- Top accent bar removed, replaced by subtle left-border gradient
- Hover: `translateY(-2px)`, `box-shadow: 0 8px 24px rgba(0,0,0,0.2)`, border color to `#334155`

### Regular Cards
- Background: `#1e293b`, border: `1px solid #1e293b`, border-radius: 12px
- Hover: border to `#334155`, box-shadow enhancement

### Tables
- Header: Background `#0f172a`, text `#94a3b8`, border-bottom `2px solid #334155`
- Rows: Background `#1e293b`, border-bottom `1px solid #1e293b`
- Hover: Background `rgba(59,130,246,0.06)`
- Empty state: Text `#64748b`

### Forms
- Input background: `#0f172a`, border: `1.5px solid #334155`, text: `#f1f5f9`
- Focus: border `#3b82f6`, box-shadow `0 0 0 3px rgba(59,130,246,0.15)`
- Label: `#94a3b8`

### Buttons
- Primary: `#3b82f6` bg, hover `#2563eb`
- Success: `#10b981` bg, hover `#059669`
- Danger: `#ef4444` bg, hover `#dc2626`
- Outline: transparent bg, `#3b82f6` border + text, hover `rgba(59,130,246,0.1)` bg
- All buttons: border-radius 8px, smooth transition

### Badges (for dark mode)
- Success: `rgba(16,185,129,0.15)` bg, `#34d399` text
- Danger: `rgba(239,68,68,0.15)` bg, `#f87171` text
- Warning: `rgba(245,158,11,0.15)` bg, `#fbbf24` text
- Info: `rgba(59,130,246,0.15)` bg, `#60a5fa` text
- Purple: `rgba(139,92,246,0.15)` bg, `#a78bfa` text
- Gray: `rgba(148,163,184,0.15)` bg, `#cbd5e1` text

### Login/Register Pages
- Background: `linear-gradient(135deg, #0f172a, #1e293b)`
- Card: `#1e293b` with border `1px solid #334155`
- Shadow: `0 8px 40px rgba(0,0,0,0.3)`

### Modal
- Overlay: `rgba(0,0,0,0.7)`
- Modal background: `#1e293b`, border `1px solid #334155`
- Title: `#f1f5f9`, description: `#94a3b8`

### Toast
- Success: `#065f46` bg
- Error: `#991b1b` bg  
- Info: `#1e40af` bg

## Pages to Modify
- `src/main/resources/static/css/banking.css` — Complete CSS overhaul for dark mode
- `src/main/resources/templates/fragments/layout.html` — Minor navbar/sidebar tweaks
- `src/main/resources/templates/login.html` — Dark theme adjustments
- `src/main/resources/templates/register.html` — Dark theme adjustments (consistent with login)
- No HTML structural changes to dashboard/transfer/etc — CSS-only changes via classes

## Files Not Changed
- All Java backend code remains unchanged
- All JavaScript (banking.js) remains unchanged  
- All template HTML structure (except inline styles in login/register)

## Implementation Order
1. Update `banking.css` with dark color variables + component restyling
2. Minor adjustments to `layout.html` (navbar gradient, sidebar active style)
3. Update `login.html` inline styles for dark theme
4. Update `register.html` inline styles for dark theme
