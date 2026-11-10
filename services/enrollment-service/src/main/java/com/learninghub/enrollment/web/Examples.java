package com.learninghub.enrollment.web;

/**
 * Response examples for the OpenAPI documentation (captured from the running stack against the
 * seed data and trimmed to a few items). Compile-time constants so they can be used in annotations.
 */
public final class Examples {
    private Examples() {}

    /** GET /classes — catalog with the caller's enrolment status and filters */
    public static final String CLASSES = """
{
  "term": {
    "start": "Aug 24, 2026",
    "end": "Oct 23, 2026",
    "name": "Q1 · Fall 2026"
  },
  "available": [
    {
      "id": "ap-biology",
      "name": "AP Biology",
      "gradeLevel": "Sophomore",
      "teacher": "Mr Tom Teacher",
      "teacherUsername": "teacher1",
      "color": "teal",
      "days": [
        "Mon",
        "Wed",
        "Fri"
      ],
      "start": "1:00 PM",
      "end": "2:00 PM",
      "credits": 4,
      "dates": "Aug 24 - Oct 23",
      "status": "available"
    },
    {
      "id": "ap-calc-ab",
      "name": "AP Calc AB",
      "gradeLevel": "Senior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Wed",
        "Fri"
      ],
      "start": "1:00 PM",
      "end": "2:00 PM",
      "credits": 4,
      "dates": "Aug 24 - Oct 23",
      "status": "available"
    },
    {
      "id": "ap-calc-bc",
      "name": "AP Calc BC",
      "gradeLevel": "Senior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Tue",
        "Wed"
      ],
      "start": "2:00 PM",
      "end": "3:00 PM",
      "credits": 4,
      "dates": "Aug 24 - Oct 23",
      "status": "available"
    }
  ],
  "filters": {
    "subjects": [
      {
        "color": "teal",
        "name": "AP Biology"
      },
      {
        "color": "blue",
        "name": "AP Calc AB"
      },
      {
        "color": "blue",
        "name": "AP Calc BC"
      }
    ],
    "days": [
      "Mon",
      "Tue",
      "Wed"
    ],
    "teachers": [
      "Mr Tom Teacher",
      "Dr John Anderson",
      "Dr Priya Patel"
    ],
    "gradeLevels": [
      "Freshman",
      "Sophomore",
      "Junior"
    ]
  }
}
            """;

    /** GET /my-classes as a student */
    public static final String MY_CLASSES_STUDENT = """
{
  "term": {
    "start": "Aug 24, 2026",
    "end": "Oct 23, 2026",
    "name": "Q1 · Fall 2026"
  },
  "role": "student",
  "totalCredits": "18 / 30",
  "classes": [
    {
      "id": "ap-precalc",
      "name": "AP Pre Calculus",
      "gradeLevel": "Junior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Tue",
        "Wed"
      ],
      "credits": 4,
      "time": "10:00 AM - 11:00 AM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "status": "Enrolled"
    },
    {
      "id": "ap-chemistry",
      "name": "AP Chemistry",
      "gradeLevel": "Junior",
      "teacher": "Dr Priya Patel",
      "teacherUsername": "ppatel",
      "color": "red",
      "days": [
        "Mon",
        "Wed",
        "Fri"
      ],
      "credits": 4,
      "time": "8:00 AM - 9:00 AM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "status": "Enrolled"
    },
    {
      "id": "ap-english-lit",
      "name": "AP English Literature",
      "gradeLevel": "Junior",
      "teacher": "Ms Sarah Wilson",
      "teacherUsername": "swilson",
      "color": "green",
      "days": [
        "Mon",
        "Wed",
        "Fri"
      ],
      "credits": 3,
      "time": "1:00 PM - 2:00 PM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "status": "Enrolled"
    }
  ]
}
            """;

