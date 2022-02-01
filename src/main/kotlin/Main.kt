// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview
fun FrameWindowScope.App() {

    val scope = rememberCoroutineScope()

    val historyTopicList = remember { mutableStateListOf<GitHubTopic>() }

    var topicList by remember { mutableStateOf<List<GitHubTopic>>(emptyList()) }
    val topicsToSearch = remember { mutableStateListOf<String>() }
    var text by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    val topicState = rememberLazyListState()
    val historyState = rememberLazyListState()

    var page by remember { mutableStateOf(1) }

    val scaffoldState = rememberScaffoldState()

    var topicSelected by remember { mutableStateOf(-1) }
    var repoSelected by remember { mutableStateOf(-1) }
    var historySelected by remember { mutableStateOf(-1) }

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
                        state.scrollToItem(0)
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
                    state.scrollToItem(0)
                }
            } else {
                isAskingToClose = true
            }
        }

        fun search(pageToSearch: Int = 1) {
            scope.launch {
                if (topicsToSearch.isNotEmpty()) {
                    page = pageToSearch
                    repoSelected = -1
                    withContext(Dispatchers.IO) {
                        topicList = getTopics2(
                            searching = { showSearching = true },
                            done = { showSearching = false },
                            topics = topicsToSearch.toTypedArray(),
                            page = pageToSearch
                        )
                    }
                    state.scrollToItem(0)
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
                    shortcut = KeyShortcut(Key.PageUp, meta = true, shift = true)
                )
                Item(
                    "Next",
                    onClick = {
                        if (topicsToSearch.isNotEmpty() && topicSelected < topicsToSearch.lastIndex) {
                            scope.launch { topicState.animateScrollToItem(++topicSelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.PageDown, meta = true, shift = true)
                )
                Separator()
                Item(
                    "Delete",
                    onClick = {
                        if (topicsToSearch.isNotEmpty() && topicSelected in 0..topicsToSearch.lastIndex)
                            topicsToSearch.removeAt(topicSelected)
                    },
                    shortcut = KeyShortcut(Key.Delete, meta = true, shift = true)
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
                    enabled = topicList.isNotEmpty() && repoSelected in 0..topicList.lastIndex,
                    onClick = {
                        if (topicList.isNotEmpty() && repoSelected in 0..topicList.lastIndex) {
                            topicList.getOrNull(repoSelected)?.let {
                                val url = it.url
                                if (url.isNotEmpty()) {
                                    Desktop.getDesktop().browse(URI(url))
                                }
                                if (it !in historyTopicList) historyTopicList.add(it)
                            }
                        }
                    },
                    shortcut = KeyShortcut(Key.O, meta = true)
                )
            }

            // Page and Search
            Menu("Search", 'S') {
                Item(
                    "Search",
                    onClick = { search(1) },
                    shortcut = KeyShortcut(Key.S, meta = true)
                )
                Item(
                    "Refresh",
                    onClick = { search(page) },
                    shortcut = KeyShortcut(Key.R, meta = true)
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

            // History
            Menu("History", mnemonic = 'H') {

                Item(
                    "${if (scaffoldState.drawerState.isClosed) "Open" else "Close"}  History",
                    onClick = {
                        scope.launch {
                            if (scaffoldState.drawerState.isClosed) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
                        }
                    },
                    shortcut = KeyShortcut(Key.H, meta = true, alt = true, shift = true)
                )

                Item(
                    "Previous",
                    onClick = {
                        if (historyTopicList.isNotEmpty() && historySelected > -1) {
                            scope.launch { historyState.animateScrollToItem(--historySelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.PageUp, meta = true, alt = true, shift = true)
                )
                Item(
                    "Next",
                    onClick = {
                        if (historyTopicList.isNotEmpty() && historySelected < historyTopicList.lastIndex) {
                            scope.launch { historyState.animateScrollToItem(++historySelected) }
                        }
                    },
                    shortcut = KeyShortcut(Key.PageDown, meta = true, alt = true, shift = true)
                )
                Separator()
                Item(
                    "Open",
                    enabled = historyTopicList.isNotEmpty() && historySelected in 0..historyTopicList.lastIndex,
                    onClick = {
                        if (historyTopicList.isNotEmpty() && historySelected in 0..historyTopicList.lastIndex) {
                            historyTopicList.getOrNull(historySelected)?.let {
                                val url = it.url
                                if (url.isNotEmpty()) {
                                    Desktop.getDesktop().browse(URI(url))
                                }
                            }
                        }
                    },
                    shortcut = KeyShortcut(Key.O, meta = true, alt = true, shift = true)
                )
            }

            Menu("Settings") {
                CheckboxItem(
                    "Show Icon",
                    onCheckedChange = { showIcon = it },
                    checked = showIcon
                )
            }
        }

        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("History") },
                            actions = {
                                IconButton(onClick = { scope.launch { scaffoldState.drawerState.close() } }) { Icon(Icons.Default.Close, null) }
                            }
                        )
                    },
                ) {
                    LazyColumn(
                        contentPadding = it,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        state = historyState
                    ) {
                        itemsIndexed(historyTopicList) { index, topic ->
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
                                TopicItem(topic, historySelected == index, topicsToSearch, MaterialTheme.colors.primarySurface) { t ->
                                    if (t in topicsToSearch) {
                                        topicsToSearch.remove(t)
                                    } else {
                                        topicsToSearch.add(t)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Topics") },
                    actions = {

                        CustomTooltip(
                            tooltip = { Box(Modifier.padding(10.dp)) { Text("History (Cmd+Alt+Shift+H)") } },
                        ) {
                            IconButton(onClick = {
                                scope.launch {
                                    if (scaffoldState.drawerState.isClosed) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
                                }
                            }) { Icon(Icons.Default.History, null) }
                        }

                        CustomTooltip(
                            tooltip = { Box(Modifier.padding(10.dp)) { Text("Refresh (Cmd+R)") } },
                        ) { IconButton(onClick = { search(page) }) { Icon(Icons.Default.Refresh, null) } }

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
                CustomBottomAppBar {
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
                                ) { IconButton(onClick = { search(1) }) { Icon(Icons.Default.Search, null) } }
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
                            //.padding(4.dp)
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
                Box(modifier = Modifier.weight(5f)) {

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
                                    TopicItem(
                                        topic,
                                        repoSelected == index,
                                        topicsToSearch,
                                        onClick = { if (topic !in historyTopicList) historyTopicList.add(topic) }
                                    ) { t ->
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

private val timePrinter = PrettyTime()

// take timestamp and return a formatted string
fun formatTimestamp(timestamp: String): String {
    //2022-01-27T13:11:53Z
    val format = SimpleDateFormat("MMM dd, yyyy hh:mm a")
    val date = Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return timePrinter.format(Date(date)) + " on " + format.format(date)
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
fun TopicItem(
    item: GitHubTopic,
    isSelected: Boolean,
    topicList: List<String> = emptyList(),
    unselectedColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
    onChipClick: (String) -> Unit
) {
    Card(
        onClick = { Desktop.getDesktop().browse(URI.create(item.url)).also { onClick() } },
        border = BorderStroke(
            width = animateDpAsState(if (isSelected) 4.dp else 0.dp).value,
            color = animateColorAsState(if (isSelected) MaterialBlue else unselectedColor).value
        )
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            ListItem(
                text = { Text(item.name) },
                secondaryText = { Text(item.description) },
                icon = if (showIcon) item.avatarUrl?.let {
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
                } else null,
                overlineText = {
                    Text(
                        item.url,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialBlue
                    )
                }
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
                text = "Updated ${formatTimestamp(item.pushedAt)}",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

var showIcon by mutableStateOf(false)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "GitHub Topics") {
        App()
    }
}
