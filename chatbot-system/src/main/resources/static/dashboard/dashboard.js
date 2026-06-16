/**
 * Dashboard JavaScript
 * Quản lý hiển thị dashboard cảnh báo sinh viên
 * 
 * @author Nguyễn Đình Nhật Huy
 */

// ==================== CONFIGURATION ====================
const API_BASE = 'http://localhost:8081/api';
const REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes
let riskChart = null;
let trendChart = null;
let allStudents = []; // Cache for search

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Dashboard initialized');
    
    // Load initial data
    loadDashboardData();
    loadRedWarnings();
    
    // Setup event listeners
    setupEventListeners();
    
    // Auto-refresh every 5 minutes
    setInterval(() => {
        loadDashboardData();
        loadRedWarnings();
    }, REFRESH_INTERVAL);
});

// ==================== EVENT LISTENERS ====================
function setupEventListeners() {
    // Refresh button
    document.getElementById('refresh-btn').addEventListener('click', () => {
        console.log('Manual refresh triggered');
        loadDashboardData();
        loadRedWarnings();
    });
    
    // Export button
    document.getElementById('export-btn').addEventListener('click', exportToCSV);
    
    // Search input
    document.getElementById('search-input').addEventListener('input', (e) => {
        filterStudents(e.target.value);
    });
}

// ==================== API CALLS ====================

/**
 * Load dashboard statistics
 */
async function loadDashboardData() {
    try {
        const response = await fetch(`${API_BASE}/warnings/dashboard`);
        const data = await response.json();
        
        if (data.success) {
            updateSummaryCards(data.data);
            updateCharts(data.data);
        } else {
            console.error('Failed to load dashboard:', data.message);
            showError('Không thể tải dữ liệu dashboard');
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('Lỗi kết nối API');
    }
}

/**
 * Load RED warnings (students at high risk)
 */
async function loadRedWarnings() {
    try {
        const response = await fetch(`${API_BASE}/warnings/red`);
        const data = await response.json();
        
        if (data.success) {
            allStudents = data.data; // Cache for search
            displayStudents(data.data);
        } else {
            console.error('Failed to load warnings:', data.message);
            showError('Không thể tải danh sách sinh viên');
        }
    } catch (error) {
        console.error('Error loading warnings:', error);
        showError('Lỗi kết nối API');
    }
}

/**
 * Acknowledge a warning
 */
async function acknowledgeWarning(warningId) {
    try {
        const lecturerId = 1; // TODO: Get from session/login
        
        const response = await fetch(
            `${API_BASE}/warnings/${warningId}/acknowledge?lecturerId=${lecturerId}`,
            { method: 'PUT' }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showSuccess('Đã xác nhận cảnh báo');
            loadRedWarnings(); // Reload list
        } else {
            showError('Không thể xác nhận: ' + data.message);
        }
    } catch (error) {
        console.error('Error acknowledging warning:', error);
        showError('Lỗi kết nối API');
    }
}

// ==================== UI UPDATES ====================

/**
 * Update summary cards with statistics
 */
function updateSummaryCards(data) {
    document.getElementById('green-count').textContent = data.greenCount || 0;
    document.getElementById('yellow-count').textContent = data.yellowCount || 0;
    document.getElementById('red-count').textContent = data.redCount || 0;
    document.getElementById('total-count').textContent = data.totalCount || 0;
}

/**
 * Update charts with data
 */
function updateCharts(data) {
    updateRiskChart(data);
    updateTrendChart(data);
}

/**
 * Update risk distribution pie chart
 */
function updateRiskChart(data) {
    const ctx = document.getElementById('risk-chart').getContext('2d');
    
    // Destroy existing chart
    if (riskChart) {
        riskChart.destroy();
    }
    
    riskChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['An toàn (GREEN)', 'Cảnh báo (YELLOW)', 'Nguy cơ cao (RED)'],
            datasets: [{
                data: [
                    data.greenCount || 0,
                    data.yellowCount || 0,
                    data.redCount || 0
                ],
                backgroundColor: [
                    '#4caf50',
                    '#ff9800',
                    '#f44336'
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Update trend line chart
 * Note: This is mock data for now. In production, fetch historical data from API
 */
function updateTrendChart(data) {
    const ctx = document.getElementById('trend-chart').getContext('2d');
    
    // Destroy existing chart
    if (trendChart) {
        trendChart.destroy();
    }
    
    // Mock trend data (last 7 days)
    const labels = [];
    const greenData = [];
    const yellowData = [];
    const redData = [];
    
    for (let i = 6; i >= 0; i--) {
        const date = new Date();
        date.setDate(date.getDate() - i);
        labels.push(date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }));
        
        // Mock data with slight variation
        greenData.push(Math.max(0, (data.greenCount || 0) + Math.floor(Math.random() * 10 - 5)));
        yellowData.push(Math.max(0, (data.yellowCount || 0) + Math.floor(Math.random() * 6 - 3)));
        redData.push(Math.max(0, (data.redCount || 0) + Math.floor(Math.random() * 4 - 2)));
    }
    
    trendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'An toàn',
                    data: greenData,
                    borderColor: '#4caf50',
                    backgroundColor: 'rgba(76, 175, 80, 0.1)',
                    tension: 0.4,
                    fill: true
                },
                {
                    label: 'Cảnh báo',
                    data: yellowData,
                    borderColor: '#ff9800',
                    backgroundColor: 'rgba(255, 152, 0, 0.1)',
                    tension: 0.4,
                    fill: true
                },
                {
                    label: 'Nguy cơ cao',
                    data: redData,
                    borderColor: '#f44336',
                    backgroundColor: 'rgba(244, 67, 54, 0.1)',
                    tension: 0.4,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1
                    }
                }
            }
        }
    });
}

