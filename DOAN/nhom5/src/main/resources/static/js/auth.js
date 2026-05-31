import { getFirebaseAuth } from "./firebase-config.js";
import {
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

let authPromise = null;
let authReadyPromise = null;

async function getAuthInstance() {
  if (!authPromise) {
    authPromise = getFirebaseAuth();
  }
  return await authPromise;
}

async function waitForAuthReady() {
  const auth = await getAuthInstance();
  if (!authReadyPromise) {
    authReadyPromise = new Promise((resolve) => {
      const unsubscribe = onAuthStateChanged(auth, (user) => {
        unsubscribe();
        resolve(user);
      });
    });
  }
  return await authReadyPromise;
}

export async function getToken() {
  const auth = await getAuthInstance();
  await waitForAuthReady();
  const user = auth.currentUser;
  if (!user) {
    return null;
  }
  return await user.getIdToken(true);
}

export async function logout() {
  const auth = await getAuthInstance();
  try {
    await fetch("/logout", {
      method: "POST",
      credentials: "same-origin",
    });
  } catch (error) {
    console.warn("Không thể đóng session backend:", error);
  }

  await signOut(auth);
  localStorage.clear();
  sessionStorage.clear();
  window.location.href = "/login";
}

export function requireAuth() {
  getAuthInstance()
    .then((auth) => {
      onAuthStateChanged(auth, (user) => {
        if (!user) {
          window.location.href = "/login";
        }
      });
    })
    .catch((error) => {
      console.error("Không thể khởi tạo Firebase auth:", error);
      window.location.href = "/login";
    });
}

export function requireAdmin() {
  getAuthInstance()
    .then((auth) => {
      onAuthStateChanged(auth, (user) => {
        if (!user) {
          window.location.href = "/login";
          return;
        }
        const role = JSON.parse(localStorage.getItem("userInfo") || sessionStorage.getItem("userInfo") || "{}").role;
        if (role !== "ADMIN") {
          window.location.href = "/";
        }
      });
    })
    .catch((error) => {
      console.error("Không thể khởi tạo Firebase auth:", error);
      window.location.href = "/login";
    });
}

function bindNavbarAuthState() {
  getAuthInstance()
    .then((auth) => {
      onAuthStateChanged(auth, async (user) => {
        if (!user) {
          document
            .getElementById("nav-login-item")
            ?.style.setProperty("display", "block");
          
          const userNameEl = document.getElementById("nav-user-name");
          if (userNameEl) {
            userNameEl.textContent = "";
            userNameEl.style.display = "none";
          }
          const userBtn = document.getElementById("nav-user-dropdown-btn");
          if (userBtn) {
            userBtn.style.background = "none";
            userBtn.style.padding = "0";
            userBtn.style.borderRadius = "0";
          }
          return;
        }

        const info = JSON.parse(localStorage.getItem("userInfo") || sessionStorage.getItem("userInfo") || "{}");
        const role = info.role;
        
        document
          .getElementById("nav-login-item")
          ?.style.setProperty("display", "none");
        document
          .getElementById("nav-orders-item")
          ?.style.setProperty("display", "block");
        document
          .getElementById("nav-logout-item")
          ?.style.setProperty("display", "block");
        if (role === "ADMIN") {
          document
            .getElementById("nav-admin-item")
            ?.style.setProperty("display", "block");
        }
        
        // Dynamic User Chip styling
        const displayName = info.fullName || info.email || "Tài khoản";
        const userNameEl = document.getElementById("nav-user-name");
        if (userNameEl) {
          userNameEl.textContent = displayName;
          userNameEl.style.display = "inline-block";
        }
        const userBtn = document.getElementById("nav-user-dropdown-btn");
        if (userBtn) {
          userBtn.style.background = "#f1f5f9";
          userBtn.style.padding = "6px 14px";
          userBtn.style.borderRadius = "20px";
        }

        const adminNameEl = document.getElementById("adminName");
        if (adminNameEl) {
          adminNameEl.textContent = info.email || "";
        }
        const topbarAdminNameEl = document.getElementById("topbarAdminName");
        if (topbarAdminNameEl) {
          topbarAdminNameEl.textContent = info.email || "";
        }
      });
    })
    .catch((error) => {
      console.error("Không thể khởi tạo Firebase auth cho navbar:", error);
    });
}

bindNavbarAuthState();

export async function getAuthHeaders() {
  const token = await getToken();
  if (!token) {
    window.location.href = '/login';
    return null;
  }
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}

window.logout = logout;
window.getToken = getToken;
window.getAuthHeaders = getAuthHeaders;
window.waitForAuthReady = waitForAuthReady;
window.requireAuth = requireAuth;
window.requireAdmin = requireAdmin;
