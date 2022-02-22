import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import java.awt.event.KeyEvent

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun KeyboardView(onCloseRequest: () -> Unit, onClick: (Key) -> Unit) {
    val state = rememberWindowState()

    val keysPressed = remember { mutableStateListOf<Key>() }

    Window(
        state = state,
        title = "Keyboard",
        onCloseRequest = onCloseRequest,
        undecorated = true,
        transparent = true,
        onKeyEvent = {
            if (it.key !in keysPressed && it.type == KeyEventType.KeyDown) {
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
                Scaffold(
                    topBar = {
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
                                title = { Text("Keyboard Configurations") },
                                elevation = animateDpAsState(if (hasFocus) AppBarDefaults.TopAppBarElevation else 0.dp).value,
                                backgroundColor = MaterialTheme.colors.primarySurface.copy(alpha = focusedAlpha),
                                actions = {

                                    IconButton(onClick = onCloseRequest) { Icon(Icons.Default.Close, null) }
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
                    },
                    bottomBar = {
                        CustomBottomAppBar {
                            Column {
                                Text("Search = Cmd+S")
                                Text("Add Topic = Enter")
                            }
                        }
                    }
                ) { p ->
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