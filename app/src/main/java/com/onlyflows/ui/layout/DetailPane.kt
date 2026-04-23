package com.onlyflows.ui.layout

sealed class DetailPane {
    data object None                                                         : DetailPane()
    data class Task(val id: String)                                          : DetailPane()
    data class Project(val id: String)                                       : DetailPane()
    data class Note(val noteId: String, val projectId: String)               : DetailPane()
}
