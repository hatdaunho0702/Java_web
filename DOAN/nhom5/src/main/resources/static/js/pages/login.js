import { getFirebaseAuth } from "/js/firebase-config.js";
import { signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import {
  GoogleAuthProvider,
  signInWithPopup,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

const form = document.getElementById("loginForm");
const submitButton = document.getElementById("login-submit-btn");

function showError(message) {
  console.log("showError called with:", message);
  const el = document.getElementById("error-message");
  const successEl = document.getElementById("success-message");
  if (!el) {
    console.error("KHONG TIM THAY div#error-message!");
    return;
  }
  el.innerHTML = message;
  el.style.display = "block";
  if (successEl) {
    successEl.style.display = "none";
  }
  setTimeout(() => {
    el.style.display = "none";
  }, 5000);
}

function showSuccess(message) {
  const el = document.getElementById("success-message");
  const errorEl = document.getElementById("error-message");
  if (!el) {
    return;
  }
  el.innerHTML = message;
  el.style.display = "block";
  if (errorEl) {
    errorEl.style.display = "none";
  }
}

function getLoginErrorMessage(errorCode) {
  const errors = {
    "auth/user-not-found": "Email chưa được đăng ký.",
    "auth/wrong-password": "Mật khẩu không đúng.",
    "auth/invalid-credential": "Email hoặc mật khẩu không đúng.",
    "auth/too-many-requests": "Quá nhiều lần thử. Vui lòng thử lại sau.",
    "auth/network-request-failed": "Lỗi kết nối mạng. Vui lòng thử lại.",
    "auth/user-disabled": "Tài khoản đã bị khóa. Liên hệ hỗ trợ.",
  };
  return errors[errorCode] || "Đăng nhập thất bại. Vui lòng thử lại.";
}

function getFirebaseErrorCode(error) {
  if (error && typeof error === "object" && error.code) {
    return error.code;
  }

  const message =
    error && typeof error === "object" ? error.message : String(error || "");
  const match = message.match(/\((auth\/[^)]+)\)/);
  if (match) {
    return match[1];
  }

  return "unknown";
}

function validateLoginForm(email, password) {
  if (!email) {
    showError("Vui lòng nhập email.");
    return false;
  }
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email)) {
    showError("Email không hợp lệ.");
    return false;
  }
  if (!password) {
    showError("Vui lòng nhập mật khẩu.");
    return false;
  }
  if (password.length < 6) {
    showError("Mật khẩu phải có ít nhất 6 ký tự.");
    return false;
  }
  return true;
}

function setLoading(isLoading) {
  if (!submitButton) {
    return;
  }
  if (isLoading) {
    submitButton.disabled = true;
    submitButton.dataset.originalText =
      submitButton.dataset.originalText || submitButton.innerHTML;
    submitButton.innerHTML =
      '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';
    return;
  }
  submitButton.disabled = false;
  if (submitButton.dataset.originalText) {
    submitButton.innerHTML = submitButton.dataset.originalText;
  }
}

if (form) {
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    console.log("Login submit triggered");
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (!validateLoginForm(email, password)) {
      return;
    }

    setLoading(true);
    try {
      const auth = await getFirebaseAuth();
      console.log("Auth instance:", auth);
      const cred = await signInWithEmailAndPassword(auth, email, password);
      const token = await cred.user.getIdToken(true);
      console.log("Login success:", cred.user.uid);
      const resp = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken: token }),
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        showError(err.message || getLoginErrorMessage(err.error));
        return;
      }
      const data = await resp.json();
      localStorage.setItem("firebase_token", token);
      localStorage.setItem("userInfo", JSON.stringify(data));
      showSuccess("Đăng nhập thành công. Đang chuyển trang...");
      setTimeout(() => {
        window.location.href = "/";
      }, 500);
    } catch (ex) {
      console.error("Login error full object:", ex);
      console.error(
        "error.code:",
        ex && typeof ex === "object" ? ex.code : undefined,
      );
      console.error(
        "error.message:",
        ex && typeof ex === "object" ? ex.message : ex,
      );
      const errorCode = getFirebaseErrorCode(ex);
      showError(getLoginErrorMessage(errorCode));
    } finally {
      setLoading(false);
    }
  });
}

// Google login
const googleBtn = document.getElementById("googleLoginBtn");
if (googleBtn) {
  googleBtn.addEventListener("click", async () => {
    try {
      const auth = await getFirebaseAuth();
      const provider = new GoogleAuthProvider();
      const cred = await signInWithPopup(auth, provider);
      const token = await cred.user.getIdToken(true);
      const resp = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken: token }),
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        alert(err.message || "Đăng nhập Google thất bại");
        return;
      }
      const data = await resp.json();
      localStorage.setItem("firebase_token", token);
      localStorage.setItem("userInfo", JSON.stringify(data));
      window.location.href = "/";
    } catch (err) {
      console.error("Google login error", err);
      alert("Đăng nhập Google thất bại");
    }
  });
}

// Toggle password
const togglePassword = document.getElementById("togglePassword");
if (togglePassword) {
  togglePassword.addEventListener("click", () => {
    const pwd = document.getElementById("password");
    if (!pwd) return;
    if (pwd.type === "password") {
      pwd.type = "text";
      togglePassword.textContent = "Ẩn";
    } else {
      pwd.type = "password";
      togglePassword.textContent = "Hiện";
    }
  });
}

// Tabs
const tabLogin = document.getElementById("tabLogin");
const tabRegister = document.getElementById("tabRegister");
if (tabLogin && tabRegister) {
  tabLogin.addEventListener("click", (e) => {
    e.preventDefault();
    document.getElementById("loginForm").style.display = "block";
    document.getElementById("registerForm").style.display = "none";
  });
  tabRegister.addEventListener("click", (e) => {
    e.preventDefault();
    document.getElementById("loginForm").style.display = "none";
    document.getElementById("registerForm").style.display = "block";
  });
}
