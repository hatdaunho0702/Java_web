document.addEventListener("DOMContentLoaded", () => {
  const sidebar = document.getElementById("sidebar");
  if (sidebar && window.innerWidth < 768) {
    sidebar.classList.add("collapsed");
  }
});
