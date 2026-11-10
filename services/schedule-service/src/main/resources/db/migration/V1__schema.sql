-- schedule-service · scheduledb · Ask 46 baseline (Flyway reset): the calendar context.
-- term is owned here (every other service references term ids). class_session is the time slot of a
-- course in a term; the course itself (name, grade level, teacher, colour) is owned by enrollment-service
-- and resolved over REST when a timetable is built.
CREATE TABLE term (
    id            text PRIMARY KEY,                  -- 'q1-2026'
    name          text NOT NULL,                     -- 'Q1 · Fall 2026'
    start_date    date NOT NULL,
    end_date      date NOT NULL,
    is_current    boolean NOT NULL DEFAULT false,
    max_credits   integer NOT NULL DEFAULT 30,
    quarter       integer,                           -- 1..4
    academic_year text                               -- '2026 - 2027'
);
CREATE TABLE class_session (
    id          bigserial PRIMARY KEY,
    course_id   text NOT NULL UNIQUE,                -- logical → enrollment-service course.id
    term_id     text NOT NULL REFERENCES term(id) ON DELETE CASCADE,
    days        jsonb NOT NULL DEFAULT '[]',         -- ["Mon","Wed","Fri"]
    start_time  time NOT NULL,
    end_time    time NOT NULL,                       -- start + 1 h
    start_date  date,                                -- null = whole term
    end_date    date
);
CREATE INDEX class_session_term_idx ON class_session(term_id);
