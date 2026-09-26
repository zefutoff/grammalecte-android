package fr.grammalecteandroid.engine

internal object AssetPathNormalizer {
    private const val RESOURCE_PREFIX = "resource://grammalecte/"

    fun normalize(path: String): String {
        val clean = path
            .removePrefix(RESOURCE_PREFIX)
            .removePrefix("./")
            .removePrefix("/")

        require(".." !in clean.split('/')) { "Parent path traversal is not allowed: $path" }

        return if (path.startsWith(RESOURCE_PREFIX)) {
            "grammalecte/$clean"
        } else {
            clean
        }
    }
}
