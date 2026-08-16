package com.myreminder.app.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreminder.app.data.local.AppDatabase
import com.myreminder.app.data.local.TaskEntity
import com.myreminder.app.data.model.TaskType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).taskDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<TaskEntity>> = combine(_searchQuery, _selectedFilter) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val results = if (query.isBlank()) {
            dao.getAllTasks()
        } else {
            dao.searchTasks(query)
        }
        
        results.map { list ->
            when (filter) {
                "Interviews" -> list.filter { it.type == TaskType.INTERVIEW || it.type == TaskType.TECHNICAL_ROUND || it.type == TaskType.HR_ROUND }
                "Coding Tests" -> list.filter { it.type == TaskType.CODING_TEST }
                "Deadlines" -> list.filter { it.type == TaskType.DEADLINE }
                "Completed" -> list.filter { it.completed }
                else -> list // "All"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }
}
