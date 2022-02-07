// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview
fun FrameWindowScope.App() {

    val viewModel = remember { TopicViewModel() }
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    val topicState = rememberLazyListState()
    val historyState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()

    if (viewModel.isAskingToClose) {
        AlertDialog(
            onDismissRequest = { viewModel.isAskingToClose = false },
            title = { Text("Please enter topics to search") },
            text = { Text("Enter topics to search") },
            confirmButton = { Button(onClick = { viewModel.isAskingToClose = false }) { Text("Close") } },
            dialogProvider = PopupAlertDialogProvider
        )
    }

    MenuBar {
        // Topic Selector and Deleter
        Menu("Topics", mnemonic = 'T') {
            Item(
                "Previous",
                onClick = { scope.launch { viewModel.previousTopicSelect(topicState) } },
                shortcut = KeyShortcut(Key.PageUp, meta = true, shift = true)
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextTopicSelect(topicState) } },
                shortcut = KeyShortcut(Key.PageDown, meta = true, shift = true)
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopTopic(topicState) } },
                shortcut = KeyShortcut(Key.MoveHome, meta = true, shift = true)
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomTopic(topicState) } },
                shortcut = KeyShortcut(Key.MoveEnd, meta = true, shift = true)
            )
            Separator()
            Item(
                "Delete",
                onClick = { viewModel.deleteTopic() },
                shortcut = KeyShortcut(Key.Delete, meta = true, shift = true)
            )
        }

        // Repo Selector
        Menu("Repo", mnemonic = 'R') {
            Item(
                "Previous",
                onClick = { scope.launch { viewModel.previousRepoSelect(state) } },
                shortcut = KeyShortcut(Key.PageUp, meta = true)
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextRepoSelect(state) } },
                shortcut = KeyShortcut(Key.PageDown, meta = true)
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopRepo(state) } },
                shortcut = KeyShortcut(Key.MoveHome, meta = true)
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomRepo(state) } },
                shortcut = KeyShortcut(Key.MoveEnd, meta = true)
            )
            Separator()
            Item(
                "Open",
                enabled = viewModel.repoList.isNotEmpty() && viewModel.repoSelected in 0..viewModel.repoList.lastIndex,
                onClick = { viewModel.openSelectedRepo() },
                shortcut = KeyShortcut(Key.O, meta = true)
            )
            Item(
                "Add to History",
                onClick = { viewModel.addSelectedTopicToHistory() },
                shortcut = KeyShortcut(Key.A, meta = true, shift = true, alt = true)
            )
        }

        // Page and Search
        Menu("Search", 'S') {
            Item(
                "Search",
                onClick = { scope.launch { viewModel.search(state, 1) } },
                shortcut = KeyShortcut(Key.S, meta = true)
            )
            Item(
                "Refresh",
                onClick = { scope.launch { viewModel.refresh(state) } },
                shortcut = KeyShortcut(Key.R, meta = true)
            )
            Separator()
            Item(
                "Previous Page",
                onClick = { scope.launch { viewModel.previousPage(state) } },
                shortcut = KeyShortcut(Key.LeftBracket, meta = true)
            )
            Item(
                "Next Page",
                onClick = { scope.launch { viewModel.nextPage(state) } },
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
                onClick = { scope.launch { viewModel.previousHistorySelect(historyState) } },
                shortcut = KeyShortcut(Key.PageUp, meta = true, alt = true, shift = true)
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextHistorySelect(historyState) } },
                shortcut = KeyShortcut(Key.PageDown, meta = true, alt = true, shift = true)
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopHistory(historyState) } },
                shortcut = KeyShortcut(Key.MoveHome, meta = true, alt = true, shift = true)
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomHistory(historyState) } },
                shortcut = KeyShortcut(Key.MoveEnd, meta = true, alt = true, shift = true)
            )
            Separator()
            Item(
                "Open",
                enabled = viewModel.historyDBList.isNotEmpty() && viewModel.historySelected in 0..viewModel.historyDBList.lastIndex,
                onClick = { viewModel.openSelectedHistory() },
                shortcut = KeyShortcut(Key.O, meta = true, alt = true, shift = true)
            )
            Item(
                "Delete",
                onClick = { viewModel.removeSelectedTopicFromHistory() },
                shortcut = KeyShortcut(Key.Delete, meta = true, shift = true, alt = true)
            )
        }

        Menu("Settings") {
            CheckboxItem(
                "Show Icon",
                onCheckedChange = { showIcon = it },
                checked = showIcon
            )
            CheckboxItem(
                "Dark Theme",
                onCheckedChange = { darkTheme = it },
                checked = darkTheme
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
                    modifier = Modifier.padding(4.dp),
                    state = historyState
                ) {
                    itemsIndexed(viewModel.historyDBList) { index, topic ->
                        ContextMenuArea(
                            items = {
                                listOf(
                                    ContextMenuItem("Open") { viewModel.historyClick(topic) },
                                    ContextMenuItem("Remove") { viewModel.removeTopicFromHistory(topic) },
                                    ContextMenuItem("Stars: ${topic.stars}") {},
                                    ContextMenuItem("Watchers: ${topic.watchers}") {}
                                )
                            }
                        ) {
                            TopicItem(
                                topic,
                                viewModel.historySelected == index,
                                viewModel,
                                MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                                modifier = Modifier
                                    .onPointerEvent(PointerEventType.Enter) { viewModel.historySelected = index }
                                    .onPointerEvent(PointerEventType.Exit) { viewModel.historySelected = -1 }
                            ) { viewModel.historyClick(topic) }
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
                        }) { Icon(Icons.Default.Menu, null) }
                    }

                    CustomTooltip(
                        tooltip = { Box(Modifier.padding(10.dp)) { Text("Refresh (Cmd+R)") } },
                    ) { IconButton(onClick = { scope.launch { viewModel.refresh(state) } }) { Icon(Icons.Default.Refresh, null) } }

                    Text("Page: ${viewModel.page}")

                    CustomTooltip(
                        tooltip = { Box(Modifier.padding(10.dp)) { Text("Previous Page (Cmd+Left)") } }
                    ) { IconButton(onClick = { scope.launch { viewModel.previousPage(state) } }) { Icon(Icons.Default.KeyboardArrowLeft, null) } }
                    CustomTooltip(
                        tooltip = { Box(Modifier.padding(10.dp)) { Text("Next Page (Cmd+Right)") } }
                    ) { IconButton(onClick = { scope.launch { viewModel.nextPage(state) } }) { Icon(Icons.Default.KeyboardArrowRight, null) } }

                    Text("${viewModel.repoList.size}")
                }
            )
        },
        bottomBar = {
            CustomBottomAppBar {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = { viewModel.text = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    label = { Text("Add Topic") },
                    placeholder = { Text("Press Enter to Add Topic") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomTooltip(
                                tooltip = { Box(Modifier.padding(10.dp)) { Text("Add to List (Enter)") } },
                                backgroundColor = MaterialTheme.colors.primarySurface,
                            ) { IconButton(onClick = { viewModel.onTopicAdd() }) { Icon(Icons.Default.Add, null) } }

                            CustomTooltip(
                                tooltip = { Box(Modifier.padding(10.dp)) { Text("Search (Cmd+S)") } },
                                backgroundColor = MaterialTheme.colors.primarySurface,
                            ) { IconButton(onClick = { scope.launch { viewModel.search(state, 1) } }) { Icon(Icons.Default.Search, null) } }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { viewModel.onTopicAdd() }),
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            when {
                                // Add to search list
                                it.key == Key.Enter -> {
                                    viewModel.onTopicAdd()
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
                if (viewModel.showSearching) {
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
                        itemsIndexed(viewModel.repoList) { index, topic ->
                            ContextMenuArea(
                                items = {
                                    listOf(
                                        ContextMenuItem("Open") { viewModel.cardClick(topic) },
                                        ContextMenuItem("Add to History") { viewModel.addTopicToHistory(topic) },
                                        ContextMenuItem("Stars: ${topic.stars}") {},
                                        ContextMenuItem("Watchers: ${topic.watchers}") {},
                                    )
                                }
                            ) {
                                TopicItem(
                                    topic,
                                    viewModel.repoSelected == index,
                                    viewModel = viewModel,
                                    modifier = Modifier
                                        .onPointerEvent(PointerEventType.Enter) { viewModel.repoSelected = index }
                                        .onPointerEvent(PointerEventType.Exit) { viewModel.repoSelected = -1 }
                                ) { viewModel.cardClick(topic) }
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
                itemsIndexed(viewModel.topicsToSearch) { index, topic ->
                    ContextMenuArea(items = { listOf(ContextMenuItem("Remove") { viewModel.removeTopic(topic) }) }) {
                        Card(
                            onClick = { viewModel.removeTopic(topic) },
                            border = BorderStroke(
                                width = animateDpAsState(if (viewModel.topicSelected == index) 4.dp else 0.dp).value,
                                color = animateColorAsState(
                                    if (viewModel.topicSelected == index) MaterialBlue
                                    else MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                                ).value
                            ),
                            modifier = Modifier
                                .onPointerEvent(PointerEventType.Enter) { viewModel.topicSelected = index }
                                .onPointerEvent(PointerEventType.Exit) { viewModel.topicSelected = -1 }
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
@ExperimentalMaterialApi
@Composable
fun TopicItem(
    item: GitHubTopic,
    isSelected: Boolean,
    viewModel: TopicViewModel,
    unselectedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        border = BorderStroke(
            width = animateDpAsState(if (isSelected) 4.dp else 0.dp).value,
            color = animateColorAsState(if (isSelected) MaterialBlue else unselectedColor).value
        ),
        modifier = modifier
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
                        item.fullName,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialBlue
                    )
                }
            )

            FlowRow(modifier = Modifier.padding(4.dp)) {
                item.topics.forEach {
                    CustomChip(
                        it,
                        modifier = Modifier.padding(2.dp),
                        textColor = animateColorAsState(
                            if (viewModel.topicsToSearch.any { t -> t.equals(it, true) }) MaterialTheme.colors.onPrimary
                            else MaterialTheme.colors.onSurface
                        ).value,
                        backgroundColor = animateColorAsState(
                            if (viewModel.topicsToSearch.any { t -> t.equals(it, true) }) MaterialBlue
                            else MaterialTheme.colors.surface
                        ).value
                    ) { viewModel.toggleTopic(it) }
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
var darkTheme by mutableStateOf(false)

@Composable
fun InitialSetup() {
    val isSystemDark = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        darkTheme = isSystemDark
        dbInit()
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun main() = application {
    InitialSetup()
    val state = rememberWindowState()

    Window(
        state = state,
        onCloseRequest = ::exitApplication,
        title = "GitHub Topics",
        undecorated = true,
        transparent = true
    ) {
        MaterialTheme(colors = if (darkTheme) darkColors(primary = MaterialBlue) else lightColors(primary = MaterialBlue)) {
            Surface(shape = if (state.placement == WindowPlacement.Maximized) RectangleShape else RoundedCornerShape(8.dp)) {
                Column {
                    WindowDraggableArea(
                        modifier = Modifier.combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                            onDoubleClick = {
                                state.placement = if (state.placement != WindowPlacement.Maximized) {
                                    WindowPlacement.Maximized
                                } else {
                                    WindowPlacement.Floating
                                }
                            }
                        )
                    ) {
                        val hasFocus = LocalWindowInfo.current.isWindowFocused
                        val focusedAlpha by animateFloatAsState(if (hasFocus || darkTheme) 1.0f else 0.5f)

                        TopAppBar(
                            title = { Text("GitHub Topics") },
                            elevation = animateDpAsState(
                                if (darkTheme) {
                                    if (hasFocus) 0.dp else AppBarDefaults.TopAppBarElevation
                                } else {
                                    if (hasFocus) AppBarDefaults.TopAppBarElevation else 0.dp
                                }
                            ).value,
                            backgroundColor = MaterialTheme.colors.primarySurface.copy(alpha = focusedAlpha),
                            actions = {
                                IconButton(onClick = ::exitApplication) { Icon(Icons.Default.Close, null) }
                                IconButton(onClick = { state.isMinimized = true }) { Icon(Icons.Default.Minimize, null) }
                                IconButton(
                                    onClick = {
                                        state.placement = if (state.placement != WindowPlacement.Maximized) {
                                            WindowPlacement.Maximized
                                        } else {
                                            WindowPlacement.Floating
                                        }
                                    }
                                ) { Icon(Icons.Default.Maximize, null) }
                            }
                        )
                    }
                    App()
                }
            }
        }
    }
}
