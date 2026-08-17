/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.sdl

/**
 * SDL 集成桥
 *
 * 由 CallbackBridge 收到 SDL 集成通知（SDL_InitSubSystem 被拦截）时置为 true，
 * 通知 surface / 输入层把事件同时转发给 SDL（org.libsdl.app）。
 */
object SdlBridge {
    @JvmStatic
    var sdlEnabled: Boolean = false
}
