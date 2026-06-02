(function () {
  let chatHistory = [];
  const HISTORY_KEY = "electrabot_history";
  const DEFAULT_MESSAGE = {
    role: "assistant",
    content: "Xin chào! Mình là ElectraBot, trợ lý ảo cực kỳ dễ thương của Electra Shop ⚡. Mình có thể giúp gì cho bạn hôm nay? 😊"
  };

  // Load history from session storage
  try {
    const cached = sessionStorage.getItem(HISTORY_KEY);
    if (cached) {
      chatHistory = JSON.parse(cached);
    } else {
      chatHistory = [DEFAULT_MESSAGE];
    }
  } catch (e) {
    chatHistory = [DEFAULT_MESSAGE];
  }

  document.addEventListener("DOMContentLoaded", () => {
    initChatbox();
  });

  function initChatbox() {
    // 1. Create HTML elements if they don't exist
    if (document.getElementById("electrabot-container")) return;

    const widget = document.createElement("div");
    widget.id = "electrabot-container";
    widget.innerHTML = `
      <!-- Floating Bubble Button -->
      <button id="electrabot-toggle-btn" class="electrabot-floating-btn" title="Trò chuyện với ElectraBot">
        <span class="electrabot-icon">🤖</span>
      </button>

      <!-- Chat Window Panel -->
      <div id="electrabot-window" class="electrabot-chat-window electrabot-hidden">
        <div class="electrabot-header">
          <div class="electrabot-info">
            <span class="electrabot-avatar">🤖</span>
            <div>
              <div class="electrabot-name">ElectraBot</div>
              <div class="electrabot-status">Trợ lý ảo ⚡ Online</div>
            </div>
          </div>
          <button id="electrabot-close-btn" class="electrabot-close-x">&times;</button>
        </div>
        <div id="electrabot-messages" class="electrabot-messages-container"></div>
        <form id="electrabot-form" class="electrabot-input-area">
          <input type="text" id="electrabot-input" placeholder="Nhập câu hỏi của bạn..." autocomplete="off">
          <button type="submit" id="electrabot-send-btn">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>
    `;

    document.body.appendChild(widget);

    // 2. Inject CSS styles (Minimalist Black & White layout)
    const style = document.createElement("style");
    style.innerHTML = `
      .electrabot-floating-btn {
        position: fixed;
        bottom: 24px;
        right: 24px;
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: #000000;
        border: 2px solid #ffffff;
        color: #ffffff;
        font-size: 26px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      }
      .electrabot-floating-btn:hover {
        transform: scale(1.08) translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
      }
      @keyframes electrabot-pulse {
        0% { transform: scale(1); }
        50% { transform: scale(1.04); }
        100% { transform: scale(1); }
      }
      .electrabot-floating-btn {
        animation: electrabot-pulse 3s infinite ease-in-out;
      }
      .electrabot-chat-window {
        position: fixed;
        bottom: 96px;
        right: 24px;
        width: 350px;
        height: 480px;
        border-radius: 16px;
        background: #ffffff;
        border: 1px solid #e0e0e0;
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
        display: flex;
        flex-direction: column;
        z-index: 9998;
        overflow: hidden;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        transform-origin: bottom right;
      }
      .electrabot-chat-window.electrabot-hidden {
        transform: scale(0.8) translateY(20px);
        opacity: 0;
        pointer-events: none;
      }
      .electrabot-header {
        background: #000000;
        color: #ffffff;
        padding: 14px 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        border-bottom: 1px solid #333333;
      }
      .electrabot-info {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .electrabot-avatar {
        font-size: 22px;
      }
      .electrabot-name {
        font-weight: 700;
        font-size: 14px;
      }
      .electrabot-status {
        font-size: 11px;
        opacity: 0.7;
        margin-top: 1px;
      }
      .electrabot-close-x {
        background: none;
        border: none;
        color: #ffffff;
        font-size: 24px;
        cursor: pointer;
        padding: 0;
        opacity: 0.8;
        transition: opacity 0.2s;
      }
      .electrabot-close-x:hover {
        opacity: 1;
      }
      .electrabot-messages-container {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        background: #fafafa;
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .electrabot-bubble {
        max-width: 80%;
        padding: 10px 14px;
        border-radius: 14px;
        font-size: 13px;
        line-height: 1.45;
        word-wrap: break-word;
        box-shadow: 0 1px 2px rgba(0,0,0,0.05);
      }
      .electrabot-bubble-assistant {
        background: #ffffff;
        border: 1px solid #e5e5e5;
        color: #1a1a1a;
        align-self: flex-start;
        border-bottom-left-radius: 4px;
      }
      .electrabot-bubble-user {
        background: #000000;
        color: #ffffff;
        align-self: flex-end;
        border-bottom-right-radius: 4px;
      }
      .electrabot-bubble-typing {
        display: flex;
        gap: 4px;
        padding: 12px 16px;
        align-items: center;
      }
      .electrabot-typing-dot {
        width: 6px;
        height: 6px;
        background: #888888;
        border-radius: 50%;
        animation: electrabot-typing-anim 1.4s infinite ease-in-out;
      }
      .electrabot-typing-dot:nth-child(1) { animation-delay: -0.32s; }
      .electrabot-typing-dot:nth-child(2) { animation-delay: -0.16s; }
      @keyframes electrabot-typing-anim {
        0%, 80%, 100% { transform: scale(0); }
        40% { transform: scale(1); }
      }
      .electrabot-input-area {
        display: flex;
        padding: 10px;
        background: #ffffff;
        border-top: 1px solid #eeeeee;
        gap: 8px;
      }
      .electrabot-input-area input {
        flex: 1;
        border: 1px solid #dddddd;
        border-radius: 20px;
        padding: 8px 14px;
        font-size: 13px;
        outline: none;
        transition: border-color 0.2s;
      }
      .electrabot-input-area input:focus {
        border-color: #000000;
      }
      #electrabot-send-btn {
        background: #000000;
        border: none;
        color: #ffffff;
        width: 34px;
        height: 34px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: transform 0.2s;
        flex-shrink: 0;
      }
      #electrabot-send-btn:hover {
        transform: scale(1.05);
      }
    `;
    document.head.appendChild(style);

    // 3. Setup event listeners
    const toggleBtn = document.getElementById("electrabot-toggle-btn");
    const closeBtn = document.getElementById("electrabot-close-btn");
    const win = document.getElementById("electrabot-window");
    const form = document.getElementById("electrabot-form");
    const input = document.getElementById("electrabot-input");

    toggleBtn.addEventListener("click", () => {
      win.classList.toggle("electrabot-hidden");
      if (!win.classList.contains("electrabot-hidden")) {
        renderMessages();
        input.focus();
      }
    });

    closeBtn.addEventListener("click", () => {
      win.classList.add("electrabot-hidden");
    });

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const val = input.value.trim();
      if (!val) return;

      input.value = "";
      
      // User message
      chatHistory.push({ role: "user", content: val });
      renderMessages();

      // Show typing indicator
      showTypingIndicator();

      try {
        const res = await fetch("/api/ai/chat", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(chatHistory)
        });

        removeTypingIndicator();

        if (res.ok) {
          const data = await res.json();
          chatHistory.push({ role: "assistant", content: data.reply });
          sessionStorage.setItem(HISTORY_KEY, JSON.stringify(chatHistory));
        } else {
          chatHistory.push({
            role: "assistant",
            content: "Xin lỗi, đã xảy ra lỗi kết nối từ máy chủ. Vui lòng thử lại sau! 😢"
          });
        }
      } catch (err) {
        removeTypingIndicator();
        chatHistory.push({
          role: "assistant",
          content: "Không thể kết nối mạng. Vui lòng kiểm tra lại kết nối của bạn! 🌐"
        });
      }

      renderMessages();
    });
  }

  function renderMessages() {
    const container = document.getElementById("electrabot-messages");
    if (!container) return;

    container.innerHTML = "";
    chatHistory.forEach(msg => {
      const bubble = document.createElement("div");
      bubble.className = `electrabot-bubble electrabot-bubble-${msg.role}`;
      bubble.textContent = msg.content;
      container.appendChild(bubble);
    });

    container.scrollTop = container.scrollHeight;
  }

  function showTypingIndicator() {
    const container = document.getElementById("electrabot-messages");
    if (!container) return;

    // Remove if exists
    removeTypingIndicator();

    const indicator = document.createElement("div");
    indicator.id = "electrabot-typing";
    indicator.className = "electrabot-bubble electrabot-bubble-assistant electrabot-bubble-typing";
    indicator.innerHTML = `
      <div class="electrabot-typing-dot"></div>
      <div class="electrabot-typing-dot"></div>
      <div class="electrabot-typing-dot"></div>
    `;
    container.appendChild(indicator);
    container.scrollTop = container.scrollHeight;
  }

  function removeTypingIndicator() {
    document.getElementById("electrabot-typing")?.remove();
  }

})();
