@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.event.KeyEvent
import java.util.prefs.Preferences

sealed class Shortcuts(val defaultKeys: List<Key>, private val visibleText: String) {

    var keys by mutableStateOf(defaultKeys)
    val name = this::class.simpleName ?: this::class.java.name

    init {
        val preferences = Preferences.userNodeForPackage(ShortcutViewModel::class.java)
        keys = preferences.get(name, defaultKeys.toJson()).fromJson<List<Key>>() ?: defaultKeys
    }

    object Search : Shortcuts(listOf(Key.MetaLeft, Key.S), "Search")

    object AddTopic : Shortcuts(listOf(Key.Enter), "Add Topic")
    object PreviousTopic : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.PageUp), "Previous Topic")
    object NextTopic : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.PageDown), "Next Topic")
    object ScrollToTopTopic : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.MoveHome), "Scroll to Top Topic")
    object ScrollToBottomTopic : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.MoveEnd), "Scroll to Bottom Topic")
    object DeleteTopic : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.Delete), "Delete Topic")

    object PreviousRepo : Shortcuts(listOf(Key.MetaLeft, Key.PageUp), "Previous Repo")
    object NextRepo : Shortcuts(listOf(Key.MetaLeft, Key.PageDown), "Next Repo")
    object ScrollToTopRepo : Shortcuts(listOf(Key.MetaLeft, Key.MoveHome), "Scroll to Top Repo")
    object ScrollToBottomRepo : Shortcuts(listOf(Key.MetaLeft, Key.MoveEnd), "Scroll to Bottom Repo")
    object OpenRepo : Shortcuts(listOf(Key.MetaLeft, Key.O), "Open Repo")
    object AddRepoToHistory : Shortcuts(listOf(Key.MetaLeft, Key.ShiftLeft, Key.AltLeft, Key.A), "Add Repo to History")

    override fun toString(): String = "$visibleText = ${keys.joinToString(separator = "+") { KeyEvent.getKeyText(it.nativeKeyCode) }}"

    private fun List<Key>.anyMeta() = any { it == Key.MetaLeft || it == Key.MetaRight }
    private fun List<Key>.anyCtrl() = any { it == Key.CtrlLeft || it == Key.CtrlRight }
    private fun List<Key>.anyAlt() = any { it == Key.AltLeft || it == Key.AltRight }
    private fun List<Key>.anyShift() = any { it == Key.ShiftLeft || it == Key.ShiftRight }
    private fun List<Key>.filterOutModifiers() = filterNot {
        it in listOf(Key.MetaLeft, Key.MetaRight, Key.CtrlLeft, Key.CtrlRight, Key.AltLeft, Key.AltRight, Key.ShiftLeft, Key.ShiftRight)
    }

    fun keyShortcut(): KeyShortcut = KeyShortcut(
        keys.filterOutModifiers().firstOrNull() ?: Key.PageUp,
        meta = keys.anyMeta(),
        shift = keys.anyShift(),
        ctrl = keys.anyCtrl(),
        alt = keys.anyAlt()
    )

    companion object {
        fun values() = arrayOf(
            Search,
            AddTopic,
            PreviousTopic,
            NextTopic,
            ScrollToTopTopic,
            ScrollToBottomTopic,
            DeleteTopic,
            PreviousRepo,
            NextRepo,
            ScrollToTopRepo,
            ScrollToBottomRepo,
            OpenRepo,
            AddRepoToHistory
        )
    }
}

inline fun <reified T> String?.fromJson(): T? = try {
    Gson().fromJson(this, object : TypeToken<T>() {}.type)
} catch (e: Exception) {
    null
}

fun Any?.toJson(): String = Gson().toJson(this)

class ShortcutViewModel {
    private val preferences = Preferences.systemNodeForPackage(ShortcutViewModel::class.java)

