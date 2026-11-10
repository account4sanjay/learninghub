-- enrollment-service · enrollmentdb · Ask 46 baseline (Flyway reset): the academic catalogue.
-- course is the per-term offering of a subject (term_id is a logical reference to schedule-service term.id;
-- the meeting slot lives in schedule-service class_session). teacher_username references the user context.
CREATE TABLE grade_level (
    id           text PRIMARY KEY,                   -- 'freshman' | 'sophomore' | 'junior' | 'senior'
    name         text NOT NULL,                      -- 'Freshman'
    grade_number integer NOT NULL,                   -- 9..12
    sort_order   integer NOT NULL DEFAULT 0
);
CREATE TABLE course (
    id               text PRIMARY KEY,               -- 'ap-precalc' (shared id across services)
    term_id          text NOT NULL,                  -- logical → schedule-service term.id
    name             text NOT NULL,                  -- 'AP Pre Calculus'
    grade_level      text NOT NULL REFERENCES grade_level(id),
    teacher_username text NOT NULL,                  -- logical → user-service profile.username
    credits          integer NOT NULL DEFAULT 4,
    color            text NOT NULL DEFAULT 'blue'    -- SPA colour token
);
CREATE INDEX course_term_idx ON course(term_id);
CREATE INDEX course_teacher_idx ON course(teacher_username);
CREATE TABLE enrollment (
    id          bigserial PRIMARY KEY,
    course_id   text NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    username    text NOT NULL,                       -- logical → user-service profile.username
    status      text NOT NULL DEFAULT 'Enrolled',
    enrolled_at timestamp NOT NULL DEFAULT now(),
    UNIQUE (course_id, username)
);
CREATE INDEX enrollment_user_idx ON enrollment(username);
