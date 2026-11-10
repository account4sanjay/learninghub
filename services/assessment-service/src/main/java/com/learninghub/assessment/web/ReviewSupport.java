package com.learninghub.assessment.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.assessment.common.Courses.Course;
import com.learninghub.assessment.common.Fmt;
import com.learninghub.assessment.domain.Assessment;
import com.learninghub.assessment.domain.Attempt;
import com.learninghub.assessment.domain.Question;

/** Builds the review shape of an attempt; shared by the student review and the teacher submission review. */
public final class ReviewSupport {
    private ReviewSupport() {}

    public static Map<String, Object> questionJson(Question q, int total, boolean withAnswer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId()); m.put("number", q.getNumber()); m.put("total", total); m.put("section", q.getSection());
        m.put("points", q.getPoints()); m.put("kind", q.getKind()); m.put("text", q.getText()); m.put("options", q.getOptions());
        m.put("topic", q.getTopic());
        if (withAnswer) { m.put("correct", q.getCorrect()); m.put("explanation", q.getExplanation()); }
        return m;
    }

    /** Assignment submission block (text, files, grade); null for quiz-style attempts. */
    public static Map<String, Object> submissionJson(Assessment a, Attempt at) {
        if (!"Assignment".equals(a.getType()) && at.getSubmissionText() == null && at.getSubmissionFiles().isEmpty()) return null;
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("status", at.getStatus()); s.put("submittedAt", Fmt.dateTimeAt(at.getCompletedAt()));
        s.put("text", at.getSubmissionText()); s.put("files", at.getSubmissionFiles());
        s.put("points", a.getPoints()); s.put("pointsAwarded", at.getPointsAwarded()); s.put("feedback", at.getFeedback());
        s.put("gradedBy", at.getGradedBy()); s.put("gradedAt", Fmt.dateTimeAt(at.getGradedAt()));
        return s;
    }

    /** Review of one attempt: results, per-question topics/outcomes, every question with chosen vs correct. */
    public static Map<String, Object> review(Assessment a, Course c, Attempt at, List<Question> qs) {
        int total = qs.size();
        int seconds = at.getSecondsTaken() == null ? 0 : at.getSecondsTaken();

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("correct", at.getCorrect()); results.put("total", total); results.put("percent", at.getPercent());
        results.put("incorrect", at.getIncorrect()); results.put("skipped", at.getSkipped()); results.put("timeTaken", Fmt.mmss(seconds));
        boolean graded = at.getPointsAwarded() != null;
        results.put("pointsEarned", graded ? at.getPointsAwarded() + " / " + a.getPoints() : at.getCorrect() + " / " + total);
        results.put("score", Attempt.SUBMITTED.equals(at.getStatus()) ? "Pending" : at.getPercent() + "%"); results.put("status", at.getStatus());
        results.put("autoSubmitted", at.isAutoSubmitted());

        List<Map<String, Object>> topics = new ArrayList<>();
        List<Map<String, Object>> reviewQuestions = new ArrayList<>();
        for (Question q : qs) {
            String chosen = at.getAnswers().get(String.valueOf(q.getNumber()));
            String result = chosen == null ? "skipped" : chosen.equalsIgnoreCase(q.getCorrect()) ? "correct" : "incorrect";
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("n", q.getNumber()); t.put("topic", q.getTopic() == null ? "Question " + q.getNumber() : q.getTopic()); t.put("result", result);
            topics.add(t);
            Map<String, Object> rq = questionJson(q, total, true);
            rq.put("chosen", chosen);
            rq.put("result", Character.toUpperCase(result.charAt(0)) + result.substring(1));
            rq.put("pointsLabel", "Points: " + ("correct".equals(result) ? q.getPoints() : 0) + " / " + q.getPoints());
            reviewQuestions.add(rq);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId()); out.put("title", a.getTitle());
        out.put("subtitle", a.getSubtitle() + (c.gradeLevel().isEmpty() ? "" : " (" + c.gradeLevel() + ")") + " · " + c.name());
        out.put("completedOn", Fmt.dateTimeAt(at.getCompletedAt())); out.put("questions", total);
        out.put("minutes", Math.round(seconds / 60.0)); out.put("results", results); out.put("questionTopics", topics);
        out.put("reviewQuestion", reviewQuestions.isEmpty() ? null : reviewQuestions.get(0));
        out.put("reviewQuestions", reviewQuestions); out.put("relatedLearning", a.getRelatedLearning());
        out.put("submission", submissionJson(a, at));
        return out;
    }
}
