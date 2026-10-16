package com.nanascode.app.feature.editor

data class EditorTab(
    val id: String,
    val name: String,
    val path: String,
    val content: String,
    val isDirty: Boolean = false,
    val language: String = "text",
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1
) {
    companion object {
        fun detectLanguage(fileName: String): String {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "js", "mjs", "cjs" -> "javascript"
                "ts", "mts" -> "typescript"
                "html", "htm" -> "html"
                "css", "scss", "sass" -> "css"
                "json" -> "json"
                "py" -> "python"
                "kt", "kts" -> "kotlin"
                "java" -> "java"
                "md", "markdown" -> "markdown"
                "sh", "bash" -> "shell"
                "xml", "svg" -> "xml"
                "yml", "yaml" -> "yaml"
                "sql" -> "sql"
                else -> "text"
            }
        }
    }
}
