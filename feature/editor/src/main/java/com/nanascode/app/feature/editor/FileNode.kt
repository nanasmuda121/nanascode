package com.nanascode.app.feature.editor

import java.io.File

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val icon: String
        get() = if (isDirectory) {
            if (isExpanded) "[-]" else "[+]"
        } else {
            when (extension) {
                "html", "htm" -> "HTML"
                "css", "scss" -> "CSS"
                "js", "mjs", "cjs" -> "JS"
                "ts", "tsx" -> "TS"
                "json" -> "JSON"
                "py" -> "PY"
                "kt", "kts" -> "KT"
                "md" -> "MD"
                "png", "jpg", "jpeg", "svg", "webp" -> "IMG"
                else -> "FILE"
            }
        }

    companion object {
        fun fromFile(file: File, expandedPaths: Set<String> = emptySet()): FileNode {
            val isDir = file.isDirectory
            val isExpanded = expandedPaths.contains(file.absolutePath)
            val children = if (isDir && isExpanded) {
                file.listFiles()
                    ?.filter { !it.name.startsWith(".") || it.name == ".gitignore" }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?.map { fromFile(it, expandedPaths) }
                    ?: emptyList()
            } else {
                emptyList()
            }

            return FileNode(
                name = file.name,
                path = file.absolutePath,
                isDirectory = isDir,
                children = children,
                isExpanded = isExpanded
            )
        }
    }
}
