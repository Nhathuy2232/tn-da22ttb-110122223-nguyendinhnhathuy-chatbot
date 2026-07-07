// Configuration
const API_BASE = (window.chatbotConfig && window.chatbotConfig.apiUrl)
    ? window.chatbotConfig.apiUrl
    : `${window.location.protocol}//${window.location.hostname}:8081/api`;
let sessionId = 'session-' + Date.now();
let lecturerId = 1; // TODO: Get from authentication

// DOM Elements
let chatWidget, chatMessages, chatInput, sendBtn, minimizeBtn, toggleBtn, quickBtns;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Get DOM elements
    chatWidget = document.getElementById('chat-widget');
    chatMessages = document.getElementById('chat-messages');
    chatInput = document.getElementById('chat-input');
    sendBtn = document.getElementById('send-btn');
    minimizeBtn = document.getElementById('minimize-btn');
    toggleBtn = document.getElementById('chat-toggle-btn');
    quickBtns = document.querySelectorAll('.quick-btn');

    // Event listeners
    sendBtn.addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });
    
    minimizeBtn.addEventListener('click', toggleChat);
    toggleBtn.addEventListener('click', toggleChat);
    
    quickBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            chatInput.value = btn.dataset.message;
            sendMessage();
        });
    });

    // Welcome message
    setTimeout(() => {
        appendMessage('bot', 'Xin chào! Tôi là chatbot hỗ trợ giảng viên. 👋\n\nBạn có thể hỏi tôi về:\n• Danh sách sinh viên nguy cơ\n• Tình trạng sinh viên cụ thể\n• Thống kê tổng quan\n\nHãy thử hỏi tôi nhé! 😊');
    }, 500);
});

// Toggle chat widget
function toggleChat() {
    chatWidget.classList.toggle('minimized');
    toggleBtn.classList.toggle('show');
    
    if (!chatWidget.classList.contains('minimized')) {
        chatInput.focus();
    }
}

// Send message
async function sendMessage() {
    const message = chatInput.value.trim();
    
    if (!message) return;
    
    // Display user message
    appendMessage('user', message);
    chatInput.value = '';
    
    // Show typing indicator
    showTypingIndicator();
    
    try {
        // Call API
        const response = await fetch(`${API_BASE}/chat/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                lecturerId: lecturerId,
                sessionId: sessionId
            })
        });
        
        const data = await response.json();
        
        // Remove typing indicator
        removeTypingIndicator();
        
        if (data.success) {
            // Display bot response
            appendMessage('bot', data.data.reply);
            
            // Display rich data if available
            if (data.data.richData) {
                displayRichData(data.data.richData);
            }
        } else {
            appendMessage('bot', '❌ Lỗi: ' + data.message);
        }
    } catch (error) {
        console.error('Error:', error);
        removeTypingIndicator();
        appendMessage('bot', '❌ Lỗi kết nối. Vui lòng kiểm tra:\n• Server đang chạy?\n• URL API đúng chưa?\n• CORS đã được cấu hình?');
    }
}

// Append message to chat
function appendMessage(role, content) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;
    
    // Format message (support markdown-like syntax)
    content = formatMessage(content);
    messageDiv.innerHTML = content;
    
    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

// Format message (simple markdown)
function formatMessage(text) {
    // Bold: **text**
    text = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // Line breaks
    text = text.replace(/\n/g, '<br>');
    
    return text;
}

// Show typing indicator
function showTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'typing-indicator';
    indicator.id = 'typing-indicator';
    indicator.innerHTML = '<span></span><span></span><span></span>';
    chatMessages.appendChild(indicator);
    scrollToBottom();
}

// Remove typing indicator
function removeTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) {
        indicator.remove();
    }
}

// Display rich data (table, list, chart)
function displayRichData(richData) {
    if (richData.type === 'table') {
        displayTable(richData.data);
    } else if (richData.type === 'list') {
        displayList(richData.data);
    }
}

// Display table
function displayTable(data) {
    if (!data || data.length === 0) return;
    
    let tableHTML = '<table style="width:100%; border-collapse: collapse; margin-top: 10px;">';
    
    // Header
    tableHTML += '<thead><tr style="background: #f0f2f5;">';
    Object.keys(data[0]).forEach(key => {
        tableHTML += `<th style="padding: 8px; border: 1px solid #ddd; text-align: left;">${key}</th>`;
    });
    tableHTML += '</tr></thead>';
    
    // Body
    tableHTML += '<tbody>';
    data.forEach(row => {
        tableHTML += '<tr>';
        Object.values(row).forEach(value => {
            tableHTML += `<td style="padding: 8px; border: 1px solid #ddd;">${value}</td>`;
        });
        tableHTML += '</tr>';
    });
    tableHTML += '</tbody></table>';
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message bot';
    messageDiv.innerHTML = tableHTML;
    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

// Display list
function displayList(data) {
    if (!data || data.length === 0) return;
    
    let listHTML = '<ul style="margin: 10px 0; padding-left: 20px;">';
    data.forEach(item => {
        listHTML += `<li style="margin: 5px 0;">${item}</li>`;
    });
    listHTML += '</ul>';
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message bot';
    messageDiv.innerHTML = listHTML;
    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

// Scroll to bottom
function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Auto-focus input when widget is visible
setInterval(() => {
    if (!chatWidget.classList.contains('minimized') && document.activeElement !== chatInput) {
        // Don't auto-focus to avoid interrupting user
    }
}, 1000);
