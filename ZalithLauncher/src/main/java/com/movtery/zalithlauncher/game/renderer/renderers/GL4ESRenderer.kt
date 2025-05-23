package com.movtery.zalithlauncher.game.renderer.renderers

import com.movtery.zalithlauncher.game.renderer.RendererInterface

class GL4ESRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3"

    override fun getUniqueIdentifier(): String = "8b52d82d-8f6d-4d3a-a767-dc93f8b72fc7"

    override fun getRendererName(): String = "GL4ES-115-extra"

    override fun getMaxMCVersion(): String = "1.21.4"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libgl4es_114.so"
}
