package com.learninghub.assessment.web;

/**
 * Response examples for the OpenAPI documentation (captured from the running stack against the
 * seed data and trimmed to a few items). Compile-time constants so they can be used in annotations.
 */
public final class Examples {
    private Examples() {}

    /** GET /summary (student) */
    public static final String SUMMARY = """
{
  "term": {
    "start": "Aug 24, 2026",
    "name": "Q1 · Fall 2026",
    "end": "Oct 23, 2026"
  },
  "stats": [
    {
      "label": "Total Assessments",
      "value": "10",
      "sub": "This term",
      "color": "blue",
      "icon": "total"
    },
    {
      "label": "Average Score",
      "value": "84%",
      "sub": "Across 4 results",
      "color": "green",
      "icon": "score"
    },
    {
      "label": "High Score",
      "value": "1",
      "sub": "Scored 90%+",
      "color": "purple",
      "icon": "trophy"
    },
    {
      "label": "Upcoming This Week",
      "value": "1",
      "sub": "1 due today · 3 due this week",
      "color": "red",
      "icon": "target"
    }
  ],
  "recentResults": [
    {
      "title": "Unit 2 Quiz",
      "subject": "AP Pre Calculus",
      "score": "80%",
      "date": "Sep 15, 2026",
      "color": "blue"
    },
    {
      "title": "Unit 1 Quiz",
      "subject": "AP Pre Calculus",
      "score": "88%",
      "date": "Aug 30, 2026",
      "color": "blue"
    },
    {
      "title": "Chapter 1 Test",
      "subject": "AP Physics 2",
      "score": "92%",
      "date": "Aug 28, 2026",
      "color": "purple"
    },
    {
      "title": "Lab Safety Quiz",
      "subject": "AP Chemistry",
      "score": "76%",
      "date": "Aug 26, 2026",
      "color": "red"
    }
  ],
  "role": "student"
}
            """;

    /** GET /summary (teacher/admin) */
    public static final String SUMMARY_STAFF = """
{
  "term": {
    "start": "Aug 24, 2026",
    "name": "Q1 · Fall 2026",
    "end": "Oct 23, 2026"
  },
  "stats": [
    {
      "label": "Total Assessments",
      "value": "10",
      "sub": "All teachers, this term",
      "color": "blue",
      "icon": "total"
    },
    {
      "label": "Submissions",
      "value": "4",
      "sub": "1 student",
      "color": "green",
      "icon": "score"
    },
    {
      "label": "Average Score",
      "value": "84%",
      "sub": "Across all submissions",
      "color": "purple",
      "icon": "trophy"
    },
    {
      "label": "Overdue",
      "value": "6",
      "sub": "Past due date",
      "color": "red",
      "icon": "target"
    }
  ],
  "recentResults": [
    {
      "title": "Unit 2 Quiz",
      "subject": "AP Pre Calculus",
      "score": "80%",
      "date": "Sep 15, 2026",
      "color": "blue",
      "student": "student1"
    },
    {
      "title": "Unit 1 Quiz",
      "subject": "AP Pre Calculus",
      "score": "88%",
      "date": "Aug 30, 2026",
      "color": "blue",
      "student": "student1"
    },
    {
      "title": "Chapter 1 Test",
      "subject": "AP Physics 2",
      "score": "92%",
      "date": "Aug 28, 2026",
      "color": "purple",
      "student": "student1"
    },
    {
      "title": "Lab Safety Quiz",
      "subject": "AP Chemistry",
      "score": "76%",
      "date": "Aug 26, 2026",
      "color": "red",
      "student": "student1"
    }
  ],
  "role": "admin"
}
            """;

    /** GET /list (student) */
    public static final String LIST = """
[
  {
    "id": "lab-safety",
    "title": "Lab Safety Quiz",
    "subtitle": "Safety Fundamentals",
    "courseId": "ap-chemistry",
    "subject": "AP Chemistry",
    "type": "Quiz",
    "gradeLevel": "Junior",
    "due": "Aug 26, 2026",
    "dueTime": "11:59 PM",
    "status": "Completed",
    "score": "76%",
    "action": "Review",
    "color": "red",
    "dueAt": "2026-08-26T23:59:00",
    "timeLimitMinutes": 20,
    "secondsLeft": null
  },
  {
    "id": "ch1-test",
    "title": "Chapter 1 Test",
    "subtitle": "Motion and Forces",
    "courseId": "ap-physics-2",
    "subject": "AP Physics 2",
    "type": "Test",
    "gradeLevel": "Junior",
    "due": "Aug 28, 2026",
    "dueTime": "11:59 PM",
    "status": "Completed",
    "score": "92%",
    "action": "Review",
    "color": "purple",
    "dueAt": "2026-08-28T23:59:00",
    "timeLimitMinutes": 60,
    "secondsLeft": null
  }
]
            """;

