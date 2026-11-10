package com.learninghub.learning.web;

/** Swagger response examples for the learning views (Ask 25: term lesson plans). */
public final class Examples {
    private Examples() {}

    public static final String OVERVIEW_STUDENT = """
{
  "term": {"name": "Q1 · Fall 2026", "start": "Aug 24, 2026", "end": "Oct 23, 2026"},
  "stats": [
    {"label": "Enrolled Classes", "value": "5", "sub": "This term", "color": "blue", "icon": "classes"},
    {"label": "Classes Completed", "value": "71 / 130", "sub": "59 remaining", "color": "green", "icon": "check", "progress": 55},
    {"label": "Next Class", "value": "Mon, Sep 28", "sub": "AP Chemistry · class #16", "color": "purple", "icon": "schedule"},
    {"label": "Study Hours This Week", "value": "5.5", "sub": "+10% from last week", "color": "red", "icon": "hours"}
  ],
  "role": "student"
}
            """;

    public static final String OVERVIEW_TEACHER = """
{
  "term": {"name": "Q1 · Fall 2026", "start": "Aug 24, 2026", "end": "Oct 23, 2026"},
  "stats": [
    {"label": "Courses Taught", "value": "6", "sub": "This term", "color": "blue", "icon": "classes"},
    {"label": "Classes This Term", "value": "162", "sub": "Across my courses", "color": "green", "icon": "lessons"},
    {"label": "Classes Completed", "value": "84 / 162", "sub": "Marked complete", "color": "purple", "icon": "check", "progress": 52},
    {"label": "Next Class", "value": "Mon, Sep 28", "sub": "Geometry · class #16", "color": "red", "icon": "schedule"}
  ],
  "role": "teacher"
}
            """;

    public static final String OVERVIEW_ADMIN = """
{
  "term": {"name": "Q1 · Fall 2026", "start": "Aug 24, 2026", "end": "Oct 23, 2026"},
  "stats": [
    {"label": "Courses", "value": "18", "sub": "All teachers", "color": "blue", "icon": "classes"},
    {"label": "Classes This Term", "value": "468", "sub": "Across all courses", "color": "green", "icon": "lessons"},
    {"label": "Classes Completed", "value": "240 / 468", "sub": "Marked by teachers", "color": "purple", "icon": "check", "progress": 51},
    {"label": "Study Hours This Week", "value": "12", "sub": "All students", "color": "red", "icon": "hours"}
  ],
  "role": "admin"
}
            """;

    /** GET /classes — student: enrolled courses */
    public static final String CLASSES_STUDENT = """
[
  {
    "id": "ap-precalc",
    "name": "AP Pre Calculus",
    "gradeLevel": "Junior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "color": "blue",
    "classes": 27,
    "classesCompleted": 14,
    "classesRemaining": 13,
    "percent": 52,
    "status": "in-progress",
    "nextClass": {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": false, "completedAt": null, "completedBy": null, "status": "Not completed"},
    "canMark": false
  }
]
            """;

    /** GET /classes — teacher: own courses (canMark) */
    public static final String CLASSES_TEACHER = """
[
  {
    "id": "ap-precalc",
    "name": "AP Pre Calculus",
    "gradeLevel": "Junior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "color": "blue",
    "classes": 27,
    "classesCompleted": 14,
    "classesRemaining": 13,
    "percent": 52,
    "status": "in-progress",
    "nextClass": {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": false, "completedAt": null, "completedBy": null, "status": "Not completed"},
    "canMark": true
  }
]
            """;

    /** GET /classes — admin: all courses (canMark) */
    public static final String CLASSES_ADMIN = """
[
  {
    "id": "ap-precalc",
    "name": "AP Pre Calculus",
    "gradeLevel": "Junior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "color": "blue",
    "classes": 27,
    "classesCompleted": 14,
    "classesRemaining": 13,
    "percent": 52,
    "status": "in-progress",
    "nextClass": {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": false, "completedAt": null, "completedBy": null, "status": "Not completed"},
    "canMark": true
  }
]
            """;

    /** GET /plan/{courseId} (items trimmed to 3) */
    public static final String PLAN = """
{
  "id": "ap-precalc",
  "name": "AP Pre Calculus",
  "gradeLevel": "Junior",
  "teacher": "Dr John Anderson",
  "teacherUsername": "janderson",
  "color": "blue",
  "classes": 27,
  "classesCompleted": 14,
  "classesRemaining": 13,
  "percent": 52,
  "status": "in-progress",
  "nextClass": {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": false, "completedAt": null, "completedBy": null, "status": "Not completed"},
  "canMark": false,
  "term": {"name": "Q1 · Fall 2026", "start": "Aug 24, 2026", "end": "Oct 23, 2026"},
  "items": [
    {"id": 47, "classNumber": 1, "date": "2026-08-24", "dateLabel": "Mon, Aug 24", "title": "Unit 1: Functions and Graphs", "topic": "Functions and Graphs — lesson 1", "completed": true, "completedAt": "Aug 24, 2026 at 4:00 PM", "completedBy": "janderson", "status": "Completed"},
    {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": false, "completedAt": null, "completedBy": null, "status": "Not completed"},
    {"id": 62, "classNumber": 16, "date": "2026-09-28", "dateLabel": "Mon, Sep 28", "title": "Unit 6: Trigonometric Functions", "topic": "Trigonometric Functions — lesson 1", "completed": false, "completedAt": null, "completedBy": null, "status": "Today"}
  ]
}
            """;

    /** PUT /plan/items/{id} */
    public static final String PLAN_ITEM_SET = """
{
  "item": {"id": 61, "classNumber": 15, "date": "2026-09-25", "dateLabel": "Fri, Sep 25", "title": "Unit 5: Logarithmic Functions", "topic": "Logarithmic Functions — lesson 3 (unit review)", "completed": true, "completedAt": "Sep 28, 2026 at 9:10 AM", "completedBy": "janderson", "status": "Completed"},
  "courseId": "ap-precalc",
  "coursePercent": 56,
  "courseStatus": "in-progress",
  "completed": 15,
  "total": 27
}
            """;

}
