package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.User;
import com.kltn.chatbot.model.entity.User.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByMoodleUserId(Long moodleUserId);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByStudentCode(String studentCode);
    
    List<User> findByUserType(UserType userType);
    
    List<User> findByUserTypeAndIsActive(UserType userType, Boolean isActive);
    
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.isActive = true ORDER BY u.fullName")
    List<User> findActiveUsersByType(@Param("userType") UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.userType = 'STUDENT' AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchStudents(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType AND u.isActive = true")
    Long countActiveUsersByType(@Param("userType") UserType userType);
    
    boolean existsByMoodleUserId(Long moodleUserId);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