    /** GET /list (teacher/admin: submissions + average) */
    public static final String LIST_STAFF = """
[
  {
    "id": "unit1-quiz",
    "title": "Unit 1 Quiz",
    "subtitle": "Foundations of Algebra",
    "courseId": "ap-precalc",
    "subject": "AP Pre Calculus",
    "type": "Quiz",
    "gradeLevel": "Junior",
    "due": "Aug 30, 2026",
    "dueTime": "11:59 PM",
    "status": "Closed",
    "dueAt": "2026-09-30T23:59:00",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "submissions": 1,
    "toGrade": 0,
    "average": "88%",
    "score": null,
    "action": "View",
    "color": "blue"
  },
  {
    "id": "unit2-quiz",
    "title": "Unit 2 Quiz",
    "subtitle": "Functions and Graphs",
    "courseId": "ap-precalc",
    "subject": "AP Pre Calculus",
    "type": "Quiz",
    "gradeLevel": "Junior",
    "due": "Sep 16, 2026",
    "dueTime": "11:59 PM",
    "status": "Closed",
    "dueAt": "2026-09-30T23:59:00",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "submissions": 1,
    "toGrade": 0,
    "average": "80%",
    "score": null,
    "action": "View",
    "color": "blue"
  }
]
            """;

    /** GET /performance */
    public static final String PERFORMANCE = """
[
  {
    "subject": "AP Pre Calculus",
    "percent": 84,
    "color": "blue"
  },
  {
    "subject": "AP Physics 2",
    "percent": 92,
    "color": "purple"
  },
  {
    "subject": "AP Chemistry",
    "percent": 76,
    "color": "red"
  }
]
            """;

    /** GET /{id} — assessment details page */
    public static final String DETAILS = """
{
  "id": "lab-report",
  "title": "Lab Report: Chemical Reactions",
  "courseId": "ap-chemistry",
  "subject": "AP Chemistry",
  "type": "Assignment",
  "gradeLevel": "Junior",
  "teacher": "Dr Priya Patel",
  "due": "Oct 2, 2026 at 11:59 PM",
  "instructions": {
    "intro": "In this lab report, you will investigate different chemical reactions and analyze the results. Follow the steps below, record your observations, and answer the questions at the end.",
    "steps": [
      "Read the lab manual and safety guidelines.",
      "Conduct the three experiments as described.",
      "Record your observations in the provided template."
    ]
  },
  "files": [
    {
      "icon": "pdf",
      "meta": "PDF · 2.4 MB",
      "name": "Lab Manual - Chemical Reactions.pdf",
      "action": "Download"
    },
    {
      "icon": "doc",
      "meta": "DOCX · 485 KB",
      "name": "Lab Report Template.docx",
      "action": "Download"
    },
    {
      "icon": "video",
      "meta": "Video · 10 min",
      "name": "Introduction Video",
      "action": "Watch"
    }
  ],
  "questionsPreview": [],
  "status": {
    "status": "Upcoming",
    "action": "Submit",
    "dueDate": "Oct 2, 2026 11:59 PM",
    "timeRemaining": "5 days 19 hours",
    "secondsToDue": 501540,
    "closed": false,
    "points": "100",
    "attempts": "0 of 1 used",
    "attemptsUsed": 0,
    "attemptsAllowed": 1,
    "timeLimitMinutes": null,
    "timeLimit": "Until the due date",
    "canStart": false,
    "canRetake": false
  },
  "attempt": null,
  "classInfo": {
    "teacher": "Dr Priya Patel",
    "gradeLevel": "Junior",
    "subject": "AP Chemistry"
  },
  "rubric": [
    {
      "label": "Lab Procedure & Observations",
      "percent": 30
    },
    {
      "label": "Data Analysis",
      "percent": 25
    },
    {
      "label": "Conclusions",
      "percent": 25
    }
  ],
  "submission": {
    "open": true,
    "note": "You can submit until Oct 2, 2026 11:59 PM",
    "submitted": false,
    "status": null,
    "submittedAt": null,
    "text": null,
    "files": [],
    "points": 100,
    "pointsAwarded": null,
    "percent": null,
    "feedback": null,
    "gradedBy": null,
    "gradedAt": null,
    "autoSubmitted": false,
    "correct": null,
    "total": 0
  }
}
            """;

