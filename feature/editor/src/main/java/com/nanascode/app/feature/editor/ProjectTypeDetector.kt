package com.nanascode.app.feature.editor

import java.io.File

enum class ProjectType(val label: String, val defaultCommand: String) {
    HTML_STATIC("HTML / Web Static", "open index.html"),
    NODE_JS("Node.js Runtime", "node"),
    PYTHON("Python 3 Runtime", "python3"),
    REACT_VITE("React / Vite (SPA)", "npm run dev"),
    UNKNOWN("Script / Text", "sh")
}

object ProjectTypeDetector {

    fun detect(workspaceDir: File, activeFile: File?): Pair<ProjectType, String> {
        val hasViteConfig = File(workspaceDir, "vite.config.js").exists() ||
                File(workspaceDir, "vite.config.ts").exists() ||
                File(workspaceDir, "vite.config.mjs").exists()

        val packageJson = File(workspaceDir, "package.json")
        val hasPackageJson = packageJson.exists()
        val hasIndexHtml = File(workspaceDir, "index.html").exists()

        val activeExt = activeFile?.extension?.lowercase() ?: ""
        val activeName = activeFile?.name ?: ""

        return when {
            hasViteConfig -> {
                ProjectType.REACT_VITE to "npm run dev"
            }
            activeExt in listOf("html", "htm") || (hasIndexHtml && activeExt !in listOf("js", "ts", "py")) -> {
                ProjectType.HTML_STATIC to (activeFile?.name ?: "index.html")
            }
            activeExt in listOf("py") || File(workspaceDir, "main.py").exists() || File(workspaceDir, "requirements.txt").exists() -> {
                val script = if (activeExt == "py") activeName else "main.py"
                ProjectType.PYTHON to "python3 $script"
            }
            hasPackageJson || activeExt in listOf("js", "ts", "mjs", "cjs") -> {
                val script = if (activeExt in listOf("js", "ts", "mjs", "cjs")) activeName else "index.js"
                ProjectType.NODE_JS to "node $script"
            }
            else -> {
                ProjectType.UNKNOWN to "sh"
            }
        }
    }
}
