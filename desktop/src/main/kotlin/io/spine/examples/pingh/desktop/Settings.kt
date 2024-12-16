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

@file:Suppress("TooManyFunctions" /* Using Compose requires many functions to render the UI. */)

package io.spine.examples.pingh.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.material3.IconButtonDefaults.iconButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role.Companion.RadioButton
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.add
import io.spine.example.pingh.desktop.generated.resources.back
import io.spine.example.pingh.desktop.generated.resources.remove
import io.spine.examples.pingh.client.SettingsFlow
import io.spine.examples.pingh.client.SettingsState
import io.spine.examples.pingh.client.settings.IgnoredSource
import io.spine.examples.pingh.client.settings.SnoozeTime
import io.spine.examples.pingh.client.settings.label
import io.spine.examples.pingh.client.settings.supported
import io.spine.examples.pingh.github.OrganizationLogin
import io.spine.examples.pingh.github.Repo
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.isValidOrganization
import io.spine.examples.pingh.github.isValidRepoName
import io.spine.examples.pingh.github.of
import org.jetbrains.compose.resources.painterResource

/**
 * Displays an application settings.
 *
 * All changes are saved automatically and applied immediately.
 *
 * @param flow The application settings control flow.
 * @param toMentionsPage The navigation to the 'Mentions' page.
 * @param toLoginPage The navigation to the 'Login' page.
 */
@Composable
internal fun SettingsPage(
    flow: SettingsFlow,
    toMentionsPage: () -> Unit,
    toLoginPage: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsHeader {
            flow.saveSettings()
            toMentionsPage()
        }
        SettingsBox {
            Profile(flow, toLoginPage)
            SnoozeTimeOption(flow.settings)
            DndOption(flow.settings)
            IgnoredSourcesOption(flow.settings)
        }
    }
}

/**
 * Displays a header of the `Settings` page.
 *
 * Includes a button to return to the `Mentions` page.
 *
 * @param onExit Called when exiting the page.
 */
@Composable
private fun SettingsHeader(
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = CenterVertically
    ) {
        IconButton(
            icon = painterResource(Res.drawable.back),
            onClick = onExit,
            modifier = Modifier.size(35.dp),
            colors = iconButtonColors(
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
 * @param content The composable function that displays settings options.
 */
@Composable
private fun SettingsBox(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = cardColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(25.dp),
                content = content
            )
        }
    }
}

/**
 * Displays user information and provides an option to log out of the account.
 *
 * @param flow The application settings control flow.
 * @param toLoginPage The navigation to the 'Login' page.
 */
@Composable
private fun Profile(
    flow: SettingsFlow,
    toLoginPage: () -> Unit
) {
    val username = flow.username
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = CenterVertically
    ) {
        Avatar(
            name = username,
            size = 52.dp
        )
        Spacer(Modifier.width(8.dp))
        ProfileControl(flow, toLoginPage, username)
    }
}

/**
 * Displays a username and a button to log out of the account.
 *
 * @param flow The application settings control flow.
 * @param toLoginPage The navigation to the 'Login' page.
 * @param username The name of the user to whom the current session belongs.
 */
@Composable
private fun ProfileControl(
    flow: SettingsFlow,
    toLoginPage: () -> Unit,
    username: Username
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Center
    ) {
        Text(
            text = username.value,
            overflow = Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(5.dp))
        LogOutButton {
            flow.saveSettings()
            flow.logOut {
                toLoginPage()
            }
        }
    }
}

/**
 * Displays a button to log out of the account.
 *
 * @param onClick Called when this button is clicked.
 */
@Composable
private fun LogOutButton(
    onClick: () -> Unit
) {
    SettingsButton(
        onClick = onClick,
        text = "Log out",
        modifier = Modifier.height(22.dp).testTag("logout-button")
    )
}

/**
 * Displays the button in the style of the settings page.
 *
 * @param onClick Called when this button is clicked.
 * @param text The text displayed on this button.
 * @param modifier The modifier to be applied to this button.
 * @param enabled Controls the enabled state of this button.
 *   If `false`, the button cannot be pressed.
 * @param contentPadding The spacing values to apply internally between
 *   the container and the content.
 * @param usePrimaryColors If `true`, the button uses primary colors for its various states.
 *   If `false`, it uses secondary colors instead.
 */
