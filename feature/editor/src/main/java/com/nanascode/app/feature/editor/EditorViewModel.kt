package com.nanascode.app.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanascode.app.core.common.DispatcherProvider
import com.nanascode.app.core.common.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _workspacePath = MutableStateFlow<String?>(null)
    val workspacePath: StateFlow<String?> = _workspacePath.asStateFlow()

    private val _workspaceName = MutableStateFlow("Editor")
    val workspaceName: StateFlow<String> = _workspaceName.asStateFlow()

    private val _tabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths.asStateFlow()

    private val _showUnsavedCloseDialog = MutableStateFlow<EditorTab?>(null)
    val showUnsavedCloseDialog: StateFlow<EditorTab?> = _showUnsavedCloseDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun initWorkspace(rootPath: String, name: String) {
        _workspacePath.value = rootPath
        _workspaceName.value = name
        _expandedPaths.value = setOf(rootPath)

        viewModelScope.launch {
            refreshFileTree()
            // Buka file utama jika ada (misal index.html atau README.md atau file pertama)
            autoOpenInitialFile(File(rootPath))
        }
    }

    private suspend fun autoOpenInitialFile(rootDir: File) = withContext(dispatcherProvider.io) {
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext
        val priorityFiles = listOf("index.html", "src/App.tsx", "src/App.jsx", "main.py", "README.md", "package.json")
        for (rel in priorityFiles) {
            val candidate = File(rootDir, rel)
            if (candidate.exists() && candidate.isFile) {
                openFile(candidate)
                return@withContext
            }
        }
        val firstFile = rootDir.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
        if (firstFile != null) {
            openFile(firstFile)
        }
    }

    fun openFile(file: File) {
        if (!file.exists() || file.isDirectory) return

        val existing = _tabs.value.find { it.path == file.absolutePath }
        if (existing != null) {
            _activeTabId.value = existing.id
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            val content = try {
                file.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal membaca isi file: ${file.name}", e)
                "// Tidak dapat membaca file (bukan format teks UTF-8)"
            }

            val newTab = EditorTab(
                id = UUID.randomUUID().toString(),
                name = file.name,
                path = file.absolutePath,
                content = content,
                isDirty = false,
                language = EditorTab.detectLanguage(file.name)
            )

            _tabs.update { it + newTab }
            _activeTabId.value = newTab.id
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun requestCloseTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        if (tab.isDirty) {
            _showUnsavedCloseDialog.value = tab
        } else {
            forceCloseTab(tabId)
        }
    }

    fun forceCloseTab(tabId: String) {
        _tabs.update { list ->
            val updated = list.filterNot { it.id == tabId }
            if (_activeTabId.value == tabId) {
                _activeTabId.value = updated.lastOrNull()?.id
            }
            updated
        }
        _showUnsavedCloseDialog.value = null
    }

    fun dismissUnsavedDialog() {
        _showUnsavedCloseDialog.value = null
    }

    fun updateActiveTabContent(newContent: String) {
        val currentId = _activeTabId.value ?: return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    val wasDirty = tab.isDirty
                    val isNowDirty = wasDirty || (tab.content != newContent)
                    tab.copy(content = newContent, isDirty = isNowDirty)
                } else {
                    tab
                }
            }
        }
    }

    fun updateCursor(line: Int, col: Int) {
        val currentId = _activeTabId.value ?: return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    tab.copy(cursorLine = line, cursorColumn = col)
                } else {
                    tab
                }
            }
        }
    }

    fun saveActiveFile() {
        val currentId = _activeTabId.value ?: return
        val currentTab = _tabs.value.find { it.id == currentId } ?: return

        viewModelScope.launch(dispatcherProvider.io) {
            try {
                File(currentTab.path).writeText(currentTab.content, Charsets.UTF_8)
                _tabs.update { list ->
                    list.map { tab ->
                        if (tab.id == currentId) tab.copy(isDirty = false) else tab
                    }
                }
                _statusMessage.value = "Tersimpan: ${currentTab.name}"
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Gagal menyimpan file", e)
                _statusMessage.value = "Gagal menyimpan file: ${e.message}"
            }
        }
    }

    fun toggleFolder(path: String) {
        _expandedPaths.update { current ->
            if (current.contains(path)) current - path else current + path
        }
        refreshFileTree()
    }

    fun refreshFileTree() {
        val rootPath = _workspacePath.value ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            val rootFile = File(rootPath)
            if (rootFile.exists()) {
                _fileTree.value = FileNode.fromFile(rootFile, _expandedPaths.value)
            }
        }
    }

    fun createFile(parentDir: File, fileName: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val newFile = File(parentDir, fileName)
                if (newFile.createNewFile()) {
                    refreshFileTree()
                    openFile(newFile)
                    _statusMessage.value = "File dibuat: $fileName"
                } else {
                    _statusMessage.value = "File sudah ada atau gagal dibuat"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun createFolder(parentDir: File, folderName: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val newDir = File(parentDir, folderName)
                if (newDir.mkdirs()) {
                    _expandedPaths.update { it + newDir.absolutePath }
                    refreshFileTree()
                    _statusMessage.value = "Folder dibuat: $folderName"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                // Tutup tab jika sedang terbuka
                _tabs.value.find { it.path == file.absolutePath }?.let {
                    forceCloseTab(it.id)
                }
                if (file.isDirectory) file.deleteRecursively() else file.delete()
                refreshFileTree()
                _statusMessage.value = "Dihapus: ${file.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menghapus: ${e.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    companion object {
        private const val TAG = "EditorViewModel"
    }
}
