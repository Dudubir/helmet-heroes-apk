package com.helmetheroes.mobile

import android.view.KeyEvent

/** Central configuration. No secrets belong here. */
object AppConfig {
    const val GAME_URL = "https://www.helmet-heroes.com/"

    // ---- Key mapping for the on-screen controls --------------------------------
    // These are the keyboard keys the touch controls "press" inside the game.
    // If a control does nothing in the game, change its key here.
    // To use WASD for movement, replace the four DPAD lines with
    //   KEYCODE_W / KEYCODE_S / KEYCODE_A / KEYCODE_D.
    const val KEY_MOVE_UP = KeyEvent.KEYCODE_DPAD_UP
    const val KEY_MOVE_DOWN = KeyEvent.KEYCODE_DPAD_DOWN
    const val KEY_MOVE_LEFT = KeyEvent.KEYCODE_DPAD_LEFT
    const val KEY_MOVE_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT

    const val KEY_ATTACK = KeyEvent.KEYCODE_SPACE   // ASSUMPTION: confirm the real attack key
    const val KEY_SKILL_B = KeyEvent.KEYCODE_B
    const val KEY_SKILL_N = KeyEvent.KEYCODE_N
    const val KEY_SKILL_M = KeyEvent.KEYCODE_M
    const val KEY_PICKUP = KeyEvent.KEYCODE_E
    const val KEY_HEAL = KeyEvent.KEYCODE_COMMA
}