    /** POST /{id}/start */
    public static final String START = """
{
  "attemptId": 91,
  "status": "InProgress",
  "startedAt": "Sep 27, 2026 at 10:15 AM",
  "deadlineAt": "2026-09-27T10:45:00",
  "secondsLeft": 1800,
  "answers": {},
  "resumed": false,
  "timeLimitMinutes": 30
}
            """;

    /** PUT /{id}/answers request */
    public static final String ANSWERS_BODY = """
{
  "answers": {
    "1": "B",
    "2": "A"
  }
}
            """;

    /** PUT /{id}/answers response */
    public static final String ANSWERS = """
{
  "attemptId": 91,
  "saved": 2,
  "secondsLeft": 1523
}
            """;

    /** POST /{id}/assignment request */
    public static final String ASSIGNMENT_BODY = """
{
  "text": "Observations and analysis for the three reactions are in the attached report.",
  "files": [
    {
      "name": "lab-report-student1.pdf",
      "size": 245760,
      "type": "application/pdf"
    }
  ]
}
            """;

    /** POST /{id}/assignment response */
    public static final String ASSIGNMENT = """
{
  "attemptId": 92,
  "status": "Submitted",
  "submittedAt": "Sep 27, 2026 at 10:20 AM",
  "files": 1,
  "resubmitted": false
}
            """;

    /** POST /submissions/{attemptId}/grade request */
    public static final String GRADE_BODY = """
{
  "points": 88,
  "feedback": "Clear analysis; label the axes on graph 2."
}
            """;

    /** POST /submissions/{attemptId}/grade response */
    public static final String GRADE = """
{
  "attemptId": 92,
  "student": "student1",
  "status": "Graded",
  "pointsAwarded": 88,
  "points": 100,
  "percent": 88,
  "feedback": "Clear analysis; label the axes on graph 2.",
  "gradedBy": "ppatel",
  "gradedAt": "Sep 28, 2026 at 9:05 AM"
}
            """;

    /** GET /{id}/questions (answers hidden; trimmed to 2 questions) */
    public static final String QUESTIONS = """
{
  "id": "unit2-quiz",
  "title": "Unit 2 Quiz",
  "type": "Quiz",
  "subtitle": "Functions and Graphs · AP Pre Calculus (Junior)",
  "timeLimitMinutes": 30,
  "timeRemaining": "24:37",
  "attempt": {
    "attemptId": 91,
    "status": "InProgress",
    "startedAt": "Sep 27, 2026 at 10:15 AM",
    "deadlineAt": "2026-09-27T10:45:00",
    "secondsLeft": 1477,
    "answers": {
      "1": "B"
    }
  },
  "totalQuestions": 20,
  "questions": [
    {
      "id": 1,
      "number": 1,
      "total": 20,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Question 1: which statement about simplify an expression is correct?",
      "options": [
        {
          "key": "A",
          "text": "Simplify an Expression: option A"
        },
        {
          "key": "B",
          "text": "Simplify an Expression: option B"
        }
      ],
      "topic": "Simplify an Expression"
    },
    {
      "id": 2,
      "number": 2,
      "total": 20,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Question 2: which statement about equations is correct?",
      "options": [
        {
          "key": "A",
          "text": "Equations: option A"
        },
        {
          "key": "B",
          "text": "Equations: option B"
        }
      ],
      "topic": "Equations"
    }
  ],
  "question": {
    "id": 1,
    "number": 1,
    "total": 20,
    "section": "Multiple Choice",
    "points": 1,
    "kind": "choice",
    "text": "Question 1: which statement about simplify an expression is correct?",
    "options": [
      {
        "key": "A",
        "text": "Simplify an Expression: option A"
      },
      {
        "key": "B",
        "text": "Simplify an Expression: option B"
      }
    ],
    "topic": "Simplify an Expression"
  },
  "topics": [
    "Simplify an Expression",
    "Equations"
  ]
}
            """;

    /** POST /{id}/submit request */
    public static final String SUBMIT_BODY = """
{
  "answers": {
    "1": "B",
    "2": "A",
    "3": "D"
  },
  "secondsTaken": 1470
}
            """;

    /** POST /{id}/submit response */
    public static final String SUBMIT = """
{
  "attemptId": 5,
  "correct": 16,
  "incorrect": 3,
  "skipped": 1,
  "total": 20,
  "percent": 80,
  "status": "Completed",
  "secondsTaken": 1470,
  "autoSubmitted": false
}
            """;

