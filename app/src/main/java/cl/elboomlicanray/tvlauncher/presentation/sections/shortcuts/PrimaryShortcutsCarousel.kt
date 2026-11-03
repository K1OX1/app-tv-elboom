package cl.elboomlicanray.tvlauncher.presentation.sections.shortcuts

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import cl.elboomlicanray.tvlauncher.config.QuickAccessConfig
import cl.elboomlicanray.tvlauncher.config.QuickAccessItem
import cl.elboomlicanray.tvlauncher.presentation.components.cards.PrimaryShortcutCard

/**
 * Fila Fija de Accesos Directos Principales (PrimaryShortcutsCarousel).
 *
 * Responsabilidad:
 * Renderizar las tarjetas de accesos rápidos del sistema traducidas al idioma activo,
 * garantizando simetría y foco nítido para TV.
 */
@Composable
fun PrimaryShortcutsCarousel(
    selectedLanguage: String = "ES",
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onShortcutClick: (QuickAccessItem) -> Unit
) {
    val shortcuts = remember(selectedLanguage) {
        QuickAccessConfig.getShortcuts(selectedLanguage)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        shortcuts.forEachIndexed { index, item ->
            PrimaryShortcutCard(
                item = item,
                focusRequester = if (index == 0) initialFocusRequester else null,
                onClick = { onShortcutClick(item) }
            )
        }
    }
}
