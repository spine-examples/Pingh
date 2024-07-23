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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy

@Composable
internal fun SettingsPage(
    client: DesktopClient,
    state: SettingsState,
    toMentionsPage: () -> Unit,
    toLoginPage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Header(toMentionsPage)
        Settings(client, state)
    }
}

@Composable
private fun Header(
    toMentionsPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 4.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.back,
            onClick = toMentionsPage,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Settings",
            modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.displayLarge
        )
    }
}

@Composable
private fun Settings(client: DesktopClient, state: SettingsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Profile(client)
        Delimiter()
        SnoozeTimeOption(state)
        Spacer(Modifier.height(2.dp))
        DndOption(state)
    }
}

@Composable
private fun Profile(client: DesktopClient) {
    val username = Username::class.buildBy("MykytaPimonovTD")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 4.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = username,
            size = 52.dp
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = username.value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(5.dp))
            LogOutButton(client, {})
        }
    }
}

@Composable
private fun Delimiter() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = 10.dp,
            vertical = 10.dp
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomButtons() {
    var selectedIndex by remember { mutableStateOf(1) }
    val options = listOf("half hour", "2 hours", "a day")

    Row(
        modifier = Modifier.selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-1).dp)
    ) {
        options.forEachIndexed { index, label ->
            ButC(
                selected = index == selectedIndex,
                onClick = { selectedIndex = index },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.width(52.dp).height(20.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ButC(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    label: @Composable () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary
    val contentColor = if (selected)
        MaterialTheme.colorScheme.primary
    else Color.Black
    val border =
        BorderStroke(1.dp, if (selected) containerColor else MaterialTheme.colorScheme.onBackground)

    Surface(
        modifier = modifier.semantics { role = Role.RadioButton },
        selected = selected,
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(0.dp)
        ) {
            label()
        }
    }
}

@Composable
private fun SnoozeTimeOption(state: SettingsState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Snooze time:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(5.dp))
        CustomButtons()
    }
}

@Composable
private fun DndOption(state: SettingsState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = state.isEnableDndMode(),
            onCheckedChange = state::setDndMode,
            modifier = Modifier
                .scale(0.5f)
                .width(36.dp),
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.onBackground,
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = MaterialTheme.colorScheme.onBackground,
                checkedBorderColor = Color.Unspecified,
                uncheckedTrackColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
        Text(
            text = "Do not disturb",
            modifier = Modifier.width(160.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CustomSwitch(
    state: MutableState<Boolean>
) {
    val height = 16.dp
    val width = 26.dp
    val gapBetweenThumbAndTrackEdge = 4.dp
    val thumbRadius = (height / 2) - gapBetweenThumbAndTrackEdge
    val animationPosition = animateFloatAsState(
        targetValue = if (state.value)
            with(LocalDensity.current) { (width - thumbRadius - gapBetweenThumbAndTrackEdge).toPx() }
        else
            with(LocalDensity.current) {
                (thumbRadius + gapBetweenThumbAndTrackEdge).toPx()
            }
    )
    val color1 = MaterialTheme.colorScheme.secondary
    val color2 = MaterialTheme.colorScheme.primary
    val color3 = MaterialTheme.colorScheme.onBackground
    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(2f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        state.value = !state.value
                    }
                )
            }
    ) {
        drawRoundRect(
            color = if (state.value) color1 else color2,
            cornerRadius = CornerRadius(
                x = 10.dp.toPx(),
                y = 10.dp.toPx()
            ),
            style = Stroke(width = 2.dp.toPx())
        )

        drawCircle(
            color = if (state.value) color2 else color3,
            radius = thumbRadius.toPx(),
            center = Offset(
                x = animationPosition.value,
                y = size.height / 2
            )
        )
    }
}

@Composable
private fun LogOutButton(
    client: DesktopClient,
    toLoginPage: () -> Unit
) {
    OutlinedButton(
        onClick = {
            client.logOut {
                toLoginPage()
            }
        },
        modifier = Modifier.height(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.Black,
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Log out",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

internal class SettingsState {

    private var enableDndMode: MutableState<Boolean> = mutableStateOf(false)

    internal fun isEnableDndMode(): Boolean = enableDndMode.value

    internal fun setDndMode(value: Boolean) {
        enableDndMode.value = value
    }
}
