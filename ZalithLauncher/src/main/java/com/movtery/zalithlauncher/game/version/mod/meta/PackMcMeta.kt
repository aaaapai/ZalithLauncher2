package com.movtery.zalithlauncher.game.version.mod.meta

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class PackMcMeta(
    @SerializedName("pack")
    val pack: PackInfo
) {
    data class PackInfo(
        @SerializedName("pack_format")
        val packFormat: Int,

        @JsonAdapter(Adapter::class)
        @SerializedName("description")
        val description: DescriptionContent
    )

    sealed class DescriptionContent {
        /**
         * 纯文本描述
         */
        data class Text(val text: String) : DescriptionContent()
        
        /**
         * 带格式的描述
         */
        data class Formatted(val parts: List<Part>) : DescriptionContent()
        
        /**
         * 描述部分
         */
        data class Part(
            val text: String,
            val color: String? = null
        )

        fun toPlainText(): String {
            return when (this) {
                is Text -> text
                is Formatted -> parts.joinToString("") { it.text }
            }
        }
    }
    
    companion object {
        class Adapter : JsonDeserializer<DescriptionContent> {
            override fun deserialize(
                json: JsonElement,
                type: java.lang.reflect.Type,
                context: JsonDeserializationContext
            ): DescriptionContent {
                return when {
                    json.isJsonPrimitive -> DescriptionContent.Text(json.asString)
                    json.isJsonObject -> {
                        val obj = json.asJsonObject
                        DescriptionContent.Text(obj.get("text")?.asString ?: "")
                    }
                    json.isJsonArray -> {
                        val parts = mutableListOf<DescriptionContent.Part>()
                        for (element in json.asJsonArray) {
                            when {
                                element.isJsonPrimitive -> parts.add(
                                    DescriptionContent.Part(element.asString)
                                )
                                element.isJsonObject -> {
                                    val partObj = element.asJsonObject
                                    parts.add(
                                        DescriptionContent.Part(
                                            text = partObj.get("text")?.asString ?: "",
                                            color = partObj.get("color")?.asString
                                        )
                                    )
                                }
                            }
                        }
                        DescriptionContent.Formatted(parts)
                    }
                    else -> DescriptionContent.Text("")
                }
            }
        }
    }
}