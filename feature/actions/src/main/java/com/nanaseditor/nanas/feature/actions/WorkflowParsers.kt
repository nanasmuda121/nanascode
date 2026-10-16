package com.nanaseditor.nanas.feature.actions

import org.json.JSONObject

object WorkflowYamlParser {

    /**
     * Mem-parsing definisi inputs dari blok workflow_dispatch pada YAML GitHub Actions.
     */
    fun parseDispatchInputs(yamlContent: String): List<WorkflowDispatchInput> {
        val inputs = mutableListOf<WorkflowDispatchInput>()
        val lines = yamlContent.lines()

        var insideWorkflowDispatch = false
        var insideInputs = false
        var currentInputKey: String? = null
        var currentDesc = ""
        var currentRequired = false
        var currentDefault = ""
        var currentType = "string"
        val currentOptions = mutableListOf<String>()

        fun commitCurrentInput() {
            if (currentInputKey != null) {
                inputs.add(
                    WorkflowDispatchInput(
                        key = currentInputKey!!,
                        description = currentDesc,
                        required = currentRequired,
                        defaultValue = currentDefault,
                        type = currentType,
                        options = currentOptions.toList()
                    )
                )
                currentInputKey = null
                currentDesc = ""
                currentRequired = false
                currentDefault = ""
                currentType = "string"
                currentOptions.clear()
            }
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("#") || line.isEmpty()) continue

            if (line.startsWith("workflow_dispatch:")) {
                insideWorkflowDispatch = true
                continue
            }

            if (insideWorkflowDispatch && line.startsWith("inputs:")) {
                insideInputs = true
                continue
            }

            if (insideWorkflowDispatch && !insideInputs && line.endsWith(":") && !line.startsWith("inputs")) {
                // Keluar dari workflow_dispatch jika menemukan blok event lain
                insideWorkflowDispatch = false
                continue
            }

            if (insideInputs) {
                // Cek indentasi baris asli untuk melihat apakah keluar dari blok inputs
                val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                if (leadingSpaces <= 2 && !line.startsWith("inputs") && !line.startsWith("workflow_dispatch")) {
                    commitCurrentInput()
                    insideInputs = false
                    insideWorkflowDispatch = false
                    continue
                }

                // Cek input key (contoh: "environment:" atau "logLevel:")
                if (line.endsWith(":") && !line.startsWith("-") && leadingSpaces in 4..6) {
                    commitCurrentInput()
                    currentInputKey = line.removeSuffix(":").trim()
                } else if (currentInputKey != null) {
                    when {
                        line.startsWith("description:") -> {
                            currentDesc = line.removePrefix("description:").trim().trim('\'', '"')
                        }
                        line.startsWith("required:") -> {
                            currentRequired = line.removePrefix("required:").trim().toBoolean()
                        }
                        line.startsWith("default:") -> {
                            currentDefault = line.removePrefix("default:").trim().trim('\'', '"')
                        }
                        line.startsWith("type:") -> {
                            currentType = line.removePrefix("type:").trim().trim('\'', '"')
                        }
                        line.startsWith("-") -> {
                            val option = line.removePrefix("-").trim().trim('\'', '"')
                            currentOptions.add(option)
                        }
                    }
                }
            }
        }
        commitCurrentInput()

        return inputs
    }

    /**
     * Membuat JSON payload untuk POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches
     */
    fun createDispatchPayload(ref: String, inputs: Map<String, String>): String {
        val root = JSONObject()
        root.put("ref", ref)
        if (inputs.isNotEmpty()) {
            val inputsObj = JSONObject()
            inputs.forEach { (k, v) -> inputsObj.put(k, v) }
            root.put("inputs", inputsObj)
        }
        return root.toString()
    }
}

object WorkflowStatusParser {
    enum class DisplayStatus(val label: String, val isFinal: Boolean) {
        QUEUED("Antrean (Queued)", false),
        IN_PROGRESS("Sedang Berjalan", false),
        SUCCESS("Berhasil (Success)", true),
        FAILURE("Gagal (Failure)", true),
        CANCELLED("Dibatalkan", true),
        UNKNOWN("Status Tidak Diketahui", true)
    }

    fun parseStatus(status: String, conclusion: String?): DisplayStatus {
        return when (status.lowercase()) {
            "queued", "waiting", "pending" -> DisplayStatus.QUEUED
            "in_progress" -> DisplayStatus.IN_PROGRESS
            "completed" -> when (conclusion?.lowercase()) {
                "success" -> DisplayStatus.SUCCESS
                "failure", "timed_out" -> DisplayStatus.FAILURE
                "cancelled" -> DisplayStatus.CANCELLED
                else -> DisplayStatus.UNKNOWN
            }
            else -> DisplayStatus.UNKNOWN
        }
    }
}
