package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Axis
import com.example.model.CubeColor
import com.example.model.CubeMove
import com.example.model.RubikCubeState
import com.example.model.Vec3f
import com.example.model.Vec3i
import com.example.model.rotateVec3f
import com.example.model.rotateVec3i
import com.example.ui.theme.CyanPrimary
import com.example.viewmodel.ControlMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class ProjectedVertex(
    val cam: Vec3f,
    val screen: Offset
)

private data class RenderableQuad(
    val cubieId: Int,
    val cubiePos: Vec3i,
    val faceNormal: Vec3i,
    val stickerColor: CubeColor?,
    val outerScreen: List<Offset>,
    val stickerScreen: List<Offset>,
    val avgDepthZ: Float,
    val brightness: Float,
    val centerScreen: Offset,
    val isHintLayer: Boolean
)

private data class HitSticker(
    val cubieId: Int,
    val cubiePos: Vec3i,
    val faceNormal: Vec3i
)

private val SIX_NORMALS = listOf(
    Vec3i(1, 0, 0),
    Vec3i(-1, 0, 0),
    Vec3i(0, 1, 0),
    Vec3i(0, -1, 0),
    Vec3i(0, 0, 1),
    Vec3i(0, 0, -1)
)

@Composable
fun Rubik3DViewport(
    cubeState: RubikCubeState,
    activeMove: CubeMove?,
    activeMoveProgress: Float,
    cameraYaw: Float,
    cameraPitch: Float,
    cameraZoom: Float,
    controlMode: ControlMode,
    showVisualGuideOverlay: Boolean,
    showFaceNotationBadges: Boolean,
    showMiniNet: Boolean,
    nextHintMove: CubeMove?,
    onPlayerMove: (CubeMove) -> Unit,
    onCameraOrbit: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCubeState by rememberUpdatedState(cubeState)
    val currentControlMode by rememberUpdatedState(controlMode)
    val currentYaw by rememberUpdatedState(cameraYaw)
    val currentPitch by rememberUpdatedState(cameraPitch)
    val currentZoom by rememberUpdatedState(cameraZoom)
    val currentOnPlayerMove by rememberUpdatedState(onPlayerMove)
    val currentOnCameraOrbit by rememberUpdatedState(onCameraOrbit)

    var touchedCubieId by remember { mutableStateOf<Int?>(null) }
    var lastRenderedQuads by remember { mutableStateOf<List<RenderableQuad>>(emptyList()) }
    var canvasCenter by remember { mutableStateOf(Offset.Zero) }
    var baseScalePx by remember { mutableStateOf(100f) }

    val swipeThresholdPx = with(LocalDensity.current) { 20.dp.toPx() }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("rubik_3d_canvas")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position

                        // Check if touch hit an outer sticker quad (search from frontmost to backmost)
                        val hitQuad = if (currentControlMode == ControlMode.SWIPE_CUBE) {
                            lastRenderedQuads.asReversed().firstOrNull { quad ->
                                quad.stickerColor != null && isPointInPolygon(downPos, quad.outerScreen)
                            }
                        } else {
                            null
                        }

                        val hitInfo = hitQuad?.let {
                            HitSticker(it.cubieId, it.cubiePos, it.faceNormal)
                        }
                        touchedCubieId = hitInfo?.cubieId

                        var swipeTriggered = false
                        var prevPos = downPos

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val currentPos = change.position
                                if (hitInfo == null) {
                                    // Orbit 3D camera freely
                                    val dx = currentPos.x - prevPos.x
                                    val dy = currentPos.y - prevPos.y
                                    if (abs(dx) > 0.2f || abs(dy) > 0.2f) {
                                        currentOnCameraOrbit(dx * 0.48f, dy * 0.42f)
                                    }
                                    prevPos = currentPos
                                } else if (!swipeTriggered) {
                                    // Direct 3D face swipe manipulation
                                    val totalDrag = currentPos - downPos
                                    val dragDist = sqrt(totalDrag.x * totalDrag.x + totalDrag.y * totalDrag.y)
                                    if (dragDist >= swipeThresholdPx) {
                                        val detectedMove = resolve3DSwipeMove(
                                            cubiePos = hitInfo.cubiePos,
                                            faceNormal = hitInfo.faceNormal,
                                            dragDelta = totalDrag,
                                            yawDeg = currentYaw,
                                            pitchDeg = currentPitch,
                                            scalePx = baseScalePx,
                                            center = canvasCenter
                                        )
                                        if (detectedMove != null) {
                                            swipeTriggered = true
                                            touchedCubieId = null
                                            currentOnPlayerMove(detectedMove)
                                        }
                                    }
                                }
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        touchedCubieId = null
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            canvasCenter = center
            val unitScale = (min(w, h) * 0.165f) * cameraZoom
            baseScalePx = unitScale

            // Subtle studio radial floor glow behind the 3D cube
            drawCircle(
                color = CyanPrimary.copy(alpha = 0.07f),
                radius = min(w, h) * 0.44f,
                center = center
            )
            drawCircle(
                color = CyanPrimary.copy(alpha = 0.04f),
                radius = min(w, h) * 0.30f,
                center = center
            )

            val yawRad = (cameraYaw * PI / 180.0).toFloat()
            val pitchRad = (cameraPitch * PI / 180.0).toFloat()
            val lightDir = Vec3f(0.35f, 0.55f, 0.75f) // Camera-space directional light

            val quads = mutableListOf<RenderableQuad>()

            for (cubie in cubeState.cubies) {
                val isAnimating = activeMove != null && cubie.pos.get(activeMove.axis) == activeMove.layer
                val animAngleRad = if (isAnimating && activeMove != null) {
                    activeMove.dir * activeMoveProgress * (PI.toFloat() / 2f)
                } else {
                    0f
                }

                val isHintLayer = showVisualGuideOverlay &&
                    nextHintMove != null &&
                    cubie.pos.get(nextHintMove.axis) == nextHintMove.layer

                val baseCenter3D = cubie.pos.toVec3f()

                for (normal in SIX_NORMALS) {
                    val stickerColor = cubie.stickers[normal]
                    // Optimization: Only draw internal black faces if the cube is currently mid-rotation
                    if (stickerColor == null && activeMove == null) continue

                    val (t1, t2) = getFaceTangents(normal)
                    val nVec = normal.toVec3f()
                    val t1Vec = t1.toVec3f()
                    val t2Vec = t2.toVec3f()

                    // Cubie half-size is 0.48f (leaving a realistic 0.04f seam between cubies)
                    val halfCubie = 0.485f
                    val halfSticker = 0.415f
                    val faceCenter3D = baseCenter3D + (nVec * halfCubie)

                    val outerCorners3D = listOf(
                        faceCenter3D + (t1Vec * -halfCubie) + (t2Vec * -halfCubie),
                        faceCenter3D + (t1Vec * halfCubie) + (t2Vec * -halfCubie),
                        faceCenter3D + (t1Vec * halfCubie) + (t2Vec * halfCubie),
                        faceCenter3D + (t1Vec * -halfCubie) + (t2Vec * halfCubie)
                    )

                    val stickerCorners3D = listOf(
                        faceCenter3D + (t1Vec * -halfSticker) + (t2Vec * -halfSticker),
                        faceCenter3D + (t1Vec * halfSticker) + (t2Vec * -halfSticker),
                        faceCenter3D + (t1Vec * halfSticker) + (t2Vec * halfSticker),
                        faceCenter3D + (t1Vec * -halfSticker) + (t2Vec * halfSticker)
                    )

                    val projOuter = outerCorners3D.map { pt ->
                        val worldPt = if (isAnimating && activeMove != null) {
                            rotateVec3f(pt, activeMove.axis, animAngleRad)
                        } else {
                            pt
                        }
                        projectPoint(worldPt, yawRad, pitchRad, unitScale, center)
                    }

                    // Backface culling via 2D signed area of projected quad
                    val e1x = projOuter[1].screen.x - projOuter[0].screen.x
                    val e1y = projOuter[1].screen.y - projOuter[0].screen.y
                    val e2x = projOuter[2].screen.x - projOuter[0].screen.x
                    val e2y = projOuter[2].screen.y - projOuter[0].screen.y
                    val crossZ = e1x * e2y - e1y * e2x
                    // In screen coordinates (Y down), counter-clockwise in 3D projects to crossZ < 0
                    if (crossZ >= 0f) continue

                    val projSticker = stickerCorners3D.map { pt ->
                        val worldPt = if (isAnimating && activeMove != null) {
                            rotateVec3f(pt, activeMove.axis, animAngleRad)
                        } else {
                            pt
                        }
                        projectPoint(worldPt, yawRad, pitchRad, unitScale, center).screen
                    }

                    val worldNormal = if (isAnimating && activeMove != null) {
                        rotateVec3f(nVec, activeMove.axis, animAngleRad)
                    } else {
                        nVec
                    }
                    val camNormal = worldToCamera(worldNormal, yawRad, pitchRad)
                    val diffuse = camNormal.dot(lightDir).coerceIn(0f, 1f)
                    val brightness = 0.72f + 0.28f * diffuse

                    val avgZ = (projOuter[0].cam.z + projOuter[1].cam.z + projOuter[2].cam.z + projOuter[3].cam.z) * 0.25f
                    val centerScreen = Offset(
                        (projSticker[0].x + projSticker[2].x) * 0.5f,
                        (projSticker[0].y + projSticker[2].y) * 0.5f
                    )

                    quads.add(
                        RenderableQuad(
                            cubieId = cubie.id,
                            cubiePos = cubie.pos,
                            faceNormal = normal,
                            stickerColor = stickerColor,
                            outerScreen = projOuter.map { it.screen },
                            stickerScreen = projSticker,
                            avgDepthZ = avgZ,
                            brightness = brightness,
                            centerScreen = centerScreen,
                            isHintLayer = isHintLayer
                        )
                    )
                }
            }

            // Sort back-to-front (smaller cam.z is further away)
            quads.sortBy { it.avgDepthZ }
            lastRenderedQuads = quads

            // Draw each visible quad
            val path = Path()
            for (quad in quads) {
                // 1. Draw dark plastic bevel base
                path.reset()
                path.moveTo(quad.outerScreen[0].x, quad.outerScreen[0].y)
                path.lineTo(quad.outerScreen[1].x, quad.outerScreen[1].y)
                path.lineTo(quad.outerScreen[2].x, quad.outerScreen[2].y)
                path.lineTo(quad.outerScreen[3].x, quad.outerScreen[3].y)
                path.close()

                val basePlasticColor = if (quad.isHintLayer) {
                    Color(0xFF1E293B)
                } else {
                    Color(0xFF0F172A)
                }
                drawPath(path = path, color = shadeColor(basePlasticColor, quad.brightness))
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.85f),
                    style = Stroke(width = 1.5f, join = StrokeJoin.Round)
                )

                // 2. Draw colored sticker if outer face
                val sticker = quad.stickerColor
                if (sticker != null) {
                    path.reset()
                    path.moveTo(quad.stickerScreen[0].x, quad.stickerScreen[0].y)
                    path.lineTo(quad.stickerScreen[1].x, quad.stickerScreen[1].y)
                    path.lineTo(quad.stickerScreen[2].x, quad.stickerScreen[2].y)
                    path.lineTo(quad.stickerScreen[3].x, quad.stickerScreen[3].y)
                    path.close()

                    val litStickerColor = shadeColor(sticker.displayColor, quad.brightness)
                    drawPath(path = path, color = litStickerColor)

                    // Highlight border if touched or part of hint layer
                    val isTouched = quad.cubieId == touchedCubieId
                    if (isTouched) {
                        drawPath(
                            path = path,
                            color = CyanPrimary,
                            style = Stroke(width = 5f, join = StrokeJoin.Round)
                        )
                    } else if (quad.isHintLayer) {
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.65f),
                            style = Stroke(width = 2.6f, join = StrokeJoin.Round)
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = Color.Black.copy(alpha = 0.35f),
                            style = Stroke(width = 1.2f, join = StrokeJoin.Round)
                        )
                    }

                    // Draw face notation letter (U, D, L, R, F, B) on center pieces
                    val isCenterPiece = abs(quad.cubiePos.x) + abs(quad.cubiePos.y) + abs(quad.cubiePos.z) == 1
                    if (showFaceNotationBadges && isCenterPiece) {
                        val badgeText = when (quad.faceNormal) {
                            Vec3i(0, 1, 0) -> "U"
                            Vec3i(0, -1, 0) -> "D"
                            Vec3i(1, 0, 0) -> "R"
                            Vec3i(-1, 0, 0) -> "L"
                            Vec3i(0, 0, 1) -> "F"
                            Vec3i(0, 0, -1) -> "B"
                            else -> ""
                        }
                        if (badgeText.isNotEmpty()) {
                            labelPaint.textSize = unitScale * 0.34f
                            labelPaint.color = if (sticker == CubeColor.WHITE || sticker == CubeColor.YELLOW) {
                                Color(0xCC0F172A).toArgb()
                            } else {
                                Color(0xE6FFFFFF).toArgb()
                            }
                            val fontMetrics = labelPaint.fontMetrics
                            val textOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
                            drawContext.canvas.nativeCanvas.drawText(
                                badgeText,
                                quad.centerScreen.x,
                                quad.centerScreen.y - textOffset,
                                labelPaint
                            )
                        }
                    }
                }
            }

            // 3. Draw 3D Visual Guide Arrow for Beginner Hint / Active Practice Step
            if (showVisualGuideOverlay && nextHintMove != null && activeMove == null) {
                draw3DMoveGuideArrow(
                    move = nextHintMove,
                    yawRad = yawRad,
                    pitchRad = pitchRad,
                    unitScale = unitScale,
                    center = center
                )
            }
        }

        // Top-right 2D Mini-Net (Jaring-jaring 6 Sisi) so player can inspect all sides at a glance
        if (showMiniNet) {
            CubeMiniNetOverlay(
                cubeState = currentCubeState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
            )
        }
    }
}

