import { getFirebaseAuth } from "/js/firebase-config.js";
import {
  signInWithEmailAndPassword,
  setPersistence,
  browserLocalPersistence,
  browserSessionPersistence,
  GoogleAuthProvider,
  getRedirectResult,
  signInWithRedirect,
  signInWithPopup,
  sendPasswordResetEmail,
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

async function loginToBackend(idToken) {
  const resp = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });

  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.message || "Đăng nhập thất bại");
  }

  return await resp.json();
}

if (form) {
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    console.log("Login submit triggered");
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const rememberMe = document.getElementById("rememberMe")?.checked;

    if (!validateLoginForm(email, password)) {
      return;
    }

    setLoading(true);
    try {
      const auth = await getFirebaseAuth();
      console.log("Auth instance:", auth);
      
      const persistence = rememberMe ? browserLocalPersistence : browserSessionPersistence;
      await setPersistence(auth, persistence);
      
      const cred = await signInWithEmailAndPassword(auth, email, password);
      const token = await cred.user.getIdToken(true);
      console.log("Login success:", cred.user.uid);
      const data = await loginToBackend(token);
      
      if (rememberMe) {
        localStorage.setItem("firebase_token", token);
        localStorage.setItem("userInfo", JSON.stringify(data));
      } else {
        sessionStorage.setItem("firebase_token", token);
        sessionStorage.setItem("userInfo", JSON.stringify(data));
      }
      
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

async function handleGoogleRedirectResult() {
  try {
    const auth = await getFirebaseAuth();
    const result = await getRedirectResult(auth);
    if (!result || !result.user) {
      return;
    }

    const token = await result.user.getIdToken(true);
    const data = await loginToBackend(token);
    
    const rememberMe = sessionStorage.getItem("rememberMe_redirect") === "true";
    sessionStorage.removeItem("rememberMe_redirect");

    if (rememberMe) {
      localStorage.setItem("firebase_token", token);
      localStorage.setItem("userInfo", JSON.stringify(data));
    } else {
      sessionStorage.setItem("firebase_token", token);
      sessionStorage.setItem("userInfo", JSON.stringify(data));
    }
    window.location.href = "/";
  } catch (err) {
    console.error("Google redirect login error", err);
    showError("Đăng nhập Google thất bại");
  }
}

handleGoogleRedirectResult();

// Google login
const googleBtn = document.getElementById("googleLoginBtn");
if (googleBtn) {
  googleBtn.addEventListener("click", async () => {
    const rememberMe = document.getElementById("rememberMe")?.checked;
    try {
      const auth = await getFirebaseAuth();
      const provider = new GoogleAuthProvider();
      
      const persistence = rememberMe ? browserLocalPersistence : browserSessionPersistence;
      await setPersistence(auth, persistence);
      
      const cred = await signInWithPopup(auth, provider);
      const token = await cred.user.getIdToken(true);
      const data = await loginToBackend(token);
      
      if (rememberMe) {
        localStorage.setItem("firebase_token", token);
        localStorage.setItem("userInfo", JSON.stringify(data));
      } else {
        sessionStorage.setItem("firebase_token", token);
        sessionStorage.setItem("userInfo", JSON.stringify(data));
      }
      window.location.href = "/";
    } catch (err) {
      const code = getFirebaseErrorCode(err);
      if (
        code === "auth/popup-blocked" ||
        code === "auth/cancelled-popup-request"
      ) {
        try {
          const auth = await getFirebaseAuth();
          const provider = new GoogleAuthProvider();
          
          sessionStorage.setItem("rememberMe_redirect", rememberMe ? "true" : "false");
          await signInWithRedirect(auth, provider);
          return;
        } catch (redirectError) {
          console.error("Google redirect fallback error", redirectError);
        }
      }
      console.error("Google login error", err);
      showError("Đăng nhập Google thất bại");
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

// Forgot password
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const forgotPasswordForm = document.getElementById("forgotPasswordForm");
const forgotSubmitBtn = document.getElementById("forgot-submit-btn");

function showModalError(message) {
  const el = document.getElementById("modal-error-message");
  const successEl = document.getElementById("modal-success-message");
  if (el) {
    el.innerHTML = message;
    el.style.display = "block";
  }
  if (successEl) {
    successEl.style.display = "none";
  }
}

function showModalSuccess(message) {
  const el = document.getElementById("modal-success-message");
  const errorEl = document.getElementById("modal-error-message");
  if (el) {
    el.innerHTML = message;
    el.style.display = "block";
  }
  if (errorEl) {
    errorEl.style.display = "none";
  }
}

function setModalLoading(isLoading) {
  if (!forgotSubmitBtn) return;
  if (isLoading) {
    forgotSubmitBtn.disabled = true;
    forgotSubmitBtn.dataset.originalText = forgotSubmitBtn.dataset.originalText || forgotSubmitBtn.innerHTML;
    forgotSubmitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';
  } else {
    forgotSubmitBtn.disabled = false;
    if (forgotSubmitBtn.dataset.originalText) {
      forgotSubmitBtn.innerHTML = forgotSubmitBtn.dataset.originalText;
    }
  }
}

if (forgotPasswordLink) {
  forgotPasswordLink.addEventListener("click", (e) => {
    e.preventDefault();
    
    // Autofill email from login email input if available
    const currentEmail = document.getElementById("email")?.value.trim();
    const modalEmailInput = document.getElementById("forgotEmail");
    if (currentEmail && modalEmailInput) {
      modalEmailInput.value = currentEmail;
    }
    
    // Clear previous alerts
    const modalError = document.getElementById("modal-error-message");
    const modalSuccess = document.getElementById("modal-success-message");
    if (modalError) modalError.style.display = "none";
    if (modalSuccess) modalSuccess.style.display = "none";
    
    // Open Bootstrap modal
    const modalEl = document.getElementById("forgotPasswordModal");
    if (modalEl && window.bootstrap) {
      const modal = new window.bootstrap.Modal(modalEl);
      modal.show();
    }
  });
}

if (forgotPasswordForm) {
  forgotPasswordForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("forgotEmail")?.value.trim();
    
    if (!email) {
      showModalError("Vui lòng nhập email.");
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
      showModalError("Email không hợp lệ.");
      return;
    }

    setModalLoading(true);
    try {
      const auth = await getFirebaseAuth();
      await sendPasswordResetEmail(auth, email);
      showModalSuccess("Đã gửi liên kết đặt lại mật khẩu đến email: <b>" + email + "</b>. Vui lòng kiểm tra hộp thư.");
      
      // Also pre-fill the main login email field for user convenience
      const mainEmail = document.getElementById("email");
      if (mainEmail) {
        mainEmail.value = email;
      }
    } catch (ex) {
      console.error("Forgot password error:", ex);
      const errorCode = getFirebaseErrorCode(ex);
      let msg = "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại.";
      if (errorCode === "auth/user-not-found") {
        msg = "Email này chưa được đăng ký trong hệ thống.";
      } else if (errorCode === "auth/invalid-email") {
        msg = "Email không hợp lệ.";
      } else if (errorCode === "auth/too-many-requests") {
        msg = "Yêu cầu quá nhiều lần. Vui lòng thử lại sau.";
      }
      showModalError(msg);
    } finally {
      setModalLoading(false);
    }
  });
}

