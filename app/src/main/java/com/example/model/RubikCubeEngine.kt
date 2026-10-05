package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CubeBlue
import com.example.ui.theme.CubeCoreDark
import com.example.ui.theme.CubeGreen
import com.example.ui.theme.CubeOrange
import com.example.ui.theme.CubeRed
import com.example.ui.theme.CubeWhite
import com.example.ui.theme.CubeYellow
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Axis { X, Y, Z }

enum class CubeColor(val labelId: String, val displayColor: Color, val shortCode: String) {
    WHITE("Putih (Atas)", CubeWhite, "U"),
    YELLOW("Kuning (Bawah)", CubeYellow, "D"),
    RED("Merah (Kanan)", CubeRed, "R"),
    ORANGE("Oranye (Kiri)", CubeOrange, "L"),
    GREEN("Hijau (Depan)", CubeGreen, "F"),
    BLUE("Biru (Belakang)", CubeBlue, "B"),
    CORE("Inti", CubeCoreDark, "")
}

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    fun get(axis: Axis): Int = when (axis) {
        Axis.X -> x
        Axis.Y -> y
        Axis.Z -> z
    }

    fun toVec3f(): Vec3f = Vec3f(x.toFloat(), y.toFloat(), z.toFloat())
}

data class Vec3f(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3f) = Vec3f(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3f(x * scalar, y * scalar, z * scalar)

    fun dot(other: Vec3f): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3f): Vec3f = Vec3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

/**
 * Discrete 90-degree right-hand rule rotation around [axis] by [dir] (+1 or -1).
 */
fun rotateVec3i(v: Vec3i, axis: Axis, dir: Int): Vec3i {
    // Standard right-hand rule 90 deg rotation:
    // Around X (+1): (x, -z, y), (-1): (x, z, -y)
    // Around Y (+1): (z, y, -x), (-1): (-z, y, x)
    // Around Z (+1): (-y, x, z), (-1): (y, -x, z)
    return when (axis) {
        Axis.X -> if (dir > 0) Vec3i(v.x, -v.z, v.y) else Vec3i(v.x, v.z, -v.y)
        Axis.Y -> if (dir > 0) Vec3i(v.z, v.y, -v.x) else Vec3i(-v.z, v.y, v.x)
        Axis.Z -> if (dir > 0) Vec3i(-v.y, v.x, v.z) else Vec3i(v.y, -v.x, v.z)
    }
}

/**
 * Continuous 3D rotation around [axis] by [angleRad] radians (matching rotateVec3i when angleRad = dir * PI/2).
 */
fun rotateVec3f(v: Vec3f, axis: Axis, angleRad: Float): Vec3f {
    val c = cos(angleRad)
    val s = sin(angleRad)
    return when (axis) {
        Axis.X -> Vec3f(
            x = v.x,
            y = v.y * c - v.z * s,
            z = v.y * s + v.z * c
        )
        Axis.Y -> Vec3f(
            x = v.x * c + v.z * s,
            y = v.y,
            z = -v.x * s + v.z * c
        )
        Axis.Z -> Vec3f(
            x = v.x * c - v.y * s,
            y = v.x * s + v.y * c,
            z = v.z
        )
    }
}

/**
 * Represents a single 90-degree turn of a 3x3x3 Rubik's Cube layer.
 * [axis]: X (L/M/R), Y (D/E/U), Z (B/S/F)
 * [layer]: -1, 0, or +1
 * [dir]: +1 or -1 (Right-Hand Rule around [axis])
 */
