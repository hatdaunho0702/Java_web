import { auth } from "./firebase-config.js";
import {
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

export async function getToken() {
  const user = auth.currentUser;
  if (!user) {
    return null;
  }
  return await user.getIdToken(true);
}

export async function logout() {
  await signOut(auth);
  localStorage.clear();
  window.location.href = "/login";
}

export function requireAuth() {
  onAuthStateChanged(auth, (user) => {
    if (!user) {
      window.location.href = "/login";
    }
  });
}

export function requireAdmin() {
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
}

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
  document.getElementById("topbarAdminName")?.textContent = info.email || "";
});

window.logout = logout;
window.requireAuth = requireAuth;
window.requireAdmin = requireAdmin;
