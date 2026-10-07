package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SavedCubePosition
import com.example.data.SolveRecord
import com.example.data.SolveRepository
import com.example.model.Axis
import com.example.model.CubeMove
import com.example.model.GuideStep
import com.example.model.RubikCubeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ControlMode {
    SWIPE_CUBE,   // Usap langsung di permukaan kubus untuk memutar sisi; usap di luar kubus untuk putar kamera
    FREE_CAMERA   // Usap di mana saja untuk memutar kamera 360° bebas + gunakan tombol kontrol cepat
}

enum class ScrambleDifficulty(val label: String, val moveCount: Int) {
    EASY("Mudah (8 Acakan)", 8),
    MEDIUM("Standar (16 Acakan)", 16),
    HARD("Master (24 Acakan)", 24)
}

enum class CameraPreset(val label: String, val yaw: Float, val pitch: Float) {
    FRONT_RIGHT("Depan-Kanan", -35f, 25f),
    FRONT_LEFT("Depan-Kiri", 35f, 25f),
    BACK_RIGHT("Belakang-Kanan", -145f, 25f),
    BACK_LEFT("Belakang-Kiri", 145f, 25f),
    TOP_VIEW("Atas Penuh", -25f, 68f),
    BOTTOM_VIEW("Bawah Penuh", -35f, -55f)
}

enum class GameStatus {
    IDLE,           // Belum diacak / latihan bebas
    SCRAMBLING,     // Sedang mengacak
    READY_TO_PLAY,  // Sudah diacak, waktu mulai saat langkah pertama
    PLAYING,        // Sedang bermain (timer berjalan)
    AUTO_SOLVING,   // Sedang menyelesaikan otomatis (tekan sekali lagi untuk skip instan)
    SOLVED          // Berhasil diselesaikan!
}

enum class AppTab {
    PLAY,
    GUIDE,
    RECORDS
}

data class ActivePracticeAlgorithm(
    val title: String,
    val moves: List<CubeMove>,
    val currentIndex: Int = 0
) {
    val currentMove: CubeMove?
        get() = moves.getOrNull(currentIndex)
    val isCompleted: Boolean
        get() = currentIndex >= moves.size
}

data class RubikUiState(
    val cubeState: RubikCubeState = RubikCubeState.createSolved(),
    val activeMove: CubeMove? = null,
    val activeMoveProgress: Float = 0f,
    val moveHistory: List<CubeMove> = emptyList(),
    val redoStack: List<CubeMove> = emptyList(),
    val playerMovesCount: Int = 0,
    val elapsedMillis: Long = 0L,
    val gameStatus: GameStatus = GameStatus.IDLE,
    val controlMode: ControlMode = ControlMode.SWIPE_CUBE,
    val scrambleDifficulty: ScrambleDifficulty = ScrambleDifficulty.MEDIUM,
    val cameraYaw: Float = -35f,
    val cameraPitch: Float = 25f,
    val cameraZoom: Float = 1.0f,
    val showVisualGuideOverlay: Boolean = true,
    val showFaceNotationBadges: Boolean = true,
    val showMiniNet: Boolean = true,
    val isPrimeControlMode: Boolean = false,
    val selectedTab: AppTab = AppTab.PLAY,
    val practiceAlgorithm: ActivePracticeAlgorithm? = null,
    val usedAssistInCurrentRun: Boolean = false,
    val statusBannerMessage: String = "Usap permukaan kubus untuk memutar lapisannya, atau tekan Acak untuk mulai tantangan!"
) {
    val solutionSteps: List<CubeMove>
        get() = RubikCubeState.computeSolutionMoves(moveHistory)

    val nextHintMove: CubeMove?
        get() = practiceAlgorithm?.currentMove ?: solutionSteps.firstOrNull()

    val currentMoveSequenceNotation: String
        get() = if (moveHistory.isEmpty()) "" else moveHistory.joinToString(" ") { it.notation }
}

