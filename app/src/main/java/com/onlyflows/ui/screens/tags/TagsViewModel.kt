package com.onlyflows.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.repository.TagRepository
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
    data class CreateTag(val name: String, val colorHex: String) : TagsEvent
    data class UpdateTag(val tag: Tag)                           : TagsEvent
    data class DeleteTag(val tagId: String)                      : TagsEvent
    data object DeleteUnused                                     : TagsEvent
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
