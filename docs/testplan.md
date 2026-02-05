# Test Plan

Manual test cases organized by feature area. Each test has a priority: **P0** (critical path), **P1** (important), or **P2** (edge case / nice-to-have).

## Login & Authentication

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| AUTH-01 | Login | Successful login | 1. Navigate to `/login` 2. Enter valid username and password 3. Click Login | Redirected to `/library`, username shown in navbar | P0 |
| AUTH-02 | Login | Invalid credentials | 1. Navigate to `/login` 2. Enter wrong password 3. Click Login | Error message displayed, remains on login page | P0 |
| AUTH-03 | Login | Empty fields | 1. Navigate to `/login` 2. Leave fields empty 3. Click Login | Validation error shown for required fields | P1 |
| AUTH-04 | Session | Session persistence | 1. Login successfully 2. Close browser tab 3. Reopen app | User is still authenticated (session cookie valid) | P0 |
| AUTH-05 | Session | Session expiry | 1. Login successfully 2. Wait 24+ hours (or manually expire session) 3. Make any API call | Redirected to login page | P1 |
| AUTH-06 | Logout | Successful logout | 1. Login successfully 2. Click logout button in navbar | Redirected to `/login`, session invalidated | P0 |
| AUTH-07 | Registration | New user registration | 1. Navigate to `/login` 2. Register with new username, email, password | Account created, user can login | P0 |
| AUTH-08 | Registration | Duplicate username | 1. Register with an already-taken username | 409 error, message about username already exists | P1 |
| AUTH-09 | Route protection | Access library without login | 1. Clear cookies 2. Navigate directly to `/library` | Redirected to `/login` | P0 |
| AUTH-10 | Route protection | Access viewer without login | 1. Clear cookies 2. Navigate directly to `/viewer/1` | Redirected to `/login` | P1 |

## Comic Library

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| LIB-01 | List | View comic library | 1. Login 2. Navigate to `/library` | Grid of comic cards displayed with covers, titles, page counts | P0 |
| LIB-02 | List | Empty library | 1. Login with new user 2. Navigate to `/library` | Empty state message shown | P1 |
| LIB-03 | Upload | Upload CBZ file | 1. Click upload button 2. Select a valid .cbz file 3. Confirm upload | Progress bar fills, comic appears in library with cover | P0 |
| LIB-04 | Upload | Upload CBR file | 1. Click upload button 2. Select a valid .cbr file 3. Confirm upload | Progress bar fills, comic appears in library with cover | P0 |
| LIB-05 | Upload | Upload invalid file | 1. Click upload button 2. Select a .pdf or .txt file | File picker rejects non-CBR/CBZ files | P1 |
| LIB-06 | Upload | Upload oversized file | 1. Click upload button 2. Select a file > 500 MB | Error message about file size limit | P1 |
| LIB-07 | Upload | Upload corrupt archive | 1. Upload a corrupted .cbz file (valid extension, bad content) | Server rejects with "Invalid Archive" error | P1 |
| LIB-08 | Delete | Delete a comic | 1. Click delete button on a comic card 2. Confirm in dialog | Comic removed from library, files cleaned up | P0 |
| LIB-09 | Delete | Cancel delete | 1. Click delete button 2. Click Cancel in confirmation dialog | Comic remains in library | P1 |
| LIB-10 | Card | Cover image display | 1. Upload a comic 2. View library | Cover image extracted and displayed on card | P0 |
| LIB-11 | Card | Comic metadata | 1. View library | Each card shows title, page count, upload date | P1 |
| LIB-12 | Navigation | Open comic from library | 1. Click on a comic card | Navigated to `/viewer/{fileId}`, comic loads | P0 |

