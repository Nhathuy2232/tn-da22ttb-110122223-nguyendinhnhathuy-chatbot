package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.entity.CourseGradeSummary;
import com.kltn.chatbot.model.entity.GradeItem;
import com.kltn.chatbot.model.entity.StudentGrade;
import com.kltn.chatbot.repository.CourseGradeSummaryRepository;
import com.kltn.chatbot.repository.GradeItemRepository;
import com.kltn.chatbot.repository.StudentGradeRepository;
import com.kltn.chatbot.service.GradeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản lý điểm số
 */
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Tag(name = "Grade Management", description = "API quản lý điểm số")
public class GradeController {
    
    private final GradeManagementService gradeManagementService;
    private final StudentGradeRepository studentGradeRepository;
    private final GradeItemRepository gradeItemRepository;
    private final CourseGradeSummaryRepository courseSummaryRepository;
    
    @Operation(summary = "Đồng bộ grade items của khóa học")
    @PostMapping("/sync/items/{courseId}")
    public ResponseEntity<Map<String, String>> syncGradeItems(@PathVariable Long courseId) {
        try {
            gradeManagementService.syncGradeItemsForCourse(courseId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đồng bộ grade items thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "Đồng bộ điểm của sinh viên")
    @PostMapping("/sync/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, String>> syncStudentGrades(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            gradeManagementService.syncStudentGrades(studentId, courseId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đồng bộ điểm thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "Lấy chi tiết điểm của sinh viên trong khóa học")
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getDetailedGradeReport(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        Map<String, Object> report = gradeManagementService.getDetailedGradeReport(studentId, courseId);
        return ResponseEntity.ok(report);
    }
    
    @Operation(summary = "Lấy tất cả điểm của sinh viên")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentGrade>> getStudentGrades(@PathVariable Long studentId) {
        List<StudentGrade> grades = studentGradeRepository.findByStudentId(studentId);
        return ResponseEntity.ok(grades);
    }
    
    @Operation(summary = "Lấy tổng hợp điểm của sinh viên")
    @GetMapping("/summary/student/{studentId}")
    public ResponseEntity<List<CourseGradeSummary>> getStudentGradeSummaries(@PathVariable Long studentId) {
        List<CourseGradeSummary> summaries = courseSummaryRepository.findByStudentId(studentId);
        return ResponseEntity.ok(summaries);
    }
    
    @Operation(summary = "Lấy grade items của khóa học")
    @GetMapping("/items/course/{courseId}")
    public ResponseEntity<List<GradeItem>> getGradeItemsByCourse(@PathVariable Long courseId) {
        List<GradeItem> items = gradeItemRepository.findByCourseId(courseId);
        return ResponseEntity.ok(items);
    }
    
    @Operation(summary = "Lấy điểm trung bình của sinh viên")
    @GetMapping("/average/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentAverageGrade(@PathVariable Long studentId) {
        var average = studentGradeRepository.getAverageGradeByStudentId(studentId);
        var overallAverage = courseSummaryRepository.getOverallAverageByStudent(studentId);
        
        return ResponseEntity.ok(Map.of(
            "studentId", studentId,
            "averageGrade", average != null ? average : 0,
            "overallAverage", overallAverage != null ? overallAverage : 0
        ));
    }
    
    @Operation(summary = "Lấy danh sách sinh viên có completion thấp")
    @GetMapping("/low-completion/course/{courseId}")
    public ResponseEntity<List<CourseGradeSummary>> getLowCompletionStudents(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "70.0") Double threshold) {
        List<CourseGradeSummary> students = courseSummaryRepository
                .findStudentsWithLowCompletion(courseId, threshold);
        return ResponseEntity.ok(students);
    }
    
    @Operation(summary = "Lấy xếp hạng sinh viên trong khóa học")
    @GetMapping("/ranking/course/{courseId}")
    public ResponseEntity<List<CourseGradeSummary>> getCourseRanking(@PathVariable Long courseId) {
        List<CourseGradeSummary> ranking = courseSummaryRepository.findByCourseIdOrderByGrade(courseId);
        return ResponseEntity.ok(ranking);
    }
}
