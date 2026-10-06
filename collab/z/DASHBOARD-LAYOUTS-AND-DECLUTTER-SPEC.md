# Spec: DASHBOARD-LAYOUTS-AND-DECLUTTER — Multi-Layout Toggle Architecture & UI Noise Purge

| Metadata | Value |
|---|---|
| **Task ID** |  |
| **Author Agent** | Lovable (System & UI Architect) |
| **Base Commit** |  |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directives 2026-10-07 (remove points, countdown, promo cards, add 3-layout live toggle) |
| **Executor** | Gemini / Antigravity |
| **Status** |  |

---

## 1. Executive Summary & Purpose

The Operator reviewed the GateLMS dashboard UI and identified several distracting gamified and promotional elements that compromise academic focus. Additionally, three distinct, polished UI layout concepts were designed and approved for implementation.

This specification tasks **Gemini / Antigravity** with:
1. **Purging legacy UI clutter** from the header, navigation, and right rail.
2. **Implementing a client-side Layout Engine** allowing live in-application toggling between three design variants, persisted across reloads.
3. **Implementing the 3 layout variants**:
   - **Variant 1: Modern Minimalist** (Clean slate dark theme, linear study progress, 4-subject cluster grid)
   - **Variant 2: Editorial & Academic** (Deep obsidian, textbook chapter tracking, 2x2 subject cards)
   - **Variant 3: Bento Grid** (High-contrast modular bento cards, weightage analytics)

---

## 2. Mandatory UI Deprecations & Removals

### 2.1 Header Purge
- ❌ **Remove Points Badge ()**: Remove the points indicator and associated gamification counter from the top header navigation.
- ❌ **Remove Countdown Pill ()**: Remove the countdown pill from the global navigation header. (Move target exam metadata to user profile/settings only).

### 2.2 Navigation Purge
- ❌ **Remove Redundant 'Planly Study Schedule' Nav Link**: Eliminate duplicate schedule navigation item from the prep sidebar. Unify active session navigation under the main Dashboard view.

### 2.3 Right Rail & Body Purge
- ❌ **Remove Promo Card ('One-stop platform / Too many tabs open?')**: Completely remove the promotional container and 'Open Focus Mode' banner from the right sidebar.
- ❌ **Remove Gimmicky Point Badges**: Remove  badge from the Problem of the Day header.

---

## 3. Architecture & Implementation Plan

### 3.1 Layout Context & Persistence ( or equivalent)
Implement a lightweight React Context providing the current layout state:



- Store user preference in  key .
- Fall back to  on first load.
- Ensure instantaneous rendering without flashes of unstyled content or full page reloads.

### 3.2 Layout Toggle Control ()
Add a clean, minimal switcher in the top navigation bar (adjacent to the user profile/theme toggle):
- Segmented pill or discreet dropdown with icons:
  1.  (Default)
  2. 
  3. 

### 3.3 Modular Layout Views
Refactor  to mount the active variant view while sharing underlying hooks and data queries:



---

## 4. Variant Component Details

### 4.1 Variant 1: Modern Minimalist ()
- **Theme:** Clean dark slate (), subtle zinc borders (), electric blue accent ().
- **Active Study Session Hero:** Prominently displays the current subject & topic (e.g., OS Process Synchronization), syllabus chapter links (Silberschatz/Tanenbaum), and a clean percentage progress bar.
- **Subject Cluster Grid:** 4-column layout displaying Discrete Math, Data Structures, Algorithms, and Systems Core with GATE mark weightage tags and 'Explore' links.
- **Right Rail:** Circular SVG/canvas donut progress meter for Syllabus Completion + distraction-free Problem of the Day MCQ.

### 4.2 Variant 2: Editorial & Academic ()
- **Theme:** Deep obsidian (), high typographical hierarchy, soft indigo accents ().
- **Active Study Session Hero:** Focuses on textbook reading flow (e.g. CLRS Introduction to Algorithms Chapter 2), section progress (e.g. 3/5 sections, 60%), with a direct 'Start Studying' CTA.
- **Subject Cluster Grid:** 2x2 grid displaying subject marks weightage, completion percentage, and topic counts ().
- **Right Rail:** Clean numeric syllabus progress ring and compact Problem of the Day card.

### 4.3 Variant 3: Bento Grid ()
- **Theme:** Modern modular bento layout with dark charcoal containers, rounded-2xl cards, and crisp borders.
- **Hero Bento Tile (Col-span-2):** Prominent 'Active Study Focus' card featuring current topic (e.g. Discrete Math - Graphs), recommended textbook reference, bulleted sub-topics, and progress bar.
- **Cluster Bento Tiles:** Compact grid tiles with color-coded topic progress bars and weightage percentages.
- **Right Bento Column:** Circular syllabus completion widget and integrated Problem of the Day card with one-click answer submission.

---

## 5. Verification & Acceptance Criteria for Gemini

1.  /  passes with zero type errors.
2. Clicking the layout switcher changes the view immediately without full page reload.
3. Refreshing the browser preserves the active layout variant.
4. No traces of , countdown badges, or  promotional banners exist in any of the 3 views.
5. All interactive elements (Problem of the Day, Question Bank links, Topic navigation) work seamlessly across all 3 views.
