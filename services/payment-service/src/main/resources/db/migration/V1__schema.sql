-- payment-service · paymentdb · Ask 46 baseline (Flyway reset): tuition.
-- An invoice is raised per student per term (term_id is a logical reference to schedule-service term.id) and
-- carries its lines: one per billed course (course_id is a logical reference to enrollment-service course.id).
-- Lines are frozen at billing time so the invoice stays a historical record; invoice.amount = sum(lines).
CREATE TABLE billing_profile (
    id        bigserial PRIMARY KEY,
    username  text NOT NULL UNIQUE,                  -- logical → user-service profile.username
    name      text NOT NULL,
    email     text,
    address   text
);
CREATE TABLE payment_method (
    id         bigserial PRIMARY KEY,
    username   text NOT NULL,
    brand      text NOT NULL,                        -- 'Visa', 'Mastercard'
    last4      text NOT NULL,
    expires    text NOT NULL,                        -- 'MM/YYYY'
    is_default boolean NOT NULL DEFAULT false
);
CREATE INDEX payment_method_user_idx ON payment_method(username);
CREATE TABLE invoice (
    id          bigserial PRIMARY KEY,
    username    text NOT NULL,
    term_id     text NOT NULL,                       -- logical → schedule-service term.id
    description text NOT NULL,                       -- 'Q1 · Fall 2026 Tuition'
    amount      numeric(10,2) NOT NULL,              -- = sum(invoice_line.amount)
    due_date    date NOT NULL,
    status      text NOT NULL,                       -- 'Paid' | 'Upcoming' | 'Scheduled' | 'Pending'
    paid_on     date,
    method_id   bigint REFERENCES payment_method(id) ON DELETE SET NULL,
    UNIQUE (username, term_id)
);
CREATE INDEX invoice_user_idx ON invoice(username);
CREATE TABLE invoice_line (
    id          bigserial PRIMARY KEY,
    invoice_id  bigint NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    course_id   text NOT NULL,                       -- logical → enrollment-service course.id
    description text NOT NULL,                       -- subject name as billed (frozen)
    amount      numeric(10,2) NOT NULL
);
CREATE INDEX invoice_line_invoice_idx ON invoice_line(invoice_id);