    /** GET /{id}/review (trimmed to 2 questions) */
    public static final String REVIEW = """
{
  "id": "unit2-quiz",
  "title": "Unit 2 Quiz",
  "subtitle": "Functions and Graphs (Junior) · AP Pre Calculus",
  "completedOn": "Sep 15, 2026 at 7:42 PM",
  "questions": 20,
  "minutes": 25,
  "results": {
    "correct": 16,
    "total": 20,
    "percent": 80,
    "incorrect": 3,
    "skipped": 1,
    "timeTaken": "24:30",
    "pointsEarned": "16 / 20",
    "score": "80%",
    "status": "Completed",
    "autoSubmitted": false
  },
  "questionTopics": [
    {
      "n": 1,
      "topic": "Simplify an Expression",
      "result": "correct"
    },
    {
      "n": 2,
      "topic": "Equations",
      "result": "correct"
    }
  ],
  "reviewQuestion": {
    "id": 1,
    "number": 1,
    "total": 20,
    "section": "Multiple Choice",
    "points": 1,
    "kind": "choice",
    "text": "Question 1: which statement about simplify an expression is correct?",
    "options": [
      {
        "key": "A",
        "text": "Simplify an Expression: option A"
      },
      {
        "key": "B",
        "text": "Simplify an Expression: option B"
      }
    ],
    "topic": "Simplify an Expression",
    "correct": "A",
    "explanation": "Option A is the correct statement about simplify an expression.",
    "chosen": "A",
    "result": "Correct",
    "pointsLabel": "Points: 1 / 1"
  },
  "reviewQuestions": [
    {
      "id": 1,
      "number": 1,
      "total": 20,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Question 1: which statement about simplify an expression is correct?",
      "options": [
        {
          "key": "A",
          "text": "Simplify an Expression: option A"
        },
        {
          "key": "B",
          "text": "Simplify an Expression: option B"
        }
      ],
      "topic": "Simplify an Expression",
      "correct": "A",
      "explanation": "Option A is the correct statement about simplify an expression.",
      "chosen": "A",
      "result": "Correct",
      "pointsLabel": "Points: 1 / 1"
    },
    {
      "id": 2,
      "number": 2,
      "total": 20,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Question 2: which statement about equations is correct?",
      "options": [
        {
          "key": "A",
          "text": "Equations: option A"
        },
        {
          "key": "B",
          "text": "Equations: option B"
        }
      ],
      "topic": "Equations",
      "correct": "A",
      "explanation": "Option A is the correct statement about equations.",
      "chosen": "A",
      "result": "Correct",
      "pointsLabel": "Points: 1 / 1"
    }
  ],
  "relatedLearning": [
    {
      "icon": "video",
      "kind": "Video",
      "meta": "12 min",
      "title": "Vertex Form of Quadratics"
    },
    {
      "icon": "practice",
      "kind": "Practice",
      "meta": "10 questions",
      "title": "Practice Problems: Graphing Parabolas"
    }
  ],
  "submission": null
}
            """;

