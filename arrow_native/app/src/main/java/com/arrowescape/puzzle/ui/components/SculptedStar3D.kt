package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity 3D sculpted beveled star matching the Unity One victory aesthetic.
 * Renders geometric facet shading with specular highlights and depth bevels.
 */
@Composable
fun SculptedStar3D(
    isEarned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    scale: Float = 1.0f,
    rotation: Float = 0f,
    haloAura: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val center = Offset(width / 2f, height / 2f)
            val outerRadius = (minOf(width, height) / 2f) * 0.94f
            val innerRadius = outerRadius * 0.40f

            // Optional ambient back-glow for active hero stars
            if (isEarned && haloAura) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x60F59E0B),
                            Color(0x25FBBF24),
                            Color.Transparent
                        ),
                        center = center,
                        radius = outerRadius * 1.35f
                    ),
                    radius = outerRadius * 1.35f,
                    center = center
                )
            }

            // Calculate 10 star boundary vertices
            val vertices = mutableListOf<Offset>()
            for (i in 0 until 10) {
                val angleDeg = -90f + i * 36f
                val angleRad = angleDeg * (PI.toFloat() / 180f)
                val r = if (i % 2 == 0) outerRadius else innerRadius
                vertices.add(
                    Offset(
                        center.x + r * cos(angleRad),
                        center.y + r * sin(angleRad)
                    )
                )
            }

            // 1. Draw Drop Shadow / Bevel Base
            val shadowOffset = Offset(0f, outerRadius * 0.08f)
            val shadowPath = Path().apply {
                moveTo(vertices[0].x + shadowOffset.x, vertices[0].y + shadowOffset.y)
                for (i in 1 until 10) {
                    lineTo(vertices[i].x + shadowOffset.x, vertices[i].y + shadowOffset.y)
                }
                close()
            }
            drawPath(
                path = shadowPath,
                color = if (isEarned) Color(0x3892400E) else Color(0x22000000)
            )

            // 2. Draw Base Star Silhouette
            val basePath = Path().apply {
                moveTo(vertices[0].x, vertices[0].y)
                for (i in 1 until 10) {
                    lineTo(vertices[i].x, vertices[i].y)
                }
                close()
            }

            val baseBrush = if (isEarned) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFEA79), // Light gold top
                        Color(0xFFF59E0B), // Vibrant amber mid
                        Color(0xFFD97706)  // Rich bronze bottom
                    ),
                    startY = center.y - outerRadius,
                    endY = center.y + outerRadius
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0), // Platinum silver top
                        Color(0xFFCBD5E1), // Cool grey mid
                        Color(0xFF94A3B8)  // Slate shadow bottom
                    ),
                    startY = center.y - outerRadius,
                    endY = center.y + outerRadius
                )
            }
            drawPath(path = basePath, brush = baseBrush)

            // 3. Draw 10 Facets emanating from star center to give authentic 3D bevel depth
            for (i in 0 until 10) {
                val p1 = vertices[i]
                val p2 = vertices[(i + 1) % 10]
                val facetPath = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }

                // Alternate between Lit side and Shaded side
                val isLitFacet = (i % 2 == 1)
                val facetColor = if (isEarned) {
                    if (isLitFacet) {
                        Color.White.copy(alpha = 0.32f) // Sun-facing facet gleam
                    } else {
                        Color(0xFF78350F).copy(alpha = 0.22f) // Ambient shadow facet
                    }
                } else {
                    if (isLitFacet) {
                        Color.White.copy(alpha = 0.38f)
                    } else {
                        Color(0xFF334155).copy(alpha = 0.20f)
                    }
                }
                drawPath(path = facetPath, color = facetColor)
            }

            // 4. Subtle Outer Bevel Edge Highlight
            val rimColor = if (isEarned) Color(0x60FFFBEB) else Color(0x55FFFFFF)
            drawPath(
                path = basePath,
                color = rimColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )


        }
    }
}