## Comic Viewer

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| VIEW-01 | Page mode | View single page | 1. Open a comic 2. Verify default page mode | Single page displayed, page counter shows "Page 1 / N" | P0 |
| VIEW-02 | Page mode | Navigate forward | 1. Open a comic 2. Click Next or press → | Page 2 displayed, counter updates | P0 |
| VIEW-03 | Page mode | Navigate backward | 1. Navigate to page 3 2. Click Previous or press ← | Page 2 displayed | P0 |
| VIEW-04 | Page mode | First page boundary | 1. On page 1 2. Press ← or click Previous | Previous button disabled, stays on page 1 | P1 |
| VIEW-05 | Page mode | Last page boundary | 1. Navigate to last page 2. Press → or click Next | Next button disabled, stays on last page | P1 |
| VIEW-06 | Page mode | Jump to first/last | 1. Navigate to middle page 2. Press Home, then End | Jumps to page 1, then to last page | P1 |
| VIEW-07 | Scroll mode | Continuous scroll | 1. Open a comic 2. Toggle to scroll mode | All pages displayed in vertical scroll, lazy-loaded | P0 |
| VIEW-08 | Scroll mode | Page tracking | 1. In scroll mode 2. Scroll through pages | Page counter in toolbar updates based on visible page | P1 |
| VIEW-09 | Scroll mode | Mode toggle | 1. Switch from page to scroll mode 2. Switch back | Both modes work, current page position preserved | P1 |
| VIEW-10 | Zoom | Zoom in | 1. In page mode 2. Press + or use zoom slider | Image scales up (max 200%) | P0 |
| VIEW-11 | Zoom | Zoom out | 1. Zoom to 150% 2. Press - or use zoom slider | Image scales down (min 50%) | P0 |
| VIEW-12 | Zoom | Zoom limits | 1. Try to zoom past 200% 2. Try to zoom below 50% | Zoom clamped at boundaries | P1 |
| VIEW-13 | Zoom | Zoom persistence | 1. Set zoom to 150% 2. Close viewer 3. Reopen any comic | Zoom level restored from localStorage | P2 |
| VIEW-14 | Fullscreen | Enter fullscreen | 1. Press F or click fullscreen button | Browser enters fullscreen mode | P1 |
| VIEW-15 | Fullscreen | Exit fullscreen | 1. In fullscreen 2. Press Escape or F | Returns to windowed mode | P1 |
| VIEW-16 | Toolbar | Back button | 1. In viewer 2. Click back button | Returns to `/library` | P0 |
| VIEW-17 | Toolbar | Title display | 1. Open a comic | Comic filename shown in toolbar | P1 |
| VIEW-18 | Preloading | Next page preload | 1. Open a comic on page 1 | Page 2 is preloaded in background (check Network tab) | P2 |
| VIEW-19 | Mode preference | Mode persistence | 1. Switch to scroll mode 2. Close viewer 3. Open any comic | Scroll mode is remembered from localStorage | P2 |

## Reading Progress

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| PROG-01 | Save | Auto-save progress | 1. Open a comic 2. Navigate to page 5 3. Wait 2+ seconds 4. Close viewer 5. Reopen same comic | Opens on page 5 | P0 |
| PROG-02 | Badge | Progress badge on card | 1. Read a comic partially 2. Return to library | Comic card shows circular progress badge with percentage | P0 |
| PROG-03 | Badge | No badge for unread | 1. View library with unstarted comics | No progress badge shown on unread comics | P1 |
| PROG-04 | Multi-user | Independent progress | 1. User A reads to page 10 2. User B opens same comic | User B starts at page 1, User A's progress unchanged | P1 |
| PROG-05 | Debounce | Rapid page turning | 1. Quickly flip through 10 pages | Only final position is saved (not every intermediate page) | P2 |

## Ratings

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| RATE-01 | Submit | Rate a comic | 1. Click stars on a comic card 2. Select 4 stars | Rating saved, stars reflect selection | P0 |
| RATE-02 | Update | Change rating | 1. Rate a comic 3 stars 2. Click 5 stars | Rating updated to 5 stars | P1 |
| RATE-03 | Average | Average display | 1. Two users rate same comic (3 and 5 stars) | Average shows 4.0 | P1 |
| RATE-04 | Persistence | Rating persists | 1. Rate a comic 2. Refresh page | Rating still displayed | P1 |
| RATE-05 | Error | Rating with invalid value | 1. Attempt to submit rating outside 1-5 range (API test) | 400 Bad Request | P2 |

## TTS (Text-to-Speech)

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| TTS-01 | Job creation | Create TTS job | 1. Open a comic 2. Click TTS button | Job created, button shows processing state | P0 |
| TTS-02 | Progress | Monitor job progress | 1. Create TTS job 2. Observe progress | Progress updates from 0 to 100%, status shown in tooltip | P0 |
| TTS-03 | Duplicate | Prevent duplicate jobs | 1. Create a TTS job 2. Try to create another for same file | Button disabled, 409 error handled gracefully | P1 |
| TTS-04 | Audio playback | Play page audio | 1. Complete TTS job 2. Navigate to a page | Audio player appears, play button works | P0 |
| TTS-05 | Audio controls | Volume and seek | 1. Play audio 2. Adjust volume slider 3. Seek in progress bar | Audio responds to controls | P1 |
| TTS-06 | Audio mute | Mute/unmute | 1. Play audio 2. Press M or click mute button | Audio mutes, press again to unmute | P1 |
| TTS-07 | Audio persistence | Volume remembered | 1. Set volume to 50% 2. Change page 3. Return | Volume still at 50% (localStorage) | P2 |
| TTS-08 | Job failure | Handle TTS failure | 1. Create TTS job without valid API keys configured | Job shows FAILED status with error message, retry available | P1 |
| TTS-09 | Page change | Audio switches with page | 1. Playing audio on page 3 2. Navigate to page 4 | Audio switches to page 4's audio | P1 |

