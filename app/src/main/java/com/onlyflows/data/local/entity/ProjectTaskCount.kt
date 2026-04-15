package com.onlyflows.data.local.entity

data class ProjectTaskCount(
    val projectId: String,
    val total: Int,
    val completed: Int,
)
