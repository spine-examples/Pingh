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

@file:Suppress("TooManyFunctions") // Using Compose requires many functions to render the UI.

package io.spine.examples.pingh.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.protobuf.Duration
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.minutes

@Composable
internal fun SettingsPage(
    client: DesktopClient,
    state: SettingsState,
    toMentionsPage: () -> Unit,
    toLoginPage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Header(toMentionsPage)
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(5.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Spacer(Modifier.height(5.dp))
                Settings(client, state, toLoginPage)
            }
        }
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
            .padding(vertical = 4.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.back,
            onClick = toMentionsPage,
            modifier = Modifier.size(30.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Settings",
            modifier = Modifier.width(140.dp),
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.displayLarge
        )
    }
}

@Composable
private fun Settings(
    client: DesktopClient,
    state: SettingsState,
    toLoginPage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Profile(client, toLoginPage)
        Spacer(Modifier.height(15.dp))
        SnoozeTimeOption(state)
        Spacer(Modifier.height(15.dp))
        DndOption(state)
    }
}

@Composable
private fun Profile(
    client: DesktopClient,
    toLoginPage: () -> Unit
) {
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
            LogOutButton(client, toLoginPage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomButtons(state: SettingsState) {
    val snoozeTimes = SnoozeTime.entries
    Row(
        modifier = Modifier.selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-1).dp)
    ) {
        snoozeTimes.forEachIndexed { index, snoozeTime ->
            ButC(
                selected = snoozeTime == state.snoozeTimeOption.value,
                onClick = { state.snoozeTimeOption.value = snoozeTime },
                shape = SegmentedButtonDefaults.itemShape(index, snoozeTimes.size)
            ) {
                Text(
                    text = snoozeTime.label,
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
    label: @Composable () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary
    val contentColor = if (selected)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val border =
        BorderStroke(1.dp, if (selected) containerColor else MaterialTheme.colorScheme.onBackground)

    Surface(
        modifier = Modifier
            .width(48.dp)
            .height(20.dp)
            .semantics { role = Role.RadioButton },
        selected = selected,
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border
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
    Option(
        title = "Snooze time",
        description = "Time after which the notification is repeated.",
        titleWight = 68.dp
    ) {
        CustomButtons(state)
    }
}

@Composable
private fun DndOption(state: SettingsState) {
    Option(
        title = "Do not disturb",
        description = "Turn off notifications for new mentions or snooze expirations.",
        titleWight = 174.dp
    ) {
        Switch(
            checked = state.enableDndMode.value,
            onCheckedChange = {
                state.enableDndMode.value = it
            },
            modifier = Modifier
                .scale(0.6f)
                .width(36.dp)
                .height(20.dp),
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.6f
                ),
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.6f
                ),
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}

@Composable
private fun Option(
    title: String,
    description: String,
    titleWight: Dp,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.width(titleWight),
                style = MaterialTheme.typography.bodyMedium
            )
            control()
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = description,
            modifier = Modifier.width(170.dp),
            color = Color(150, 150, 150),
            style = MaterialTheme.typography.bodySmall
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
            contentColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Log out",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 *
 */
internal class SettingsState {

    /**
     *
     */
    internal var enableDndMode: MutableState<Boolean> = mutableStateOf(false)

    /**
     *
     */
    internal var snoozeTimeOption: MutableState<SnoozeTime> = mutableStateOf(SnoozeTime.TWO_HOURS)
}

/**
 * Possible value of snooze time.
 */
internal enum class SnoozeTime(
    val label: String,
    val value: Duration
) {

    /**
     *
     */
    THIRTY_MINUTES("30 mins", minutes(30)),

    /**
     *
     */
    TWO_HOURS("2 hours", hours(2)),

    /**
     *
     */
    ONE_DAY("1 day", hours(24))
}