    /** GET /my-classes as a teacher (status Teaching, studentCount) */
    public static final String MY_CLASSES_TEACHER = """
{
  "term": {
    "start": "Aug 24, 2026",
    "end": "Oct 23, 2026",
    "name": "Q1 · Fall 2026"
  },
  "role": "teacher",
  "totalCredits": "6 classes",
  "classes": [
    {
      "id": "ap-calc-ab",
      "name": "AP Calc AB",
      "gradeLevel": "Senior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Wed",
        "Fri"
      ],
      "credits": 4,
      "time": "1:00 PM - 2:00 PM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "studentCount": 5,
      "status": "Teaching"
    },
    {
      "id": "ap-calc-bc",
      "name": "AP Calc BC",
      "gradeLevel": "Senior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Tue",
        "Wed"
      ],
      "credits": 4,
      "time": "2:00 PM - 3:00 PM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "studentCount": 5,
      "status": "Teaching"
    },
    {
      "id": "ap-precalc",
      "name": "AP Pre Calculus",
      "gradeLevel": "Junior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Tue",
        "Wed"
      ],
      "credits": 4,
      "time": "10:00 AM - 11:00 AM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "studentCount": 5,
      "status": "Teaching"
    }
  ]
}
            """;

    /** GET /state — enrolled course ids (students only) */
    public static final String STATE = """
{
  "enrolled": [
    "ap-precalc",
    "ap-chemistry",
    "ap-english-lit",
    "ap-physics-2",
    "ap-statistics"
  ]
}
            """;

    /** POST /enroll/{courseId} */
    public static final String ENROLLED = """
{
  "status": "enrolled",
  "classId": "ap-statistics"
}
            """;

    /** DELETE /drop/{courseId} */
    public static final String DROPPED = """
{
  "status": "dropped",
  "classId": "ap-statistics"
}
            """;

    /** GET /subjects (admin) */
    public static final String SUBJECTS = """
[
  {
    "id": "ap-biology",
    "name": "AP Biology",
    "gradeLevel": "sophomore",
    "gradeLevelName": "Sophomore",
    "teacher": "Mr Tom Teacher",
    "teacherUsername": "teacher1",
    "termId": "q1-2026",
    "termName": "Q1 · Fall 2026",
    "days": [
      "Mon",
      "Wed",
      "Fri"
    ],
    "startTime": "13:00",
    "endTime": "14:00",
    "time": "1:00 PM - 2:00 PM",
    "credits": 4,
    "studentCount": 4,
    "color": "teal",
    "startDate": "Aug 24, 2026",
    "endDate": "Oct 23, 2026"
  },
  {
    "id": "ap-calc-ab",
    "name": "AP Calc AB",
    "gradeLevel": "senior",
    "gradeLevelName": "Senior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "termId": "q1-2026",
    "termName": "Q1 · Fall 2026",
    "days": [
      "Mon",
      "Wed",
      "Fri"
    ],
    "startTime": "13:00",
    "endTime": "14:00",
    "time": "1:00 PM - 2:00 PM",
    "credits": 4,
    "studentCount": 5,
    "color": "blue",
    "startDate": "Aug 24, 2026",
    "endDate": "Oct 23, 2026"
  },
  {
    "id": "ap-calc-bc",
    "name": "AP Calc BC",
    "gradeLevel": "senior",
    "gradeLevelName": "Senior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "termId": "q1-2026",
    "termName": "Q1 · Fall 2026",
    "days": [
      "Mon",
      "Tue",
      "Wed"
    ],
    "startTime": "14:00",
    "endTime": "15:00",
    "time": "2:00 PM - 3:00 PM",
    "credits": 4,
    "studentCount": 5,
    "color": "blue",
    "startDate": "Aug 24, 2026",
    "endDate": "Oct 23, 2026"
  }
]
            """;

    /** GET/POST/PUT /subjects/{id} (admin) */
    public static final String SUBJECT = """
{
  "id": "ap-precalc",
  "name": "AP Pre Calculus",
  "gradeLevel": "junior",
  "gradeLevelName": "Junior",
  "teacher": "Dr John Anderson",
  "teacherUsername": "janderson",
  "termId": "q1-2026",
  "termName": "Q1 · Fall 2026",
  "days": [
    "Mon",
    "Tue",
    "Wed"
  ],
  "startTime": "10:00",
  "endTime": "11:00",
  "time": "10:00 AM - 11:00 AM",
  "credits": 4,
  "studentCount": 5,
  "color": "blue",
  "startDate": "Aug 24, 2026",
  "endDate": "Oct 23, 2026"
}
            """;

