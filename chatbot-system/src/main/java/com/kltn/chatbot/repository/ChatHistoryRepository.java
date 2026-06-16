package com.kltn.chatbot.repository;

import com.kltn.chatbot.model.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho ChatHistory entity
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /**
     * Tìm lịch sử chat theo session ID
     */
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Tìm lịch sử chat của giảng viên
     */
    List<ChatHistory> findByLecturerIdOrderByCreatedAtDesc(Long lecturerId);

    /**
     * Tìm lịch sử chat gần đây của giảng viên
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.lecturerId = :lecturerId AND ch.createdAt >= :fromDate ORDER BY ch.createdAt DESC")
    List<ChatHistory> findRecentChatsByLecturer(@Param("lecturerId") Long lecturerId, 
                                                  @Param("fromDate") LocalDateTime fromDate);

    /**
     * Đếm số lượng chat theo intent
     */
    @Query("SELECT COUNT(ch) FROM ChatHistory ch WHERE ch.intent = :intent")
    Long countByIntent(@Param("intent") String intent);

    /**
     * Xóa lịch sử chat cũ hơn N ngày
     */
    @Query("DELETE FROM ChatHistory ch WHERE ch.createdAt < :beforeDate")
    void deleteOldChats(@Param("beforeDate") LocalDateTime beforeDate);
}
