package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CubeMove
import com.example.model.RubikCubeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rubik 3D", appName)
    }

    @Test
    fun `rubik cube scramble and instant solve restores solved state`() {
        var cube = RubikCubeState.createSolved()
        assertTrue(cube.isSolved())

        val scramble = CubeMove.parseAlgorithm("R U R' U' F D L' B2")
        for (move in scramble) {
            cube = cube.applyMove(move)
        }
        assertFalse(cube.isSolved())

        val solution = RubikCubeState.computeSolutionMoves(scramble)
        for (move in solution) {
            cube = cube.applyMove(move)
        }
        assertTrue(cube.isSolved())
    }
}