/**
 * Display students in table
 */
function displayStudents(warnings) {
    const tbody = document.getElementById('students-tbody');
    
    if (!warnings || warnings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading">Không có sinh viên nguy cơ cao</td></tr>';
        return;
    }
    
    tbody.innerHTML = warnings.map((warning, index) => {
        const student = warning.student || {};
        const riskClass = `risk-${warning.riskLevel.toLowerCase()}`;
        const riskText = getRiskLevelText(warning.riskLevel);
        
        return `
            <tr>
                <td>${index + 1}</td>
                <td>${student.studentCode || 'N/A'}</td>
                <td>${student.fullName || 'N/A'}</td>
                <td>${student.email || 'N/A'}</td>
                <td>${warning.gradeAverage ? warning.gradeAverage.toFixed(2) : 'N/A'}</td>
                <td>${warning.attendanceRate ? warning.attendanceRate.toFixed(1) + '%' : 'N/A'}</td>
                <td><span class="risk-badge ${riskClass}">${riskText}</span></td>
                <td>
                    <button class="action-btn" onclick="viewDetails(${warning.id})">
                        Chi tiết
                    </button>
                    ${!warning.acknowledged ? 
                        `<button class="action-btn" onclick="acknowledgeWarning(${warning.id})" style="margin-left: 5px;">
                            Xác nhận
                        </button>` : 
                        '<span style="color: #4caf50; font-size: 12px;">✓ Đã xác nhận</span>'
                    }
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * Filter students by search query
 */
function filterStudents(query) {
    if (!query.trim()) {
        displayStudents(allStudents);
        return;
    }
    
    const lowerQuery = query.toLowerCase();
    const filtered = allStudents.filter(warning => {
        const student = warning.student || {};
        return (
            (student.fullName && student.fullName.toLowerCase().includes(lowerQuery)) ||
            (student.studentCode && student.studentCode.toLowerCase().includes(lowerQuery)) ||
            (student.email && student.email.toLowerCase().includes(lowerQuery))
        );
    });
    
    displayStudents(filtered);
}

/**
 * View student details (open modal or navigate)
 */
function viewDetails(warningId) {
    const warning = allStudents.find(w => w.id === warningId);
    if (!warning) return;
    
    const student = warning.student || {};
    const reasons = warning.reasons ? warning.reasons.split(';').join('\n• ') : 'Không có';
    
    alert(`
📋 CHI TIẾT CẢNH BÁO

👤 Sinh viên: ${student.fullName || 'N/A'}
🆔 Mã SV: ${student.studentCode || 'N/A'}
📧 Email: ${student.email || 'N/A'}

📊 Thông tin học tập:
• Điểm TB: ${warning.gradeAverage ? warning.gradeAverage.toFixed(2) : 'N/A'}
• Chuyên cần: ${warning.attendanceRate ? warning.attendanceRate.toFixed(1) + '%' : 'N/A'}
• Mức độ: ${getRiskLevelText(warning.riskLevel)}

⚠️ Lý do cảnh báo:
• ${reasons}

🕐 Phát hiện: ${new Date(warning.detectedAt).toLocaleString('vi-VN')}
    `.trim());
}

// ==================== EXPORT ====================

/**
 * Export student list to CSV
 */
function exportToCSV() {
    if (!allStudents || allStudents.length === 0) {
        showError('Không có dữ liệu để xuất');
        return;
    }
    
    // CSV header
    let csv = 'STT,Mã SV,Họ tên,Email,Điểm TB,Chuyên cần,Mức độ,Lý do,Ngày phát hiện\n';
    
    // CSV rows
    allStudents.forEach((warning, index) => {
        const student = warning.student || {};
        csv += [
            index + 1,
            student.studentCode || 'N/A',
            student.fullName || 'N/A',
            student.email || 'N/A',
            warning.gradeAverage ? warning.gradeAverage.toFixed(2) : 'N/A',
            warning.attendanceRate ? warning.attendanceRate.toFixed(1) + '%' : 'N/A',
            getRiskLevelText(warning.riskLevel),
            `"${warning.reasons || 'N/A'}"`,
            new Date(warning.detectedAt).toLocaleDateString('vi-VN')
        ].join(',') + '\n';
    });
    
    // Download
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `canh-bao-sinh-vien-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showSuccess('Đã xuất báo cáo CSV');
}

// ==================== UTILITIES ====================

/**
 * Get risk level text in Vietnamese
 */
function getRiskLevelText(riskLevel) {
    const map = {
        'GREEN': 'An toàn',
        'YELLOW': 'Cảnh báo',
        'RED': 'Nguy cơ cao'
    };
    return map[riskLevel] || riskLevel;
}

/**
 * Show success message
 */
function showSuccess(message) {
    // Simple alert for now. Can be replaced with toast notification
    console.log('✓ Success:', message);
    alert('✓ ' + message);
}

/**
 * Show error message
 */
function showError(message) {
    // Simple alert for now. Can be replaced with toast notification
    console.error('✗ Error:', message);
    alert('✗ ' + message);
}

// ==================== GLOBAL FUNCTIONS ====================
// Make functions available globally for onclick handlers
window.acknowledgeWarning = acknowledgeWarning;
window.viewDetails = viewDetails;
