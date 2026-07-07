-- warnings.student_id / course_id store Moodle IDs (mdl_user.id, mdl_course.id),
-- not local students.id / courses.id. Drop legacy FK constraints.
ALTER TABLE warnings DROP FOREIGN KEY IF EXISTS FK_warning_student;
ALTER TABLE warnings DROP FOREIGN KEY IF EXISTS FK_warning_course;
