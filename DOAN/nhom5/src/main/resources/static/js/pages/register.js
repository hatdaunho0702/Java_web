import { getFirebaseAuth } from "/js/firebase-config.js";
import {
  createUserWithEmailAndPassword,
  updateProfile,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

const form = document.getElementById("registerForm");
const submitButton = document.getElementById("register-submit-btn");

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

function getRegisterErrorMessage(errorCode) {
  const errors = {
    "auth/email-already-in-use":
      'Email đã được đăng ký. <a href="/login">Đăng nhập ngay</a>',
    "auth/invalid-email": "Email không hợp lệ.",
    "auth/weak-password": "Mật khẩu phải có ít nhất 6 ký tự.",
    "auth/network-request-failed": "Lỗi kết nối mạng. Vui lòng thử lại.",
    "auth/too-many-requests": "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
  };
  return errors[errorCode] || "Đăng ký thất bại. Vui lòng thử lại.";
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

function validateRegisterForm(email, password, fullName, phone) {
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
  if (!fullName) {
    showError("Vui lòng nhập họ tên.");
    return false;
  }
  const phonePattern = /^\d{10,11}$/;
  if (phone && !phonePattern.test(phone)) {
    showError("Số điện thoại chỉ được chứa số, độ dài từ 10 đến 11 số.");
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
    console.log("Register submit triggered");
    const email = (
      document.getElementById("regEmail") || document.getElementById("email")
    ).value.trim();
    const password = (
      document.getElementById("regPassword") ||
      document.getElementById("password")
    ).value;
    const fullName = (
      document.getElementById("regName") || document.getElementById("fullName")
    ).value.trim();
    const phone =
      (
        document.getElementById("regPhone") || document.getElementById("phone")
      )?.value?.trim() || "";

    if (!validateRegisterForm(email, password, fullName, phone)) {
      return;
    }

    setLoading(true);
    try {
      const auth = await getFirebaseAuth();
      console.log("Auth instance:", auth);
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      if (fullName) {
        await updateProfile(cred.user, { displayName: fullName });
      }
      console.log("Register success:", cred.user.uid);
      const resp = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          uid: cred.user.uid,
          email,
          fullName,
          phone,
          avatarUrl: cred.user.photoURL || "",
        }),
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        showError(err.message || getRegisterErrorMessage(err.error));
        return;
      }
      const data = await resp.json();
      localStorage.setItem("userInfo", JSON.stringify(data));
      showSuccess("Đăng ký thành công. Đang chuyển sang trang đăng nhập...");
      setTimeout(() => {
        window.location.href = "/login";
      }, 800);
    } catch (ex) {
      console.error("Register error full object:", ex);
      console.error(
        "error.code:",
        ex && typeof ex === "object" ? ex.code : undefined,
      );
      console.error(
        "error.message:",
        ex && typeof ex === "object" ? ex.message : ex,
      );
      const errorCode = getFirebaseErrorCode(ex);
      showError(getRegisterErrorMessage(errorCode));
    } finally {
      setLoading(false);
    }
  });
}
