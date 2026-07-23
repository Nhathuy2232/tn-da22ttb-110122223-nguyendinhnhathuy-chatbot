package com.kltn.chatbot.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mapping JSON response từ Moodle API
 * Dùng cho gradereport_user_get_grade_items
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleGradeDTO {

    @JsonProperty("userid")
    private Long userId;

    @JsonProperty("courseid")
    private Long courseId;

    @JsonProperty("itemname")
    private String itemName;

    @JsonProperty("itemtype")
    private String itemType;

    @JsonProperty("graderaw")
    private Double gradeRaw;

    @JsonProperty("gradeformatted")
    private String gradeFormatted;

    @JsonProperty("grademax")
    private Double gradeMax;

    @JsonProperty("grademin")
    private Double gradeMin;

    @JsonProperty("percentageformatted")
    private String percentageFormatted;

    @JsonProperty("feedback")
    private String feedback;
}
