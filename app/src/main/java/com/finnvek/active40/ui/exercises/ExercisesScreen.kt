package com.finnvek.active40.ui.exercises

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.R
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.ManageNamesScreen
import com.finnvek.active40.ui.components.ManageNamesTexts
import com.finnvek.active40.ui.components.ManagedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Yhteinen ViewModel liikkeiden ja oireiden hallintaan; erot annetaan funktioina. */
class ManageNamesViewModel<T>(
    source: Flow<List<T>>,
    private val toManaged: (T) -> ManagedName,
    private val addItem: suspend (String) -> Unit,
    private val renameItem: suspend (T, String) -> Unit,
    private val archiveItem: suspend (T, Boolean) -> Unit,
) : ViewModel() {
    private val all = source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<ManagedName>> =
        all.map { list -> list.map(toManaged) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun find(item: ManagedName) = all.value.firstOrNull { toManaged(it).id == item.id }

    fun add(name: String) {
        viewModelScope.launch { addItem(name) }
    }

    fun rename(
        item: ManagedName,
        name: String,
    ) {
        val target = find(item) ?: return
        viewModelScope.launch { renameItem(target, name) }
    }

    fun setArchived(
        item: ManagedName,
        archived: Boolean,
    ) {
        val target = find(item) ?: return
        viewModelScope.launch { archiveItem(target, archived) }
    }
}

@Composable
fun ExercisesScreen(onBack: () -> Unit) {
    val viewModel =
        appViewModel { c ->
            val repo = c.exerciseRepository
            ManageNamesViewModel(
                repo.observeAll(),
                { ManagedName(it.id, it.name, it.isBuiltIn, it.isArchived) },
                { repo.add(it) },
                repo::rename,
                repo::setArchived,
            )
        }
    val items by viewModel.items.collectAsStateWithLifecycle()
    ManageNamesScreen(
        texts =
            ManageNamesTexts(
                title = stringResource(R.string.exercises_title),
                add = stringResource(R.string.exercises_add),
                newTitle = stringResource(R.string.exercises_new_title),
                renameTitle = stringResource(R.string.exercises_rename_title),
                nameLabel = stringResource(R.string.exercises_name_label),
                nameTaken = stringResource(R.string.exercises_name_taken),
                archivedHint = stringResource(R.string.exercises_archived_hint),
            ),
        items = items,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onSetArchived = viewModel::setArchived,
    )
}

@Composable
fun SymptomsScreen(onBack: () -> Unit) {
    val viewModel =
        appViewModel { c ->
            val repo = c.symptomRepository
            ManageNamesViewModel(
                repo.observeAll(),
                { ManagedName(it.id, it.name, it.isBuiltIn, it.isArchived) },
                { repo.add(it) },
                repo::rename,
                repo::setArchived,
            )
        }
    val items by viewModel.items.collectAsStateWithLifecycle()
    ManageNamesScreen(
        texts =
            ManageNamesTexts(
                title = stringResource(R.string.symptoms_title),
                add = stringResource(R.string.symptoms_add),
                newTitle = stringResource(R.string.symptoms_new_title),
                renameTitle = stringResource(R.string.symptoms_rename_title),
                nameLabel = stringResource(R.string.checkin_symptom_name),
                nameTaken = stringResource(R.string.symptoms_name_taken),
                archivedHint = stringResource(R.string.symptoms_archived_hint),
            ),
        items = items,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onSetArchived = viewModel::setArchived,
    )
}
