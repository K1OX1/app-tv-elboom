package cl.elboomlicanray.tvlauncher.presentation.modals.apps

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.presentation.components.rememberNativeDrawablePainter
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

private const val APPS_PER_PAGE = 10
private const val COLUMNS_PER_ROW = 5

/**
 * Sección "Apps & Entretenimiento" optimizada para máxima fluidez en Android TV
 * y soporte completo de ordenamiento interactivo por D-Pad.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamingAppsDialog(
    apps: List<AppItem>,
    selectedLanguage: String = "ES",
    isSmartDnsEnabled: Boolean = false,
    onAppClick: (AppItem) -> Unit,
    onReorderApps: ((List<AppItem>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) {
        cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(selectedLanguage)
    }

    // Lista mutable reactiva para ordenamiento en vivo
    var currentAppsList by remember(apps) { mutableStateOf(apps) }

    // Estados de modo organización / reordenamiento
    val isReorderMode = onReorderApps != null
    var movingAppPackage by remember { mutableStateOf<String?>(null) }

    val totalPages = remember(currentAppsList) {
        if (currentAppsList.isEmpty()) 1 else (currentAppsList.size + APPS_PER_PAGE - 1) / APPS_PER_PAGE
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var targetFocusIndex by remember { mutableIntStateOf(0) }
    var shouldApplyFocus by remember { mutableStateOf(false) }

    val appFocusRequesters = remember { List(APPS_PER_PAGE) { FocusRequester() } }
    val organizeButtonFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    val prevButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }

    var isVisible by remember { mutableStateOf(false) }

    // Función para navegar entre páginas
    fun navigateToPage(newPage: Int, focusIndex: Int) {
        if (newPage in 0 until totalPages) {
            currentPage = newPage
            targetFocusIndex = focusIndex
            shouldApplyFocus = true
        }
    }

    // Función de intercambio de posición con D-Pad en Modo Organizar
    fun moveApp(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentAppsList.indices || toIndex !in currentAppsList.indices || fromIndex == toIndex) return
        val updated = currentAppsList.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        currentAppsList = updated

        // Verificar si cruzó a otra página
        val newPage = toIndex / APPS_PER_PAGE
        val newFocusIdx = toIndex % APPS_PER_PAGE
        currentPage = newPage
        targetFocusIndex = newFocusIdx
        shouldApplyFocus = true
    }

    // Asegurar límites de página si la lista cambia
    LaunchedEffect(totalPages) {
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }
    }

    // Aplicar foco inmediato (sin demoras que causen lag)
    LaunchedEffect(currentPage, shouldApplyFocus) {
        if (shouldApplyFocus) {
            try {
                val currentCount = minOf(APPS_PER_PAGE, maxOf(1, currentAppsList.size - currentPage * APPS_PER_PAGE))
                val safeIndex = targetFocusIndex.coerceIn(0, currentCount - 1)
                appFocusRequesters.getOrNull(safeIndex)?.requestFocus()
            } catch (_: Exception) {}
            shouldApplyFocus = false
        }
    }

    // Entrada animada
    LaunchedEffect(Unit) {
        isVisible = true
        try {
            appFocusRequesters.firstOrNull()?.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        BackHandler(onBack = onDismiss)

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(140, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(140, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.97f)
        ) {
            // Fondo con degradado vertical ultra-eficiente en GPU
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF102C36),
                                Color(0xFF050B18),
                                Color(0xFF020612)
                            )
                        )
                    )
                    .padding(horizontal = 48.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // =========================================================================
                    // 1. ENCABEZADO: TÍTULO, SUBTÍTULO, BOTÓN ORGANIZAR Y BOTÓN CERRAR
                    // =========================================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.width(4.dp).height(48.dp).background(TvTokens.CyanNeon, RoundedCornerShape(2.dp)))
                                Column {
                                Text(
                                    text = if (isReorderMode) "Organizar aplicaciones" else strings.streamingDialogTitle,
                                    style = androidx.tv.material3.MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isReorderMode) "Administrador · Selecciona una app para moverla" else "${strings.streamingDialogSubtitle} · ${currentAppsList.size}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isSmartDnsEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "🇺🇸 Netflix USA Activo",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                }

                                // Botón "⇅ Organizar" / "✓ Listo"
                                if (isReorderMode) OrganizeModePillButton(
                                    isReorderMode = isReorderMode,
                                    focusRequester = organizeButtonFocusRequester,
                                    onRight = { closeButtonFocusRequester.requestFocus(); true },
                                    onDown = {
                                        val safeCount = minOf(APPS_PER_PAGE, maxOf(1, currentAppsList.size - currentPage * APPS_PER_PAGE))
                                        val targetIndex = minOf(1, safeCount - 1)
                                        try { appFocusRequesters[targetIndex].requestFocus() } catch (_: Exception) {}
                                    },
                                    onClick = {
                                        onReorderApps?.invoke(currentAppsList)
                                        onDismiss()
                                    }
                                )

                                // Botón "✕ Cerrar"
                                CloseModalPillButton(
                                    focusRequester = closeButtonFocusRequester,
                                    label = strings.close,
                                    onLeft = { if (isReorderMode) organizeButtonFocusRequester.requestFocus(); isReorderMode },
                                    onDown = {
                                        val safeCount = minOf(APPS_PER_PAGE, maxOf(1, currentAppsList.size - currentPage * APPS_PER_PAGE))
                                        val targetIndex = minOf(COLUMNS_PER_ROW - 1, safeCount - 1)
                                        try { appFocusRequesters[targetIndex].requestFocus() } catch (_: Exception) {}
                                    },
                                    onClick = onDismiss
                                )
                            }
                        }

                        // Banner de instrucción cuando el modo organizar está activo
                        if (isReorderMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TvTokens.CyanNeon.copy(alpha = 0.14f))
                                    .border(1.dp, TvTokens.CyanNeon.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (movingAppPackage != null) {
                                        "Moviendo app: Usa las flechas ◀ ▶ ▲ ▼ del control para reubicarla. Presiona OK para fijar posición."
                                    } else {
                                        "Modo Organizar: Presiona OK sobre una app para comenzar a moverla con las flechas, o '✓ Listo' para guardar."
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // 2. CUADRÍCULA COMPACTA DE 5 COLUMNAS X 2 FILAS (10 TILES POR PÁGINA)
                    // =========================================================================
                    val pageApps = remember(currentAppsList, currentPage) {
                        currentAppsList.drop(currentPage * APPS_PER_PAGE).take(APPS_PER_PAGE)
                    }

                    val row1Apps = remember(pageApps) { pageApps.take(COLUMNS_PER_ROW) }
                    val row2Apps = remember(pageApps) { pageApps.drop(COLUMNS_PER_ROW).take(COLUMNS_PER_ROW) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // FILA 1 (Índices 0..4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            row1Apps.forEachIndexed { colIndex, app ->
                                val overallIndex = colIndex
                                val isMovingThis = isReorderMode && movingAppPackage == app.packageName

                                AppLandscapeTile(
                                    app = app,
                                    isReorderMode = isReorderMode,
                                    isBeingMoved = isMovingThis,
                                    focusRequester = appFocusRequesters[overallIndex],
                                    onClick = {
                                        if (isReorderMode) {
                                            movingAppPackage = if (isMovingThis) null else app.packageName
                                        } else {
                                            onAppClick(app)
                                        }
                                    },
                                    onDirectionRight = {
                                        if (isMovingThis) {
                                            val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                            if (currentIdx < currentAppsList.size - 1) {
                                                moveApp(currentIdx, currentIdx + 1)
                                                true
                                            } else false
                                        } else {
                                            if (overallIndex < pageApps.size - 1) {
                                                appFocusRequesters[overallIndex + 1].requestFocus()
                                                true
                                            } else if (currentPage < totalPages - 1) {
                                                navigateToPage(currentPage + 1, focusIndex = 0)
                                                true
                                            } else false
                                        }
                                    },
                                    onDirectionLeft = {
                                        if (isMovingThis) {
                                            val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                            if (currentIdx > 0) {
                                                moveApp(currentIdx, currentIdx - 1)
                                                true
                                            } else false
                                        } else {
                                            if (overallIndex > 0) {
                                                appFocusRequesters[overallIndex - 1].requestFocus()
                                                true
                                            } else if (currentPage > 0) {
                                                val prevCount = minOf(APPS_PER_PAGE, currentAppsList.size - (currentPage - 1) * APPS_PER_PAGE)
                                                navigateToPage(currentPage - 1, focusIndex = prevCount - 1)
                                                true
                                            } else false
                                        }
                                    },
                                    onDirectionUp = {
                                        if (isMovingThis) {
                                            val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                            val targetIdx = maxOf(0, currentIdx - COLUMNS_PER_ROW)
                                            if (targetIdx != currentIdx) {
                                                moveApp(currentIdx, targetIdx)
                                                true
                                            } else false
                                        } else {
                                            if (isReorderMode && colIndex < 3) {
                                                organizeButtonFocusRequester.requestFocus()
                                            } else {
                                                closeButtonFocusRequester.requestFocus()
                                            }
                                            true
                                        }
                                    },
                                    onDirectionDown = {
                                        if (isMovingThis) {
                                            val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                            val targetIdx = minOf(currentAppsList.size - 1, currentIdx + COLUMNS_PER_ROW)
                                            if (targetIdx != currentIdx) {
                                                moveApp(currentIdx, targetIdx)
                                                true
                                            } else false
                                        } else {
                                            val targetIndex = overallIndex + COLUMNS_PER_ROW
                                            if (targetIndex < pageApps.size) {
                                                appFocusRequesters[targetIndex].requestFocus()
                                                true
                                            } else if (pageApps.size > COLUMNS_PER_ROW) {
                                                appFocusRequesters[pageApps.size - 1].requestFocus()
                                                true
                                            } else if (totalPages > 1) {
                                                if (currentPage < totalPages - 1) {
                                                    nextButtonFocusRequester.requestFocus()
                                                } else {
                                                    prevButtonFocusRequester.requestFocus()
                                                }
                                                true
                                            } else false
                                        }
                                    }
                                )
                            }
                            val missingRow1 = COLUMNS_PER_ROW - row1Apps.size
                            repeat(missingRow1) {
                                Spacer(modifier = Modifier.width(160.dp))
                            }
                        }

                        // FILA 2 (Índices 5..9)
                        if (row2Apps.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                row2Apps.forEachIndexed { colIndex, app ->
                                    val overallIndex = COLUMNS_PER_ROW + colIndex
                                    val isMovingThis = isReorderMode && movingAppPackage == app.packageName

                                    AppLandscapeTile(
                                        app = app,
                                        isReorderMode = isReorderMode,
                                        isBeingMoved = isMovingThis,
                                        focusRequester = appFocusRequesters[overallIndex],
                                        onClick = {
                                            if (isReorderMode) {
                                                movingAppPackage = if (isMovingThis) null else app.packageName
                                            } else {
                                                onAppClick(app)
                                            }
                                        },
                                        onDirectionRight = {
                                            if (isMovingThis) {
                                                val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                                if (currentIdx < currentAppsList.size - 1) {
                                                    moveApp(currentIdx, currentIdx + 1)
                                                    true
                                                } else false
                                            } else {
                                                if (overallIndex < pageApps.size - 1) {
                                                    appFocusRequesters[overallIndex + 1].requestFocus()
                                                    true
                                                } else if (currentPage < totalPages - 1) {
                                                    navigateToPage(currentPage + 1, focusIndex = 0)
                                                    true
                                                } else false
                                            }
                                        },
                                        onDirectionLeft = {
                                            if (isMovingThis) {
                                                val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                                if (currentIdx > 0) {
                                                    moveApp(currentIdx, currentIdx - 1)
                                                    true
                                                } else false
                                            } else {
                                                if (overallIndex > 0) {
                                                    appFocusRequesters[overallIndex - 1].requestFocus()
                                                    true
                                                } else if (currentPage > 0) {
                                                    val prevCount = minOf(APPS_PER_PAGE, currentAppsList.size - (currentPage - 1) * APPS_PER_PAGE)
                                                    navigateToPage(currentPage - 1, focusIndex = prevCount - 1)
                                                    true
                                                } else false
                                            }
                                        },
                                        onDirectionUp = {
                                            if (isMovingThis) {
                                                val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                                val targetIdx = maxOf(0, currentIdx - COLUMNS_PER_ROW)
                                                if (targetIdx != currentIdx) {
                                                    moveApp(currentIdx, targetIdx)
                                                    true
                                                } else false
                                            } else {
                                                val targetIndex = overallIndex - COLUMNS_PER_ROW
                                                appFocusRequesters[targetIndex].requestFocus()
                                                true
                                            }
                                        },
                                        onDirectionDown = {
                                            if (isMovingThis) {
                                                val currentIdx = currentAppsList.indexOfFirst { it.packageName == app.packageName }
                                                val targetIdx = minOf(currentAppsList.size - 1, currentIdx + COLUMNS_PER_ROW)
                                                if (targetIdx != currentIdx) {
                                                    moveApp(currentIdx, targetIdx)
                                                    true
                                                } else false
                                            } else {
                                                if (totalPages > 1) {
                                                    if (colIndex >= (COLUMNS_PER_ROW / 2) && currentPage < totalPages - 1) {
                                                        nextButtonFocusRequester.requestFocus()
                                                    } else if (currentPage > 0) {
                                                        prevButtonFocusRequester.requestFocus()
                                                    } else {
                                                        nextButtonFocusRequester.requestFocus()
                                                    }
                                                    true
                                                } else false
                                            }
                                        }
                                    )
                                }
                                val missingRow2 = COLUMNS_PER_ROW - row2Apps.size
                                repeat(missingRow2) {
                                    Spacer(modifier = Modifier.width(160.dp))
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(138.dp))
                        }
                    }

                    // =========================================================================
                    // 3. PAGINACIÓN INFERIOR (SÓLO SI TOTAL DE PÁGINAS > 1)
                    // =========================================================================
                    if (totalPages > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaginationCircleButton(
                                iconText = "‹",
                                enabled = currentPage > 0,
                                focusRequester = prevButtonFocusRequester,
                                onDirectionRight = {
                                    if (currentPage < totalPages - 1) {
                                        nextButtonFocusRequester.requestFocus()
                                        true
                                    } else false
                                },
                                onDirectionUp = {
                                    val targetIndex = if (pageApps.size > COLUMNS_PER_ROW) {
                                        minOf(COLUMNS_PER_ROW + 1, pageApps.size - 1)
                                    } else 0
                                    appFocusRequesters[targetIndex].requestFocus()
                                    true
                                },
                                onClick = {
                                    if (currentPage > 0) {
                                        navigateToPage(currentPage - 1, focusIndex = 0)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (p in 0 until totalPages) {
                                        val isCurrent = p == currentPage
                                        Box(
                                            modifier = Modifier
                                                .size(if (isCurrent) 8.dp else 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) TvTokens.CyanNeon
                                                    else Color(0xFF1E2E48)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${currentPage + 1} / $totalPages",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            PaginationCircleButton(
                                iconText = "›",
                                enabled = currentPage < totalPages - 1,
                                focusRequester = nextButtonFocusRequester,
                                onDirectionLeft = {
                                    if (currentPage > 0) {
                                        prevButtonFocusRequester.requestFocus()
                                        true
                                    } else false
                                },
                                onDirectionUp = {
                                    val targetIndex = pageApps.size - 1
                                    appFocusRequesters[targetIndex].requestFocus()
                                    true
                                },
                                onClick = {
                                    if (currentPage < totalPages - 1) {
                                        navigateToPage(currentPage + 1, focusIndex = 0)
                                    }
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tile individual apaisado de la cuadrícula de aplicaciones.
 * Optimizado a 60 FPS con texturas aceleradas por GPU y soporte de reordenamiento D-Pad.
 */
