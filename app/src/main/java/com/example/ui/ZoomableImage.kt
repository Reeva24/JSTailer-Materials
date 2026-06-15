package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun HighPerformanceZoomCanvas(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Overlay Toggles
    var showStitchGrid by remember { mutableStateOf(false) }
    var showTextureMicrometer by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clipToBounds()
    ) {
        // Multi-touch Gesture Listener
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1.0f, 6.0f)
                        
                        // Limit dragging offsets based on active scale
                        if (scale > 1f) {
                            val maxOffsetX = (scale - 1f) * size.width / 2f
                            val maxOffsetY = (scale - 1f) * size.height / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            // Main Loader Image Async load
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Zoomable product material view",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // 1. STITCH GRID OVERLAY
            if (showStitchGrid) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val gridSpacing = 24.dp.toPx()
                    val strokeColor = Color(0x992196F3) // transparent blue
                    
                    // Draw horizontal matrix lines
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += gridSpacing
                    }

                    // Draw vertical matrix lines
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.dp.toPx()
                        )
                        x += gridSpacing
                    }
                }
            }

            // 2. TEXTURE MICROMETER OVERLAY (mm gauge ruler)
            if (showTextureMicrometer) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw a micro circular overlay scope in center of view
                    val circleCenter = Offset(width / 2f, height / 2f)
                    val circleRadius = 70.dp.toPx()
                    
                    drawCircle(
                        color = Color(0xE6E91E63), // transparent pinkish-red
                        radius = circleRadius,
                        center = circleCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw ruler ticks inside the micrometer scope
                    val startX = circleCenter.x - circleRadius
                    val endX = circleCenter.x + circleRadius
                    val step = (endX - startX) / 10f

                    for (i in 0..10) {
                        val tickX = startX + (i * step)
                        // Make center ticks taller
                        val tickHeight = if (i == 5) 20.dp.toPx() else 10.dp.toPx()
                        
                        drawLine(
                            color = Color(0xE6E91E63),
                            start = Offset(tickX, circleCenter.y - tickHeight / 2f),
                            end = Offset(tickX, circleCenter.y + tickHeight / 2f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }
        }

        // Action Overlay Buttons for Tooling (Quick zoom, micro measurements)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color(0xB3000000), RoundedCornerShape(8.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    showStitchGrid = !showStitchGrid
                    showTextureMicrometer = false
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (showStitchGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                    contentDescription = "Toggle Stitch Grid",
                    tint = if (showStitchGrid) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            IconButton(
                onClick = {
                    showTextureMicrometer = !showTextureMicrometer
                    showStitchGrid = false
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = "Toggle Texture Micrometer Ruler",
                    tint = if (showTextureMicrometer) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            IconButton(
                onClick = {
                    scale = 1.0f
                    offset = Offset.Zero
                    showStitchGrid = false
                    showTextureMicrometer = false
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Zoom Lens",
                    tint = Color.White
                )
            }
        }

        // Active magnification indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC000000)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = String.format("Lens Zoom: %.1fx", scale),
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
