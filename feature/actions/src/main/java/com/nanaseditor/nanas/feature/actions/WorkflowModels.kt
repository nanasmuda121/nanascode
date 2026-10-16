package com.nanaseditor.nanas.feature.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Workflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String
)

@Serializable
data class WorkflowRunsResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun>
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
    val status: String, // queued, in_progress, completed
    val conclusion: String? = null, // success, failure, cancelled, skipped
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class WorkflowJob(
    val id: Long,
    @SerialName("run_id") val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null
)

data class WorkflowDispatchInput(
    val key: String,
    val description: String = "",
    val required: Boolean = false,
    val defaultValue: String = "",
    val type: String = "string",
    val options: List<String> = emptyList()
)