/**
 * Draws a high-visibility 3D directional arrow directly over the target slice to guide beginners.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DMoveGuideArrow(
    move: CubeMove,
    yawRad: Float,
    pitchRad: Float,
    unitScale: Float,
    center: Offset
) {
    // Pick a visible face perpendicular to the rotation axis to draw the swipe arrow across
    val candidateFaces = when (move.axis) {
        Axis.Y -> listOf(Vec3i(0, 0, 1), Vec3i(1, 0, 0), Vec3i(0, 0, -1), Vec3i(-1, 0, 0))
        Axis.X -> listOf(Vec3i(0, 0, 1), Vec3i(0, 1, 0), Vec3i(0, 0, -1), Vec3i(0, -1, 0))
        Axis.Z -> listOf(Vec3i(0, 1, 0), Vec3i(1, 0, 0), Vec3i(0, -1, 0), Vec3i(-1, 0, 0))
    }

    // Choose the face whose normal points most strongly toward the camera (+Z in camera space)
    val bestFace = candidateFaces.maxByOrNull { normal ->
        worldToCamera(normal.toVec3f(), yawRad, pitchRad).z
    } ?: return

    // Tangent direction of motion on bestFace when rotating around move.axis by move.dir:
    // Velocity vector v = omega x r, where omega = axis * dir and r = bestFace
    val omega = when (move.axis) {
        Axis.X -> Vec3i(move.dir, 0, 0)
        Axis.Y -> Vec3i(0, move.dir, 0)
        Axis.Z -> Vec3i(0, 0, move.dir)
    }
    val tangentDir = Vec3f(
        (omega.y * bestFace.z - omega.z * bestFace.y).toFloat(),
        (omega.z * bestFace.x - omega.x * bestFace.z).toFloat(),
        (omega.x * bestFace.y - omega.y * bestFace.x).toFloat()
    )

    val axisVec = when (move.axis) {
        Axis.X -> Vec3f(1f, 0f, 0f)
        Axis.Y -> Vec3f(0f, 1f, 0f)
        Axis.Z -> Vec3f(0f, 0f, 1f)
    }

    val sliceCenterOnFace = (axisVec * move.layer.toFloat()) + (bestFace.toVec3f() * 1.56f)
    val start3D = sliceCenterOnFace - (tangentDir * 0.95f)
    val end3D = sliceCenterOnFace + (tangentDir * 0.95f)

    val startScreen = projectPoint(start3D, yawRad, pitchRad, unitScale, center).screen
    val endScreen = projectPoint(end3D, yawRad, pitchRad, unitScale, center).screen

    // Outer glow + crisp arrow line
    drawLine(
        color = Color.Black.copy(alpha = 0.65f),
        start = startScreen,
        end = endScreen,
        strokeWidth = 16f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = CyanPrimary,
        start = startScreen,
        end = endScreen,
        strokeWidth = 9f,
        cap = StrokeCap.Round
    )

    // Arrowhead
    val angle = atan2(endScreen.y - startScreen.y, endScreen.x - startScreen.x)
    val headLen = 28f
    val headAngle1 = angle + (PI * 0.8f).toFloat()
    val headAngle2 = angle - (PI * 0.8f).toFloat()

    val p1 = Offset(
        endScreen.x + headLen * cos(headAngle1),
        endScreen.y + headLen * sin(headAngle1)
    )
    val p2 = Offset(
        endScreen.x + headLen * cos(headAngle2),
        endScreen.y + headLen * sin(headAngle2)
    )

    val arrowHeadPath = Path().apply {
        moveTo(endScreen.x, endScreen.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path = arrowHeadPath, color = Color.Black.copy(alpha = 0.7f), style = Stroke(width = 6f))
    drawPath(path = arrowHeadPath, color = CyanPrimary)
}

/**
 * Compact 2D unfolded net of all 6 faces (U, L, F, R, B, D) so the player can always see hidden sides.
 */
