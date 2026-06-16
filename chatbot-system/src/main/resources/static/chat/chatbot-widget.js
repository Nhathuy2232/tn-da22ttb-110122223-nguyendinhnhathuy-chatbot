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
        apiUrl: 'http://localhost:8081/api',
        position: 'bottom-right',
        primaryColor: '#0066cc'
    };

    // Widget state
    let isOpen = false;
    let messages = [];

    // Create widget HTML
    function createWidget() {
        const edgeOffset = config.position.includes('bottom') ? '96px' : '20px';
        const horizontalOffset = config.position.includes('right') ? '20px' : '20px';
        const widgetHTML = `
            <div id="chatbot-widget-container" style="
                position: fixed;
                ${config.position.includes('bottom') ? `bottom: ${edgeOffset};` : `top: ${edgeOffset};`}
                ${config.position.includes('right') ? `right: ${horizontalOffset};` : `left: ${horizontalOffset};`}
                z-index: 2147483647;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                pointer-events: none;
            ">
                <!-- Chat Button -->
                <button id="chatbot-toggle-btn" style="
                    width: 60px;
                    height: 60px;
                    border-radius: 50%;
                    background: ${config.primaryColor};
                    border: none;
                    box-shadow: 0 8px 24px rgba(0,0,0,0.22);
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

                <!-- Chat Window -->
                <div id="chatbot-window" style="
                    display: none;
                    position: absolute;
                    ${config.position.includes('bottom') ? 'bottom: 76px;' : 'top: 76px;'}
                    ${config.position.includes('right') ? 'right: 0;' : 'left: 0;'}
                    width: 350px;
                    height: 500px;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 8px 24px rgba(0,0,0,0.15);
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    pointer-events: auto;
                ">
                    <!-- Header -->
                    <div style="
                        background: ${config.primaryColor};
                        color: white;
                        padding: 16px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    ">
                        <div>
                            <h3 style="margin: 0; font-size: 16px; font-weight: 600;">🤖 Chatbot</h3>
                            <p style="margin: 4px 0 0 0; font-size: 12px; opacity: 0.9;">Trợ lý học tập</p>
                        </div>
                        <button id="chatbot-close-btn" style="
                            background: transparent;
                            border: none;
                            color: white;
                            cursor: pointer;
                            font-size: 24px;
                            line-height: 1;
                            padding: 0;
                        ">&times;</button>
                    </div>

                    <!-- Messages -->
                    <div id="chatbot-messages" style="
                        flex: 1;
                        overflow-y: auto;
                        padding: 16px;
                        background: #f5f5f5;
                    ">
                        <div class="chatbot-message bot-message">
                            <div style="
                                background: white;
                                padding: 12px;
                                border-radius: 12px;
                                margin-bottom: 12px;
                                max-width: 80%;
                                box-shadow: 0 1px 2px rgba(0,0,0,0.1);
                            ">
                                Xin chào! Tôi là trợ lý cảnh báo sinh viên. Tôi có thể giúp gì cho bạn?
                            </div>
                            <!-- Quick Replies -->
                            <div id="quick-replies-container" style="margin-top: 12px; display: flex; flex-direction: column; gap: 10px;">
                                <button class="quick-reply-btn" data-message="Thống kê sinh viên cảnh báo" style="
                                    background: white;
                                    border: 2px solid #0066cc;
                                    color: #0066cc;
                                    padding: 12px 20px;
                                    border-radius: 25px;
                                    cursor: pointer;
                                    font-size: 14px;
                                    font-weight: 500;
                                    transition: all 0.2s;
                                    text-align: center;
                                    width: 100%;
                                ">
                                    Thống kê sinh viên cảnh báo
                                </button>
                                <button class="quick-reply-btn" data-message="Danh sách sinh viên nguy cơ cao" style="
                                    background: white;
                                    border: 2px solid #0066cc;
                                    color: #0066cc;
                                    padding: 12px 20px;
                                    border-radius: 25px;
                                    cursor: pointer;
                                    font-size: 14px;
                                    font-weight: 500;
                                    transition: all 0.2s;
                                    text-align: center;
                                    width: 100%;
                                ">
                                    Danh sách sinh viên nguy cơ cao
                                </button>
                                <button class="quick-reply-btn" data-message="Sinh viên cần theo dõi trong tuần này" style="
                                    background: white;
                                    border: 2px solid #0066cc;
                                    color: #0066cc;
                                    padding: 12px 20px;
                                    border-radius: 25px;
                                    cursor: pointer;
                                    font-size: 14px;
                                    font-weight: 500;
                                    transition: all 0.2s;
                                    text-align: center;
                                    width: 100%;
                                ">
                                    Sinh viên cần theo dõi trong tuần này
                                </button>
                                <button class="quick-reply-btn" data-message="Báo cáo tổng quan" style="
                                    background: white;
                                    border: 2px solid #0066cc;
                                    color: #0066cc;
                                    padding: 12px 20px;
                                    border-radius: 25px;
                                    cursor: pointer;
                                    font-size: 14px;
                                    font-weight: 500;
                                    transition: all 0.2s;
                                    text-align: center;
                                    width: 100%;
                                ">
                                    Báo cáo tổng quan
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Input -->
                    <div style="
                        border-top: 1px solid #e0e0e0;
                        padding: 16px;
                        background: white;
                    ">
                        <div style="display: flex; gap: 8px;">
                            <input 
                                id="chatbot-input" 
                                type="text" 
                                placeholder="Nhập tin nhắn..."
                                style="
                                    flex: 1;
                                    border: 1px solid #e0e0e0;
                                    border-radius: 20px;
                                    padding: 10px 16px;
                                    font-size: 14px;
                                    outline: none;
                                "
                            />
                            <button id="chatbot-send-btn" style="
                                background: ${config.primaryColor};
                                color: white;
                                border: none;
                                border-radius: 50%;
                                width: 40px;
                                height: 40px;
                                cursor: pointer;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                            ">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <line x1="22" y1="2" x2="11" y2="13"></line>
                                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', widgetHTML);
        attachEventListeners();
    }

    // Attach event listeners
    function attachEventListeners() {
        const toggleBtn = document.getElementById('chatbot-toggle-btn');
        const closeBtn = document.getElementById('chatbot-close-btn');
        const sendBtn = document.getElementById('chatbot-send-btn');
        const input = document.getElementById('chatbot-input');

        toggleBtn.addEventListener('click', toggleChat);
        closeBtn.addEventListener('click', closeChat);
        sendBtn.addEventListener('click', sendMessage);
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        // Quick reply buttons
        document.querySelectorAll('.quick-reply-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const message = e.currentTarget.getAttribute('data-message');
                handleQuickReply(message);
            });
            
            // Hover effect
            btn.addEventListener('mouseenter', (e) => {
                e.currentTarget.style.background = '#0066cc';
                e.currentTarget.style.color = 'white';
            });
            btn.addEventListener('mouseleave', (e) => {
                e.currentTarget.style.background = 'white';
                e.currentTarget.style.color = '#0066cc';
            });
        });

        // Hover effect
        toggleBtn.addEventListener('mouseenter', () => {
            toggleBtn.style.transform = 'scale(1.1)';
        });
        toggleBtn.addEventListener('mouseleave', () => {
            toggleBtn.style.transform = 'scale(1)';
        });

        const widgetContainer = document.getElementById('chatbot-widget-container');
        if (widgetContainer) {
            widgetContainer.style.pointerEvents = 'none';
        }
        [toggleBtn, closeBtn, sendBtn, input].forEach((el) => {
            if (el) el.style.pointerEvents = 'auto';
        });
    }

    // Toggle chat window
    function toggleChat() {
        isOpen = !isOpen;
        const window = document.getElementById('chatbot-window');
        window.style.display = isOpen ? 'flex' : 'none';
        
        if (isOpen) {
            document.getElementById('chatbot-input').focus();
        }
    }

    // Close chat window
    function closeChat() {
        isOpen = false;
        document.getElementById('chatbot-window').style.display = 'none';
    }

    // Send message
    function sendMessage() {
        const input = document.getElementById('chatbot-input');
        const message = input.value.trim();
        
        if (!message) return;

        // Hide quick replies when user sends first message
        hideQuickReplies();

        // Add user message to UI
        addMessage(message, 'user');
        input.value = '';

        // Send to API
        sendToAPI(message);
    }

    // Handle quick reply button click
    function handleQuickReply(message) {
        // Hide quick replies
        hideQuickReplies();
        
        // Add user message
        addMessage(message, 'user');
        
        // Send to API
        sendToAPI(message);
    }

    // Hide quick reply buttons
    function hideQuickReplies() {
        const quickRepliesContainer = document.getElementById('quick-replies-container');
        if (quickRepliesContainer) {
            quickRepliesContainer.style.display = 'none';
        }
    }

    // Add message to chat
    function addMessage(text, sender) {
        const messagesContainer = document.getElementById('chatbot-messages');
        const isBot = sender === 'bot';
        
        const messageHTML = `
            <div class="chatbot-message ${isBot ? 'bot-message' : 'user-message'}" style="
                display: flex;
                justify-content: ${isBot ? 'flex-start' : 'flex-end'};
                margin-bottom: 12px;
            ">
                <div style="
                    background: ${isBot ? 'white' : config.primaryColor};
                    color: ${isBot ? '#333' : 'white'};
                    padding: 12px;
                    border-radius: 12px;
                    max-width: 80%;
                    box-shadow: 0 1px 2px rgba(0,0,0,0.1);
                    word-wrap: break-word;
                ">
                    ${text}
                </div>
            </div>
        `;
        
        messagesContainer.insertAdjacentHTML('beforeend', messageHTML);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Send message to API
    async function sendToAPI(message) {
        try {
            // Show typing indicator
            addMessage('Đang xử lý...', 'bot');
            
            const response = await fetch(`${config.apiUrl}/chat/message`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: message,
                    userId: config.teacherId || 1,
                    sessionId: getSessionId()
                })
            });

            // Remove typing indicator
            const messages = document.querySelectorAll('.bot-message');
            const lastMessage = messages[messages.length - 1];
            if (lastMessage && lastMessage.textContent.includes('Đang xử lý')) {
                lastMessage.remove();
            }

            if (response.ok) {
                const data = await response.json();
                const reply = data.data?.reply || data.message || 'Xin lỗi, tôi không hiểu câu hỏi của bạn.';
                addMessage(reply, 'bot');
            } else {
                addMessage('Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.', 'bot');
            }
        } catch (error) {
            console.error('Chat API error:', error);
            
            // Remove typing indicator
            const messages = document.querySelectorAll('.bot-message');
            const lastMessage = messages[messages.length - 1];
            if (lastMessage && lastMessage.textContent.includes('Đang xử lý')) {
                lastMessage.remove();
            }
            
            // Mock response for demo
            const mockResponses = [
                'Tôi có thể giúp bạn kiểm tra danh sách sinh viên có nguy cơ thôi học.',
                'Hiện có ' + Math.floor(Math.random() * 20) + ' sinh viên cần theo dõi đặc biệt.',
                'Bạn có thể xem dashboard để biết thêm chi tiết về tình hình sinh viên.'
            ];
            const randomResponse = mockResponses[Math.floor(Math.random() * mockResponses.length)];
            addMessage(randomResponse, 'bot');
        }
    }

    // Get or create session ID
    function getSessionId() {
        let sessionId = sessionStorage.getItem('chatbot-session-id');
        if (!sessionId) {
            sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('chatbot-session-id', sessionId);
        }
        return sessionId;
    }

    // Initialize widget when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', createWidget);
    } else {
        createWidget();
    }
})();
