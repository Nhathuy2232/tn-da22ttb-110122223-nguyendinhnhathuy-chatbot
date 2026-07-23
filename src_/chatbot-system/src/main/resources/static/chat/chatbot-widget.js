/**
 * Chatbot Early Warning Widget
 * Embeddable chatbot widget for Moodle integration
 * 
 * @author Nguyễn Đình Nhật Huy
 */

(function() {
    'use strict';

    // Configuration
    const config = window.chatbotConfig || {
        apiUrl: `${window.location.protocol}//${window.location.hostname}:8081/api`,
        position: 'bottom-right',
        primaryColor: '#2196F3'
    };

    // Widget state
    // Only show the chatbot icon; do not render the popup by default.

    function createWidget() {
        const edgeOffset = '20px';
        const horizontalOffset = '20px';
        const widgetHTML = `
            <div id="chatbot-widget-container" style="
                position: fixed;
                bottom: ${edgeOffset};
                right: ${horizontalOffset};
                z-index: 2147483647;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                pointer-events: none;
            ">
                <button id="chatbot-toggle-btn" aria-label="Mở chatbot" style="
                    width: 60px;
                    height: 60px;
                    border-radius: 50%;
                    background: ${config.primaryColor};
                    border: 3px solid #ffffff;
                    box-shadow: 0 8px 24px rgba(33, 150, 243, 0.45), 0 0 0 4px rgba(33, 150, 243, 0.18);
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: transform 0.2s;
                    pointer-events: auto;
                ">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                    </svg>
                </button>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', widgetHTML);
        attachEventListeners();
    }

    function attachEventListeners() {
        const toggleBtn = document.getElementById('chatbot-toggle-btn');
        if (!toggleBtn) return;

        toggleBtn.addEventListener('click', () => {
            const chatUrl = `${window.location.protocol}//${window.location.hostname}:8081/api/chat/chat.html`;
            window.open(chatUrl, '_blank');
        });

        toggleBtn.addEventListener('mouseenter', () => {
            toggleBtn.style.transform = 'scale(1.1)';
        });
        toggleBtn.addEventListener('mouseleave', () => {
            toggleBtn.style.transform = 'scale(1)';
        });
    }

    // Initialize widget when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', createWidget);
    } else {
        createWidget();
    }
})();
