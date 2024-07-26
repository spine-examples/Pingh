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
import androidx.compose.foundation.layout.ColumnScope
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
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.minutes

/**
 * Displays a application settings.
 *
 * All changes are saved automatically and applied immediately.
 *
 * @param client enables interaction with the Pingh server.
 * @param state the state of the application settings.
 * @param toMentionsPage the navigation to the 'Mentions' page.
 * @param toLoginPage the navigation to the 'Login' page.
 */
@Composable
internal fun SettingsPage(
    client: DesktopClient,
    state: SettingsState,
    toMentionsPage: () -> Unit,
    toLoginPage: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsHeader(toMentionsPage)
        SettingsBox {
            Profile(client, toLoginPage)
            SnoozeTimeOption(state)
            DndOption(state)
        }
    }
}

/**
 * Displays a header of the `Settings` page.
 *
 * Includes a button to return to the `Mentions` page.
 *
 * @param toMentionsPage the navigation to the 'Mentions' page.
 */
@Composable
private fun SettingsHeader(
    toMentionsPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.back,
            onClick = toMentionsPage,
            modifier = Modifier.size(30.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Settings",
            modifier = Modifier.width(140.dp),
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.displayLarge
        )
    }
}

/**
 * Displays a container for settings.
 *
 * @param content the composable function that displays settings options.
 */
@Composable
private fun SettingsBox(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(5.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                content = content
            )
        }
    }
}

/**
 * Displays user information and provides an option to log out of the account.
 *
 * @param client enables interaction with the Pingh server.
 * @param toLoginPage the navigation to the 'Login' page.
 */
@Composable
private fun Profile(
    client: DesktopClient,
    toLoginPage: () -> Unit
) {
    val username = client.session?.username
    checkNotNull(username) { "User is not logged in." }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = username,
            size = 52.dp
        )
        Spacer(Modifier.width(8.dp))
        ProfileControl(client, toLoginPage, username)
    }
}

/**
 * Displays a username and a button to log out of the account.
 *
 * @param client enables interaction with the Pingh server.
 * @param toLoginPage the navigation to the 'Login' page.
 * @param username the name of the user to whom the current session belongs.
 */
@Composable
private fun ProfileControl(
    client: DesktopClient,
    toLoginPage: () -> Unit,
    username: Username
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = username.value,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(5.dp))
        LogOutButton {
            client.logOut {
                toLoginPage()
            }
        }
    }
}

/**
 * Displays a button to log out of the account.
 *
 * @param onClick called when this button is clicked.
 */
@Composable
private fun LogOutButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = "Log out",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a setting option for specifying the snooze time.
 *
 * @param state the state of the application settings.
 */
@Composable
private fun SnoozeTimeOption(state: SettingsState) {
    Option(
        title = "Snooze time",
        description = "Time after which the notification is repeated.",
        titleWight = 68.dp
    ) {
        SnoozeTimeSegmentedButtonRow(state)
    }
}

/**
 * Displays a setting option for toggling 'Do not disturb' mode.
 *
 * @param state the state of the application settings.
 * @param switchScale the multiplier to scale switch along the horizontal and vertical axis.
 */
@Composable
private fun DndOption(
    state: SettingsState,
    switchScale: Float = 0.6f
) {
    Option(
        title = "Do not disturb",
        description = "Turn off notifications for new mentions or snooze expirations.",
        titleWight = 174.dp
    ) {
        Switch(
            checked = state.enabledDndMode.value,
            onCheckedChange = {
                state.enabledDndMode.value = it
            },
            modifier = Modifier
                .scale(switchScale)
                .width(36.dp)
                .height(20.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedBorderColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}

/**
 * Displays a setting option that contains a title, description, and a control element.
 *
 * @param title the string with the name of the option.
 * @param description the string with the description of the option.
 * @param titleWight the width that the title occupies.
 * @param control the composable function that displays control element of this option.
 */
@Composable
private fun Option(
    title: String,
    description: String,
    titleWight: Dp,
    control: @Composable () -> Unit
) {
    Column(
        Modifier
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
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a row of segmented buttons that allow specifying the snooze time value.
 *
 * @param state the state of the application settings.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class) // Required for `SegmentedButtonDefaults.itemShape()`.
private fun SnoozeTimeSegmentedButtonRow(state: SettingsState) {
    val snoozeTimeOptions = SnoozeTime.entries
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        snoozeTimeOptions.forEachIndexed { index, snoozeTime ->
            SegmentedButton(
                selected = snoozeTime == state.snoozeTime.value,
                onClick = {
                    state.snoozeTime.value = snoozeTime
                },
                shape = SegmentedButtonDefaults.itemShape(index, snoozeTimeOptions.size)
            ) {
                Text(
                    text = snoozeTime.label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Displays a button from the [SnoozeTimeSegmentedButtonRow].
 *
 * @param selected whether this button is selected.
 * @param onClick called when this button is clicked.
 * @param shape the shape of this button.
 * @param label the composable function that displays the label of this button.
 */
@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    label: @Composable () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    val border = BorderStroke(
        1.dp,
        if (selected) containerColor else MaterialTheme.colorScheme.onBackground
    )
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .width(48.dp)
            .height(20.dp)
            .semantics { role = Role.RadioButton },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            label()
        }
    }
}

/**
 * State of application settings.
 */
internal class SettingsState {

    /**
     * If `true`, the user is not notified about new mentions and snooze expirations.
     * If `false`, the user receives notifications.
     */
    internal var enabledDndMode: MutableState<Boolean> = mutableStateOf(false)

    /**
     * The interval after which the new mention notification is repeated.
     */
    internal var snoozeTime: MutableState<SnoozeTime> = mutableStateOf(SnoozeTime.TWO_HOURS)
}

/**
 * Time after which the notification about the new mention is repeated.
 *
 * @param label the text corresponding to this interval.
 * @param value the duration corresponding to this interval.
 */
@Suppress("MagicNumber") // The durations are specified using numbers.
internal enum class SnoozeTime(
    internal val label: String,
    internal val value: Duration
) {
    /**
     * The interval is 30 minutes in duration.
     */
    THIRTY_MINUTES("30 mins", minutes(30)),

    /**
     * The interval is 2 hours in duration.
     */
    TWO_HOURS("2 hours", hours(2)),

    /**
     * The interval is one day in duration.
     */
    ONE_DAY("1 day", hours(24))
}
