-- user-service · userdb · Ask 46 baseline (Flyway reset): one owner per entity.
-- profile is the user record behind the Keycloak account. grade_level is a logical reference to
-- enrollment-service grade_level.id; classes, grade names and the academic year are read from
-- enrollment-service / schedule-service at request time, never stored here.
CREATE TABLE profile (
    id                 bigserial PRIMARY KEY,
    username           text NOT NULL UNIQUE,            -- Keycloak preferred_username (shared id)
    full_name          text NOT NULL,
    title              text,                            -- Mr | Ms | Dr
    first_name         text,
    last_name          text,
    gender             text,                            -- male | female | other
    role               text NOT NULL DEFAULT 'Student', -- Student | Teacher | Admin (mirrors the Keycloak realm role)
    email              text,
    phone              text,
    location           text,
    bio                text,
    date_of_birth      date,
    school             text,
    address            text,
    guardian_name      text,
    guardian_relation  text,
    guardian_email     text,
    guardian_phone     text,
    pref_timezone      text NOT NULL DEFAULT 'Pacific Time (PT)',
    pref_notifications text NOT NULL DEFAULT 'Email & Push',
    grade_level        text,                            -- logical → enrollment-service grade_level.id (students)
    avatar             bytea,
    avatar_content_type text
);
CREATE INDEX profile_role_idx ON profile(role);
