import { getToken } from "./auth.js";
import { getFirebaseAuth } from "./firebase-config.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

function showToast(message, type = "success") {
  const icons = { success: "✅", error: "❌", warning: "⚠️", info: "ℹ️" };
  const div = document.createElement("div");
  div.className = `toast-custom toast-${type}`;
  div.innerHTML = `<span>${icons[type] || icons.info}</span><span>${message}</span>`;
  document.getElementById("toastContainer")?.appendChild(div);
  setTimeout(() => div.remove(), 3000);
}

function startCountdown(targetDate) {
  const tick = () => {
    const diff = targetDate - Date.now();
    if (diff <= 0) {
      return;
    }
    const d = Math.floor(diff / 86400000);
    const h = Math.floor((diff % 86400000) / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    const s = Math.floor((diff % 60000) / 1000);
    const cdDaysEl = document.getElementById("cd-days");
    if (cdDaysEl) {
      cdDaysEl.textContent = String(d).padStart(2, "0");
    }
    const cdHoursEl = document.getElementById("cd-hours");
    if (cdHoursEl) {
      cdHoursEl.textContent = String(h).padStart(2, "0");
    }
    const cdMinsEl = document.getElementById("cd-mins");
    if (cdMinsEl) {
      cdMinsEl.textContent = String(m).padStart(2, "0");
    }
    const cdSecsEl = document.getElementById("cd-secs");
    if (cdSecsEl) {
      cdSecsEl.textContent = String(s).padStart(2, "0");
    }
  };
  tick();
  setInterval(tick, 1000);
}

const nextSunday = new Date();
nextSunday.setDate(nextSunday.getDate() + (7 - nextSunday.getDay()));
nextSunday.setHours(23, 59, 59, 0);
startCountdown(nextSunday);

function resolveProductId(productId) {
  const normalized = Number(productId);
  if (Number.isFinite(normalized) && normalized > 0) {
    return normalized;
  }

  const detailRoot = document.getElementById("product-detail-root");
  const rootProductId = Number(detailRoot?.dataset?.productId || 0);
  if (Number.isFinite(rootProductId) && rootProductId > 0) {
    return rootProductId;
  }

  return 0;
}

function resolveQuantity(quantity) {
  const normalized = Number(quantity);
  if (Number.isFinite(normalized) && normalized > 0) {
    return normalized;
  }

  const qtyInput = document.getElementById("qty-input");
  const inputQuantity = Number(qtyInput?.value || 1);
  return Number.isFinite(inputQuantity) && inputQuantity > 0
    ? inputQuantity
    : 1;
}

async function addToCart(productId, quantity = 1, triggerEl = null) {
  let token = null;
  try {
    token = await getToken();
  } catch (e) {
    token = null;
  }

  if (!token) {
    showToast("Vui lòng đăng nhập để thêm vào giỏ hàng", "warning");
    setTimeout(() => {
      window.location.href = "/login";
    }, 1500);
    return;
  }

  const resolvedProductId = resolveProductId(productId);
  const resolvedQuantity = resolveQuantity(quantity);
  if (!resolvedProductId) {
    showToast("Không tìm thấy sản phẩm để thêm vào giỏ", "error");
    return;
  }

  const btn =
    triggerEl ||
    (typeof event !== "undefined" && event?.target?.closest
      ? event.target.closest(".btn-add-cart")
      : null) ||
    null;

  if (btn) {
    btn.disabled = true;
    btn.dataset.originalText = btn.innerHTML;
    btn.innerHTML = "⏳ Đang thêm...";
  }

  try {
    const res = await fetch("/api/cart", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        productId: resolvedProductId,
        quantity: resolvedQuantity,
      }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Có lỗi xảy ra");
    }

    await updateCartBadge();
    showToast("Đã thêm vào giỏ hàng!", "success");
  } catch (err) {
    showToast(err.message || "Có lỗi xảy ra", "error");
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = btn.dataset.originalText || "🛒 Thêm vào giỏ";
    }
  }
}

async function updateCartBadge() {
  const badge = document.getElementById("cartCount");
  if (!badge) {
    return;
  }

  let token = null;
  try {
    token = await getToken();
  } catch (e) {
    token = null;
  }

  if (!token) {
    badge.textContent = "0";
    badge.style.display = "none";
    return;
  }

  try {
    const res = await fetch("/api/cart/count", {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      const data = await res.json();
      const count = Number(data.count || 0);
      badge.textContent = String(count);
      badge.style.display = count > 0 ? "flex" : "none";
    }
  } catch (e) {
    console.log("Cart badge error:", e);
  }
}

function quickView(slug) {
  const modalEl = document.getElementById("quickViewModal");
  if (!modalEl || typeof bootstrap === "undefined") {
    return;
  }
  new bootstrap.Modal(modalEl).show();
}

function changeQty(delta) {
  const el = document.getElementById("qv-qty");
  if (!el) {
    return;
  }
  el.textContent = String(
    Math.max(1, parseInt(el.textContent || "1", 10) + delta),
  );
}

function initScrollReveal() {
  const elements = document.querySelectorAll(".reveal-on-scroll");
  if (elements.length === 0) return;

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const el = entry.target;
          const delayIndex = el.getAttribute("data-delay-index") || el.style.getPropertyValue("--delay-index");
          if (delayIndex) {
            el.style.transitionDelay = `${Number(delayIndex) * 80}ms`;
          }
          el.classList.add("revealed");
          observer.unobserve(el);
        }
      });
    },
    {
      threshold: 0.05,
      rootMargin: "0px 0px -40px 0px"
    }
  );

  elements.forEach((el) => {
    if (!el.classList.contains("revealed")) {
      observer.observe(el);
    }
  });
}

window.showToast = showToast;
window.addToCart = addToCart;
window.updateCartBadge = updateCartBadge;
window.quickView = quickView;
window.changeQty = changeQty;
window.initScrollReveal = initScrollReveal;

// Run initial reveal on DOM content loaded
document.addEventListener("DOMContentLoaded", () => {
  initScrollReveal();
});


// Update cart badge only after Firebase auth state resolved to avoid
// calling /api/cart when user is not logged in.
getFirebaseAuth()
  .then((auth) => {
    onAuthStateChanged(auth, (user) => {
      updateCartBadge();
    });
  })
  .catch((e) => {
    // If firebase cannot init, keep badge hidden
    const badge = document.getElementById("cartCount");
    if (badge) {
      badge.textContent = "0";
      badge.style.display = "none";
    }
    console.debug("Firebase init failed for cart badge:", e);
  });

// Delegate clicks for dynamically rendered add-to-cart buttons and quick view
document.addEventListener("click", (ev) => {
  const addBtn = ev.target.closest && ev.target.closest(".btn-add-cart");
  if (addBtn) {
    const pid = addBtn.dataset.productId || addBtn.dataset.id;
    if (pid) {
      const qtyInput = document.getElementById("qty-input");
      const qty = Number(qtyInput?.value || 1);
      addToCart(pid, qty, addBtn);
    }
    return;
  }

  const qv = ev.target.closest && ev.target.closest(".btn-quick-view");
  if (qv) {
    const slug = qv.dataset.slug;
    if (slug) {
      quickView(slug);
    }
  }
});
