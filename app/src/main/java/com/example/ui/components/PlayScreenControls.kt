package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CubeColor
import com.example.model.CubeMove
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.viewmodel.ActivePracticeAlgorithm
import com.example.viewmodel.CameraPreset
import com.example.viewmodel.ControlMode
import com.example.viewmodel.GameStatus
import com.example.viewmodel.ScrambleDifficulty
import java.util.Locale

@Composable
fun TopHudBar(
    elapsedMillis: Long,
    movesCount: Int,
    remainingSolutionSteps: Int,
    gameStatus: GameStatus,
    showVisualGuide: Boolean,
    showMiniNet: Boolean,
    showFaceBadges: Boolean,
    onToggleVisualGuide: () -> Unit,
    onToggleMiniNet: () -> Unit,
    onToggleFaceBadges: () -> Unit,
    onResetCube: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate900.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Timer & Moves Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = "WAKTU",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatDuration(elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (gameStatus == GameStatus.SOLVED) EmeraldSuccess else CyanPrimary,
                        modifier = Modifier.testTag("hud_timer_text")
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Column {
                    Text(
                        text = "LANGKAH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$movesCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("hud_moves_text")
                    )
                }

                if (remainingSolutionSteps > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberWarning.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$remainingSolutionSteps langkah ke selesai",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick visual assist toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleVisualGuide,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_visual_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Panduan Panah 3D",
                        tint = if (showVisualGuide) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleMiniNet,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_mini_net_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Peta 6 Sisi",
                        tint = if (showMiniNet) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleFaceBadges,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_face_badges_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Label Notasi Sisi",
                        tint = if (showFaceBadges) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onResetCube,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("reset_cube_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Rubik",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveGuideBanner(
    statusMessage: String,
    nextHintMove: CubeMove?,
    practiceAlgorithm: ActivePracticeAlgorithm?,
    gameStatus: GameStatus,
    onPerformHintStep: () -> Unit,
    onClosePractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Practice Algorithm Bar if active
        AnimatedVisibility(visible = practiceAlgorithm != null) {
            if (practiceAlgorithm != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = practiceAlgorithm.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFE0E7FF),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onClosePractice,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("close_practice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Latihan",
                                    tint = Color(0xFFC7D2FE),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            practiceAlgorithm.moves.forEachIndexed { idx, move ->
                                val isDone = idx < practiceAlgorithm.currentIndex
                                val isCurrent = idx == practiceAlgorithm.currentIndex
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCurrent -> CyanPrimary
                                                isDone -> EmeraldSuccess.copy(alpha = 0.25f)
                                                else -> Color.White.copy(alpha = 0.1f)
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = move.notation,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = when {
                                            isCurrent -> Color(0xFF090D16)
                                            isDone -> EmeraldSuccess
                                            else -> Color.White
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Solved Celebration Banner or Live Step Hint Banner
        Surface(
            color = if (gameStatus == GameStatus.SOLVED) {
                Color(0xFF064E3B).copy(alpha = 0.9f)
            } else {
                Slate800.copy(alpha = 0.85f)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (gameStatus == GameStatus.SOLVED) {
                            Icons.Default.EmojiEvents
                        } else {
                            Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = if (gameStatus == GameStatus.SOLVED) EmeraldSuccess else AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (nextHintMove != null && gameStatus != GameStatus.SCRAMBLING && gameStatus != GameStatus.AUTO_SOLVING) {
                            "Saran Langkah: ${nextHintMove.notation} (${nextHintMove.indonesianDescription})"
                        } else {
                            statusMessage
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (nextHintMove != null && gameStatus != GameStatus.SCRAMBLING && gameStatus != GameStatus.AUTO_SOLVING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onPerformHintStep,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("banner_hint_step_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Langkah (${nextHintMove.notation})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraAndControlModeStrip(
    controlMode: ControlMode,
    cameraZoom: Float,
    onSetControlMode: (ControlMode) -> Unit,
    onSelectCameraPreset: (CameraPreset) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Control Mode Toggle + Zoom controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = controlMode == ControlMode.SWIPE_CUBE,
                    onClick = { onSetControlMode(ControlMode.SWIPE_CUBE) },
                    label = { Text("Usap Kubus", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.22f),
                        selectedLabelColor = CyanPrimary,
                        selectedLeadingIconColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("mode_swipe_cube_chip")
                )

                FilterChip(
                    selected = controlMode == ControlMode.FREE_CAMERA,
                    onClick = { onSetControlMode(ControlMode.FREE_CAMERA) },
                    label = { Text("Rotasi Kamera 360°", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.22f),
                        selectedLabelColor = CyanPrimary,
                        selectedLeadingIconColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("mode_free_camera_chip")
                )
            }

            // Zoom Out / In buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onZoomChange(cameraZoom - 0.12f) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("camera_zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Perkecil Kamera",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onZoomChange(cameraZoom + 0.12f) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("camera_zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Perbesar Kamera",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 6-Side Camera Angle Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sudut Pandang:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CameraPreset.entries.forEach { preset ->
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectCameraPreset(preset) }
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .testTag("camera_preset_${preset.name.lowercase()}")
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private data class QuickFaceButtonInfo(
    val baseNotation: String,
    val shortLabel: String,
    val color: CubeColor
)

private val QUICK_FACE_BUTTONS = listOf(
    QuickFaceButtonInfo("U", "Atas", CubeColor.WHITE),
    QuickFaceButtonInfo("D", "Bawah", CubeColor.YELLOW),
    QuickFaceButtonInfo("L", "Kiri", CubeColor.ORANGE),
    QuickFaceButtonInfo("R", "Kanan", CubeColor.RED),
    QuickFaceButtonInfo("F", "Depan", CubeColor.GREEN),
    QuickFaceButtonInfo("B", "Blkng", CubeColor.BLUE)
)

@Composable
fun QuickControlPad(
    isPrimeMode: Boolean,
    canUndo: Boolean,
    nextHintMove: CubeMove?,
    scrambleDifficulty: ScrambleDifficulty,
    isBusy: Boolean,
    onTogglePrimeMode: () -> Unit,
    onExecuteNotation: (String) -> Unit,
    onUndo: () -> Unit,
    onScramble: () -> Unit,
    onCycleDifficulty: () -> Unit,
    onInstantSolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate900.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: Direction toggle (Clockwise vs Counter-Clockwise Prime) + Undo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kontrol Putar Cepat:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        color = if (isPrimeMode) AmberWarning.copy(alpha = 0.2f) else CyanPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTogglePrimeMode() }
                            .border(
                                1.dp,
                                if (isPrimeMode) AmberWarning else CyanPrimary,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("toggle_prime_direction_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPrimeMode) {
                                    Icons.AutoMirrored.Filled.RotateLeft
                                } else {
                                    Icons.AutoMirrored.Filled.RotateRight
                                },
                                contentDescription = null,
                                tint = if (isPrimeMode) AmberWarning else CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPrimeMode) "Arah: Berlawanan (')" else "Arah: Searah Jarum Jam",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPrimeMode) AmberWarning else CyanPrimary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onUndo,
                    enabled = canUndo && !isBusy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("undo_move_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Urung",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Urung", fontSize = 12.sp)
                }
            }

            // 6 Face Quick Rotation Buttons (U, D, L, R, F, B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QUICK_FACE_BUTTONS.forEach { item ->
                    val notation = if (isPrimeMode) "${item.baseNotation}'" else item.baseNotation
                    val isRecommended = nextHintMove?.notation == notation
                    Surface(
                        color = if (isRecommended) CyanPrimary.copy(alpha = 0.28f) else Slate800,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isBusy) { onExecuteNotation(notation) }
                            .border(
                                width = if (isRecommended) 2.dp else 1.dp,
                                color = if (isRecommended) CyanPrimary else item.color.displayColor.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("quick_move_${item.baseNotation.lowercase()}_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(item.color.displayColor, CircleShape)
                                )
                                Text(
                                    text = notation,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = item.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Primary Action Row: Scramble (with Difficulty) + Instant Solve
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty selector pill
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBusy) { onCycleDifficulty() }
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .testTag("scramble_difficulty_button")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Level Acak",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${scrambleDifficulty.moveCount} Langkah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                // Scramble Button ("Acak Rubik")
                Button(
                    onClick = onScramble,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF090D16)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scramble_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Acak Rubik",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Instant Solve Button ("Selesai Instan")
                Button(
                    onClick = onInstantSolve,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF064E3B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("instant_solve_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selesai Instan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (millis % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}
