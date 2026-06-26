package com.aruuu.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val isLoading: StateFlow<Boolean> = uiState.map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    init {
        viewModelScope.launch {
            repository.settings.first().let { settings ->
                _uiState.value = MainUiState(
                    isLoading = false,
                    onboardingComplete = settings.onboardingComplete,
                    themeMode = settings.themeMode,
                )
            }
        }
    }
}
