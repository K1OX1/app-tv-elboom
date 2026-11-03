package cl.elboomlicanray.tvlauncher.presentation.modals.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.presentation.components.rememberNativeDrawablePainter
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import kotlinx.coroutines.delay

private const val APPS_PER_PAGE = 10
private const val COLUMNS_PER_ROW = 5

/**
 * Sección "Apps & Entretenimiento" rediseñada para Android TV 1080p.
 *
 * Características:
 * - Navegación direccional fluida y continua por D-Pad entre todas las aplicaciones.
 * - Al navegar con D-Pad hacia la derecha en la última app de la fila o página,
 *   avanza automáticamente a la siguiente fila o página.
 * - Al navegar con D-Pad hacia la izquierda en la primera app de la página,
 *   retrocede a la página anterior colocándose en la última app.
 * - Foco turquesa (#00E5FF) con ligero aumento de luminosidad y escala sutil.
 * - Cuadrícula compacta de 5 columnas x 2 filas (10 tiles apaisados por página).
 * - Icono / banner oficial de cada aplicación conservando sus colores y logos propios.
 * - Botones de paginación inferior opcionales y botón de cerrar accesibles por D-Pad.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamingAppsDialog(
    apps: List<AppItem>,
    selectedLanguage: String = "ES",
    isSmartDnsEnabled: Boolean = false,
    onAppClick: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = remember(selectedLanguage) {
        cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(selectedLanguage)
    }

    val totalPages = remember(apps) {
        if (apps.isEmpty()) 1 else (apps.size + APPS_PER_PAGE - 1) / APPS_PER_PAGE
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var targetFocusIndex by remember { mutableIntStateOf(0) }
    var shouldApplyFocus by remember { mutableStateOf(false) }

    val appFocusRequesters = remember { List(APPS_PER_PAGE) { FocusRequester() } }
    val closeButtonFocusRequester = remember { FocusRequester() }
    val prevButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }

    var isVisible by remember { mutableStateOf(false) }

    // Función para navegar entre páginas cambiando el foco al elemento adecuado
    fun navigateToPage(newPage: Int, focusIndex: Int) {
        if (newPage in 0 until totalPages) {
            currentPage = newPage
            targetFocusIndex = focusIndex
            shouldApplyFocus = true
        }
    }

    // Asegurar que la página esté dentro de los límites si la lista de apps cambia
    LaunchedEffect(totalPages) {
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }
    }

    // Aplicar foco al cambiar de página
    LaunchedEffect(currentPage, shouldApplyFocus) {
        if (shouldApplyFocus) {
            delay(50)
            try {
                val currentCount = minOf(APPS_PER_PAGE, maxOf(1, apps.size - currentPage * APPS_PER_PAGE))
                val safeIndex = targetFocusIndex.coerceIn(0, currentCount - 1)
                appFocusRequesters.getOrNull(safeIndex)?.requestFocus()
            } catch (_: Exception) {}
            shouldApplyFocus = false
        }
    }

    // Entrada animada y foco inicial en la primera app
    LaunchedEffect(Unit) {
        isVisible = true
        delay(180)
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
        BackHandler {
            onDismiss()
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF091428),
                                Color(0xFF050B18),
                                Color(0xFF030712)
                            ),
                            radius = 1600f
                        )
                    )
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // =========================================================================
                    // 1. ENCABEZADO COMPACTO: TÍTULO, SUBTÍTULO Y BOTÓN CERRAR
                    // =========================================================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.streamingDialogTitle,
                                style = androidx.tv.material3.MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 27.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = strings.streamingDialogSubtitle,
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isSmartDnsEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🇺🇸 Netflix USA Activo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }

                            // Botón "✕ Cerrar"
                            CloseModalPillButton(
                                focusRequester = closeButtonFocusRequester,
                                label = strings.close,
                                onDown = {
                                    val safeCount = minOf(APPS_PER_PAGE, maxOf(1, apps.size - currentPage * APPS_PER_PAGE))
                                    val targetIndex = minOf(COLUMNS_PER_ROW - 1, safeCount - 1)
                                    try {
                                        appFocusRequesters[targetIndex].requestFocus()
                                    } catch (_: Exception) {}
                                },
                                onClick = onDismiss
                            )
                        }
                    }

                    // =========================================================================
                    // 2. CUADRÍCULA COMPACTA DE 5 COLUMNAS X 2 FILAS (10 TILES POR PÁGINA)
                    // =========================================================================
                    val pageApps = remember(apps, currentPage) {
                        apps.drop(currentPage * APPS_PER_PAGE).take(APPS_PER_PAGE)
                    }

                    val row1Apps = remember(pageApps) { pageApps.take(COLUMNS_PER_ROW) }
                    val row2Apps = remember(pageApps) { pageApps.drop(COLUMNS_PER_ROW).take(COLUMNS_PER_ROW) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                AppLandscapeTile(
                                    app = app,
                                    focusRequester = appFocusRequesters[overallIndex],
                                    onClick = { onAppClick(app) },
                                    onDirectionRight = {
                                        if (overallIndex < pageApps.size - 1) {
                                            appFocusRequesters[overallIndex + 1].requestFocus()
                                            true
                                        } else if (currentPage < totalPages - 1) {
                                            navigateToPage(currentPage + 1, focusIndex = 0)
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                    onDirectionLeft = {
                                        if (overallIndex > 0) {
                                            appFocusRequesters[overallIndex - 1].requestFocus()
                                            true
                                        } else if (currentPage > 0) {
                                            val prevCount = minOf(APPS_PER_PAGE, apps.size - (currentPage - 1) * APPS_PER_PAGE)
                                            navigateToPage(currentPage - 1, focusIndex = prevCount - 1)
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                    onDirectionUp = {
                                        closeButtonFocusRequester.requestFocus()
                                        true
                                    },
                                    onDirectionDown = {
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
                                        } else {
                                            false
                                        }
                                    }
                                )
                            }
                            // Si la fila 1 tiene menos de 5 apps, rellenar espacios vacíos
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
                                    AppLandscapeTile(
                                        app = app,
                                        focusRequester = appFocusRequesters[overallIndex],
                                        onClick = { onAppClick(app) },
                                        onDirectionRight = {
                                            if (overallIndex < pageApps.size - 1) {
                                                appFocusRequesters[overallIndex + 1].requestFocus()
                                                true
                                            } else if (currentPage < totalPages - 1) {
                                                navigateToPage(currentPage + 1, focusIndex = 0)
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        onDirectionLeft = {
                                            if (overallIndex > 0) {
                                                appFocusRequesters[overallIndex - 1].requestFocus()
                                                true
                                            } else if (currentPage > 0) {
                                                val prevCount = minOf(APPS_PER_PAGE, apps.size - (currentPage - 1) * APPS_PER_PAGE)
                                                navigateToPage(currentPage - 1, focusIndex = prevCount - 1)
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        onDirectionUp = {
                                            val targetIndex = overallIndex - COLUMNS_PER_ROW
                                            appFocusRequesters[targetIndex].requestFocus()
                                            true
                                        },
                                        onDirectionDown = {
                                            if (totalPages > 1) {
                                                if (colIndex >= (COLUMNS_PER_ROW / 2) && currentPage < totalPages - 1) {
                                                    nextButtonFocusRequester.requestFocus()
                                                } else if (currentPage > 0) {
                                                    prevButtonFocusRequester.requestFocus()
                                                } else {
                                                    nextButtonFocusRequester.requestFocus()
                                                }
                                                true
                                            } else {
                                                false
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
                            // Espacio reservado para balancear la cuadrícula si sólo hay 1 fila
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
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón Anterior "<"
                            PaginationCircleButton(
                                iconText = "‹",
                                enabled = currentPage > 0,
                                focusRequester = prevButtonFocusRequester,
                                onDirectionRight = {
                                    if (currentPage < totalPages - 1) {
                                        nextButtonFocusRequester.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                },
                                onDirectionUp = {
                                    val targetIndex = if (pageApps.size > COLUMNS_PER_ROW) {
                                        minOf(COLUMNS_PER_ROW + 1, pageApps.size - 1)
                                    } else {
                                        0
                                    }
                                    appFocusRequesters[targetIndex].requestFocus()
                                    true
                                },
                                onClick = {
                                    if (currentPage > 0) {
                                        navigateToPage(currentPage - 1, focusIndex = 0)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(22.dp))

                            // Indicador Central: Puntos + "1 / 3" + "Más aplicaciones"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Fila de puntos
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (p in 0 until totalPages) {
                                        val isCurrent = p == currentPage
                                        Box(
                                            modifier = Modifier
                                                .size(if (isCurrent) 8.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) TvTokens.CyanNeon
                                                    else Color(0xFF1E2E48)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = "${currentPage + 1} / $totalPages",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(1.dp))

                                Text(
                                    text = "Más aplicaciones",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.width(22.dp))

                            // Botón Siguiente ">"
                            PaginationCircleButton(
                                iconText = "›",
                                enabled = currentPage < totalPages - 1,
                                focusRequester = nextButtonFocusRequester,
                                onDirectionLeft = {
                                    if (currentPage > 0) {
                                        prevButtonFocusRequester.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                },
                                onDirectionUp = {
                                    val targetIndex = if (pageApps.size > COLUMNS_PER_ROW) {
                                        pageApps.size - 1
                                    } else {
                                        pageApps.size - 1
                                    }
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
                        // Espacio inferior equilibrado si no hay paginación
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tile individual apaisado de la cuadrícula de aplicaciones con soporte D-Pad nativo.
 */
