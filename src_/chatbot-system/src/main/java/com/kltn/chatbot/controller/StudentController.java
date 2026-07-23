package com.kltn.chatbot.controller;

import com.kltn.chatbot.model.dto.ApiResponse;
import com.kltn.chatbot.model.dto.StudentRiskDTO;
import com.kltn.chatbot.model.entity.Student;
import com.kltn.chatbot.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý student requests
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student Management APIs")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Get all students", description = "Retrieve list of all students")
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents(
            @RequestParam(required = false) String search) {
        
        List<Student> students;
        if (search != null && !search.isEmpty()) {
            students = studentService.searchStudentsByName(search);
        } else {
            students = studentService.getAllStudents();
        }
        
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student by ID", description = "Retrieve student details by ID")
    public ResponseEntity<ApiResponse<Student>> getStudent(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @GetMapping("/{id}/risk")
    @Operation(summary = "Get student risk analysis", description = "Retrieve risk analysis for a student")
    public ResponseEntity<ApiResponse<StudentRiskDTO>> getStudentRisk(@PathVariable Long id) {
        StudentRiskDTO risk = studentService.getStudentRisk(id);
        return ResponseEntity.ok(ApiResponse.success(risk));
    }
}
