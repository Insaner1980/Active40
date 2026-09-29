package com.finnvek.active40.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.active40.R
import com.finnvek.active40.ui.theme.Active40Colors

/** Hallittava nimi (liike tai oire). Sisäänrakennettuja ei voi nimetä uudelleen, vain arkistoida. */
data class ManagedName(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean,
)

/** Tekstit, jotka erottavat liikkeiden ja oireiden hallinnan toisistaan. */
data class ManageNamesTexts(
    val title: String,
    val add: String,
    val newTitle: String,
    val renameTitle: String,
    val nameLabel: String,
    val nameTaken: String,
    val archivedHint: String,
)

private sealed interface NameDialog {
    data object Add : NameDialog

    data class Rename(
        val item: ManagedName,
    ) : NameDialog
}

/** Yhteinen hallintanäkymä: lisää, nimeä omat uudelleen, arkistoi ja palauta. */
@Composable
fun ManageNamesScreen(
    texts: ManageNamesTexts,
    items: List<ManagedName>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (ManagedName, String) -> Unit,
    onSetArchived: (ManagedName, Boolean) -> Unit,
) {
    var dialog by remember { mutableStateOf<NameDialog?>(null) }
    val (archived, active) = items.partition { it.isArchived }

    // Nimi on varattu, jos jollain muulla (myös arkistoidulla) on sama nimi kirjainkoosta riippumatta.
    fun validator(exceptId: Long?): (String) -> String? =
        { name -> if (items.any { it.id != exceptId && it.name.equals(name, ignoreCase = true) }) texts.nameTaken else null }

    FullScreen(title = texts.title, onBack = onBack) {
        PillButton(
            text = texts.add,
            onClick = { dialog = NameDialog.Add },
            iconRes = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
        active.forEach { item ->
            NameRow(
                item,
                buildList {
                    if (!item.isBuiltIn) add(MenuAction(stringResource(R.string.action_rename)) { dialog = NameDialog.Rename(item) })
                    add(MenuAction(stringResource(R.string.action_archive)) { onSetArchived(item, true) })
                },
            )
        }
        if (archived.isNotEmpty()) {
            SectionTitle(stringResource(R.string.exercises_archived_section))
            Text(texts.archivedHint, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackgroundMuted)
            archived.forEach { item ->
                NameRow(item, listOf(MenuAction(stringResource(R.string.action_restore)) { onSetArchived(item, false) }))
            }
        }
    }

    when (val current = dialog) {
        NameDialog.Add -> {
            TextInputDialog(
                title = texts.newTitle,
                label = texts.nameLabel,
                confirmText = stringResource(R.string.action_add),
                onConfirm = {
                    onAdd(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
                validate = validator(null),
            )
        }

        is NameDialog.Rename -> {
            TextInputDialog(
                title = texts.renameTitle,
                label = texts.nameLabel,
                confirmText = stringResource(R.string.action_save),
                initialValue = current.item.name,
                onConfirm = {
                    onRename(current.item, it)
                    dialog = null
                },
                onDismiss = { dialog = null },
                validate = validator(current.item.id),
            )
        }

        null -> {}
    }
}

@Composable
private fun NameRow(
    item: ManagedName,
    actions: List<MenuAction>,
) {
    GlassListRow(title = item.name) {
        OverflowMenu(stringResource(R.string.action_options_for, item.name), actions)
    }
}