## Admin Panel

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| ADMIN-01 | Access | Admin access control | 1. Login as USER role 2. Try to access `/api/admin/*` | 403 Forbidden | P0 |
| ADMIN-02 | Users | List users | 1. Login as ADMIN 2. View user list | Paginated list of active users | P0 |
| ADMIN-03 | Users | Create user | 1. As admin, create new user with role | User appears in list, can login | P1 |
| ADMIN-04 | Users | Update user | 1. As admin, update a user's email | Change persisted | P1 |
| ADMIN-05 | Users | Delete user | 1. As admin, delete a user | User soft-deleted (no longer in list, can't login) | P1 |
| ADMIN-06 | Users | Prevent self-demotion | 1. As admin, try to change own role to USER | Error: cannot demote yourself | P1 |
| ADMIN-07 | Users | Protect last admin | 1. As sole admin, try to demote to USER | Error: cannot remove last admin | P1 |
| ADMIN-08 | LLM | View LLM configs | 1. As admin, view LLM configuration page | Configs shown with masked API keys | P1 |
| ADMIN-09 | LLM | Update API key | 1. As admin, update OpenAI API key | Key encrypted and stored, displayed masked | P1 |
| ADMIN-10 | LLM | Test connection | 1. As admin, click test on a valid LLM config | Success response with response time | P1 |
| ADMIN-11 | LLM | Test invalid key | 1. As admin, test config with invalid API key | Failure response with error message | P1 |
| ADMIN-12 | Audit | Audit trail | 1. Perform admin actions 2. Check audit logs | All actions recorded with admin ID, timestamp, details | P1 |

## Security

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| SEC-01 | Session | HttpOnly cookie | 1. Login 2. Inspect cookies in browser DevTools | SESSION cookie has HttpOnly flag | P0 |
| SEC-02 | Session | SameSite cookie | 1. Login 2. Inspect cookie | SameSite=Lax attribute present | P1 |
| SEC-03 | CORS | Cross-origin blocked | 1. Make API request from unauthorized origin | Request blocked by CORS | P1 |
| SEC-04 | Upload | Path traversal | 1. Upload file with name `../../../etc/passwd.cbz` | Filename sanitized, no path traversal | P0 |
| SEC-05 | API keys | Key encryption | 1. As admin, save an API key 2. Check database directly | Key stored encrypted (not plaintext) | P1 |
| SEC-06 | API keys | Key masking | 1. As admin, retrieve LLM configs via API | API keys shown as `sk-****abcd` (masked) | P1 |
| SEC-07 | Auth | Unauthenticated API access | 1. Call `/api/files` without session cookie | 401 Unauthorized | P0 |
| SEC-08 | Auth | Non-admin accessing admin API | 1. Login as USER 2. Call `/api/admin/users` | 403 Forbidden | P0 |

## Edge Cases & Error Handling

| ID | Feature | Test Case | Steps | Expected Result | Priority |
|----|---------|-----------|-------|-----------------|----------|
| EDGE-01 | Viewer | Invalid file ID | 1. Navigate to `/viewer/99999` (non-existent) | Error message, option to return to library | P1 |
| EDGE-02 | Viewer | Invalid page number | 1. Request page 0 or page > pageCount via API | 400 or 404 error | P1 |
| EDGE-03 | Upload | Empty archive | 1. Upload CBZ with no image files inside | "Invalid Archive" error | P1 |
| EDGE-04 | Network | API unavailable | 1. Stop backend 2. Try any action in frontend | Meaningful error message, no blank screen | P1 |
| EDGE-05 | Network | Upload interrupted | 1. Start uploading large file 2. Kill network mid-upload | Upload error shown, can retry | P2 |
| EDGE-06 | Concurrent | Simultaneous uploads | 1. Start two uploads at the same time | Both complete successfully | P2 |
| EDGE-07 | Browser | Page refresh during viewing | 1. Open comic on page 5 2. Refresh browser | Returns to same page (progress was auto-saved) | P1 |
| EDGE-08 | Browser | Back button behavior | 1. Library → Viewer → Browser back button | Returns to library | P1 |
| EDGE-09 | Storage | Cache cleanup | 1. Fill cache beyond 100 MB 2. Wait for 2 AM cron or trigger manually | Oldest cache entries evicted | P2 |
| EDGE-10 | Unicode | Unicode filename | 1. Upload comic with Unicode characters in filename | Original filename preserved in display, UUID used for storage | P2 |
| EDGE-11 | Large file | View comic with 500+ pages | 1. Upload large comic 2. Navigate, scroll, use scroll mode | Performance remains acceptable, no crashes | P2 |
| EDGE-12 | PWA | Install as PWA | 1. Open app in Chrome 2. Install as PWA | App installs, opens in standalone window | P2 |
