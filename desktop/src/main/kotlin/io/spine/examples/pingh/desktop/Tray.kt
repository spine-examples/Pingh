/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.examples.pingh.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Notification
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.tray
import io.spine.examples.pingh.client.PinghApplication
import java.awt.Color
import java.awt.Font
import java.awt.Frame
import java.awt.Graphics
import java.awt.Image
import java.awt.MenuItem
import java.awt.Point
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.TrayIcon.MessageType
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.round
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.painterResource

/**
 * Adds the application icon to the platform taskbar.
 *
 * Left-clicking the tray icon toggles the window’s visibility,
 * hiding it if open and displaying it if hidden.
 *
 * Right-clicking the tray icon opens the application's control menu.
 *
 * Because Java AWT lacks an API to obtain exact tray icon coordinates,
 * the menu location is dynamically calculated based on the click position.
 * As a result, the menu may appear in slightly different locations depending on where
 * the tray icon is clicked.
 *
 * @param state The top-level application state.
 * @throws IllegalStateException if the system tray is not supported on the current platform.
 */
@Composable
internal fun Tray(state: AppState) {
    check(SystemTray.isSupported()) { "The platform does not support tray applications." }

    val icon = rememberIcon(state.app)
    val menu = remember { Menu(state::close) }
    val onClick by rememberUpdatedState(mouseEventHandler(state, menu))

    val tray = remember {
        TrayIcon(icon, state.window.title).apply {
            isImageAutoSize = true
            addMouseListener(onClick)
        }
    }

    SideEffect {
        if (tray.image != icon) tray.image = icon
    }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        SystemTray.getSystemTray().add(tray)

        state.tray
            .notificationFlow
            .onEach { tray.displayMessage(it) }
            .launchIn(coroutineScope)

        state.addClosureAction {
            SystemTray.getSystemTray().remove(tray)
        }

        onDispose {
            SystemTray.getSystemTray().remove(tray)
        }
    }
}

/**
 * Returns the system tray icon of the application.
 *
 * If the user is logged in and there are unread mentions,
 * a badge showing the number of unread mentions appears on the icon.
 */
@Composable
private fun rememberIcon(state: PinghApplication): Image {
    val unread by state.unreadMentionCount.collectAsState()

    // Using `LocalDensity` here is not appropriate because the tray's density does not match it.
    // The tray's density corresponds to the density of the screen where it is displayed.
    val density = GlobalDensity
    val layoutDirection = GlobalLayoutDirection

    val icon = painterResource(Res.drawable.tray)
    val style = remember { TrayStyle(density) }

    return remember(unread) {
        val awtIcon = icon.toAwtImage(density, layoutDirection, style.iconSize.toCompose())
        val buffer = BufferedImage(
            style.boxSize.width,
            style.boxSize.height,
            BufferedImage.TYPE_INT_ARGB
        )
        buffer.createGraphics().apply {
            drawImage(
                awtIcon,
                style.iconPosition.x,
                style.iconPosition.y,
                null
            )
            if (unread != null && unread!! > 0) {
                drawBadge(style)
                drawBadgeContent(unread.toString(), style)
            }
            dispose()
        }
        return@remember buffer
    }
}

/**
 * Draws red badge for number of unread mentions.
 */
private fun Graphics.drawBadge(style: TrayStyle) {
    color = style.badgeColor
    val arc = min(style.badgeSize.width, style.badgeSize.height)
    fillRoundRect(
        style.badgePosition.x,
        style.badgePosition.y,
        style.badgeSize.width,
        style.badgeSize.height,
        arc, arc
    )
}

/**
 * Draws the number of unread mentions.
 *
 * If the number exceeds 99, only the last two digits are shown,
 * with an ellipsis preceding them.
 */
private fun Graphics.drawBadgeContent(text: String, style: TrayStyle) {
    val content = if (text.length <= 2) text else ".." + text.takeLast(2)
    font = Font(style.fontName, Font.PLAIN, style.fontSize)
    color = style.fontColor
    val metrics = getFontMetrics(font)
    val textWidth = metrics.stringWidth(content)
    val textHeight = metrics.height
    val x = (style.badgeSize.width - textWidth) / 2
    val y = (style.badgeSize.height - textHeight) / 2 + metrics.ascent
    drawString(
        content,
        x + style.badgePosition.x,
        y + style.badgePosition.y
    )
}

/**
 * The tray application menu provides controls such as an exit option for the application.
 *
 * When the menu is open, clicking anywhere outside of it will close the menu.
 *
 * @param onExit Called when the “Quit” button is pressed.
 */
private class Menu(onExit: () -> Unit) {
    /**
     * Utility window on which the application menu will be displayed.
     */
    private val frame = Frame()

    /**
     * The application menu includes a "Quit" button to close the application.
     */
    private val popup = PopupMenu()

    init {
        val exitItem = MenuItem("Quit Pingh app")
        exitItem.addActionListener {
            onExit()
        }
        popup.add(exitItem)
        frame.apply {
            isUndecorated = true
            type = Window.Type.UTILITY
            add(popup)
            isVisible = true
        }
    }

    /**
     * Displays the popup menu at the specified (`x`, `y`) position relative to the full screen.
     */
    fun show(x: Int, y: Int) {
        popup.show(frame, x, y)
    }
}

/**
 * Handles click events on the system tray icon:
 * a left-click toggles the window’s visibility,
 * and a right-click opens the menu.
 */
private fun mouseEventHandler(state: AppState, menu: Menu) =
    object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.button == 1) {
                state.toggleWindowVisibility()
            }
            if (e.button == 3) {
                menu.show(e.xOnScreen, e.yOnScreen)
            }
        }
    }

/**
 * Default data for styling the tray icon.
 *
 * Dimensions and coordinates are specified in pixels and adjusted for screen [density].
 */
@Suppress("MagicNumber" /* Colors are defined using RGB components. */)
private class TrayStyle(private val density: Density) {
    val boxSize = AwtSize(22.adjusted, 22.adjusted)
    val iconSize = AwtSize(16.adjusted, 16.adjusted)
    val iconPosition = Point(3.adjusted, 3.adjusted)
    val badgeSize = AwtSize(17.adjusted, 12.adjusted)
    val badgePosition = Point(3.adjusted, 10.adjusted)
    val badgeColor = Color(240, 77, 63)
    val fontSize = 8.adjusted
    val fontName = "San Francisco"
    val fontColor = Color.WHITE!!

    /**
     * Adapts standard pixel value to fit the screen density.
     */
    private val Int.adjusted: Int
        get() = round(this * density.density).toInt()
}

/**
 * Holds a 2D integer size.
 *
 * Java AWT relies exclusively on integer values for specifying dimensions,
 * unlike other libraries that support floating-point values.
 *
 * Use to store integer dimensions for creating components with Java AWT.
 */
private data class AwtSize(val width: Int, val height: Int) {
    /**
     * Converts the size to floating-point size.
     */
    fun toCompose(): Size = Size(width.toFloat(), height.toFloat())
}

/**
 * Displays a popup message near the tray icon.
 */
private fun TrayIcon.displayMessage(notification: Notification) {
    val messageType = when (notification.type) {
        Notification.Type.None -> MessageType.NONE
        Notification.Type.Info -> MessageType.INFO
        Notification.Type.Warning -> MessageType.WARNING
        Notification.Type.Error -> MessageType.ERROR
    }
    displayMessage(notification.title, notification.message, messageType)
}