    init {
        preferences.addPreferenceChangeListener { pcl ->
            when (pcl.key) {
                Shortcuts.Search.name -> Shortcuts.Search.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.Search.defaultKeys
                Shortcuts.AddTopic.name -> Shortcuts.AddTopic.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.AddTopic.defaultKeys
                Shortcuts.PreviousTopic.name -> Shortcuts.PreviousTopic.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.PreviousTopic.defaultKeys
                Shortcuts.NextTopic.name -> Shortcuts.NextTopic.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.NextTopic.defaultKeys
                Shortcuts.ScrollToTopTopic.name -> Shortcuts.ScrollToTopTopic.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.ScrollToTopTopic.defaultKeys
                Shortcuts.ScrollToBottomTopic.name -> Shortcuts.ScrollToBottomTopic.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.ScrollToBottomTopic.defaultKeys
                Shortcuts.DeleteTopic.name -> Shortcuts.DeleteTopic.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.DeleteTopic.defaultKeys
                Shortcuts.PreviousRepo.name -> Shortcuts.PreviousRepo.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.PreviousRepo.defaultKeys
                Shortcuts.NextRepo.name -> Shortcuts.NextRepo.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.NextRepo.defaultKeys
                Shortcuts.ScrollToTopRepo.name -> Shortcuts.ScrollToTopRepo.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.ScrollToTopRepo.defaultKeys
                Shortcuts.ScrollToBottomRepo.name -> Shortcuts.ScrollToBottomRepo.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.ScrollToBottomRepo.defaultKeys
                Shortcuts.OpenRepo.name -> Shortcuts.OpenRepo.keys = pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.OpenRepo.defaultKeys
                Shortcuts.AddRepoToHistory.name -> Shortcuts.AddRepoToHistory.keys =
                    pcl.newValue.fromJson<List<Key>>() ?: Shortcuts.AddRepoToHistory.defaultKeys
            }
        }
    }

