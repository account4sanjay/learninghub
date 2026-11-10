-- learning-service · learningdb · Ask 46 baseline (Flyway reset): lesson-plan progress and study time.
-- A lesson_plan_item is one scheduled class of a course (course_id is a logical reference to
-- enrollment-service course.id); course name/teacher/colour and the term are resolved over REST.
CREATE TABLE lesson_plan_item (
    id           bigserial PRIMARY KEY,
    course_id    text NOT NULL,                      -- logical → enrollment-service course.id
    class_number integer NOT NULL,                   -- 1..n within the course's term
    class_date   date NOT NULL,
    title        text NOT NULL,                      -- unit
    topic        text,
    completed    boolean NOT NULL DEFAULT false,
    completed_at timestamp,
    completed_by text,                               -- teacher username
    UNIQUE (course_id, class_number)
);
CREATE INDEX lesson_plan_item_course_idx ON lesson_plan_item(course_id, class_date);
CREATE TABLE study_session (
    id       bigserial PRIMARY KEY,
    username text NOT NULL,                          -- logical → user-service profile.username
    day      date NOT NULL,
    minutes  integer NOT NULL
);
CREATE INDEX study_session_user_idx ON study_session(username, day);
