package com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
class McModSearchItem(
    @SerialName("mcmod_id")
    val mcmod_id : Int,

    @SerialName("mcmod_icon")
    val mcmod_icon : String,

    @SerialName("mcmod_name")
    val mcmod_name : String,

    @SerialName("mcmod_author")
    val mcmod_author : String,

    @SerialName("mcmod_text")
    val mcmod_text : String,

    @SerialName("mcmod_type")
    val mcmod_type : Int,

    @SerialName("mcmod_mod_type")
    val mcmod_mod_type : String,

    @SerialName("mcmod_mod_subtype")
    val mcmod_mod_subtype : String,

    @SerialName("mcmod_game_version")
    val mcmod_game_version : String,

    @SerialName("mcmod_create_time")
    val mcmod_create_time : String,

    @SerialName("mcmod_update_time")
    val mcmod_update_time : String,

    @SerialName("mcmod_re_time")
    val mcmod_re_time : String,

    @SerialName("curseforge_url")
    val curseforge_url : String?,

    @SerialName("curseforge_id")
    val curseforge_id : String?,

    @SerialName("modrinth_url")
    val modrinth_url : String?,

    @SerialName("modrinth_id")
    val modrinth_id : String?
)