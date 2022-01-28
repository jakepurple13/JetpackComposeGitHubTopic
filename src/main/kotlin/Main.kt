// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview
fun FrameWindowScope.App() {

    val scope = rememberCoroutineScope()

    var topicList by remember { mutableStateOf<List<GitHubTopic>>(emptyList()) }
    val topicsToSearch = remember { mutableStateListOf<String>() }
    var text by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    val topicState = rememberLazyListState()

    var page by remember { mutableStateOf(1) }

    var topicSelected by remember { mutableStateOf(-1) }
    var repoSelected by remember { mutableStateOf(-1) }

    MaterialTheme(colors = darkColors(primary = MaterialBlue)) {

        var showSearching by remember { mutableStateOf(false) }
        var isAskingToClose by remember { mutableStateOf(false) }

        if (isAskingToClose) {
            AlertDialog(
                onDismissRequest = { isAskingToClose = false },
                title = { Text("Please enter topics to search") },
                text = { Text("Enter topics to search") },
                confirmButton = { Button(onClick = { isAskingToClose = false }) { Text("Close") } },
                dialogProvider = PopupAlertDialogProvider
            )
        }

        fun previousPage() {
            if (topicsToSearch.isNotEmpty()) {
                if (page > 1) {
                    page--
                    repoSelected = -1
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            topicList = getTopics2(
                                searching = { showSearching = true },
                                done = { showSearching = false },
                                topics = topicsToSearch.toTypedArray(),
                                page = page
                            )
                        }
                        state.scrollToItem(1)
                    }
                }
            } else {
                isAskingToClose = true
            }
        }

        fun nextPage() {
            if (topicsToSearch.isNotEmpty()) {
                scope.launch {
                    repoSelected = -1
                    withContext(Dispatchers.IO) {
                        topicList = getTopics2(
                            searching = { showSearching = true },
                            done = { showSearching = false },
                            topics = topicsToSearch.toTypedArray(),
                            page = ++page
                        )
                    }
                    state.scrollToItem(1)
                }
            } else {
                isAskingToClose = true
            }
        }

        fun search() {
            scope.launch {
                if (topicsToSearch.isNotEmpty()) {
                    page = 1
                    repoSelected = -1
                    withContext(Dispatchers.IO) {
                        topicList = getTopics2(
                            searching = { showSearching = true },
                            done = { showSearching = false },
                            topics = topicsToSearch.toTypedArray(),
                            page = 1
                        )
                    }
                    state.scrollToItem(1)
                } else {
                    isAskingToClose = true
                }
            }
        }

        MenuBar {
            // Topic Selector and Deleter
            Menu("Topics", mnemonic = 'T') {
                Item(
                    "Previous",
                    onClick = {
                        if (topicsToSearch.isNotEmpty() && topicSelected > -1) {
                            scope.launch { topicState.animateScrollToItem(--topicSelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.Minus, meta = true)
                )
                Item(
                    "Next",
                    onClick = {
                        if (topicsToSearch.isNotEmpty() && topicSelected < topicsToSearch.lastIndex) {
                            scope.launch { topicState.animateScrollToItem(++topicSelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.Plus, meta = true)
                )
                Separator()
                Item(
                    "Delete",
                    onClick = {
                        if (topicsToSearch.isNotEmpty() && topicSelected in 0..topicsToSearch.lastIndex)
                            topicsToSearch.removeAt(topicSelected)
                    },
                    shortcut = KeyShortcut(Key.Delete, meta = true)
                )
            }

            // Repo Selector
            Menu("Repo", mnemonic = 'R') {
                Item(
                    "Previous",
                    onClick = {
                        if (topicList.isNotEmpty() && repoSelected > -1) {
                            scope.launch { state.animateScrollToItem(--repoSelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.PageUp, meta = true)
                )
                Item(
                    "Next",
                    onClick = {
                        if (topicList.isNotEmpty() && repoSelected < topicList.lastIndex) {
                            scope.launch { state.animateScrollToItem(++repoSelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.PageDown, meta = true)
                )
                Separator()
                Item(
                    "Open",
                    onClick = {
                        if (topicList.isNotEmpty() && repoSelected in 0..topicList.lastIndex) {
                            topicList.getOrNull(repoSelected)?.let {
                                val url = it.url
                                if (url.isNotEmpty()) {
                                    Desktop.getDesktop().browse(URI(url))
                                }
                            }
                        }
                    },
                    shortcut = KeyShortcut(Key.Multiply, meta = true)
                )
            }

            // Page and Search
            Menu("Search", 'S') {
                Item(
                    "Search",
                    onClick = { search() },
                    shortcut = KeyShortcut(Key.S, meta = true)
                )
                Separator()
                Item(
                    "Previous Page",
                    onClick = { previousPage() },
                    shortcut = KeyShortcut(Key.LeftBracket, meta = true)
                )
                Item(
                    "Next Page",
                    onClick = { nextPage() },
                    shortcut = KeyShortcut(Key.RightBracket, meta = true)
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Topics") },
                    actions = {
                        Text("Page: $page")

                        CustomTooltip(
                            tooltip = { Box(Modifier.padding(10.dp)) { Text("Previous Page (Cmd+Left)") } }
                        ) { IconButton(onClick = { previousPage() }) { Icon(Icons.Default.KeyboardArrowLeft, null) } }
                        CustomTooltip(
                            tooltip = { Box(Modifier.padding(10.dp)) { Text("Next Page (Cmd+Right)") } }
                        ) { IconButton(onClick = { nextPage() }) { Icon(Icons.Default.KeyboardArrowRight, null) } }

                        Text("${topicList.size}")
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier.background(MaterialTheme.colors.primarySurface),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        label = { Text("Add Topic") },
                        placeholder = { Text("Press Enter to Add Topic") },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CustomTooltip(
                                    tooltip = { Box(Modifier.padding(10.dp)) { Text("Add to List (Enter)") } },
                                    backgroundColor = MaterialTheme.colors.primarySurface,
                                ) {
                                    IconButton(onClick = {
                                        if (text.isNotEmpty()) {
                                            topicsToSearch.add(text)
                                            text = ""
                                        }
                                    }) { Icon(Icons.Default.Add, null) }
                                }

                                CustomTooltip(
                                    tooltip = { Box(Modifier.padding(10.dp)) { Text("Search (Cmd+S)") } },
                                    backgroundColor = MaterialTheme.colors.primarySurface,
                                ) { IconButton(onClick = { search() }) { Icon(Icons.Default.Search, null) } }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                topicsToSearch.add(text.trim())
                                text = ""
                            }
                        ),
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                when {
                                    // Add to search list
                                    text.isNotEmpty() && it.key == Key.Enter -> {
                                        topicsToSearch.add(text.trim())
                                        text = ""
                                        true
                                    }
                                    else -> false
                                }
                            }
                    )
                }
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(it)
            ) {
                Box(modifier = Modifier.weight(4f)) {

                    if (showSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.Center),
                            color = Color.Cyan
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .padding(vertical = 4.dp),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(topicList) { index, topic ->
                                CustomTooltip(
                                    tooltip = {
                                        // composable tooltip content
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, null)
                                                Text(topic.stars.toString())
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, null)
                                                Text(topic.watchers.toString())
                                            }
                                        }
                                    }
                                ) {
                                    TopicItem(topic, repoSelected == index, topicsToSearch) { t ->
                                        if (t in topicsToSearch) {
                                            topicsToSearch.remove(t)
                                        } else {
                                            topicsToSearch.add(t)
                                        }
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(scrollState = state)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    state = topicState,
                ) {
                    stickyHeader { Text("Topics to Search for:") }
                    itemsIndexed(topicsToSearch) { index, topic ->
                        Card(
                            onClick = { topicsToSearch.remove(topic) },
                            border = BorderStroke(
                                width = animateDpAsState(if (topicSelected == index) 4.dp else 0.dp).value,
                                color = animateColorAsState(if (topicSelected == index) MaterialBlue else Color.Transparent).value
                            )
                        ) {
                            ListItem(
                                text = { Text(topic) },
                                icon = { Icon(Icons.Default.Clear, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// take timestamp and return a formatted string
fun formatTimestamp(timestamp: String): String {
    //2022-01-27T13:11:53Z
    val format = SimpleDateFormat("MMM dd, yyyy hh:mm a")
    return format.format(Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
}

@ExperimentalFoundationApi
@Composable
fun CustomTooltip(
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            // composable tooltip content
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = backgroundColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(4.dp)
            ) { tooltip() }
        }
    ) { content() }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TopicItem(item: GitHubTopic, isSelected: Boolean, topicList: List<String> = emptyList(), onChipClick: (String) -> Unit) {
    Card(
        onClick = { Desktop.getDesktop().browse(URI.create(item.url)) },
        border = BorderStroke(
            width = animateDpAsState(if (isSelected) 4.dp else 0.dp).value,
            color = animateColorAsState(if (isSelected) MaterialBlue else Color.Transparent).value
        )
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            ListItem(
                text = { Text(item.name) },
                secondaryText = { Text(item.description) },
                icon = item.avatarUrl?.let {
                    {
                        Surface(shape = CircleShape) {
                            AsyncImage(
                                load = { loadImageBitmap(it) },
                                modifier = Modifier.size(48.dp),
                                painterFor = { remember { BitmapPainter(it) } },
                                contentDescription = null,
                            )
                        }
                    }
                },
                overlineText = { Text(item.url) }
            )

            FlowRow(modifier = Modifier.padding(4.dp)) {
                item.topics.forEach {
                    CustomChip(
                        it,
                        modifier = Modifier
                            .clickable { onChipClick(it) }
                            .padding(2.dp),
                        textColor = animateColorAsState(
                            if (topicList.any { t -> t.equals(it, true) }) MaterialTheme.colors.onPrimary
                            else MaterialTheme.colors.onSurface
                        ).value,
                        backgroundColor = animateColorAsState(
                            if (topicList.any { t -> t.equals(it, true) }) MaterialBlue
                            else MaterialTheme.colors.surface
                        ).value
                    )
                }
            }

            Text(
                text = "Updated at ${formatTimestamp(item.pushedAt)}",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "GitHub Topics") {
        App()
    }
}


