/**
 * admin-auth-bridge.js
 * -------------------------------------------------
 * Loads auth.js (ES Module) and exposes all needed
 * functions to window scope. Page scripts must wait
 * for the "authReady" event on window before using
 * window.getToken / window.getAuthHeaders etc.
 *
 * Usage (page script, NOT a module):
 *   window.addEventListener('authReady', async () => {
 *     const headers = await window.getAuthHeaders();
 *     ...
 *   });
 * -------------------------------------------------
 */

(async function () {
  try {
    // Dynamic import works in any modern browser regardless of the script tag type
    const authModule = await import('/js/auth.js');

    // Expose to window - auth.js already sets most of these but we ensure it here
    window.getToken       = authModule.getToken;
    window.getAuthHeaders = authModule.getAuthHeaders;
    window.logout         = authModule.logout;
    window.requireAuth    = authModule.requireAuth;
    window.requireAdmin   = authModule.requireAdmin;

    // Dispatch ready event so page scripts know auth is available
    window.dispatchEvent(new Event('authReady'));
  } catch (err) {
    console.error('[admin-auth-bridge] Failed to load auth module:', err);
    // Redirect to login as fallback
    window.location.href = '/login';
  }
})();
