package com.movtery.zalithlauncher.ui.control.input

import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.unit.IntRect
import androidx.core.content.getSystemService
import com.movtery.zalithlauncher.game.input.CharacterSenderStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 一个用于处理 UI 元素文本输入的可组合修饰符
 *
 * @param mode 控制文本输入启用或禁用的 [TextInputMode]
 * @param sender 用于将字符发送到游戏的 [CharacterSenderStrategy]
 */
@Composable
fun Modifier.textInputHandler(
    mode: TextInputMode,
    sender: CharacterSenderStrategy,
    onCloseInputMethod: () -> Unit = {}
): Modifier {
    OnKeyboardClosed {
        if (mode == TextInputMode.ENABLE) {
            onCloseInputMethod()
        }
    }
    val textMode by rememberUpdatedState(mode)
    val onCloseInputMethod1 by rememberUpdatedState(onCloseInputMethod)
    return this then TextInputModifier(sender, textMode, onCloseInputMethod1)
}

private data class TextInputModifier(
    private val sender: CharacterSenderStrategy,
    private val textMode: TextInputMode,
    private val onCloseInputMethod: () -> Unit = {}
) : ModifierNodeElement<TextInputNode>() {
    override fun create() = TextInputNode(sender, textMode, onCloseInputMethod)
    override fun update(node: TextInputNode) {
        node.update(sender, textMode, onCloseInputMethod)
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "simulatorTextInputCore"
    }
}

/**
 * 使用 Android 的输入法引擎（IME）来捕获文本输入
 *
 * 该类作为 Compose UI 框架与底层 Android 文本输入系统之间的桥梁
 * 它建立文本输入会话，配置编辑器信息（例如，输入类型、IME 操作），
 * 并提供 [InputConnection] 来处理文本提交、按键事件以及其他 IME 交互
 *
 * @param sender 用于发送处理后字符的 [CharacterSenderStrategy]
 */
private class TextInputNode(
    private var sender: CharacterSenderStrategy,
    private var textInputMode: TextInputMode,
    private var onCloseInputMethod: () -> Unit
) : Modifier.Node(), PlatformTextInputModifierNode {
    private var session: Job? = null
    private val fakeCursorRect = IntRect(100, 500, 100, 550)

    override fun onAttach() {
        if (textInputMode == TextInputMode.ENABLE) {
            session = coroutineScope.launch {
                try {
                    establishTextInputSession {
                        val inputMethodManager = view.context.getSystemService<InputMethodManager>()
                            ?: error("InputMethodManager not supported")

                        val connection = InputConnectionImpl()

                        inputMethodManager.updateCursorAnchorInfo(
                            view,
                            CursorAnchorInfo.Builder().apply {
                                setSelectionRange(0, 0)
                                setInsertionMarkerLocation(
                                    fakeCursorRect.left.toFloat(),
                                    fakeCursorRect.top.toFloat(),
                                    fakeCursorRect.right.toFloat(),
                                    fakeCursorRect.bottom.toFloat(),
                                    CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
                                )
                                setMatrix(view.matrix)
                            }.build()
                        )

                        startInputMethod { info ->
                            info.inputType = InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_NORMAL
                            info.imeOptions = EditorInfo.IME_ACTION_DONE
                            connection
                        }
                    }
                } catch (_: CancellationException) {
                }
            }
        }
    }

    private fun stopInput() {
        session?.cancel()
        session = null
    }

    /**
     * 更新 [sender] 和 [textInputMode] 的值，并重新启动
     */
    fun update(
        sender: CharacterSenderStrategy,
        textInputMode: TextInputMode,
        onCloseInputMethod: () -> Unit
    ) {
        this.sender = sender
        this.onCloseInputMethod = onCloseInputMethod
        if (this.textInputMode != textInputMode) {
            this.textInputMode = textInputMode
            stopInput()
            if (textInputMode == TextInputMode.ENABLE) {
                onAttach() //重新启动
            }
        } else {
            this.textInputMode = textInputMode
        }
    }

    /**
     * 处理来自 IME 的文本输入和按键事件
     * 它将收到的字符和关键操作转换为通过提供的 [CharacterSenderStrategy] 发送的相应操作
     *
     * 该类重写 [InputConnection] 中的各种方法来处理文本提交、按键事件、撰写文本等
     * 大多数未实现的方法都返回默认值或执行无操作操作，因为它们对于此特定用例而言不是必需的
     */
    private inner class InputConnectionImpl() : InputConnection {

        override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
            val newText = text.toString()
            newText.forEach { char -> sender.sendChar(char) }
            return true
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action != KeyEvent.ACTION_DOWN) return true
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    sender.sendEnter()
                    onCloseInputMethod()
                }
                KeyEvent.KEYCODE_DEL -> sender.sendBackspace()
                KeyEvent.KEYCODE_DPAD_LEFT -> sender.sendLeft()
                KeyEvent.KEYCODE_DPAD_RIGHT -> sender.sendRight()
                KeyEvent.KEYCODE_DPAD_UP -> sender.sendUp()
                KeyEvent.KEYCODE_DPAD_DOWN -> sender.sendDown()
                else -> {
                    sender.sendOther(event)
                }
            }
            return true
        }
        override fun setComposingRegion(p0: Int, p1: Int): Boolean = true
        override fun getTextBeforeCursor(p0: Int, p1: Int): CharSequence = ""
        override fun getTextAfterCursor(p0: Int, p1: Int): CharSequence = ""
        override fun getSelectedText(p0: Int): CharSequence? = null
        override fun setComposingText(p0: CharSequence, p1: Int): Boolean {
            //中间状态文本不提交到最终内容，不发送字符
            return true
        }

        override fun setSelection(p0: Int, p1: Int): Boolean = true

        override fun finishComposingText(): Boolean = true
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            repeat(beforeLength) { sender.sendBackspace() }
            return true
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
            val times = beforeLength.coerceAtLeast(0)
            repeat(times) { sender.sendBackspace() }
            return true
        }

        override fun beginBatchEdit(): Boolean = true
        override fun endBatchEdit(): Boolean = true
        override fun clearMetaKeyStates(p0: Int): Boolean = true
        override fun closeConnection() {}
        override fun commitCompletion(p0: CompletionInfo?): Boolean = false
        override fun commitContent(p0: InputContentInfo, p1: Int, p2: Bundle?): Boolean = false
        override fun commitCorrection(p0: CorrectionInfo?): Boolean = false

        override fun performEditorAction(editorAction: Int): Boolean {
            //用户点击了编辑器的操作按钮（可以视为用户按下回车）
            sender.sendEnter()
            onCloseInputMethod()
            return true
        }

        override fun performContextMenuAction(p0: Int): Boolean = false
        override fun performPrivateCommand(p0: String?, p1: Bundle?): Boolean = false
        override fun reportFullscreenMode(p0: Boolean): Boolean = true
        override fun requestCursorUpdates(p0: Int): Boolean = false
        override fun getCursorCapsMode(p0: Int): Int = 0
        override fun getExtractedText(p0: ExtractedTextRequest?, p1: Int): ExtractedText? = null
        override fun getHandler() = null
    }
}