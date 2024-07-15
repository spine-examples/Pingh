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

package io.spine.examples.pingh.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.desktop.component.Avatar
import io.spine.examples.pingh.desktop.component.IconButton
import io.spine.examples.pingh.github.buildBy
import io.spine.net.Url

@Composable
public fun HomePage(client: DesktopClient) {
    val model = remember { HomePageModel(client) }

    Column(
        Modifier.fillMaxSize()
    ) {
        ToolBar()
        MentionCards()
    }
}

@Composable
private fun ToolBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(bottom = 1.dp)
            .background(MaterialTheme.colorScheme.secondary)
            //.shadow()
            .drawBehind {
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        IconButton(
            Icons.Default.Add,
            30.dp,
            { println(2) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White
            )
        )
        Text(
            "Pingh",
            style = MaterialTheme.typography.displayLarge
        )
    }
}

@Composable
private fun MentionCards() {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background),
    ) {

        for (i in 1..10) {
            Spacer(Modifier.height(20.dp))
            MentionCard()
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MentionCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 10.dp)
        ) {
            Avatar(
                url = Url::class.buildBy("https://avatars.githubusercontent.com/u/160486193?v=4"),
                size = 40.dp
            )
            Spacer(Modifier.width(5.dp))
            MentionCardText()
            Spacer(Modifier.width(5.dp))
            IconButton(
                Icons.Default.Edit,
                40.dp,
                { println(1) }
            )
        }
    }
}

@Composable
private fun MentionCardText() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "amiol/Pingh",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "12:30:23",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
