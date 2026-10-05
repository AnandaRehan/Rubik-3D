package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.RubikDatabase
import com.example.data.SolveRepository
import com.example.model.CubeMove
import com.example.ui.components.BeginnerGuideScreen
import com.example.ui.components.CameraAndControlModeStrip
import com.example.ui.components.InteractiveGuideBanner
import com.example.ui.components.QuickControlPad
import com.example.ui.components.RecordsAndHistoryScreen
import com.example.ui.components.Rubik3DViewport
import com.example.ui.components.TopHudBar
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppTab
import com.example.viewmodel.GameStatus
import com.example.viewmodel.RubikUiState
import com.example.viewmodel.RubikViewModel
import com.example.viewmodel.RubikViewModelFactory
import com.example.viewmodel.ScrambleDifficulty

class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            RubikDatabase::class.java,
            "rubik_3d_database"
        ).fallbackToDestructiveMigration(false).build()
    }

    private val repository by lazy {
        SolveRepository(database.solveDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: RubikViewModel = viewModel(
                    factory = RubikViewModelFactory(repository)
                )
                RubikAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RubikAppContent(viewModel: RubikViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val bestRecord by viewModel.bestRecord.collectAsStateWithLifecycle()

    // BackHandler for secondary tabs as required by navigation guidelines
    BackHandler(enabled = uiState.selectedTab != AppTab.PLAY) {
        viewModel.selectTab(AppTab.PLAY)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == AppTab.PLAY,
                    onClick = { viewModel.selectTab(AppTab.PLAY) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == AppTab.PLAY) {
                                Icons.Filled.ViewInAr
                            } else {
                                Icons.Outlined.ViewInAr
                            },
                            contentDescription = stringResource(R.string.tab_play)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_play),
                            fontWeight = if (uiState.selectedTab == AppTab.PLAY) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_play")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AppTab.GUIDE,
                    onClick = { viewModel.selectTab(AppTab.GUIDE) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == AppTab.GUIDE) {
                                Icons.Filled.MenuBook
                            } else {
                                Icons.Outlined.MenuBook
                            },
                            contentDescription = stringResource(R.string.tab_guide)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_guide),
                            fontWeight = if (uiState.selectedTab == AppTab.GUIDE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_guide")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == AppTab.RECORDS,
                    onClick = { viewModel.selectTab(AppTab.RECORDS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == AppTab.RECORDS) {
                                Icons.Filled.EmojiEvents
                            } else {
                                Icons.Outlined.EmojiEvents
                            },
                            contentDescription = stringResource(R.string.tab_records)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_records),
                            fontWeight = if (uiState.selectedTab == AppTab.RECORDS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_records")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Slate950,
                            Color(0xFF0B1325),
                            Slate950
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState.selectedTab) {
                AppTab.PLAY -> {
                    PlayRubikScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                AppTab.GUIDE -> {
                    BeginnerGuideScreen(
                        onPracticeStep = { step ->
                            viewModel.startAlgorithmPractice(step)
                        },
                        onTryNotationMove = { notation ->
                            viewModel.selectTab(AppTab.PLAY)
                            val move = CubeMove.fromNotation(notation).firstOrNull()
                            if (move != null) {
                                viewModel.performPlayerMove(move)
                            }
                        },
                        modifier = Modifier.widthIn(max = 720.dp)
                    )
                }

                AppTab.RECORDS -> {
                    RecordsAndHistoryScreen(
                        records = allRecords,
                        bestRecord = bestRecord,
                        onDeleteRecord = { viewModel.deleteRecord(it) },
                        onClearAll = { viewModel.clearAllRecords() },
                        onGoToPlay = { viewModel.selectTab(AppTab.PLAY) },
                        modifier = Modifier.widthIn(max = 720.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayRubikScreen(
    uiState: RubikUiState,
    viewModel: RubikViewModel
) {
    val isBusy = uiState.gameStatus == GameStatus.SCRAMBLING ||
        uiState.gameStatus == GameStatus.AUTO_SOLVING ||
        uiState.activeMove != null

    val cycleDifficulty = {
        val entries = ScrambleDifficulty.entries
        val nextIdx = (entries.indexOf(uiState.scrambleDifficulty) + 1) % entries.size
        viewModel.setScrambleDifficulty(entries[nextIdx])
    }

    val executeNotation: (String) -> Unit = { notation ->
        val move = CubeMove.fromNotation(notation).firstOrNull()
        if (move != null) {
            viewModel.performPlayerMove(move)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth > 700.dp

        if (isWideLayout) {
            // Canonical Two-Pane Layout for Tablets / Foldables / Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    Rubik3DViewport(
                        cubeState = uiState.cubeState,
                        activeMove = uiState.activeMove,
                        activeMoveProgress = uiState.activeMoveProgress,
                        cameraYaw = uiState.cameraYaw,
                        cameraPitch = uiState.cameraPitch,
                        cameraZoom = uiState.cameraZoom,
                        controlMode = uiState.controlMode,
                        showVisualGuideOverlay = uiState.showVisualGuideOverlay,
                        showFaceNotationBadges = uiState.showFaceNotationBadges,
                        showMiniNet = uiState.showMiniNet,
                        nextHintMove = uiState.nextHintMove,
                        onPlayerMove = { viewModel.performPlayerMove(it) },
                        onCameraOrbit = { dyaw, dpitch -> viewModel.updateCameraOrbit(dyaw, dpitch) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopHudBar(
                        elapsedMillis = uiState.elapsedMillis,
                        movesCount = uiState.playerMovesCount,
                        remainingSolutionSteps = uiState.solutionSteps.size,
                        gameStatus = uiState.gameStatus,
                        showVisualGuide = uiState.showVisualGuideOverlay,
                        showMiniNet = uiState.showMiniNet,
                        showFaceBadges = uiState.showFaceNotationBadges,
                        onToggleVisualGuide = { viewModel.toggleVisualGuideOverlay() },
                        onToggleMiniNet = { viewModel.toggleMiniNet() },
                        onToggleFaceBadges = { viewModel.toggleFaceNotationBadges() },
                        onResetCube = { viewModel.resetCubeImmediately() }
                    )

                    InteractiveGuideBanner(
                        statusMessage = uiState.statusBannerMessage,
                        nextHintMove = uiState.nextHintMove,
                        practiceAlgorithm = uiState.practiceAlgorithm,
                        gameStatus = uiState.gameStatus,
                        onPerformHintStep = { viewModel.performHintStep() },
                        onClosePractice = { viewModel.clearAlgorithmPractice() }
                    )

                    CameraAndControlModeStrip(
                        controlMode = uiState.controlMode,
                        cameraZoom = uiState.cameraZoom,
                        onSetControlMode = { viewModel.setControlMode(it) },
                        onSelectCameraPreset = { viewModel.applyCameraPreset(it) },
                        onZoomChange = { viewModel.updateCameraZoom(it) }
                    )

                    QuickControlPad(
                        isPrimeMode = uiState.isPrimeControlMode,
                        canUndo = uiState.moveHistory.isNotEmpty(),
                        nextHintMove = uiState.nextHintMove,
                        scrambleDifficulty = uiState.scrambleDifficulty,
                        isBusy = isBusy,
                        onTogglePrimeMode = { viewModel.togglePrimeMode() },
                        onExecuteNotation = executeNotation,
                        onUndo = { viewModel.undoLastMove() },
                        onScramble = { viewModel.scrambleCube() },
                        onCycleDifficulty = cycleDifficulty,
                        onInstantSolve = { viewModel.instantSolveCube() }
                    )
                }
            }
        } else {
            // Portrait Handheld Mobile Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopHudBar(
                    elapsedMillis = uiState.elapsedMillis,
                    movesCount = uiState.playerMovesCount,
                    remainingSolutionSteps = uiState.solutionSteps.size,
                    gameStatus = uiState.gameStatus,
                    showVisualGuide = uiState.showVisualGuideOverlay,
                    showMiniNet = uiState.showMiniNet,
                    showFaceBadges = uiState.showFaceNotationBadges,
                    onToggleVisualGuide = { viewModel.toggleVisualGuideOverlay() },
                    onToggleMiniNet = { viewModel.toggleMiniNet() },
                    onToggleFaceBadges = { viewModel.toggleFaceNotationBadges() },
                    onResetCube = { viewModel.resetCubeImmediately() }
                )

                InteractiveGuideBanner(
                    statusMessage = uiState.statusBannerMessage,
                    nextHintMove = uiState.nextHintMove,
                    practiceAlgorithm = uiState.practiceAlgorithm,
                    gameStatus = uiState.gameStatus,
                    onPerformHintStep = { viewModel.performHintStep() },
                    onClosePractice = { viewModel.clearAlgorithmPractice() }
                )

                // Interactive 3D Rubik's Cube Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Rubik3DViewport(
                        cubeState = uiState.cubeState,
                        activeMove = uiState.activeMove,
                        activeMoveProgress = uiState.activeMoveProgress,
                        cameraYaw = uiState.cameraYaw,
                        cameraPitch = uiState.cameraPitch,
                        cameraZoom = uiState.cameraZoom,
                        controlMode = uiState.controlMode,
                        showVisualGuideOverlay = uiState.showVisualGuideOverlay,
                        showFaceNotationBadges = uiState.showFaceNotationBadges,
                        showMiniNet = uiState.showMiniNet,
                        nextHintMove = uiState.nextHintMove,
                        onPlayerMove = { viewModel.performPlayerMove(it) },
                        onCameraOrbit = { dyaw, dpitch -> viewModel.updateCameraOrbit(dyaw, dpitch) }
                    )
                }

                CameraAndControlModeStrip(
                    controlMode = uiState.controlMode,
                    cameraZoom = uiState.cameraZoom,
                    onSetControlMode = { viewModel.setControlMode(it) },
                    onSelectCameraPreset = { viewModel.applyCameraPreset(it) },
                    onZoomChange = { viewModel.updateCameraZoom(it) }
                )

                QuickControlPad(
                    isPrimeMode = uiState.isPrimeControlMode,
                    canUndo = uiState.moveHistory.isNotEmpty(),
                    nextHintMove = uiState.nextHintMove,
                    scrambleDifficulty = uiState.scrambleDifficulty,
                    isBusy = isBusy,
                    onTogglePrimeMode = { viewModel.togglePrimeMode() },
                    onExecuteNotation = executeNotation,
                    onUndo = { viewModel.undoLastMove() },
                    onScramble = { viewModel.scrambleCube() },
                    onCycleDifficulty = cycleDifficulty,
                    onInstantSolve = { viewModel.instantSolveCube() }
                )
            }
        }
    }
}
