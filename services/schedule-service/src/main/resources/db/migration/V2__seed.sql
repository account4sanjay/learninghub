-- schedule-service: the four quarter terms and one class slot per course
-- Ask 46 baseline seed, exported from the running stack by scripts/export_seed.py (Flyway reset). Edit by hand or re-export.

INSERT INTO term (id, name, start_date, end_date, is_current, max_credits, quarter, academic_year) VALUES ('q1-2026', 'Q1 · Fall 2026', '2026-08-24', '2026-10-23', true, 30, 1, '2026-27');
INSERT INTO term (id, name, start_date, end_date, is_current, max_credits, quarter, academic_year) VALUES ('q2-2026', 'Q2 · Winter 2026', '2026-10-26', '2027-01-15', false, 30, 2, '2026-27');
INSERT INTO term (id, name, start_date, end_date, is_current, max_credits, quarter, academic_year) VALUES ('q3-2027', 'Q3 · Winter 2027', '2027-01-19', '2027-03-26', false, 30, 3, '2026-27');
INSERT INTO term (id, name, start_date, end_date, is_current, max_credits, quarter, academic_year) VALUES ('q4-2027', 'Q4 · Spring 2027', '2027-03-29', '2027-06-04', false, 30, 4, '2026-27');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (9, 'geometry', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '08:00:00', '09:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (10, 'reg-chemistry', 'q1-2026', '["Tue", "Thu"]'::jsonb, '09:00:00', '10:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (11, 'hons-english', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '10:00:00', '11:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (12, 'hons-biology', 'q1-2026', '["Tue", "Thu"]'::jsonb, '11:00:00', '12:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (13, 'ap-cs', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '13:00:00', '14:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (14, 'trigonometry', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '09:00:00', '10:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (15, 'hons-chemistry', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '10:00:00', '11:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (16, 'ap-english-lang', 'q1-2026', '["Tue", "Thu"]'::jsonb, '11:00:00', '12:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (17, 'ap-biology', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '13:00:00', '14:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (18, 'ap-physics-1', 'q1-2026', '["Tue", "Thu"]'::jsonb, '14:00:00', '15:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (19, 'ap-precalc', 'q1-2026', '["Mon", "Tue", "Wed", "Thu", "Fri"]'::jsonb, '10:00:00', '11:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (20, 'ap-chemistry', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '08:00:00', '09:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (21, 'ap-english-lit', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '13:00:00', '14:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (22, 'ap-physics-2', 'q1-2026', '["Tue", "Thu"]'::jsonb, '09:00:00', '10:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (23, 'ap-statistics', 'q1-2026', '["Tue", "Thu"]'::jsonb, '11:00:00', '12:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (24, 'ap-calc-ab', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '13:00:00', '14:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (25, 'ap-calc-bc', 'q1-2026', '["Mon", "Tue", "Wed", "Thu", "Fri"]'::jsonb, '14:00:00', '15:00:00', '2026-08-24', '2026-10-23');
INSERT INTO class_session (id, course_id, term_id, days, start_time, end_time, start_date, end_date) VALUES (26, 'ap-physics-c', 'q1-2026', '["Mon", "Wed", "Fri"]'::jsonb, '10:00:00', '11:00:00', '2026-08-24', '2026-10-23');
SELECT setval(pg_get_serial_sequence('class_session', 'id'), COALESCE((SELECT max(id) FROM class_session), 0) + 1, false);
