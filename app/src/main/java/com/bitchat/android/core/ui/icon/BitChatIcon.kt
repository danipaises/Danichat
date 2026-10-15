package com.bitchat.android.core.ui.icon

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BitChatIcon: ImageVector
    get() {
        _BitChatIcon?.let { return it }

        return ImageVector.Builder(
            name = "BitChatIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 72f,
            viewportHeight = 72f,
        ).apply {
            // Original DaniChat mark. Retain the Kotlin symbol to avoid churn in hosts.
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(16f, 8f)
                lineTo(39f, 8f)
                curveTo(55f, 8f, 65f, 18f, 65f, 34f)
                curveTo(65f, 50f, 55f, 60f, 39f, 60f)
                lineTo(27f, 60f)
                lineTo(13f, 69f)
                lineTo(16f, 55f)
                curveTo(10f, 50f, 7f, 43f, 7f, 34f)
                curveTo(7f, 22f, 10f, 13f, 16f, 8f)
                close()
                moveTo(25f, 22f)
                lineTo(37f, 22f)
                curveTo(46f, 22f, 51f, 27f, 51f, 35f)
                curveTo(51f, 43f, 46f, 48f, 37f, 48f)
                lineTo(25f, 48f)
                close()
                moveTo(32f, 29f)
                lineTo(32f, 41f)
                lineTo(37f, 41f)
                curveTo(42f, 41f, 44f, 39f, 44f, 35f)
                curveTo(44f, 31f, 42f, 29f, 37f, 29f)
                close()
            }
        }.build().also { _BitChatIcon = it }
    }

private var _BitChatIcon: ImageVector? = null
