package cl.elboomlicanray.tvlauncher.presentation.modals.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String,
    val description: String
)

val AVAILABLE_LANGUAGES = listOf(
    LanguageOption("ES", "Español", "🇨🇱", "Predeterminado · Chile"),
    LanguageOption("EN", "English", "🇺🇸", "International Guests"),
    LanguageOption("PT", "Português", "🇧🇷", "Visitantes do Brasil"),
    LanguageOption("FR", "Français", "🇫🇷", "Visiteurs francophones"),
    LanguageOption("DE", "Deutsch", "🇩🇪", "Deutschsprachige Gäste")
)

/**
 * Diálogo de Selección de Idioma para Android TV (Español, English, Português, Français, Deutsch).
 * Totalmente navegable mediante el D-Pad del control remoto.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LanguageDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }
    var isVisible by remember { mutableStateOf(false) }
    val strings = remember(currentLanguage) { AppStringsCatalog.get(currentLanguage) }

    val availableLanguages = remember(strings) {
        listOf(
            LanguageOption("ES", "Español", "🇨🇱", strings.defaultCountryChile),
            LanguageOption("EN", "English", "🇺🇸", strings.internationalGuests),
            LanguageOption("PT", "Português", "🇧🇷", strings.brazilGuests),
            LanguageOption("FR", "Français", "🇫🇷", strings.frenchGuests),
            LanguageOption("DE", "Deutsch", "🇩🇪", strings.germanGuests)
        )
    }

    LaunchedEffect(Unit) {
        isVisible = true
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(140, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
            exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD030816)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(460.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvTokens.BgNavyPanel)
                        .border(1.5.dp, TvTokens.CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Título
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TvIcon(name = "globe", size = 20.dp, color = TvTokens.CyanNeon)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.selectLanguageTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Button(
                                onClick = onDismiss,
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.10f),
                                    contentColor = Color.White,
                                    focusedContainerColor = TvTokens.CyanNeon,
                                    focusedContentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(text = "✕", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Lista de idiomas (5 opciones)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableLanguages.forEachIndexed { index, lang ->
                                val isSelected = lang.code.equals(currentLanguage, ignoreCase = true)
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (index == 0) Modifier.focusRequester(initialFocusRequester)
                                            else Modifier
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isFocused -> Color(0x3300E5FF)
                                                isSelected -> Color(0x2238BDF8)
                                                else -> Color(0xFF071120)
                                            }
                                        )
                                        .border(
                                            width = if (isFocused) 1.8.dp else if (isSelected) 1.2.dp else 0.8.dp,
                                            color = when {
                                                isFocused -> TvTokens.CyanNeon
                                                isSelected -> Color(0xFF38BDF8)
                                                else -> Color(0xFF1E293B)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .focusable(interactionSource = interactionSource)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = {
                                                onLanguageSelected(lang.code)
                                                onDismiss()
                                            }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = lang.flag, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = lang.name,
                                                color = if (isFocused) TvTokens.CyanNeon else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = lang.description,
                                                color = TvTokens.TextMuted,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Text(
                                            text = strings.active,
                                            color = TvTokens.CyanNeon,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
