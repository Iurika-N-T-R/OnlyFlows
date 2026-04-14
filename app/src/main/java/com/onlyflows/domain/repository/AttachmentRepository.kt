package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun observeByTask(taskId: String): Flow<List<Attachment>>
    suspend fun add(attachment: Attachment)
    suspend fun delete(id: String)
    suspend fun deleteByTask(taskId: String)
    suspend fun countByTask(taskId: String): Int
}
