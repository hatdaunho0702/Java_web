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
    document.getElementById("cd-days")?.textContent = String(d).padStart(
      2,
      "0",
    );
    document.getElementById("cd-hours")?.textContent = String(h).padStart(
      2,
      "0",
    );
    document.getElementById("cd-mins")?.textContent = String(m).padStart(
      2,
      "0",
    );
    document.getElementById("cd-secs")?.textContent = String(s).padStart(
      2,
      "0",
    );
  };
  tick();
  setInterval(tick, 1000);
}

const nextSunday = new Date();
nextSunday.setDate(nextSunday.getDate() + (7 - nextSunday.getDay()));
nextSunday.setHours(23, 59, 59, 0);
startCountdown(nextSunday);

function addToCart(productId) {
  const badge = document.getElementById("cartCount");
  if (badge) {
    badge.textContent = String(
      (parseInt(badge.textContent || "0", 10) || 0) + 1,
    );
  }
  showToast(`Đã thêm sản phẩm #${productId} vào giỏ hàng!`, "success");
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

window.showToast = showToast;
window.addToCart = addToCart;
window.quickView = quickView;
window.changeQty = changeQty;
