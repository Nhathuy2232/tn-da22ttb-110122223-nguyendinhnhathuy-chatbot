package com.kltn.chatbot.service;

import com.kltn.chatbot.exception.ResourceNotFoundException;
import com.kltn.chatbot.model.dto.StudentRiskDTO;
import com.kltn.chatbot.model.entity.Student;
import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.repository.StudentRepository;
import com.kltn.chatbot.repository.WarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý business logic cho Student
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final WarningRepository warningRepository;

    /**
     * Lấy tất cả sinh viên
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Lấy thông tin sinh viên theo ID
     */
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    /**
     * Lấy thông tin risk của sinh viên
     */
    public StudentRiskDTO getStudentRisk(Long studentId) {
        Student student = getStudentById(studentId);
        
        // Get latest warning
        List<Warning> warnings = warningRepository.findByStudentAndCourse(student.getMoodleUserId(), null);
        
        if (warnings.isEmpty()) {
            return StudentRiskDTO.builder()
                    .studentId(student.getId())
                    .studentName(student.getFullName())
                    .studentCode(student.getStudentCode())
                    .email(student.getEmail())
                    .riskLevel(null)
                    .riskLevelDisplay("Chưa có dữ liệu")
                    .reasons(Arrays.asList("Chưa có dữ liệu cảnh báo"))
                    .build();
        }

        Warning latestWarning = warnings.get(0);
        
        return StudentRiskDTO.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentCode(student.getStudentCode())
                .email(student.getEmail())
                .courseId(latestWarning.getCourseId())
                .courseName("Course " + latestWarning.getCourseId()) // Temporary
                .riskLevel(latestWarning.getRiskLevel())
                .riskLevelDisplay(latestWarning.getRiskLevel().getDisplayName())
                .gradeAverage(latestWarning.getGradeAverage())
                .attendanceRate(latestWarning.getAttendanceRate())
                .completionRate(latestWarning.getCompletionRate())
                .lastAccessDays(latestWarning.getLastAccessDays())
                .reasons(Arrays.asList(latestWarning.getReasons().split("; ")))
                .detectedAt(latestWarning.getDetectedAt())
                .build();
    }

    /**
     * Tìm sinh viên theo tên
     */
    public List<Student> searchStudentsByName(String name) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getFullName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }
}