    fun updateNextTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.NextTopic.name, keys.toJson())
    fun updatePreviousTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.PreviousTopic.name, keys.toJson())
    fun updateSearchShortcut(keys: List<Key>) = preferences.put(Shortcuts.Search.name, keys.toJson())
    fun updateAddTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.AddTopic.name, keys.toJson())
    fun updateScrollToTopTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.ScrollToTopTopic.name, keys.toJson())
    fun updateScrollToBottomTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.ScrollToBottomTopic.name, keys.toJson())
    fun updateDeleteTopicShortcut(keys: List<Key>) = preferences.put(Shortcuts.DeleteTopic.name, keys.toJson())
    fun updatePreviousRepoShortcut(keys: List<Key>) = preferences.put(Shortcuts.PreviousRepo.name, keys.toJson())
    fun updateNextRepoShortcut(keys: List<Key>) = preferences.put(Shortcuts.NextRepo.name, keys.toJson())
    fun updateScrollToTopRepoShortcut(keys: List<Key>) = preferences.put(Shortcuts.ScrollToTopRepo.name, keys.toJson())
    fun updateScrollToBottomRepoShortcut(keys: List<Key>) = preferences.put(Shortcuts.ScrollToBottomRepo.name, keys.toJson())
    fun updateOpenRepoShortcut(keys: List<Key>) = preferences.put(Shortcuts.OpenRepo.name, keys.toJson())
    fun updateAddRepoToHistoryShortcut(keys: List<Key>) = preferences.put(Shortcuts.AddRepoToHistory.name, keys.toJson())

    fun resetAll() {
        preferences.clear()
        Shortcuts.values().forEach { it.keys = it.defaultKeys }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun KeyboardView(onCloseRequest: () -> Unit) {

    val vm = remember { ShortcutViewModel() }

    val state = rememberWindowState()

    var shortcutSelected: Shortcuts? by remember { mutableStateOf(null) }
    val keysPressed = remember { mutableStateListOf<Key>() }

    val onClick: (Key) -> Unit = {
        if (keysPressed.contains(it)) {
            keysPressed.remove(it)
        } else {
            keysPressed.add(it)
        }
    }

    val windowPosition = state.position
    val shortcutState = rememberWindowState(position = WindowPosition.Aligned(Alignment.CenterEnd))

    LaunchedEffect(windowPosition) {
        snapshotFlow {
            if (windowPosition is WindowPosition.Absolute)
                windowPosition.copy(x = windowPosition.x + state.size.width + 50.dp)
            else WindowPosition.Aligned(Alignment.CenterEnd)
        }
            .distinctUntilChanged()
            .debounce(200)
            .onEach { shortcutState.position = it }
            .launchIn(this)
    }

    Window(
        state = shortcutState,
        onCloseRequest = onCloseRequest,
        title = "Shortcuts",
        undecorated = true,
        transparent = true,
        focusable = false,
        resizable = false,
        alwaysOnTop = true,
    ) {
        MaterialTheme(colors = if (darkTheme) darkColors(primary = MaterialBlue) else lightColors(primary = MaterialBlue)) {
            Surface(shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedButton({ vm.resetAll() }) { Text("Reset All") }
                        OutlinedButton(
                            onClick = {
                                when (shortcutSelected) {
                                    Shortcuts.Search -> vm.updateSearchShortcut(keysPressed)
                                    Shortcuts.AddTopic -> vm.updateAddTopicShortcut(keysPressed)
                                    Shortcuts.PreviousTopic -> vm.updatePreviousTopicShortcut(keysPressed)
                                    Shortcuts.NextTopic -> vm.updateNextTopicShortcut(keysPressed)
                                    Shortcuts.AddRepoToHistory -> vm.updateAddRepoToHistoryShortcut(keysPressed)
                                    Shortcuts.DeleteTopic -> vm.updateDeleteTopicShortcut(keysPressed)
                                    Shortcuts.NextRepo -> vm.updateNextRepoShortcut(keysPressed)
                                    Shortcuts.OpenRepo -> vm.updateOpenRepoShortcut(keysPressed)
                                    Shortcuts.PreviousRepo -> vm.updatePreviousRepoShortcut(keysPressed)
                                    Shortcuts.ScrollToBottomRepo -> vm.updateScrollToBottomRepoShortcut(keysPressed)
                                    Shortcuts.ScrollToBottomTopic -> vm.updateScrollToBottomTopicShortcut(keysPressed)
                                    Shortcuts.ScrollToTopRepo -> vm.updateScrollToTopRepoShortcut(keysPressed)
                                    Shortcuts.ScrollToTopTopic -> vm.updateScrollToTopTopicShortcut(keysPressed)
                                    null -> {}
                                }
                            }
                        ) { Text("Save New Shortcut") }
                    }

                    val shortcutScrollState = rememberLazyListState()

                    Box(modifier = Modifier.padding(4.dp)) {
                        LazyColumn(
                            state = shortcutScrollState,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            items(Shortcuts.values()) {
                                CustomChip(
                                    it.toString(),
                                    textColor = animateColorAsState(
                                        if (shortcutSelected == it) MaterialTheme.colors.onPrimary
                                        else MaterialTheme.colors.onSurface
                                    ).value,
                                    backgroundColor = animateColorAsState(
                                        if (shortcutSelected == it) MaterialBlue
                                        else MaterialTheme.colors.surface
                                    ).value,
                                    modifier = Modifier.cursorForSelectable()
                                ) {
                                    keysPressed.clear()
                                    if (shortcutSelected == it) {
                                        shortcutSelected = null
                                    } else {
                                        shortcutSelected = it
                                        keysPressed.addAll(it.keys)
                                    }
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(shortcutScrollState)
                        )
                    }
                }
            }
        }
    }

    Window(
        state = state,
        title = "Keyboard",
        onCloseRequest = onCloseRequest,
        undecorated = true,
        transparent = true,
        onKeyEvent = {
            if (it.key !in keysPressed && it.type == if (shortcutSelected != null) KeyEventType.KeyUp else KeyEventType.KeyDown) {
                keysPressed.add(it.key)
            } else if (it.key in keysPressed && it.type == KeyEventType.KeyUp) {
                keysPressed.remove(it.key)
            }
            true
        }
    ) {
        MenuBar {
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

        MaterialTheme(colors = if (darkTheme) darkColors(primary = MaterialBlue) else lightColors(primary = MaterialBlue)) {
            Surface(shape = if (state.placement == WindowPlacement.Maximized) RectangleShape else RoundedCornerShape(8.dp)) {
                Scaffold(topBar = { WindowHeader(state, { Text("Keyboard Configurations") }, onCloseRequest) }) { p ->
                    //Full weight is about 13.5 - 14
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(p)
                    ) {
                        Keyboard(onClick, keysPressed)
                        Row(horizontalArrangement = Arrangement.Center) {
                            ArrowKeys(modifier = Modifier.weight(1f), onClick, keysPressed)
                            SpecialKeys(modifier = Modifier.weight(1f), onClick, keysPressed)
                            Numpad(modifier = Modifier.weight(1f), onClick, keysPressed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.Keyboard(onClick: (Key) -> Unit, pressedKeys: List<Key> = emptyList()) {
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //13 keys here
        KeyView(Key.Escape, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F1, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F2, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F3, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F4, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F5, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F6, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F7, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F8, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F9, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F10, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F11, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F12, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
    }
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //14 keys here
        KeyView(Key.Grave, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.One, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Two, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Three, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Four, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Five, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Six, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Seven, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Eight, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Nine, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Zero, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Minus, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Equals, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Backspace, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
    }
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //14 keys here
        KeyView(Key.Tab, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
        KeyView(Key.Q, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.W, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.E, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.R, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.T, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Y, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.U, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.I, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.O, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.P, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.LeftBracket, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.RightBracket, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Backslash, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
    }
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //13 keys here
        KeyView(Key.CapsLock, onClick, modifier = Modifier.weight(2.5f), keysPressed = pressedKeys)
        KeyView(Key.A, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.S, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.D, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.F, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.G, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.H, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.J, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.K, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.L, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Semicolon, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Apostrophe, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Enter, onClick, modifier = Modifier.weight(2.5f), keysPressed = pressedKeys)
    }
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //12 keys here
        KeyView(Key.ShiftLeft, onClick, modifier = Modifier.weight(3f), keysPressed = pressedKeys)
        KeyView(Key.Z, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.X, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.C, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.V, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.B, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.N, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.M, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Comma, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Period, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.Slash, onClick, modifier = Modifier.weight(1f), keysPressed = pressedKeys)
        KeyView(Key.ShiftRight, onClick, modifier = Modifier.weight(3f), keysPressed = pressedKeys)
    }
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        //7 keys here
        KeyView(Key.CtrlLeft, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
        KeyView(Key.AltLeft, onClick, modifier = Modifier.weight(1.5f), keysPressed = pressedKeys)
        KeyView(Key.MetaLeft, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
        KeyView(Key.Spacebar, onClick, modifier = Modifier.weight(3f), keysPressed = pressedKeys)
        KeyView(Key.MetaRight, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
        KeyView(Key.AltRight, onClick, modifier = Modifier.weight(1.5f), keysPressed = pressedKeys)
        KeyView(Key.CtrlRight, onClick, modifier = Modifier.weight(2f), keysPressed = pressedKeys)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ArrowKeys(modifier: Modifier = Modifier, onClick: (Key) -> Unit, pressedKeys: List<Key> = emptyList()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        KeyView(Key.DirectionUp, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.DirectionLeft, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.DirectionDown, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.DirectionRight, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SpecialKeys(modifier: Modifier = Modifier, onClick: (Key) -> Unit, pressedKeys: List<Key> = emptyList()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.Function, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.MoveHome, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.PageUp, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.Delete, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.MoveEnd, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.PageDown, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Numpad(modifier: Modifier = Modifier, onClick: (Key) -> Unit, pressedKeys: List<Key> = emptyList()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.NumLock, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadEquals, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadDivide, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadMultiply, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.NumPad7, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad8, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad9, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadSubtract, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.NumPad4, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad5, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad6, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadAdd, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.NumPad1, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad2, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPad3, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
            KeyView(
                Key.NumPadEnter,
                onClick,
                modifier = Modifier
                    .height(80.dp)
                    .width(40.dp),
                keysPressed = pressedKeys
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyView(Key.NumPad0, onClick, modifier = Modifier.width(80.dp), keysPressed = pressedKeys)
            KeyView(Key.NumPadDot, onClick, modifier = Modifier.width(40.dp), keysPressed = pressedKeys)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KeyView(key: Key, onClick: (Key) -> Unit, modifier: Modifier = Modifier, keysPressed: List<Key> = emptyList()) {
    Card(
        modifier = Modifier
            .height(40.dp)
            .cursorForSelectable()
            .then(modifier),
        shape = RoundedCornerShape(4.dp),
        onClick = { onClick(key) },
        backgroundColor = animateColorAsState(if (key in keysPressed) MaterialTheme.colors.primary else MaterialTheme.colors.surface).value
    ) {
        Box {
            Text(
                KeyEvent.getKeyText(key.nativeKeyCode),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}