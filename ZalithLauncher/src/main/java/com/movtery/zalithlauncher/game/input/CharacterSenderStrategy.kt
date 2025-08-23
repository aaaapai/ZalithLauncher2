package com.movtery.zalithlauncher.game.input

import android.view.KeyEvent

/**
 * Simple interface for sending chars through whatever bridge will be necessary
 * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/customcontrols/keyboard/CharacterSenderStrategy.java)
 */
interface CharacterSenderStrategy {
    /** Called when there is a character to delete, may be called multiple times in a row  */
    fun sendBackspace()

    /** Called when we want to send enter specifically  */
    fun sendEnter()

    /** Called when the left arrow key is pressed */
    fun sendLeft()

    /** Called when the right arrow key is pressed */
    fun sendRight()

    /** Called when the up arrow key is pressed */
    fun sendUp()

    /** Called when the down arrow key is pressed */
    fun sendDown()

    /** Called when there is a character to send, may be called multiple times in a row  */
    fun sendChar(character: Char)

    /**
     * Called when a non-character and non-arrow key event needs to be sent
     * @param key the KeyEvent representing the pressed key
     */
    fun sendOther(key: KeyEvent)
}