@Composable
fun CubeMiniNetOverlay(
    cubeState: RubikCubeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Peta 6 Sisi",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBAE6FD),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        // Row 1: U (aligned above F)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(23.dp))
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(0, 1, 0)))
            Spacer(modifier = Modifier.width(46.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        // Row 2: L - F - R - B
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(-1, 0, 0)))
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(0, 0, 1)))
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(1, 0, 0)))
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(0, 0, -1)))
        }
        Spacer(modifier = Modifier.height(2.dp))
        // Row 3: D (aligned below F)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(23.dp))
            MiniFaceGrid(grid = cubeState.getFaceGrid(Vec3i(0, -1, 0)))
            Spacer(modifier = Modifier.width(46.dp))
        }
    }
}

@Composable
private fun MiniFaceGrid(grid: Array<Array<CubeColor>>) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        for (r in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (c in 0..2) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(grid[r][c].displayColor, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

/**
 * Returns two right-handed tangent vectors (t1, t2) on the face with normal [n] such that t1 x t2 = n.
 */
private fun getFaceTangents(n: Vec3i): Pair<Vec3i, Vec3i> {
    return when (n) {
        Vec3i(0, 0, 1) -> Vec3i(1, 0, 0) to Vec3i(0, 1, 0)     // Front (+Z)
        Vec3i(0, 0, -1) -> Vec3i(-1, 0, 0) to Vec3i(0, 1, 0)   // Back (-Z)
        Vec3i(1, 0, 0) -> Vec3i(0, 0, -1) to Vec3i(0, 1, 0)    // Right (+X)
        Vec3i(-1, 0, 0) -> Vec3i(0, 0, 1) to Vec3i(0, 1, 0)    // Left (-X)
        Vec3i(0, 1, 0) -> Vec3i(1, 0, 0) to Vec3i(0, 0, -1)    // Up (+Y)
        Vec3i(0, -1, 0) -> Vec3i(1, 0, 0) to Vec3i(0, 0, 1)    // Down (-Y)
        else -> Vec3i(1, 0, 0) to Vec3i(0, 1, 0)
    }
}

private fun worldToCamera(v: Vec3f, yawRad: Float, pitchRad: Float): Vec3f {
    // 1. Yaw around world Y axis
    val cy = cos(yawRad)
    val sy = sin(yawRad)
    val x1 = v.x * cy + v.z * sy
    val y1 = v.y
    val z1 = -v.x * sy + v.z * cy

    // 2. Pitch around camera X axis
    val cp = cos(pitchRad)
    val sp = sin(pitchRad)
    val x2 = x1
    val y2 = y1 * cp - z1 * sp
    val z2 = y1 * sp + z1 * cp

    return Vec3f(x2, y2, z2)
}

private fun projectPoint(
    worldPt: Vec3f,
    yawRad: Float,
    pitchRad: Float,
    unitScale: Float,
    center: Offset
): ProjectedVertex {
    val cam = worldToCamera(worldPt, yawRad, pitchRad)
    val camDist = 9.0f
    val perspective = camDist / (camDist - cam.z).coerceAtLeast(2.0f)
    val sx = center.x + cam.x * unitScale * perspective
    val sy = center.y - cam.y * unitScale * perspective
    return ProjectedVertex(cam = cam, screen = Offset(sx, sy))
}

/**
 * Resolves a 2D touch drag vector on a specific 3D sticker face into the exact 3D CubeMove.
 */
private fun resolve3DSwipeMove(
    cubiePos: Vec3i,
    faceNormal: Vec3i,
    dragDelta: Offset,
    yawDeg: Float,
    pitchDeg: Float,
    scalePx: Float,
    center: Offset
): CubeMove? {
    val yawRad = (yawDeg * PI / 180.0).toFloat()
    val pitchRad = (pitchDeg * PI / 180.0).toFloat()

    val (t1, t2) = getFaceTangents(faceNormal)
    val origin3D = cubiePos.toVec3f() + (faceNormal.toVec3f() * 0.5f)
    val origin2D = projectPoint(origin3D, yawRad, pitchRad, scalePx, center).screen

    val p1Screen = projectPoint(origin3D + t1.toVec3f(), yawRad, pitchRad, scalePx, center).screen
    val p2Screen = projectPoint(origin3D + t2.toVec3f(), yawRad, pitchRad, scalePx, center).screen

    val dir1 = Offset(p1Screen.x - origin2D.x, p1Screen.y - origin2D.y)
    val dir2 = Offset(p2Screen.x - origin2D.x, p2Screen.y - origin2D.y)

    val len1 = sqrt(dir1.x * dir1.x + dir1.y * dir1.y).coerceAtLeast(1f)
    val len2 = sqrt(dir2.x * dir2.x + dir2.y * dir2.y).coerceAtLeast(1f)

    val score1 = (dragDelta.x * dir1.x + dragDelta.y * dir1.y) / len1
    val score2 = (dragDelta.x * dir2.x + dragDelta.y * dir2.y) / len2

    val chosenTangent: Vec3i
    val swipeSign: Int
    if (abs(score1) >= abs(score2)) {
        chosenTangent = t1
        swipeSign = if (score1 >= 0f) 1 else -1
    } else {
        chosenTangent = t2
        swipeSign = if (score2 >= 0f) 1 else -1
    }

    val signedTangent = Vec3i(
        chosenTangent.x * swipeSign,
        chosenTangent.y * swipeSign,
        chosenTangent.z * swipeSign
    )

    // The axis of rotation is perpendicular to both faceNormal and signedTangent
    val crossAxis = Vec3i(
        faceNormal.y * signedTangent.z - faceNormal.z * signedTangent.y,
        faceNormal.z * signedTangent.x - faceNormal.x * signedTangent.z,
        faceNormal.x * signedTangent.y - faceNormal.y * signedTangent.x
    )

    val axis = when {
        crossAxis.x != 0 -> Axis.X
        crossAxis.y != 0 -> Axis.Y
        crossAxis.z != 0 -> Axis.Z
        else -> return null
    }

    // Verify which direction (+1 or -1) moves faceNormal toward signedTangent
    val rotatedPlus = rotateVec3i(faceNormal, axis, 1)
    val dir = if (rotatedPlus == signedTangent) 1 else -1
    val layer = cubiePos.get(axis)

    return CubeMove(axis = axis, layer = layer, dir = dir)
}

private fun isPointInPolygon(pt: Offset, poly: List<Offset>): Boolean {
    if (poly.size < 4) return false
    var inside = false
    var j = poly.lastIndex
    for (i in poly.indices) {
        val xi = poly[i].x
        val yi = poly[i].y
        val xj = poly[j].x
        val yj = poly[j].y
        val intersect = ((yi > pt.y) != (yj > pt.y)) &&
            (pt.x < (xj - xi) * (pt.y - yi) / ((yj - yi).let { if (abs(it) < 0.0001f) 0.0001f else it }) + xi)
        if (intersect) inside = !inside
        j = i
    }
    return inside
}

private fun shadeColor(color: Color, factor: Float): Color {
    val f = factor.coerceIn(0f, 1.15f)
    return Color(
        red = (color.red * f).coerceIn(0f, 1f),
        green = (color.green * f).coerceIn(0f, 1f),
        blue = (color.blue * f).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
