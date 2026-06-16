package com.kltn.chatbot.service;

import com.kltn.chatbot.model.entity.User;
import com.kltn.chatbot.model.entity.User.UserType;
import com.kltn.chatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service quản lý người dùng (Giáo viên và Sinh viên)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    
    private final UserRepository userRepository;
    private final MoodleApiService moodleApiService;
    
    /**
     * Đồng bộ tất cả người dùng từ Moodle
     */
    @Transactional
    public void syncAllUsersFromMoodle() {
        log.info("Bắt đầu đồng bộ người dùng từ Moodle...");
        
        try {
            // Lấy danh sách tất cả users từ Moodle
            List<Map<String, Object>> moodleUsers = moodleApiService.getAllUsers();
            
            int created = 0;
            int updated = 0;
            
            for (Map<String, Object> moodleUser : moodleUsers) {
                User user = syncUserFromMoodle(moodleUser);
                if (user != null) {
                    if (user.getCreatedAt().equals(user.getUpdatedAt())) {
                        created++;
                    } else {
                        updated++;
                    }
                }
            }
            
            log.info("Đồng bộ người dùng hoàn tất. Tạo mới: {}, Cập nhật: {}", created, updated);
        } catch (Exception e) {
            log.error("Lỗi đồng bộ người dùng từ Moodle: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể đồng bộ người dùng từ Moodle", e);
        }
    }
    
    /**
     * Đồng bộ một user từ Moodle
     */
    @Transactional
    public User syncUserFromMoodle(Map<String, Object> moodleUser) {
        try {
            Long moodleUserId = getLong(moodleUser, "id");
            String username = (String) moodleUser.get("username");
            String email = (String) moodleUser.get("email");
            String firstname = (String) moodleUser.get("firstname");
            String lastname = (String) moodleUser.get("lastname");
            
            // Tìm hoặc tạo user
            User user = userRepository.findByMoodleUserId(moodleUserId)
                    .orElse(new User());
            
            user.setMoodleUserId(moodleUserId);
            user.setUsername(username);
            user.setEmail(email);
            user.setFullName(firstname + " " + lastname);
            
            // Xác định user type dựa trên roles
            UserType userType = determineUserType(moodleUser);
            user.setUserType(userType);
            
            // Nếu là sinh viên, set student code
            if (userType == UserType.STUDENT) {
                String idnumber = (String) moodleUser.get("idnumber");
                if (idnumber != null && !idnumber.isEmpty()) {
                    user.setStudentCode(idnumber);
                }
            }
            
            // Thông tin bổ sung
            user.setDepartment((String) moodleUser.get("department"));
            user.setInstitution((String) moodleUser.get("institution"));
            user.setCity((String) moodleUser.get("city"));
            user.setCountry((String) moodleUser.get("country"));
            user.setLastSyncAt(LocalDateTime.now());
            
            user = userRepository.save(user);
            log.debug("Đã đồng bộ user: {} ({})", user.getFullName(), user.getUserType());
            
            return user;
        } catch (Exception e) {
            log.error("Lỗi đồng bộ user từ Moodle: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Xác định user type từ Moodle data
     */
    private UserType determineUserType(Map<String, Object> moodleUser) {
        // Kiểm tra roles trong custom fields hoặc username prefix
        String username = (String) moodleUser.get("username");
        
        if (username != null) {
            if (username.startsWith("sv.")) {
                return UserType.STUDENT;
            } else if (username.startsWith("gv.")) {
                return UserType.TEACHER;
            } else if (username.equals("admin")) {
                return UserType.ADMIN;
            }
        }
        
        // Mặc định là student
        return UserType.STUDENT;
    }
    
    /**
     * Lấy danh sách sinh viên
     */
    public List<User> getAllStudents() {
        return userRepository.findActiveUsersByType(UserType.STUDENT);
    }
    
    /**
     * Lấy danh sách giáo viên
     */
    public List<User> getAllTeachers() {
        return userRepository.findActiveUsersByType(UserType.TEACHER);
    }
    
    /**
     * Tìm sinh viên theo từ khóa
     */
    public List<User> searchStudents(String keyword) {
        return userRepository.searchStudents(keyword);
    }
    
    /**
     * Lấy user theo Moodle ID
     */
    public User getUserByMoodleId(Long moodleUserId) {
        return userRepository.findByMoodleUserId(moodleUserId)
                .orElse(null);
    }
    
    /**
     * Lấy user theo student code
     */
    public User getUserByStudentCode(String studentCode) {
        return userRepository.findByStudentCode(studentCode)
                .orElse(null);
    }
    
    /**
     * Thống kê số lượng users
     */
    public Map<String, Long> getUserStatistics() {
        return Map.of(
            "totalStudents", userRepository.countActiveUsersByType(UserType.STUDENT),
            "totalTeachers", userRepository.countActiveUsersByType(UserType.TEACHER),
            "totalUsers", (long) userRepository.findAll().size()
        );
    }
    
    /**
     * Helper method để lấy Long từ Map
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }
}