    /** GET /roster (admin/teacher) */
    public static final String ROSTER = """
[
  {
    "id": "ap-cs",
    "name": "AP Computer Science",
    "gradeLevel": "freshman",
    "gradeLevelName": "Freshman",
    "teacher": "Mr David Lee",
    "teacherUsername": "dlee",
    "days": [
      "Mon",
      "Wed",
      "Fri"
    ],
    "time": "1:00 PM - 2:00 PM",
    "studentCount": 4,
    "students": [
      {
        "username": "ajohnson",
        "enrolledAt": "2026-08-24T09:00:00"
      },
      {
        "username": "lbrown",
        "enrolledAt": "2026-08-24T09:00:00"
      },
      {
        "username": "mdavis",
        "enrolledAt": "2026-08-24T09:00:00"
      },
      {
        "username": "nwilson",
        "enrolledAt": "2026-08-24T09:00:00"
      }
    ]
  },
  {
    "id": "geometry",
    "name": "Geometry",
    "gradeLevel": "freshman",
    "gradeLevelName": "Freshman",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "days": [
      "Mon",
      "Wed",
      "Fri"
    ],
    "time": "8:00 AM - 9:00 AM",
    "studentCount": 4,
    "students": [
      {
        "username": "lbrown",
        "enrolledAt": "2026-08-24T09:00:00"
      },
      {
        "username": "mdavis",
        "enrolledAt": "2026-08-24T09:00:00"
      }
    ]
  },
  {
    "id": "ap-precalc",
    "name": "AP Pre Calculus",
    "gradeLevel": "junior",
    "gradeLevelName": "Junior",
    "teacher": "Dr John Anderson",
    "teacherUsername": "janderson",
    "days": [
      "Mon",
      "Tue",
      "Wed",
      "Thu",
      "Fri"
    ],
    "time": "10:00 AM - 11:00 AM",
    "studentCount": 4,
    "students": [
      {
        "username": "student1",
        "enrolledAt": "2026-08-24T09:00:00"
      },
      {
        "username": "slewis",
        "enrolledAt": "2026-08-24T09:00:00"
      }
    ]
  }
]
            """;

    /** GET /grade-levels */
    public static final String GRADE_LEVELS = """
[
  {
    "id": "freshman",
    "name": "Freshman",
    "gradeNumber": 9,
    "sortOrder": 1
  },
  {
    "id": "sophomore",
    "name": "Sophomore",
    "gradeNumber": 10,
    "sortOrder": 2
  },
  {
    "id": "junior",
    "name": "Junior",
    "gradeNumber": 11,
    "sortOrder": 3
  },
  {
    "id": "senior",
    "name": "Senior",
    "gradeNumber": 12,
    "sortOrder": 4
  }
]
            """;

    /** captured from a live call (trimmed to two items) */
    public static final String MY_CLASSES_ADMIN = """
{
  "term": {
    "start": "Aug 24, 2026",
    "name": "Q1 · Fall 2026",
    "end": "Oct 23, 2026"
  },
  "role": "admin",
  "totalCredits": "18 classes",
  "classes": [
    {
      "id": "ap-biology",
      "name": "AP Biology",
      "gradeLevel": "Sophomore",
      "teacher": "Mr Tom Teacher",
      "teacherUsername": "teacher1",
      "color": "teal",
      "days": [
        "Mon",
        "Wed"
      ],
      "credits": 4,
      "time": "1:00 PM - 2:00 PM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "studentCount": 4,
      "status": "Active"
    },
    {
      "id": "ap-calc-ab",
      "name": "AP Calc AB",
      "gradeLevel": "Senior",
      "teacher": "Dr John Anderson",
      "teacherUsername": "janderson",
      "color": "blue",
      "days": [
        "Mon",
        "Wed"
      ],
      "credits": 4,
      "time": "1:00 PM - 2:00 PM",
      "duration": "1 hr",
      "startDate": "Aug 24, 2026",
      "endDate": "Oct 23, 2026",
      "studentCount": 5,
      "status": "Active"
    }
  ]
}
            """;

    /** GET /courses — the plain catalogue (Ask 46) */
    public static final String COURSES = """
[
  {
    "id": "ap-biology",
    "termId": "q1-2026",
    "name": "AP Biology",
    "gradeLevel": "sophomore",
    "gradeLevelName": "Sophomore",
    "teacherUsername": "teacher1",
    "teacher": "Mr Tom Teacher",
    "credits": 4,
    "color": "teal"
  },
  {
    "id": "ap-calc-ab",
    "termId": "q1-2026",
    "name": "AP Calc AB",
    "gradeLevel": "senior",
    "gradeLevelName": "Senior",
    "teacherUsername": "janderson",
    "teacher": "Dr John Anderson",
    "credits": 4,
    "color": "blue"
  }
]
            """;
}
