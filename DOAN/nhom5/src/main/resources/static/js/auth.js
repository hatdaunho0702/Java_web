import { getFirebaseAuth } from "./firebase-config.js";
import {
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

let authPromise = null;

async function getAuthInstance() {
  if (!authPromise) {
    authPromise = getFirebaseAuth();
  }
  return await authPromise;
}

export async function getToken() {
  const auth = await getAuthInstance();
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
        const role = JSON.parse(localStorage.getItem("userInfo") || "{}").role;
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
          return;
        }

        const role = JSON.parse(localStorage.getItem("userInfo") || "{}").role;
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
        const info = JSON.parse(localStorage.getItem("userInfo") || "{}");
        document.getElementById("adminName")?.textContent = info.email || "";
        document.getElementById("topbarAdminName")?.textContent =
          info.email || "";
      });
    })
    .catch((error) => {
      console.error("Không thể khởi tạo Firebase auth cho navbar:", error);
    });
}

bindNavbarAuthState();

window.logout = logout;
window.requireAuth = requireAuth;
window.requireAdmin = requireAdmin;
