package com.finnvek.active40.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finnvek.active40.Active40Application
import com.finnvek.active40.AppContainer

/**
 * Luo näkymän ViewModelin käsin sovelluksen AppContainerista (ei DI-kirjastoa).
 * CreationExtras-vastaanottajan kautta saa tarvittaessa createSavedStateHandle():n.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: CreationExtras.(AppContainer) -> VM): VM =
    viewModel(
        factory =
            viewModelFactory {
                initializer { create((this[APPLICATION_KEY] as Active40Application).container) }
            },
    )
