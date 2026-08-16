package com.myreminder.app.data.model

enum class TaskType(val displayName: String) {
    INTERVIEW("Interview"),
    TECHNICAL_ROUND("Technical Round"),
    HR_ROUND("HR Round"),
    CODING_TEST("Coding Test"),
    APTITUDE_TEST("Aptitude Test"),
    DEADLINE("Deadline"),
    EXAM("Exam"),
    OTHER("Other")
}