class RubikViewModel(
    private val repository: SolveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RubikUiState())
    val uiState: StateFlow<RubikUiState> = _uiState.asStateFlow()

    val allRecords: StateFlow<List<SolveRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bestRecord: StateFlow<SolveRecord?> = repository.bestRecord
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPositions: StateFlow<List<SavedCubePosition>> = repository.allSavedPositions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var animationJob: Job? = null
    private var timerJob: Job? = null
    private var timerStartRealtime: Long = 0L

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setControlMode(mode: ControlMode) {
        val msg = when (mode) {
            ControlMode.SWIPE_CUBE -> "Mode Usap Lapisan: Sentuh & usap stiker kubus untuk memutar sisi. Usap di luar kubus untuk rotasi kamera."
            ControlMode.FREE_CAMERA -> "Mode Kamera Bebas: Usap di mana saja untuk melihat seluruh sisi 360° dengan bebas."
        }
        _uiState.update { it.copy(controlMode = mode, statusBannerMessage = msg) }
    }

    fun toggleVisualGuideOverlay() {
        _uiState.update {
            val next = !it.showVisualGuideOverlay
            it.copy(
                showVisualGuideOverlay = next,
                statusBannerMessage = if (next) "Panduan panah 3D diaktifkan." else "Panduan panah 3D disembunyikan."
            )
        }
    }

    fun toggleFaceNotationBadges() {
        _uiState.update { it.copy(showFaceNotationBadges = !it.showFaceNotationBadges) }
    }

    fun toggleMiniNet() {
        _uiState.update { it.copy(showMiniNet = !it.showMiniNet) }
    }

    fun togglePrimeMode() {
        _uiState.update { it.copy(isPrimeControlMode = !it.isPrimeControlMode) }
    }

    fun setScrambleDifficulty(difficulty: ScrambleDifficulty) {
        _uiState.update { it.copy(scrambleDifficulty = difficulty) }
    }

    fun updateCameraOrbit(deltaYawDeg: Float, deltaPitchDeg: Float) {
        _uiState.update { state ->
            val newYaw = (state.cameraYaw + deltaYawDeg) % 360f
            val newPitch = (state.cameraPitch + deltaPitchDeg).coerceIn(-80f, 80f)
            state.copy(cameraYaw = newYaw, cameraPitch = newPitch)
        }
    }

    fun updateCameraZoom(newZoom: Float) {
        _uiState.update { it.copy(cameraZoom = newZoom.coerceIn(0.7f, 1.45f)) }
    }

    fun applyCameraPreset(preset: CameraPreset) {
        _uiState.update {
            it.copy(
                cameraYaw = preset.yaw,
                cameraPitch = preset.pitch,
                statusBannerMessage = "Kamera: Sudut pandang ${preset.label}"
            )
        }
    }

    /**
     * Triggered when the player performs a move via 3D touch swipe or Quick-Control pad.
     */
    fun performPlayerMove(move: CubeMove) {
        val current = _uiState.value
        if (current.gameStatus == GameStatus.SCRAMBLING || current.gameStatus == GameStatus.AUTO_SOLVING) return
        if (current.activeMove != null) return

        // Start timer if ready to play
        if (current.gameStatus == GameStatus.READY_TO_PLAY) {
            startTimer()
            _uiState.update { it.copy(gameStatus = GameStatus.PLAYING) }
        }

        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            animateSingleMove(move, durationMs = 150L)

            _uiState.update { state ->
                val newCube = state.cubeState.applyMove(move)
                val newHistory = RubikCubeState.simplifyMoves(state.moveHistory + move)
                val newMovesCount = state.playerMovesCount + 1

                // Check practice algorithm progress
                val updatedPractice = state.practiceAlgorithm?.let { alg ->
                    if (alg.currentMove == move) {
                        alg.copy(currentIndex = alg.currentIndex + 1)
                    } else {
                        alg
                    }
                }

                val solvedNow = newCube.isSolved()
                val wasPlaying = state.gameStatus == GameStatus.PLAYING || state.gameStatus == GameStatus.READY_TO_PLAY

                if (solvedNow && wasPlaying) {
                    stopTimer()
                    val finalTime = state.elapsedMillis
                    viewModelScope.launch {
                        repository.insert(
                            SolveRecord(
                                durationMillis = finalTime,
                                movesCount = newMovesCount,
                                scrambleDifficulty = state.scrambleDifficulty.label,
                                usedInstantSolve = state.usedAssistInCurrentRun
                            )
                        )
                    }
                }

                val banner = when {
                    solvedNow && wasPlaying -> "Selamat! Rubik selesai dalam $newMovesCount langkah!"
                    updatedPractice != null && updatedPractice.isCompleted -> "Rumus '${updatedPractice.title}' selesai dipraktikkan!"
                    else -> "${move.notation}: ${move.indonesianDescription}"
                }

                state.copy(
                    cubeState = newCube,
                    activeMove = null,
                    activeMoveProgress = 0f,
                    moveHistory = newHistory,
                    redoStack = emptyList(),
                    playerMovesCount = newMovesCount,
                    practiceAlgorithm = updatedPractice,
                    gameStatus = if (solvedNow && wasPlaying) GameStatus.SOLVED else state.gameStatus,
                    statusBannerMessage = banner
                )
            }
        }
    }

    fun undoLastMove() {
        val current = _uiState.value
        if (current.activeMove != null || current.moveHistory.isEmpty()) return
        if (current.gameStatus == GameStatus.SCRAMBLING || current.gameStatus == GameStatus.AUTO_SOLVING) return

        val lastMove = current.moveHistory.last()
        val inverseMove = lastMove.inverted()

        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            animateSingleMove(inverseMove, durationMs = 130L)
            _uiState.update { state ->
                state.copy(
                    cubeState = state.cubeState.applyMove(inverseMove),
                    activeMove = null,
                    activeMoveProgress = 0f,
                    moveHistory = state.moveHistory.dropLast(1),
                    redoStack = state.redoStack + lastMove,
                    statusBannerMessage = "Langkah terakhir (${lastMove.notation}) dibatalkan."
                )
            }
        }
    }

    /**
     * Executes the next optimal hint move (or next practice algorithm move) with smooth animation.
     */
    fun performHintStep() {
        val current = _uiState.value
        if (current.activeMove != null) return
        if (current.gameStatus == GameStatus.SCRAMBLING || current.gameStatus == GameStatus.AUTO_SOLVING) return

        val hintMove = current.nextHintMove
        if (hintMove == null) {
            _uiState.update { it.copy(statusBannerMessage = "Rubik sudah tersusun rapi!") }
            return
        }

        _uiState.update { it.copy(usedAssistInCurrentRun = true, showVisualGuideOverlay = true) }
        performPlayerMove(hintMove)
    }

    /**
     * Scrambles the cube starting directly from its CURRENT position (preserving & extending moveHistory).
     */
    fun scrambleCube() {
        val current = _uiState.value
        if (current.gameStatus == GameStatus.SCRAMBLING || current.gameStatus == GameStatus.AUTO_SOLVING) return

        animationJob?.cancel()
        stopTimer()

        val count = current.scrambleDifficulty.moveCount
        val lastExistingMove = current.moveHistory.lastOrNull()
        val scrambleSequence = generateScrambleMoves(
            count = count,
            initialLastAxis = lastExistingMove?.axis,
            initialLastLayer = lastExistingMove?.layer
        )

        animationJob = viewModelScope.launch {
            // Scramble directly from the current cubeState & moveHistory
            _uiState.update {
                it.copy(
                    activeMove = null,
                    activeMoveProgress = 0f,
                    redoStack = emptyList(),
                    playerMovesCount = 0,
                    elapsedMillis = 0L,
                    practiceAlgorithm = null,
                    usedAssistInCurrentRun = false,
                    gameStatus = GameStatus.SCRAMBLING,
                    statusBannerMessage = "Mengacak dari posisi sekarang ($count langkah)..."
                )
            }

            for ((index, move) in scrambleSequence.withIndex()) {
                if (!isActive) break
                _uiState.update {
                    it.copy(statusBannerMessage = "Mengacak (${index + 1}/$count): ${move.notation}")
                }
                animateSingleMove(move, durationMs = 65L)
                _uiState.update { state ->
                    state.copy(
                        cubeState = state.cubeState.applyMove(move),
                        activeMove = null,
                        activeMoveProgress = 0f,
                        moveHistory = RubikCubeState.simplifyMoves(state.moveHistory + move)
                    )
                }
            }

            _uiState.update {
                it.copy(
                    gameStatus = GameStatus.READY_TO_PLAY,
                    statusBannerMessage = "Rubik selesai diacak dari posisi terakhir! Gerakkan sisi untuk mulai waktu."
                )
            }
        }
    }

    /**
     * Solves the cube automatically with animated steps.
     * If pressed a second time while [GameStatus.AUTO_SOLVING] is already in progress,
     * immediately skips the remaining animation and snaps straight to the solved state!
     */
    fun instantSolveCube() {
        val current = _uiState.value
        if (current.gameStatus == GameStatus.SCRAMBLING) return

        // Second press during AUTO_SOLVING -> Immediately skip animation and finish instantly!
        if (current.gameStatus == GameStatus.AUTO_SOLVING) {
            animationJob?.cancel()
            stopTimer()
            _uiState.update {
                it.copy(
                    cubeState = RubikCubeState.createSolved(),
                    moveHistory = emptyList(),
                    redoStack = emptyList(),
                    activeMove = null,
                    activeMoveProgress = 0f,
                    practiceAlgorithm = null,
                    gameStatus = GameStatus.IDLE,
                    statusBannerMessage = "Selesai Instan! Proses animasi dilewati."
                )
            }
            return
        }

        animationJob?.cancel()
        stopTimer()

        val solution = current.solutionSteps
        if (solution.isEmpty() || current.cubeState.isSolved()) {
            _uiState.update {
                it.copy(
                    cubeState = RubikCubeState.createSolved(),
                    moveHistory = emptyList(),
                    redoStack = emptyList(),
                    activeMove = null,
                    activeMoveProgress = 0f,
                    practiceAlgorithm = null,
                    gameStatus = GameStatus.IDLE,
                    statusBannerMessage = "Rubik sudah berada dalam posisi selesai!"
                )
            }
            return
        }

        animationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    gameStatus = GameStatus.AUTO_SOLVING,
                    usedAssistInCurrentRun = true,
                    practiceAlgorithm = null,
                    statusBannerMessage = "Menyelesaikan (${solution.size} langkah)... Tekan 'Skip Instan' untuk langsung selesai."
                )
            }

            val stepDuration = if (solution.size > 20) 55L else 75L
            for ((idx, move) in solution.withIndex()) {
                if (!isActive) break
                _uiState.update {
                    it.copy(
                        statusBannerMessage = "Menyelesaikan (${idx + 1}/${solution.size}): ${move.notation} (Tekan tombol lagi untuk Skip)"
                    )
                }
                animateSingleMove(move, durationMs = stepDuration)
                _uiState.update { state ->
                    state.copy(
                        cubeState = state.cubeState.applyMove(move),
                        activeMove = null,
                        activeMoveProgress = 0f,
                        moveHistory = RubikCubeState.simplifyMoves(state.moveHistory + move)
                    )
                }
            }

            _uiState.update {
                it.copy(
                    cubeState = RubikCubeState.createSolved(),
                    moveHistory = emptyList(),
                    redoStack = emptyList(),
                    activeMove = null,
                    activeMoveProgress = 0f,
                    gameStatus = GameStatus.IDLE,
                    statusBannerMessage = "Selesai Instan berhasil! Seluruh 6 sisi telah tersusun sempurna."
                )
            }
        }
    }

    /**
     * Saves the current cube position (specifically the move sequence to reach this position) into Room DB.
     */
    fun saveCurrentPosition(customName: String) {
        val current = _uiState.value
        val sequenceStr = current.currentMoveSequenceNotation
        val count = current.moveHistory.size
        val defaultName = if (customName.isBlank()) {
            if (count == 0) "Posisi Solved (Awal)" else "Posisi Tersimpan ($count langkah)"
        } else {
            customName.trim()
        }

        viewModelScope.launch {
            repository.insertSavedPosition(
                SavedCubePosition(
                    name = defaultName,
                    moveSequence = sequenceStr,
                    movesCount = count
                )
            )
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Posisi '$defaultName' ($count langkah) berhasil disimpan!"
                )
            }
        }
    }

    /**
     * Loads a previously saved cube position by reconstructing its move sequence.
     * Supports either instant loading or animated step-by-step replay of the saved sequence!
     */
    fun loadSavedPosition(saved: SavedCubePosition, animateReplay: Boolean = false) {
        val current = _uiState.value
        if (current.gameStatus == GameStatus.SCRAMBLING || current.gameStatus == GameStatus.AUTO_SOLVING) return

        animationJob?.cancel()
        stopTimer()

        val moves = CubeMove.parseAlgorithm(saved.moveSequence)

        if (!animateReplay || moves.isEmpty()) {
            var targetCube = RubikCubeState.createSolved()
            for (move in moves) {
                targetCube = targetCube.applyMove(move)
            }
            val simplified = RubikCubeState.simplifyMoves(moves)
            _uiState.update {
                it.copy(
                    cubeState = targetCube,
                    activeMove = null,
                    activeMoveProgress = 0f,
                    moveHistory = simplified,
                    redoStack = emptyList(),
                    playerMovesCount = 0,
                    elapsedMillis = 0L,
                    practiceAlgorithm = null,
                    usedAssistInCurrentRun = false,
                    gameStatus = if (targetCube.isSolved()) GameStatus.IDLE else GameStatus.READY_TO_PLAY,
                    statusBannerMessage = "Posisi '${saved.name}' (${simplified.size} langkah) berhasil dimuat!"
                )
            }
        } else {
            animationJob = viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        cubeState = RubikCubeState.createSolved(),
                        activeMove = null,
                        activeMoveProgress = 0f,
                        moveHistory = emptyList(),
                        redoStack = emptyList(),
                        playerMovesCount = 0,
                        elapsedMillis = 0L,
                        practiceAlgorithm = null,
                        usedAssistInCurrentRun = false,
                        gameStatus = GameStatus.SCRAMBLING,
                        statusBannerMessage = "Memutar urutan posisi '${saved.name}' (${moves.size} langkah)..."
                    )
                }

                for ((idx, move) in moves.withIndex()) {
                    if (!isActive) break
                    _uiState.update {
                        it.copy(
                            statusBannerMessage = "Memuat '${saved.name}' (${idx + 1}/${moves.size}): ${move.notation}"
                        )
                    }
                    animateSingleMove(move, durationMs = 65L)
                    _uiState.update { state ->
                        state.copy(
                            cubeState = state.cubeState.applyMove(move),
                            activeMove = null,
                            activeMoveProgress = 0f,
                            moveHistory = RubikCubeState.simplifyMoves(state.moveHistory + move)
                        )
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        gameStatus = if (state.cubeState.isSolved()) GameStatus.IDLE else GameStatus.READY_TO_PLAY,
                        statusBannerMessage = "Posisi '${saved.name}' selesai dimuat!"
                    )
                }
            }
        }
    }

    fun deleteSavedPosition(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedPositionById(id)
        }
    }

    /**
     * Immediately resets the cube to solved state with zero animation delay.
     */
    fun resetCubeImmediately() {
        animationJob?.cancel()
        stopTimer()
        _uiState.update {
            it.copy(
                cubeState = RubikCubeState.createSolved(),
                activeMove = null,
                activeMoveProgress = 0f,
                moveHistory = emptyList(),
                redoStack = emptyList(),
                playerMovesCount = 0,
                elapsedMillis = 0L,
                practiceAlgorithm = null,
                usedAssistInCurrentRun = false,
                gameStatus = GameStatus.IDLE,
                statusBannerMessage = "Rubik di-reset ke kondisi awal."
            )
        }
    }

    /**
     * Loads a beginner guide algorithm onto the 3D cube for interactive visual training.
     */
    fun startAlgorithmPractice(step: GuideStep) {
        val parsedMoves = CubeMove.parseAlgorithm(step.algorithm)
        if (parsedMoves.isEmpty()) return

        animationJob?.cancel()
        stopTimer()

        _uiState.update {
            it.copy(
                selectedTab = AppTab.PLAY,
                showVisualGuideOverlay = true,
                practiceAlgorithm = ActivePracticeAlgorithm(
                    title = "Langkah ${step.stepNumber}: ${step.title}",
                    moves = parsedMoves,
                    currentIndex = 0
                ),
                statusBannerMessage = "Mode Latihan: Ikuti panah 3D atau tekan 'Langkah Berikutnya' untuk rumus ${step.algorithm}"
            )
        }
    }

    fun clearAlgorithmPractice() {
        _uiState.update {
            it.copy(
                practiceAlgorithm = null,
                statusBannerMessage = "Mode latihan rumus ditutup."
            )
        }
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private suspend fun animateSingleMove(move: CubeMove, durationMs: Long) {
        val steps = (durationMs / 16L).coerceAtLeast(3L).toInt()
        val delayPerStep = durationMs / steps
        _uiState.update { it.copy(activeMove = move, activeMoveProgress = 0f) }
        for (i in 1..steps) {
            delay(delayPerStep)
            val linear = i.toFloat() / steps.toFloat()
            // Smoothstep easing for natural mechanical cube snap
            val eased = linear * linear * (3f - 2f * linear)
            _uiState.update { it.copy(activeMoveProgress = eased) }
        }
    }

    private fun generateScrambleMoves(
        count: Int,
        initialLastAxis: Axis? = null,
        initialLastLayer: Int? = null
    ): List<CubeMove> {
        val allMoves = CubeMove.ALL_OUTER_MOVES
        val result = mutableListOf<CubeMove>()
        var lastAxis: Axis? = initialLastAxis
        var lastLayer: Int? = initialLastLayer

        repeat(count) {
            val candidates = allMoves.filter { move ->
                !(move.axis == lastAxis && move.layer == lastLayer)
            }
            val chosen = candidates[Random.nextInt(candidates.size)]
            result.add(chosen)
            lastAxis = chosen.axis
            lastLayer = chosen.layer
        }
        return result
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerStartRealtime = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                _uiState.update { it.copy(elapsedMillis = now - timerStartRealtime) }
                delay(50L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}

class RubikViewModelFactory(
    private val repository: SolveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RubikViewModel::class.java)) {
            return RubikViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
