package com.onlyflows.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Tag
import com.onlyflows.data.repository.TagRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

data class TagsUiState(
    val tags: List<Tag>         = emptyList(),
    val unusedCount: Int        = 0,
    val usageCounts: Map<String, Int> = emptyMap(),
)

sealed interface TagsEvent {
    data class CreateTag(val name: String, val colorHex: String, val groupName: String? = null) : TagsEvent
    data class UpdateTag(val tag: Tag)                           : TagsEvent
    data class DeleteTag(val tagId: String)                      : TagsEvent
    data object DeleteUnused                                     : TagsEvent
    /** Persists a drag result: new folder + position for every tag that actually moved. */
    data class ApplyOrder(val tags: List<Tag>)                   : TagsEvent
    data class RenameFolder(val oldName: String, val newName: String?) : TagsEvent
}

class TagsViewModel(
    private val tagRepo: TagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tagRepo.observeAll(),
                tagRepo.observeUnused(),
            ) { all, unused ->
                Pair(all, unused.size)
            }.collect { (all, unusedCount) ->
                _uiState.update { it.copy(tags = all, unusedCount = unusedCount) }
                // Load usage counts for all tags
                all.forEach { tag -> loadUsageCount(tag.id) }
            }
        }
    }

    private fun loadUsageCount(tagId: String) {
        viewModelScope.launch {
            tagRepo.observeUsageCount(tagId)
                .distinctUntilChanged()
                .collect { count ->
                    _uiState.update { state ->
                        state.copy(usageCounts = state.usageCounts + (tagId to count))
                    }
                }
        }
    }

    fun onEvent(event: TagsEvent) {
        when (event) {
            is TagsEvent.CreateTag -> viewModelScope.launch {
                tagRepo.insert(
                    Tag(
                        id        = "tag_${UUID.randomUUID().toString().take(8)}",
                        name      = event.name,
                        colorHex  = event.colorHex,
                        groupName = event.groupName?.trim()?.takeIf { it.isNotEmpty() },
                        // New tags land at the end of the manual order
                        sortOrder = (_uiState.value.tags.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                        updatedAt = LocalDateTime.now(),
                    )
                )
            }
            is TagsEvent.UpdateTag -> viewModelScope.launch {
                tagRepo.update(event.tag.copy(updatedAt = LocalDateTime.now()))
            }
            is TagsEvent.DeleteTag -> viewModelScope.launch {
                tagRepo.delete(event.tagId)
            }
            TagsEvent.DeleteUnused -> viewModelScope.launch {
                tagRepo.deleteUnused()
            }
            is TagsEvent.ApplyOrder -> viewModelScope.launch {
                val before = _uiState.value.tags.associateBy { it.id }
                event.tags.forEach { tag ->
                    val old = before[tag.id] ?: return@forEach
                    if (old.sortOrder == tag.sortOrder && old.groupName == tag.groupName) return@forEach
                    tagRepo.update(tag.copy(updatedAt = LocalDateTime.now()))
                }
            }
            is TagsEvent.RenameFolder -> viewModelScope.launch {
                tagRepo.renameGroup(event.oldName, event.newName)
            }
        }
    }

    companion object {
        fun factory(app: OnlyFlowsApp) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TagsViewModel(app.container.tagRepository) as T
        }
    }
}