data class CubeMove(
    val axis: Axis,
    val layer: Int,
    val dir: Int
) {
    fun inverted(): CubeMove = copy(dir = -dir)

    /**
     * Standard Rubik's notation label (U, U', D, D', R, R', L, L', F, F', B, B', M, E, S)
     */
    val notation: String
        get() = when (axis) {
            Axis.Y -> when (layer) {
                1 -> if (dir == -1) "U" else "U'"
                -1 -> if (dir == 1) "D" else "D'"
                else -> if (dir == 1) "E" else "E'"
            }
            Axis.X -> when (layer) {
                1 -> if (dir == -1) "R" else "R'"
                -1 -> if (dir == 1) "L" else "L'"
                else -> if (dir == 1) "M" else "M'"
            }
            Axis.Z -> when (layer) {
                1 -> if (dir == -1) "F" else "F'"
                -1 -> if (dir == 1) "B" else "B'"
                else -> if (dir == -1) "S" else "S'"
            }
        }

    val indonesianDescription: String
        get() = when (notation) {
            "U" -> "Putar Sisi Atas searah jarum jam"
            "U'" -> "Putar Sisi Atas berlawanan jarum jam"
            "D" -> "Putar Sisi Bawah searah jarum jam"
            "D'" -> "Putar Sisi Bawah berlawanan jarum jam"
            "R" -> "Putar Sisi Kanan ke atas (searah jarum jam)"
            "R'" -> "Putar Sisi Kanan ke bawah (berlawanan)"
            "L" -> "Putar Sisi Kiri ke bawah (searah jarum jam)"
            "L'" -> "Putar Sisi Kiri ke atas (berlawanan)"
            "F" -> "Putar Sisi Depan ke kanan (searah jarum jam)"
            "F'" -> "Putar Sisi Depan ke kiri (berlawanan)"
            "B" -> "Putar Sisi Belakang searah jarum jam"
            "B'" -> "Putar Sisi Belakang berlawanan jarum jam"
            "M" -> "Putar Lapisan Tengah Vertikal ke bawah"
            "M'" -> "Putar Lapisan Tengah Vertikal ke atas"
            "E" -> "Putar Lapisan Tengah Horizontal ke kanan"
            "E'" -> "Putar Lapisan Tengah Horizontal ke kiri"
            "S" -> "Putar Lapisan Tengah Dalam searah depan"
            "S'" -> "Putar Lapisan Tengah Dalam berlawanan depan"
            else -> "Putar lapisan $notation"
        }

    companion object {
        fun fromNotation(token: String): List<CubeMove> {
            val clean = token.trim()
            if (clean.isEmpty()) return emptyList()
            val isDouble = clean.endsWith("2")
            val isPrime = clean.endsWith("'")
            val baseChar = clean.first().uppercaseChar()

            val singleMove = when (baseChar) {
                'U' -> CubeMove(Axis.Y, 1, if (isPrime) 1 else -1)
                'D' -> CubeMove(Axis.Y, -1, if (isPrime) -1 else 1)
                'R' -> CubeMove(Axis.X, 1, if (isPrime) 1 else -1)
                'L' -> CubeMove(Axis.X, -1, if (isPrime) -1 else 1)
                'F' -> CubeMove(Axis.Z, 1, if (isPrime) 1 else -1)
                'B' -> CubeMove(Axis.Z, -1, if (isPrime) -1 else 1)
                'M' -> CubeMove(Axis.X, 0, if (isPrime) -1 else 1)
                'E' -> CubeMove(Axis.Y, 0, if (isPrime) -1 else 1)
                'S' -> CubeMove(Axis.Z, 0, if (isPrime) 1 else -1)
                else -> return emptyList()
            }
            return if (isDouble) listOf(singleMove, singleMove) else listOf(singleMove)
        }

        fun parseAlgorithm(alg: String): List<CubeMove> {
            return alg.split(" ", "\n", "\t")
                .filter { it.isNotBlank() }
                .flatMap { fromNotation(it) }
        }

        val ALL_OUTER_MOVES: List<CubeMove> = listOf(
            "U", "U'", "D", "D'", "R", "R'", "L", "L'", "F", "F'", "B", "B'"
        ).flatMap { fromNotation(it) }
    }
}

data class Cubie(
    val id: Int,
    val pos: Vec3i,
    val stickers: Map<Vec3i, CubeColor>
)