@Composable
@Suppress("LongParameterList" /* For detailed customization. */)
private fun SettingsButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    usePrimaryColors: Boolean = false
) {
    val colors = if (usePrimaryColors) {
        outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }
    val border = if (enabled && usePrimaryColors) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        colors = colors,
        border = BorderStroke(1.dp, border),
        contentPadding = contentPadding
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a setting option for specifying the snooze time.
 *
 * @param state The state of the application settings.
 */
@Composable
private fun SnoozeTimeOption(state: SettingsState) {
    Option(
        title = "Snooze time",
        description = "Time after which the notification is repeated.",
        titleWight = 150.dp
    ) {
        SnoozeTimeSegmentedButtonRow(state)
    }
}

/**
 * Displays a setting option for toggling 'Do not disturb' mode.
 *
 * @param state The state of the application settings.
 * @param switchScale The multiplier to scale switch along the horizontal and vertical axis.
 */
@Composable
private fun DndOption(
    state: SettingsState,
    switchScale: Float = 0.6f
) {
    val dndEnabled by state.dndEnabled.collectAsState()
    Option(
        title = "Do not disturb",
        description = "Turn off notifications for new mentions or snooze expirations.",
        titleWight = 324.dp
    ) {
        Switch(
            checked = dndEnabled,
            onCheckedChange = {
                state.setDndMode(it)
            },
            modifier = Modifier
                .scale(switchScale)
                .width(36.dp)
                .height(20.dp)
                .testTag("dnd-option"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondaryContainer,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedBorderColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

/**
 * Displays a setting option that contains a title, description, and a control element.
 *
 * @param title The string with the name of the option.
 * @param description The string with the description of the option.
 * @param titleWight The width that the title occupies.
 * @param control The composable function that displays control element of this option.
 */
@Composable
private fun Option(
    title: String,
    description: String,
    titleWight: Dp,
    control: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.width(titleWight),
                style = MaterialTheme.typography.bodyLarge
            )
            control()
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = description,
            modifier = Modifier.width(360.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a row of segmented buttons that allow specifying the snooze time value.
 *
 * @param state The state of the application settings.
 */
@Composable
private fun SnoozeTimeSegmentedButtonRow(state: SettingsState) {
    val snoozeTimeOptions = SnoozeTime::class.supported
    val currentSnoozeTime by state.snoozeTime.collectAsState()
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = CenterVertically
    ) {
        snoozeTimeOptions.forEachIndexed { index, snoozeTime ->
            SegmentedButton(
                selected = currentSnoozeTime == snoozeTime,
                onClick = {
                    state.setSnoozeTime(snoozeTime)
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
 * @param selected Whether this button is selected.
 * @param onClick Called when this button is clicked.
 * @param shape The shape of this button.
 * @param label The composable function that displays the label of this button.
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
    Box(
        modifier = Modifier
            .width(70.dp)
            .height(20.dp)
            .semantics { role = RadioButton }
            .background(containerColor, shape)
            .border(border, shape)
            .clip(shape)
            .clickable { onClick() }
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
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
}

/**
 * Displays a setting option for adding and removing ignored sources.
 *
 * @param state The state of the application settings.
 */
@Composable
private fun IgnoredSourcesOption(state: SettingsState) {
    var addDialogOpened by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Option(
            title = "Ignored repositories",
            description = "Mentions from the following sources will be ignored.",
            titleWight = 360.dp
        ) {}
        IgnoredSourceList(
            state = state,
            openDialog = { addDialogOpened = true }
        )
    }
    if (addDialogOpened) {
        AddIgnoredSourceDialog(
            state = state,
            onExit = { addDialogOpened = false }
        )
    }
}

/**
 * Displays a list of ignored sources along with list controls.
 *
 * @param state The state of the application settings.
 * @param openDialog Opens a dialog to add a new source.
 */
@Composable
private fun IgnoredSourceList(
    state: SettingsState,
    openDialog: () -> Unit
) {
    val ignored by state.ignored.collectAsState()
    val selected = remember { mutableStateOf<IgnoredSource?>(null) }
    Column(
        Modifier.fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = MaterialTheme.shapes.extraSmall
            )
    ) {
        IgnoredSourcesControl(
            onAdd = openDialog,
            onRemove = {
                selected.value?.let { source ->
                    state.removeFromIgnored(source)
                }
            },
            sourceSelected = selected
        )
        Divider(Modifier.fillMaxWidth())
        IgnoredSourceItemBox {
            ignored.forEach { source ->
                IgnoredSourceItem(source, selected)
            }
        }
    }
}

/**
 * Displays a container for items of the list of ignored sources.
 *
 * @param content The composable function that displays items of the list of ignored sources.
 */
@Composable
private fun IgnoredSourceItemBox(
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState(0)
    Column(
        modifier = Modifier.fillMaxWidth()
            .height(150.dp)
            .verticalScroll(scrollState),
        content = content
    )
}

/**
 * Displays list controls for adding and removing sources.
 *
 * @param onAdd Called when the add button is clicked.
 * @param onRemove Called when the remove button is clicked.
 * @param sourceSelected The state of the selected source.
 *   If no source is selected, the state value is `null`.
 */
@Composable
private fun IgnoredSourcesControl(
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    sourceSelected: MutableState<IgnoredSource?>
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SettingsIconButton(
            icon = painterResource(Res.drawable.add),
            onClick = onAdd,
            modifier = Modifier.testTag("add-button"),
            tooltip = "Add new to ignored"
        )
        SettingsIconButton(
            icon = painterResource(Res.drawable.remove),
            onClick = {
                onRemove()
                sourceSelected.value = null
            },
            modifier = Modifier.testTag("remove-button"),
            tooltip = "Remove selected from ignored",
            enabled = sourceSelected.value != null,
        )
    }
}

/**
 * Displays the icon button in the style of the settings page.
 *
 * @param icon The painter to draw icon.
 * @param onClick Called when this icon button is clicked.
 * @param tooltip The text to be displayed in the tooltip.
 * @param modifier The modifier to be applied to this icon button.
 * @param enabled Controls the enabled state of this icon button.
 * @param sizeFraction The proportion of the button's size that the icon occupies.
 */
@Composable
private fun SettingsIconButton(
    icon: Painter,
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sizeFraction: Float = 0.85f
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    IconButton(
        icon = icon,
        onClick = onClick,
        modifier = modifier.size(26.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.secondary
        ),
        tooltip = tooltip,
        sizeFraction = sizeFraction
    )
}

/**
 * Displays information about the ignored repository or organization.
 *
 * @param source The ignored repository or organization.
 * @param selected The state of the selected source.
 *   If no source is selected, the state value is `null`.
 */
@Composable
private fun IgnoredSourceItem(
    source: IgnoredSource,
    selected: MutableState<IgnoredSource?>
) {
    val isSelected = selected.value?.equals(source) ?: false
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    val annotationString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = contentColor)) {
            if (source.hasOrganization()) {
                append(source.organization.value.toString())
            } else {
                append(source.repository.run { "$owner/$name" })
            }
        }
        if (source.hasOrganization()) {
            withStyle(
                style = SpanStyle(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                append(" [All repos]")
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(30.dp)
            .background(containerColor)
            .semantics { role = RadioButton }
            .clickable { selected.value = if (!isSelected) source else null }
            .padding(start = 5.dp)
            .testTag("ignored-sources"),
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = annotationString,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a dialog to specify a new ignored repository or organization.
 *
 * @param state The state of the application settings.
 * @param onExit Closes the dialog.
 */
@Composable
private fun AddIgnoredSourceDialog(state: SettingsState, onExit: () -> Unit) {
    var org by remember { mutableStateOf("") }
    var isOrgValid by remember { mutableStateOf(false) }
    var repos by remember { mutableStateOf("") }
    var isReposValid by remember { mutableStateOf(false) }
    val allRepos = remember { mutableStateOf(false) }
    val isAddButtonTriggered = remember { mutableStateOf(false) }
    val isAddButtonEnabled = isOrgValid && (isReposValid || allRepos.value)
    Dialog(onDismissRequest = onExit) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .height(240.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 30.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp, CenterVertically)
        ) {
            OutlinedTextField(
                value = org,
                onValueChange = { value ->
                    isOrgValid = isValidOrganization(value)
                    org = value
                },
                label = "Organization:",
                modifier = Modifier.testTag("org-field"),
                isError = !isOrgValid,
                onEnterPressed = { if (isAddButtonEnabled) isAddButtonTriggered.value = true }
            )
            AllReposSwitch(allRepos)
            OutlinedTextField(
                value = repos,
                onValueChange = { value ->
                    isReposValid = value.split(""",\s*""".toRegex())
                        .all { isValidRepoName(it) }
                    repos = value
                },
                label = "or specified repositories (comma-separated):",
                modifier = Modifier.testTag("repos-field"),
                enabled = !allRepos.value,
                isError = !isReposValid,
                onEnterPressed = { if (isAddButtonEnabled) isAddButtonTriggered.value = true }
            )
            DialogControl(
                state = state,
                onCancel = onExit,
                addEnabled = isAddButtonEnabled,
                isAddTriggered = isAddButtonTriggered,
                org = org,
                repos = repos,
                allRepos = allRepos.value
            )
        }
    }
}

/**
 * Displays the text field in the style of the settings page.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange Called when the input service updates the text.
 * @param label The text of the label displayed above the text field.
 * @param modifier The modifier to be applied to this text field.
 * @param enabled Controls the enabled state of this text field.
 *   If `false`, the field value cannot be changed.
 * @param isError Whether the input's current value is in error.
 * @param onEnterPressed Called when this input is focused and the "Enter" key is pressed.
 */
@Composable
@Suppress("LongParameterList" /* For detailed customization. */)
private fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    onEnterPressed: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderWidth = if (isFocused) 2.dp else 1.dp
    var wasChanged by remember { mutableStateOf(false) }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.onBackground
        isError && wasChanged -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        BasicTextField(
            value = value,
            onValueChange = { value ->
                wasChanged = true
                onValueChange(value)
            },
            modifier = modifier
                .fillMaxWidth()
                .height(30.dp)
                .onKeyEvent { event ->
                    if (event.key == Key.Enter) {
                        onEnterPressed()
                        true
                    } else {
                        false
                    }
                },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = textColor
            ),
            interactionSource = interactionSource,
            singleLine = true
        ) { innerTextField ->
            Box(Modifier.fillMaxSize()) {
                OutlinedTextFieldContainer(
                    textField = innerTextField,
                    border = BorderStroke(borderWidth, borderColor)
                )
            }
        }
    }
}

/**
 * Displays a container for the text field.
 *
 * @param textField The composable function that displays the content of an input field.
 * @param border The border of this input.
 */
@Composable
private fun OutlinedTextFieldContainer(
    textField: @Composable () -> Unit,
    border: BorderStroke
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .border(border = border, shape = MaterialTheme.shapes.extraSmall)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = CenterStart,
        ) {
            textField()
        }
    }
}

/**
 * Displays a switch to add all repositories in the organization
 * to the list of ignored sources.
 *
 * @param checked Whether switch is checked.
 * @param switchScale The multiplier to scale switch along the horizontal and vertical axis.
 */
@Composable
private fun AllReposSwitch(
    checked: MutableState<Boolean>,
    switchScale: Float = 0.5f
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Switch(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            modifier = Modifier
                .scale(switchScale)
                .width(30.dp)
                .height(16.dp)
                .testTag("all-repos-switch"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondaryContainer,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedBorderColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        Text(
            text = "All within organization",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays dialog box controls, such as closing the dialog box and adding a new source.
 *
 * @param state The state of the application settings.
 * @param onCancel Called when cancel button is pressed.
 * @param addEnabled Controls the enabled state of the add button.
 *   If `false`, the add  button cannot be pressed.
 * @param org The entered name of the organization.
 * @param repos The entered name of the comma-separated repositories.
 * @param allRepos Whether to include all repositories in the organization in the ignore list.
 * @param isAddTriggered Whether the add button press is triggered externally.
 */
@Composable
@Suppress("LongParameterList" /* Requires a lot of data from the dialog form. */)
private fun DialogControl(
    state: SettingsState,
    onCancel: () -> Unit,
    addEnabled: Boolean,
    org: String,
    repos: String,
    allRepos: Boolean,
    isAddTriggered: MutableState<Boolean>
) {
    val onAdd = {
        if (allRepos) {
            val source = OrganizationLogin::class.of(org)
            state.addToIgnored(source)
        } else {
            repos.split(""",\s*""".toRegex())
                .map { name -> Repo::class.of(org, name) }
                .forEach { repo ->
                    state.addToIgnored(repo)
                }
        }
        onCancel()
    }
    if (addEnabled && isAddTriggered.value) {
        onAdd()
    }
    isAddTriggered.value = false
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, End)
    ) {
        SettingsButton(
            onClick = onCancel,
            text = "Cancel",
            modifier = Modifier.fillMaxHeight().testTag("cancel-button-in-dialog")
        )
        SettingsButton(
            onClick = onAdd,
            text = "Add to ignored",
            modifier = Modifier.fillMaxHeight().testTag("add-button-in-dialog"),
            enabled = addEnabled,
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
            usePrimaryColors = true
        )
    }
}