@Composable
private fun AppLandscapeTile(
    app: AppItem,
    isReorderMode: Boolean,
    isBeingMoved: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onDirectionRight: () -> Boolean,
    onDirectionLeft: () -> Boolean,
    onDirectionUp: () -> Boolean,
    onDirectionDown: () -> Boolean,
    modifier: Modifier = Modifier,
    tileWidth: Dp = 160.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animación ultra-ligera y nítida de escala (110ms)
    val scale by animateFloatAsState(
        targetValue = when {
            isBeingMoved -> 1.08f
            isFocused -> 1.05f
            else -> 1.0f
        },
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "tile_scale"
    )

    val shape = remember { RoundedCornerShape(16.dp) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(tileWidth)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> onDirectionRight()
                        Key.DirectionLeft -> onDirectionLeft()
                        Key.DirectionUp -> onDirectionUp()
                        Key.DirectionDown -> onDirectionDown()
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
                        else -> false
                    }
                } else false
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val borderColor = when {
            isBeingMoved -> TvTokens.CyanNeon
            isFocused -> TvTokens.CyanNeon
            isReorderMode -> TvTokens.CyanNeon.copy(alpha = 0.35f)
            else -> Color.White.copy(alpha = 0.12f)
        }

        val borderWidth = when {
            isBeingMoved -> 3.dp
            isFocused -> 2.5.dp
            else -> 1.dp
        }

        val containerBg = when {
            isBeingMoved -> Color(0xFF1E3A5F)
            isFocused -> Color(0xFF1B2C48)
            else -> Color(0xFF131F33)
        }

        // Cápsula apaisada
        Box(
            modifier = Modifier
                .width(tileWidth)
                .aspectRatio(16f / 9f)
                .drawWithCache {
                    val halo = listOf(4.dp to 0.045f, 2.dp to 0.095f)
                    onDrawBehind {
                        if (isFocused || isBeingMoved) {
                            for ((spread, opacity) in halo) {
                                val edge = spread.toPx()
                                drawRoundRect(TvTokens.CyanNeon.copy(alpha = opacity),
                                    Offset(-edge, -edge), Size(size.width + 2 * edge, size.height + 2 * edge),
                                    CornerRadius(16.dp.toPx() + edge))
                            }
                        }
                    }
                }
                .background(containerBg, shape)
                .border(borderWidth, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                // PRIORIDAD 1: Bitmaps de hardware (GPU Textures) directas sin pasadas CPU
                when {
                    app.bannerBitmap != null -> {
                        Image(
                            bitmap = app.bannerBitmap,
                            contentDescription = app.name,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // PRIORIDAD 2: Painter fallback si el bitmap no estuviera presente
                    app.bannerDrawable != null -> {
                        val bannerPainter = rememberNativeDrawablePainter(app.bannerDrawable)
                        if (bannerPainter != null) {
                            Image(
                                painter = bannerPainter,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = app.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    app.iconBitmap != null -> {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.name,
                            modifier = Modifier.size(54.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    app.iconDrawable != null -> {
                        val iconPainter = rememberNativeDrawablePainter(app.iconDrawable)
                        if (iconPainter != null) {
                            Image(
                                painter = iconPainter,
                                contentDescription = app.name,
                                modifier = Modifier.size(54.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = app.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Box(Modifier.matchParentSize().clip(shape).drawWithCache {
                val sheen = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.19f),
                    0.34f to Color.White.copy(alpha = 0.055f),
                    0.35f to Color.Transparent,
                    0.86f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.08f),
                    start = Offset.Zero, end = Offset(size.width * 0.72f, size.height)
                )
                onDrawBehind { drawRect(sheen) }
            })

            // Indicador flotante cuando la app está en modo "Moviendo"
            if (isBeingMoved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvTokens.CyanNeon)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "◀ MOVIENDO ▶",
                        color = Color(0xFF040A18),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Nombre de la App alineado debajo
        Box(
            modifier = Modifier
                .width(tileWidth)
                .height(34.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = app.name,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = if (isFocused || isBeingMoved) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isBeingMoved -> TvTokens.CyanNeon
                    isFocused -> Color.White
                    else -> Color(0xFFCBD5E1)
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Botón "⇅ Organizar" / "✓ Listo" para activar o guardar el reordenamiento.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OrganizeModePillButton(
    isReorderMode: Boolean,
    focusRequester: FocusRequester,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> { onRight(); true }
                        Key.DirectionDown -> { onDown(); true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
                        else -> false
                    }
                } else false
            },
        shape = ButtonDefaults.shape(RoundedCornerShape(50)),
        colors = ButtonDefaults.colors(
            containerColor = if (isReorderMode) Color(0x3300E5FF) else Color.White.copy(alpha = 0.10f),
            contentColor = Color.White,
            focusedContainerColor = if (isReorderMode) Color(0x5500E5FF) else Color(0x2800E5FF),
            focusedContentColor = Color.White
        ),
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (isReorderMode) TvTokens.CyanNeon else Color.White.copy(alpha = 0.28f)
                ),
                shape = RoundedCornerShape(50)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvTokens.CyanNeon),
                shape = RoundedCornerShape(50)
            )
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isReorderMode) "✓" else "⇅",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused || isReorderMode) TvTokens.CyanNeon else Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isReorderMode) "Guardar orden" else "Organizar apps",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isReorderMode) TvTokens.CyanNeon else Color.White
            )
        }
    }
}

/**
 * Botón "✕ Cerrar" con soporte D-Pad nativo.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CloseModalPillButton(
    focusRequester: FocusRequester,
    label: String = "Cerrar",
    onLeft: () -> Unit,
    onDown: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> { onLeft(); true }
                        Key.DirectionDown -> { onDown(); true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
                        else -> false
                    }
                } else false
            },
        shape = ButtonDefaults.shape(RoundedCornerShape(50)),
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.10f),
            contentColor = Color.White,
            focusedContainerColor = Color(0x2800E5FF),
            focusedContentColor = Color.White
        ),
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(50)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvTokens.CyanNeon),
                shape = RoundedCornerShape(50)
            )
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✕",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) TvTokens.CyanNeon else Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * Botón circular para avanzar y retroceder páginas con foco turquesa.
 */
@Composable
private fun PaginationCircleButton(
    iconText: String,
    enabled: Boolean,
    focusRequester: FocusRequester? = null,
    onDirectionLeft: (() -> Boolean)? = null,
    onDirectionRight: (() -> Boolean)? = null,
    onDirectionUp: (() -> Boolean)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "btn_scale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { keyEvent ->
                if (!enabled) return@onPreviewKeyEvent false
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> onDirectionLeft?.invoke() ?: false
                        Key.DirectionRight -> onDirectionRight?.invoke() ?: false
                        Key.DirectionUp -> onDirectionUp?.invoke() ?: false
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
                        else -> false
                    }
                } else false
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isFocused) Color(0xFF1E3252) else Color(0xFF131F33)
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.5.dp,
                color = when {
                    isFocused -> TvTokens.CyanNeon
                    enabled -> TvTokens.CyanNeon.copy(alpha = 0.85f)
                    else -> Color.White.copy(alpha = 0.14f)
                },
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconText,
            color = when {
                isFocused -> TvTokens.CyanNeon
                enabled -> TvTokens.CyanNeon
                else -> Color.White.copy(alpha = 0.28f)
            },
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
