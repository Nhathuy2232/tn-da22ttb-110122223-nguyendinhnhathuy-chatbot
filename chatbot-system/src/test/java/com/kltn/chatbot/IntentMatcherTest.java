package com.kltn.chatbot;

import com.kltn.chatbot.service.LocalIntentMatcher;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for LocalIntentMatcher.
 */
public class IntentMatcherTest {

    private static String s(String text) {
        return text;
    }

    @Test
    void testStudentIntents() {
        String cn1 = "Xem \u0111i\u1ec3m c\u1ee7a em";
        assertEquals("LIST_OWN_GRADES", match(cn1, "STUDENT").get("intent"));

        String cn2 = "Em c\u00f3 b\u1ecb c\u1ea3nh b\u00e1o h\u1ecdc v\u1ee5 kh\u00f4ng \u1ea1?";
        assertEquals("CHECK_OWN_RISK_STATUS", match(cn2, "STUDENT").get("intent"));

        String cn3 = "T\u00ecnh tr\u1ea1ng h\u1ecdc t\u1eadp c\u1ee7a em";
        assertEquals("CHECK_OWN_RISK_STATUS", match(cn3, "STUDENT").get("intent"));

        String cn4 = "L\u00e0m sao \u0111\u1ec3 em c\u1ea3i thi\u1ec7n \u0111i\u1ec3m m\u00f4n Java?";
        assertEquals("GET_IMPROVEMENT_SUGGESTIONS", match(cn4, "STUDENT").get("intent"));

        // Student denied (hỏi về SV khác)
        String cn5 = "Xem \u0111i\u1ec3m sinh vi\u00ean 110122005";
        assertEquals("PERMISSION_DENIED", match(cn5, "STUDENT", "110122001").get("intent"));
    }

    @Test
    void testLecturerIntents() {
        String cn1 = "L\u1edbp Java c\u00f3 ai ch\u01b0a n\u1ed9p Assignment 1?";
        assertEquals("CHECK_SUBMISSIONS_AND_REMIND", match(cn1, "LECTURER").get("intent"));

        String cn2 = "M\u00f4n C\u01a1 s\u1edf d\u1eef li\u1ec7u hi\u1ec7n t\u1ea1i c\u00f3 bao nhi\u00eau sinh vi\u00ean m\u1ee9c \u0110\u1ecf?";
        assertEquals("FILTER_COURSE_RISK", match(cn2, "LECTURER").get("intent"));

        String cn3 = "Ki\u1ec3m tra \u0111i\u1ec3m c\u1ee7a sinh vi\u00ean 110122223";
        assertEquals("QUERY_STUDENT_INFO_NLP", match(cn3, "LECTURER").get("intent"));

        String cn4 = "MSSV 110122005 ngh\u1ec9 h\u1ecdc bao nhi\u00eau bu\u1ed5i?";
        assertEquals("QUERY_STUDENT_INFO_NLP", match(cn4, "LECTURER").get("intent"));

        // Lecturer denied
        String cn5 = "C\u1ea5u h\u00ecnh ng\u01b0\u1ee1ng c\u1ea3nh b\u00e1o";
        assertEquals("PERMISSION_DENIED", match(cn5, "LECTURER").get("intent"));
    }

    @Test
    void testAdviserIntents() {
        String cn1 = "L\u1edbp c\u1ed1 v\u1ea5n DA22TTB c\u00f3 bao nhi\u00eau sinh vi\u00ean b\u1ecb c\u1ea3nh b\u00e1o?";
        assertEquals("VIEW_CLASS_RISK_SUMMARY", match(cn1, "ADVISER").get("intent"));

        String cn2 = "Xem danh s\u00e1ch sinh vi\u00ean m\u1ee9c \u0110\u1ecf c\u1ee7a l\u1edbp sinh ho\u1ea1t DA22TTA";
        assertEquals("VIEW_CLASS_RISK_SUMMARY", match(cn2, "ADVISER").get("intent"));

        String cn3 = "T\u00ecnh h\u00ecnh h\u1ecdc v\u1ee5 t\u1ed5ng quan l\u1edbp DA22TTB";
        assertEquals("VIEW_CLASS_RISK_SUMMARY", match(cn3, "ADVISER").get("intent"));

        String cn4 = "Li\u1ec7t k\u00ea sinh vi\u00ean l\u1edbp DA22TTB kh\u00f4ng online Moodle tr\u00ean 2 tu\u1ea7n";
        assertEquals("FIND_INACTIVE_STUDENTS", match(cn4, "ADVISER").get("intent"));

        // Adviser denied
        String cn5 = "C\u1ea5u h\u00ecnh h\u1ec7 th\u1ed1ng";
        assertEquals("PERMISSION_DENIED", match(cn5, "ADVISER").get("intent"));
    }

    @Test
    void testAdminIntents() {
        String cn1 = "C\u00e0i \u0111\u1eb7t l\u1ea1i ng\u01b0\u1ee1ng c\u1ea3nh b\u00e1o h\u1ecdc v\u1ee5";
        assertEquals("CONFIG_WARNING_THRESHOLD", match(cn1, "ADMIN").get("intent"));

        String cn2 = "\u0110\u1ed3ng b\u1ed9 d\u1eef li\u1ec7u t\u1eeb Moodle";
        assertEquals("TRIGGER_MOODLE_SYNC", match(cn2, "ADMIN").get("intent"));

        String cn3 = "Ki\u1ec3m tra tr\u1ea1ng th\u00e1i k\u1ebft n\u1ed1i API Moodle Web Services";
        assertEquals("ADMIN_CHECK_API_STATUS", match(cn3, "ADMIN").get("intent"));
    }

    @Test
    void testEntityExtraction() {
        String cn = "Ki\u1ec3m tra \u0111i\u1ec3m c\u1ee7a sinh vi\u00ean 110122223";
        Map<String, Object> result = match(cn, "LECTURER");
        @SuppressWarnings("unchecked")
        Map<String, String> entities = (Map<String, String>) result.get("entities");
        assertEquals("110122223", entities.get("mssv"));
    }

    private Map<String, Object> match(String message, String role) {
        return match(message, role, null);
    }

    private Map<String, Object> match(String message, String role, String username) {
        Optional<Map<String, Object>> result = LocalIntentMatcher.matchForRole(message, role, username);
        assertTrue(result.isPresent(), "No match");
        return result.get();
    }
}
