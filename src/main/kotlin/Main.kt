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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
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
                shortcut = Shortcuts.PreviousTopic.keyShortcut()
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextTopicSelect(topicState) } },
                shortcut = Shortcuts.NextTopic.keyShortcut()
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopTopic(topicState) } },
                shortcut = Shortcuts.ScrollToTopTopic.keyShortcut()
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomTopic(topicState) } },
                shortcut = Shortcuts.ScrollToBottomTopic.keyShortcut()
            )
            Separator()
            Item(
                "Delete",
                onClick = { viewModel.deleteTopic() },
                shortcut = Shortcuts.DeleteTopic.keyShortcut()
            )
        }

        // Repo Selector
        Menu("Repo", mnemonic = 'R') {
            Item(
                "Previous",
                onClick = { scope.launch { viewModel.previousRepoSelect(state) } },
                shortcut = Shortcuts.PreviousRepo.keyShortcut()
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextRepoSelect(state) } },
                shortcut = Shortcuts.NextRepo.keyShortcut()
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopRepo(state) } },
                shortcut = Shortcuts.ScrollToTopRepo.keyShortcut()
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomRepo(state) } },
                shortcut = Shortcuts.ScrollToBottomRepo.keyShortcut()
            )
            Separator()
            Item(
                "Open",
                enabled = viewModel.repoList.isNotEmpty() && viewModel.repoSelected in 0..viewModel.repoList.lastIndex,
                onClick = { viewModel.openSelectedRepo() },
                shortcut = Shortcuts.OpenRepo.keyShortcut()
            )
            Item(
                "Add to History",
                onClick = { viewModel.addSelectedTopicToHistory() },
                shortcut = Shortcuts.AddRepoToHistory.keyShortcut()
            )
        }

        // Page and Search
        Menu("Search", 'S') {
            Item(
                "Search",
                onClick = { scope.launch { viewModel.search(state, 1) } },
                shortcut = Shortcuts.Search.keyShortcut()
            )
            Item(
                "Refresh",
                onClick = { scope.launch { viewModel.refresh(state) } },
                shortcut = Shortcuts.Refresh.keyShortcut()
            )
            Separator()
            Item(
                "Previous Page",
                onClick = { scope.launch { viewModel.previousPage(state) } },
                shortcut = Shortcuts.PreviousPage.keyShortcut()
            )
            Item(
                "Next Page",
                onClick = { scope.launch { viewModel.nextPage(state) } },
                shortcut = Shortcuts.NextPage.keyShortcut()
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
                shortcut = Shortcuts.OpenCloseHistory.keyShortcut()
            )

            Item(
                "Previous",
                onClick = { scope.launch { viewModel.previousHistorySelect(historyState) } },
                shortcut = Shortcuts.PreviousHistory.keyShortcut()
            )
            Item(
                "Next",
                onClick = { scope.launch { viewModel.nextHistorySelect(historyState) } },
                shortcut = Shortcuts.NextHistory.keyShortcut()
            )
            Item(
                "Scroll to top",
                onClick = { scope.launch { viewModel.scrollToTopHistory(historyState) } },
                shortcut = Shortcuts.ScrollToTopHistory.keyShortcut()
            )
            Item(
                "Scroll to bottom",
                onClick = { scope.launch { viewModel.scrollToBottomHistory(historyState) } },
                shortcut = Shortcuts.ScrollToBottomHistory.keyShortcut()
            )
            Separator()
            Item(
                "Open",
                enabled = viewModel.historyDBList.isNotEmpty() && viewModel.historySelected in 0..viewModel.historyDBList.lastIndex,
                onClick = { viewModel.openSelectedHistory() },
                shortcut = Shortcuts.OpenHistory.keyShortcut()
            )
            Item(
                "Delete",
                onClick = { viewModel.removeSelectedTopicFromHistory() },
                shortcut = Shortcuts.DeleteHistory.keyShortcut()
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
                            Text("${viewModel.historyDBList.size}")
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
                                    ContextMenuItem("Copy Url") {
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(topic.url), null)
                                    },
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
                navigationIcon = {
                    CustomTooltip(tooltip = { Box(Modifier.padding(10.dp)) { Text("History (Cmd+Alt+Shift+H)") } }) {
                        IconButton(onClick = {
                            scope.launch {
                                if (scaffoldState.drawerState.isClosed) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
                            }
                        }) { Icon(Icons.Default.Menu, null) }
                    }
                },
                actions = {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { scope.launch { viewModel.scrollToTopRepo(state) } },
                backgroundColor = MaterialTheme.colors.primary,
            ) { Icon(Icons.Default.KeyboardArrowUp, null) }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            CustomBottomAppBar {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = { viewModel.text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = if (darkTheme) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
                        else MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.medium),
                        focusedLabelColor = if (darkTheme) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
                        else MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.medium),
                        trailingIconColor = if (darkTheme) MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity)
                        else MaterialTheme.colors.onPrimary.copy(alpha = TextFieldDefaults.IconOpacity),
                        placeholderColor = if (darkTheme) MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                        else MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.medium)
                    ),
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
                            val topic = Shortcuts.AddTopic
                            when {
                                // Add to search list
                                it.key in topic.keys.filterOutModifiers() &&
                                        it.isMetaPressed == topic.keys.anyMeta() &&
                                        it.isShiftPressed == topic.keys.anyShift() &&
                                        it.isAltPressed == topic.keys.anyAlt() &&
                                        it.isCtrlPressed == topic.keys.anyCtrl() -> {
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
                        color = MaterialTheme.colors.primary
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
                                        ContextMenuItem("Copy Url") {
                                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(topic.url), null)
                                        },
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
                stickyHeader {
                    Text(
                        "Topics to Search for:",
                        modifier = Modifier
                            .background(MaterialTheme.colors.surface)
                            .padding(4.dp)
                            .fillMaxWidth()
                    )
                }
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
                                .cursorForSelectable()
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
        modifier = modifier.cursorForSelectable()
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

            Row {
                Text(
                    text = "Updated ${formatTimestamp(item.pushedAt)}",
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f)
                )

                Text(
                    text = item.language,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f)
                )
            }
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

    var showKeyboard by remember { mutableStateOf(false) }

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
                        val focusedAlpha by animateFloatAsState(if (hasFocus) 1.0f else 0.5f)

                        TopAppBar(
                            title = { Text("GitHub Topics") },
                            elevation = animateDpAsState(if (hasFocus) AppBarDefaults.TopAppBarElevation else 0.dp).value,
                            backgroundColor = MaterialTheme.colors.primarySurface.copy(alpha = focusedAlpha),
                            actions = {
                                IconButton(onClick = { showKeyboard = true }) { Icon(Icons.Default.Settings, null) }
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
                    Divider(color = MaterialTheme.colors.onSurface)
                    App()
                }
            }
        }
    }

    if (showKeyboard) {
        KeyboardView { showKeyboard = false }
    }
}
