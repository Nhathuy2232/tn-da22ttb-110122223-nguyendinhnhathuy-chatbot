package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.GradeItem;
import com.kltn.chatbot.model.entity.GradeItem.GradeItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeItemRepository extends JpaRepository<GradeItem, Long> {
    
    Optional<GradeItem> findByMoodleGradeItemId(Long moodleGradeItemId);
    
    List<GradeItem> findByCourseId(Long courseId);
    
    List<GradeItem> findByCourseIdAndIsActive(Long courseId, Boolean isActive);
    
    List<GradeItem> findByCourseIdAndItemType(Long courseId, GradeItemType itemType);
    
    @Query("SELECT gi FROM GradeItem gi WHERE gi.courseId = :courseId AND gi.isActive = true ORDER BY gi.dueDate ASC")
    List<GradeItem> findActiveByCourseIdOrderByDueDate(@Param("courseId") Long courseId);
    
    @Query("SELECT gi FROM GradeItem gi WHERE gi.courseId = :courseId AND gi.itemType = :itemType AND gi.isActive = true")
    List<GradeItem> findActiveByCourseIdAndType(@Param("courseId") Long courseId, @Param("itemType") GradeItemType itemType);
    
    @Query("SELECT COUNT(gi) FROM GradeItem gi WHERE gi.courseId = :courseId AND gi.isActive = true")
    Long countActiveByCourseId(@Param("courseId") Long courseId);
    
    boolean existsByMoodleGradeItemId(Long moodleGradeItemId);
}
