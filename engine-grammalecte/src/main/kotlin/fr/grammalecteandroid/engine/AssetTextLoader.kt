package fr.grammalecteandroid.engine

import android.content.Context

internal fun interface AssetTextLoader {
    fun readText(path: String): String
}

internal class AndroidAssetTextLoader(
    context: Context,
) : AssetTextLoader {
    private val assets = context.applicationContext.assets

    override fun readText(path: String): String =
        assets.open(AssetPathNormalizer.normalize(path)).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
