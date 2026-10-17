package com.nanascode.app.feature.editor.plugin

import android.content.Context
import com.nanascode.app.core.common.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object PluginManager {

    private const val TAG = "PluginManager"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    private val _installedPlugins = MutableStateFlow<List<NasCodePlugin>>(emptyList())
    val installedPlugins: StateFlow<List<NasCodePlugin>> = _installedPlugins.asStateFlow()

    private var storageDir: File? = null

    // Katalog Resmi Toko Plugin (Marketplace)
    val MARKETPLACE_PLUGINS: List<NasCodePlugin> = listOf(
        NasCodePlugin(
            id = "prettier-formatter",
            name = "Prettier Code Formatter",
            version = "1.4.0",
            author = "NasCode Team",
            description = "Format kode HTML, CSS, JavaScript, dan JSON otomatis dengan indentasi rapi dan terstandarisasi.",
            icon = "format",
            category = "Formatter",
            isInstalled = true,
            isEnabled = true,
            jsCode = """
                window.NasCodePlugins.register('prettier-formatter', {
                    name: 'Prettier Formatter',
                    format: function(code, lang) {
                        if (!code) return code;
                        try {
                            if (lang === 'json') {
                                return JSON.stringify(JSON.parse(code), null, 2);
                            }
                            // Basic HTML / XML Indentation
                            if (lang === 'html' || lang === 'xml') {
                                let formatted = '';
                                let indent = 0;
                                const tokens = code.replace(/></g, '>\n<').split('\n');
                                for (let token of tokens) {
                                    token = token.trim();
                                    if (token.match(/^<\//)) indent = Math.max(0, indent - 1);
                                    formatted += '  '.repeat(indent) + token + '\n';
                                    if (token.match(/^<[^\/][^>]*[^\/]>$/) && !token.match(/^<(input|img|br|hr|meta|link)/i)) indent++;
                                }
                                return formatted.trim();
                            }
                            return code;
                        } catch (e) {
                            return code;
                        }
                    }
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "auto-close-tag",
            name = "Auto Close Tag",
            version = "1.2.0",
            author = "Jun Han",
            description = "Otomatis menambahkan tag penutup </...> saat Anda mengetik tag pembuka HTML/XML.",
            icon = "tag",
            category = "Utility",
            isInstalled = true,
            isEnabled = true,
            jsCode = """
                window.NasCodePlugins.register('auto-close-tag', {
                    name: 'Auto Close Tag',
                    onCharInserted: function(char, beforeText) {
                        if (char === '>') {
                            const match = beforeText.match(/<([a-zA-Z0-9\-]+)[^>]*$/);
                            if (match && !match[1].match(/^(input|img|br|hr|meta|link)$/i)) {
                                return '</' + match[1] + '>';
                            }
                        }
                        return null;
                    }
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "emmet-tools",
            name = "Emmet HTML/CSS Expander",
            version = "2.1.0",
            author = "FoxDebug Community",
            description = "Ekspansi singkatan HTML & CSS kilat (misal: ! untuk boilerplate HTML5, div.card>h2+p untuk hierarki tag).",
            icon = "code",
            category = "Coding Helper",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('emmet-tools', {
                    name: 'Emmet Tools',
                    expand: function(abbr) {
                        if (abbr === '!' || abbr === 'html:5') {
                            return '<!DOCTYPE html>\n<html lang="id">\n<head>\n  <meta charset="UTF-8">\n  <title>Dokumen</title>\n</head>\n<body>\n  \n</body>\n</html>';
                        }
                        if (abbr === 'a') return '<a href=""></a>';
                        if (abbr === 'link:css') return '<link rel="stylesheet" href="style.css">';
                        if (abbr === 'script:src') return '<script src="main.js"></script>';
                        return null;
                    }
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "color-picker",
            name = "Color Picker & Inline Preview",
            version = "1.0.5",
            author = "Acode Devs",
            description = "Menampilkan kotak warna mini di samping kode warna HEX (#fff) dan RGB/RGBA serta bantuan memilih palet warna.",
            icon = "palette",
            category = "Design",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('color-picker', {
                    name: 'Color Picker',
                    cssPreview: true
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "markdown-previewer",
            name = "Markdown Live Previewer",
            version = "1.3.2",
            author = "Markdown Tools",
            description = "Merender pratinjau berkas Markdown (.md) secara langsung dengan tipografi dan stylesheet GitHub.",
            icon = "preview",
            category = "Preview",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('markdown-previewer', {
                    name: 'Markdown Previewer',
                    enabled: true
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "devtools-console",
            name = "Interactive DevTools Console",
            version = "2.0.1",
            author = "Eruda Team",
            description = "Konsol JavaScript interaktif (REPL) di dalam pratinjau browser untuk mengevaluasi kode dan mengecek console.log.",
            icon = "terminal",
            category = "DevTools",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('devtools-console', {
                    name: 'DevTools Console',
                    injectEruda: true
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "bracket-matcher",
            name = "Bracket Pair Matcher",
            version = "1.1.0",
            author = "CoenraadS",
            description = "Pewarnaan pasangan tanda kurung {}, (), dan [] dengan kontras visual tinggi agar struktur blok kode jelas.",
            icon = "brackets",
            category = "Syntax",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('bracket-matcher', {
                    name: 'Bracket Matcher',
                    colorMatch: true
                });
            """.trimIndent()
        ),
        NasCodePlugin(
            id = "code-snippets",
            name = "Code Snippets Pro",
            version = "1.5.0",
            author = "NasCode Community",
            description = "Koleksi snippet cepat untuk React, Vue, Tailwind CSS, Flexbox, Express, dan JavaScript modern.",
            icon = "snippet",
            category = "Snippets",
            isInstalled = false,
            isEnabled = false,
            jsCode = """
                window.NasCodePlugins.register('code-snippets', {
                    name: 'Code Snippets Pro',
                    snippets: {
                        'clg': 'console.log();',
                        'fn': 'function name() {\n  \n}',
                        'afn': '() => {\n  \n}',
                        'useEffect': 'useEffect(() => {\n  \n}, []);'
                    }
                });
            """.trimIndent()
        )
    )

    fun resetForTesting(plugins: List<NasCodePlugin> = MARKETPLACE_PLUGINS.filter { it.isInstalled }) {
        _installedPlugins.value = plugins
    }

    fun init(context: Context) {
        val dir = File(context.filesDir, "plugins")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        storageDir = dir
        loadInstalledPlugins()
    }

    /**
     * Memuat daftar plugin yang terpasang dari penyimpanan lokal
     */
    fun loadInstalledPlugins() {
        val file = File(storageDir ?: return, "installed.json")
        if (!file.exists()) {
            // Pasang plugin default: Prettier & Auto Close Tag
            val defaults = MARKETPLACE_PLUGINS.filter { it.isInstalled }
            _installedPlugins.value = defaults
            saveInstalledPlugins(defaults)
            return
        }

        try {
            val content = file.readText()
            val list = json.decodeFromString<List<NasCodePlugin>>(content)
            _installedPlugins.value = list
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal memuat plugin terpasang", e)
            val defaults = MARKETPLACE_PLUGINS.filter { it.isInstalled }
            _installedPlugins.value = defaults
        }
    }

    private fun saveInstalledPlugins(list: List<NasCodePlugin>) {
        val dir = storageDir ?: return
        try {
            val file = File(dir, "installed.json")
            file.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Gagal menyimpan daftar plugin", e)
        }
    }

    /**
     * Mengambil seluruh plugin toko dengan status terpasang terbaru
     */
    fun getMarketplaceCatalog(): List<NasCodePlugin> {
        val installedMap = _installedPlugins.value.associateBy { it.id }
        return MARKETPLACE_PLUGINS.map { marketPlugin ->
            val installed = installedMap[marketPlugin.id]
            if (installed != null) {
                marketPlugin.copy(
                    isInstalled = true,
                    isEnabled = installed.isEnabled
                )
            } else {
                marketPlugin.copy(
                    isInstalled = false,
                    isEnabled = false
                )
            }
        }
    }

    /**
     * Pasang plugin dari toko
     */
    fun installPlugin(pluginId: String): Result<NasCodePlugin> {
        val target = MARKETPLACE_PLUGINS.find { it.id == pluginId }
            ?: return Result.failure(IllegalArgumentException("Plugin tidak ditemukan di toko"))

        val installedVersion = target.copy(isInstalled = true, isEnabled = true)
        _installedPlugins.update { current ->
            current.filterNot { it.id == pluginId } + installedVersion
        }
        saveInstalledPlugins(_installedPlugins.value)
        return Result.success(installedVersion)
    }

    /**
     * Pasang plugin kustom dari kode JavaScript langsung atau URL
     */
    fun installCustomPlugin(
        name: String,
        author: String = "Pengguna",
        description: String = "Plugin kustom JavaScript",
        jsCode: String,
        cssCode: String = ""
    ): Result<NasCodePlugin> {
        if (name.isBlank() || jsCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama dan kode JavaScript plugin tidak boleh kosong"))
        }

        val id = "custom_" + UUID.randomUUID().toString().take(8)
        val customPlugin = NasCodePlugin(
            id = id,
            name = name,
            version = "1.0.0",
            author = author,
            description = description,
            icon = "extension",
            category = "Kustom",
            isInstalled = true,
            isEnabled = true,
            jsCode = jsCode,
            cssCode = cssCode
        )

        _installedPlugins.update { it + customPlugin }
        saveInstalledPlugins(_installedPlugins.value)
        return Result.success(customPlugin)
    }

    /**
     * Hapus instalasi plugin
     */
    fun uninstallPlugin(pluginId: String): Result<Unit> {
        _installedPlugins.update { current ->
            current.filterNot { it.id == pluginId }
        }
        saveInstalledPlugins(_installedPlugins.value)
        return Result.success(Unit)
    }

    /**
     * Aktifkan / Nonaktifkan plugin
     */
    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        _installedPlugins.update { current ->
            current.map { plugin ->
                if (plugin.id == pluginId) plugin.copy(isEnabled = isEnabled) else plugin
            }
        }
        saveInstalledPlugins(_installedPlugins.value)
    }

    /**
     * Menghasilkan script gabungan seluruh plugin aktif untuk diinjeksi ke WebView editor
     */
    fun getActivePluginsJs(): String {
        val active = _installedPlugins.value.filter { it.isEnabled }
        return buildString {
            append("// NasCode Plugin Runtime Injection\n")
            for (p in active) {
                append("try {\n")
                append("// Plugin: ${p.name} (v${p.version})\n")
                append(p.jsCode)
                append("\n} catch(e) { console.error('Error loading plugin [${p.id}]:', e); }\n")
            }
        }
    }

    /**
     * Menghasilkan CSS gabungan seluruh plugin aktif
     */
    fun getActivePluginsCss(): String {
        return _installedPlugins.value
            .filter { it.isEnabled && it.cssCode.isNotBlank() }
            .joinToString("\n") { it.cssCode }
    }
}
