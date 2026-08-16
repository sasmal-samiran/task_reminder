package com.myreminder.app.data.model

enum class TaskType(val displayName: String) {
    DEADLINE("Deadline"),
    INTERVIEW("Interview"),
    TECHNICAL_ROUND("Technical Round"),
    HR_ROUND("HR Round"),
    CODING_TEST("Coding Test"),
    APTITUDE_TEST("Aptitude Test"),
    EXAM("Exam"),
    OTHER("Other")
}