@Composable
private fun AppLandscapeTile(
    app: AppItem,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onDirectionRight: () -> Boolean,
    onDirectionLeft: () -> Boolean,
    onDirectionUp: () -> Boolean,
    onDirectionDown: () -> Boolean,
    modifier: Modifier = Modifier,
    tileWidth: Dp = 160.dp,
    tileHeight: Dp = 104.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
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
                } else {
                    false
                }
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
        // Cápsula apaisada
        Box(
            modifier = Modifier
                .width(tileWidth)
                .height(tileHeight)
                .shadow(
                    elevation = if (isFocused) 14.dp else 2.dp,
                    shape = shape,
                    spotColor = if (isFocused) TvTokens.CyanNeon.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.5f)
                )
                .background(
                    color = if (isFocused) Color(0xFF1B2C48) else Color(0xFF131F33),
                    shape = shape
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) TvTokens.CyanNeon else Color.White.copy(alpha = 0.12f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                val bannerPainter = rememberNativeDrawablePainter(app.bannerDrawable)
                val iconPainter = rememberNativeDrawablePainter(app.iconDrawable)

                when {
                    // 1. BANNER OFICIAL 16:9 DE ANDROID TV
                    bannerPainter != null -> {
                        Image(
                            painter = bannerPainter,
                            contentDescription = app.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 2. ÍCONO OFICIAL CENTRADO (54DP) CONSERVA SUS COLORES
                    iconPainter != null -> {
                        Image(
                            painter = iconPainter,
                            contentDescription = app.name,
                            modifier = Modifier.size(54.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 3. FALLBACK DE TEXTO
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Nombre de la App alineado debajo (máximo 2 líneas, altura fija para mantener alineación)
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
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused) Color.White else Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                        Key.DirectionDown -> {
                            onDown()
                            true
                        }
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
                } else {
                    false
                }
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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp)
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
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
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
                } else {
                    false
                }
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
