-- assessment-service · assessmentdb · Ask 46 baseline (Flyway reset): assessments, questions and attempts.
-- An assessment belongs to a course (course_id is a logical reference to enrollment-service course.id);
-- subject name, grade level, teacher and colour are read from the course, never stored here.
CREATE TABLE assessment (
    id                  text PRIMARY KEY,                -- 'unit2-quiz'
    course_id           text NOT NULL,                   -- logical → enrollment-service course.id
    title               text NOT NULL,
    subtitle            text,
    type                text NOT NULL,                   -- Quiz | Test | Assignment | Practice
    due_at              timestamp NOT NULL,
    points              integer NOT NULL DEFAULT 100,
    attempts_allowed    integer NOT NULL DEFAULT 1,
    time_limit_minutes  integer,
    instructions_intro  text,
    instructions_steps  jsonb NOT NULL DEFAULT '[]',     -- ["step", ...]
    files               jsonb NOT NULL DEFAULT '[]',     -- [{name,meta,action,icon}]
    rubric              jsonb NOT NULL DEFAULT '[]',     -- [{label,percent}]
    related_learning    jsonb NOT NULL DEFAULT '[]',     -- [{kind,title,meta,icon}]
    created_by          text NOT NULL                    -- author: teacher username (logical → user-service)
);
CREATE INDEX assessment_course_idx ON assessment(course_id, due_at);
CREATE INDEX assessment_author_idx ON assessment(created_by);
CREATE TABLE question (
    id             bigserial PRIMARY KEY,
    assessment_id  text NOT NULL REFERENCES assessment(id) ON DELETE CASCADE,
    number         integer NOT NULL,
    section        text NOT NULL DEFAULT 'Multiple Choice',
    points         integer NOT NULL DEFAULT 1,
    kind           text NOT NULL DEFAULT 'choice',       -- choice | graph-choice
    text           text NOT NULL,
    options        jsonb NOT NULL DEFAULT '[]',          -- [{key,text}] or [{key,graph:{vertexX,vertexY,opens}}]
    correct        text NOT NULL,
    explanation    text,
    topic          text,
    UNIQUE (assessment_id, number)
);
CREATE TABLE attempt (
    id               bigserial PRIMARY KEY,
    assessment_id    text NOT NULL REFERENCES assessment(id) ON DELETE CASCADE,
    username         text NOT NULL,                      -- logical → user-service profile.username
    started_at       timestamp,
    completed_at     timestamp,
    seconds_taken    integer,
    answers          jsonb NOT NULL DEFAULT '{}',        -- {"1":"B", ...}
    correct          integer NOT NULL DEFAULT 0,
    incorrect        integer NOT NULL DEFAULT 0,
    skipped          integer NOT NULL DEFAULT 0,
    percent          integer NOT NULL DEFAULT 0,
    status           text NOT NULL DEFAULT 'Completed',  -- In Progress | Completed | Submitted | Graded
    deadline_at      timestamp,                          -- timed attempts (Ask 20)
    auto_submitted   boolean NOT NULL DEFAULT false,
    submission_text  text,                               -- assignments
    submission_files jsonb NOT NULL DEFAULT '[]',        -- [{name,size,type}]
    points_awarded   integer,
    feedback         text,
    graded_by        text,
    graded_at        timestamp
);
CREATE INDEX attempt_user_idx ON attempt(username, assessment_id);
CREATE INDEX attempt_assessment_idx ON attempt(assessment_id, status);
