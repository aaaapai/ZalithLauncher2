package com.movtery.zalithlauncher.game.version.mod.meta

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

data class FabricModMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val icon: String? = null,
    val authors: List<FabricModAuthor>? = null,
    val contact: Map<String, String>? = null
) {
    @JsonAdapter(FabricModAuthorSerializer::class)
    data class FabricModAuthor(val name: String? = null)

    class FabricModAuthorSerializer : JsonSerializer<FabricModAuthor>,
        JsonDeserializer<FabricModAuthor> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): FabricModAuthor? {
            return if (json.isJsonPrimitive) {
                FabricModAuthor(json.asString)
            } else {
                FabricModAuthor(json.asJsonObject.getAsJsonPrimitive("name")?.asString)
            }
        }

        override fun serialize(
            src: FabricModAuthor?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement? {
            return src?.name?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        }
    }
}