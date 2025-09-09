package com.devstart.startrescue.model

data class Option(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false
)

data class Question(
    val id: String,
    val text: String,
    val options: List<Option>,
    val requiresTourniquet: Boolean = false
)
