package com.finnvek.active40.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

/** Näkymän otsikko (30sp), merkitty otsikoksi TalkBackille. */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = Active40Colors.onBackground,
        modifier = modifier.semantics { heading() },
    )
}

/** Osion otsikko (16sp). */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Koko näytön näkymä ilman alanavigaatiota: yläpalkki (takaisin + toiminnot), vierittyvä sisältö
 * ja valinnainen alapalkki (esim. tallennuspainike). Toimii 200 % fonttikoolla, koska sisältö vierittyy.
 */
@Composable
fun FullScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes backIconRes: Int = R.drawable.ic_back,
    backDescription: String = stringResource(R.string.action_back),
    headerCenter: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassIconButton(iconRes = backIconRes, contentDescription = backDescription, onClick = onBack)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { headerCenter() }
            actions()
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Active40Spacing.screenHorizontal)
                    .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackgroundMuted)
                }
                ScreenTitle(title)
            }
            content()
        }
        if (bottomBar != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Active40Spacing.screenHorizontal)
                        .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = bottomBar,
            )
        }
    }
}