    /** teacher form of an assessment (captured from a live create; questions trimmed to two) */
    public static final String TEACHER_ASSESSMENT = """
{
  "id": "unit-3-quiz-junior",
  "title": "Unit 3 Quiz",
  "subtitle": "Linear Functions",
  "courseId": "ap-precalc",
  "subject": "AP Pre Calculus",
  "gradeLevel": "Junior",
  "type": "Quiz",
  "teacher": "Dr John Anderson",
  "teacherUsername": "janderson",
  "color": "blue",
  "dueAt": "2026-10-05T23:59:00",
  "points": 100,
  "attemptsAllowed": 1,
  "timeLimitMinutes": 20,
  "instructionsIntro": "Answer every question. One attempt.",
  "instructionsSteps": [
    "Read each question carefully.",
    "Pick the best answer."
  ],
  "questions": [
    {
      "id": 62,
      "number": 1,
      "total": 3,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Which equation has slope 2 and y-intercept 1?",
      "options": [
        {
          "key": "A",
          "text": "y = 2x + 1"
        },
        {
          "key": "B",
          "text": "y = x + 2"
        },
        {
          "key": "C",
          "text": "y = 2x - 1"
        },
        {
          "key": "D",
          "text": "y = -2x + 1"
        }
      ],
      "topic": "Linear equations",
      "correct": "A",
      "explanation": "Slope-intercept form is y = mx + b."
    },
    {
      "id": 63,
      "number": 2,
      "total": 3,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "What is the slope of the line through (0,0) and (2,6)?",
      "options": [
        {
          "key": "A",
          "text": "2"
        },
        {
          "key": "B",
          "text": "3"
        },
        {
          "key": "C",
          "text": "6"
        },
        {
          "key": "D",
          "text": "1/3"
        }
      ],
      "topic": "Slope",
      "correct": "B",
      "explanation": null
    }
  ],
  "submissions": 0
}
            """;
    /** completed attempts on the teacher's assessments */
    public static final String SUBMISSIONS = """
[
  {
    "attemptId": 6,
    "assessmentId": "unit-3-quiz-junior",
    "title": "Unit 3 Quiz",
    "courseId": "ap-precalc",
    "subject": "AP Pre Calculus",
    "type": "Quiz",
    "gradeLevel": "Junior",
    "student": "student1",
    "status": "Completed",
    "completedOn": "Sep 26, 2026 at 11:00 PM",
    "score": "67%",
    "percent": 67,
    "correct": 2,
    "incorrect": 1,
    "skipped": 0,
    "total": 3,
    "secondsTaken": 300,
    "autoSubmitted": false,
    "points": 3,
    "pointsAwarded": null,
    "files": 0
  }
]
            """;
    /** per-attempt review (reviewQuestions trimmed to two) */
    public static final String SUBMISSION_REVIEW = """
{
  "attemptId": 6,
  "student": "student1",
  "id": "unit-3-quiz-junior",
  "title": "Unit 3 Quiz",
  "subtitle": "Linear Functions (Junior) · AP Pre Calculus",
  "completedOn": "Sep 26, 2026 at 11:00 PM",
  "questions": 3,
  "minutes": 5,
  "results": {
    "correct": 2,
    "total": 3,
    "percent": 67,
    "incorrect": 1,
    "skipped": 0,
    "timeTaken": "5:00",
    "pointsEarned": "2 / 3",
    "score": "67%",
    "status": "Completed",
    "autoSubmitted": false
  },
  "questionTopics": [
    {
      "n": 1,
      "topic": "Linear equations",
      "result": "correct"
    },
    {
      "n": 2,
      "topic": "Slope",
      "result": "correct"
    }
  ],
  "reviewQuestion": {
    "id": 62,
    "number": 1,
    "total": 3,
    "section": "Multiple Choice",
    "points": 1,
    "kind": "choice",
    "text": "Which equation has slope 2 and y-intercept 1?",
    "options": [
      {
        "key": "A",
        "text": "y = 2x + 1"
      },
      {
        "key": "B",
        "text": "y = x + 2"
      },
      {
        "key": "C",
        "text": "y = 2x - 1"
      },
      {
        "key": "D",
        "text": "y = -2x + 1"
      }
    ],
    "topic": "Linear equations",
    "correct": "A",
    "explanation": "Slope-intercept form is y = mx + b.",
    "chosen": "A",
    "result": "Correct",
    "pointsLabel": "Points: 1 / 1"
  },
  "reviewQuestions": [
    {
      "id": 62,
      "number": 1,
      "total": 3,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "Which equation has slope 2 and y-intercept 1?",
      "options": [
        {
          "key": "A",
          "text": "y = 2x + 1"
        },
        {
          "key": "B",
          "text": "y = x + 2"
        },
        {
          "key": "C",
          "text": "y = 2x - 1"
        },
        {
          "key": "D",
          "text": "y = -2x + 1"
        }
      ],
      "topic": "Linear equations",
      "correct": "A",
      "explanation": "Slope-intercept form is y = mx + b.",
      "chosen": "A",
      "result": "Correct",
      "pointsLabel": "Points: 1 / 1"
    },
    {
      "id": 63,
      "number": 2,
      "total": 3,
      "section": "Multiple Choice",
      "points": 1,
      "kind": "choice",
      "text": "What is the slope of the line through (0,0) and (2,6)?",
      "options": [
        {
          "key": "A",
          "text": "2"
        },
        {
          "key": "B",
          "text": "3"
        },
        {
          "key": "C",
          "text": "6"
        },
        {
          "key": "D",
          "text": "1/3"
        }
      ],
      "topic": "Slope",
      "correct": "B",
      "explanation": null,
      "chosen": "B",
      "result": "Correct",
      "pointsLabel": "Points: 1 / 1"
    }
  ],
  "relatedLearning": [],
  "submission": null
}
            """;
}