data class RubikCubeState(
    val cubies: List<Cubie>
) {
    fun applyMove(move: CubeMove): RubikCubeState {
        val updated = cubies.map { cubie ->
            if (cubie.pos.get(move.axis) == move.layer) {
                val newPos = rotateVec3i(cubie.pos, move.axis, move.dir)
                val newStickers = cubie.stickers.entries.associate { (normal, color) ->
                    rotateVec3i(normal, move.axis, move.dir) to color
                }
                cubie.copy(pos = newPos, stickers = newStickers)
            } else {
                cubie
            }
        }
        return RubikCubeState(updated)
    }

    fun isSolved(): Boolean {
        // A cube is solved when for every outer normal direction, all 9 stickers have the exact same color
        val normals = listOf(
            Vec3i(1, 0, 0), Vec3i(-1, 0, 0),
            Vec3i(0, 1, 0), Vec3i(0, -1, 0),
            Vec3i(0, 0, 1), Vec3i(0, 0, -1)
        )
        for (normal in normals) {
            val colorsOnFace = cubies.mapNotNull { it.stickers[normal] }
            if (colorsOnFace.size != 9) return false
            val first = colorsOnFace.first()
            if (colorsOnFace.any { it != first }) return false
        }
        return true
    }

    /**
     * Extracts a 3x3 color grid for a specific face normal for the 2D Mini-Map / Unfolded Net view.
     */
    fun getFaceGrid(normal: Vec3i): Array<Array<CubeColor>> {
        val grid = Array(3) { Array(3) { CubeColor.CORE } }
        for (cubie in cubies) {
            val color = cubie.stickers[normal] ?: continue
            // Map 3D position to 2D (row, col) in [0..2]
            val (r, c) = when (normal) {
                Vec3i(0, 1, 0) -> (cubie.pos.z + 1) to (cubie.pos.x + 1) // U: top=Back(-Z), bottom=Front(+Z), left=-X, right=+X
                Vec3i(0, -1, 0) -> (1 - cubie.pos.z) to (cubie.pos.x + 1) // D: top=Front(+Z), bottom=Back(-Z)
                Vec3i(0, 0, 1) -> (1 - cubie.pos.y) to (cubie.pos.x + 1) // F: top=+Y, left=-X
                Vec3i(0, 0, -1) -> (1 - cubie.pos.y) to (1 - cubie.pos.x) // B: top=+Y, left=+X
                Vec3i(1, 0, 0) -> (1 - cubie.pos.y) to (1 - cubie.pos.z) // R: top=+Y, left=+Z
                Vec3i(-1, 0, 0) -> (1 - cubie.pos.y) to (cubie.pos.z + 1) // L: top=+Y, left=-Z
                else -> 0 to 0
            }
            if (r in 0..2 && c in 0..2) {
                grid[r][c] = color
            }
        }
        return grid
    }

    companion object {
        fun createSolved(): RubikCubeState {
            val list = mutableListOf<Cubie>()
            var idCounter = 0
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        val stickers = mutableMapOf<Vec3i, CubeColor>()
                        if (x == 1) stickers[Vec3i(1, 0, 0)] = CubeColor.RED
                        if (x == -1) stickers[Vec3i(-1, 0, 0)] = CubeColor.ORANGE
                        if (y == 1) stickers[Vec3i(0, 1, 0)] = CubeColor.WHITE
                        if (y == -1) stickers[Vec3i(0, -1, 0)] = CubeColor.YELLOW
                        if (z == 1) stickers[Vec3i(0, 0, 1)] = CubeColor.GREEN
                        if (z == -1) stickers[Vec3i(0, 0, -1)] = CubeColor.BLUE
                        list.add(Cubie(id = idCounter++, pos = Vec3i(x, y, z), stickers = stickers))
                    }
                }
            }
            return RubikCubeState(list)
        }

        /**
         * Reduces move history by cancelling adjacent moves on the same axis and layer (modulo 4).
         */
        fun simplifyMoves(moves: List<CubeMove>): List<CubeMove> {
            val stack = mutableListOf<CubeMove>()
            for (move in moves) {
                if (stack.isNotEmpty()) {
                    val last = stack.last()
                    if (last.axis == move.axis && last.layer == move.layer) {
                        stack.removeAt(stack.lastIndex)
                        val totalTurns = ((last.dir + move.dir) % 4 + 4) % 4
                        when (totalTurns) {
                            1 -> stack.add(CubeMove(move.axis, move.layer, 1))
                            2 -> {
                                stack.add(CubeMove(move.axis, move.layer, 1))
                                stack.add(CubeMove(move.axis, move.layer, 1))
                            }
                            3 -> stack.add(CubeMove(move.axis, move.layer, -1))
                        }
                        continue
                    }
                }
                stack.add(move)
            }
            return stack
        }

        /**
         * Returns the simplified inverse move list to solve the cube from its move history.
         */
        fun computeSolutionMoves(history: List<CubeMove>): List<CubeMove> {
            val simplified = simplifyMoves(history)
            return simplified.asReversed().map { it.inverted() }
        }
    }
}
