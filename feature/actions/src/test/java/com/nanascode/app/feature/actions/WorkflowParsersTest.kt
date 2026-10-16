package com.nanascode.app.feature.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowParsersTest {

    @Test
    fun `parseDispatchInputs parses complex yaml workflow_dispatch correctly`() {
        val yaml = """
            name: Deploy Application
            on:
              workflow_dispatch:
                inputs:
                  environment:
                    description: 'Deployment target environment'
                    required: true
                    default: 'staging'
                    type: choice
                    options:
                      - staging
                      - production
                  deploy_docs:
                    description: 'Deploy documentation'
                    required: false
                    default: 'true'
                    type: boolean
            jobs:
              deploy:
                runs-on: ubuntu-latest
        """.trimIndent()

        val inputs = WorkflowYamlParser.parseDispatchInputs(yaml)
        assertEquals(2, inputs.size)

        val envInput = inputs[0]
        assertEquals("environment", envInput.key)
        assertEquals("Deployment target environment", envInput.description)
        assertTrue(envInput.required)
        assertEquals("staging", envInput.defaultValue)
        assertEquals("choice", envInput.type)
        assertEquals(listOf("staging", "production"), envInput.options)

        val docsInput = inputs[1]
        assertEquals("deploy_docs", docsInput.key)
        assertEquals("Deploy documentation", docsInput.description)
        assertFalse(docsInput.required)
        assertEquals("true", docsInput.defaultValue)
        assertEquals("boolean", docsInput.type)
    }

    @Test
    fun `createDispatchPayload builds valid json string with ref and inputs`() {
        val payload = WorkflowYamlParser.createDispatchPayload(
            ref = "main",
            inputs = mapOf("environment" to "production", "debug" to "true")
        )

        assertTrue(payload.contains("\"ref\":\"main\""))
        assertTrue(payload.contains("\"environment\":\"production\""))
        assertTrue(payload.contains("\"debug\":\"true\""))
    }

    @Test
    fun `WorkflowStatusParser parses all run states accurately`() {
        assertEquals(
            WorkflowStatusParser.DisplayStatus.QUEUED,
            WorkflowStatusParser.parseStatus("queued", null)
        )
        assertEquals(
            WorkflowStatusParser.DisplayStatus.IN_PROGRESS,
            WorkflowStatusParser.parseStatus("in_progress", null)
        )
        assertEquals(
            WorkflowStatusParser.DisplayStatus.SUCCESS,
            WorkflowStatusParser.parseStatus("completed", "success")
        )
        assertEquals(
            WorkflowStatusParser.DisplayStatus.FAILURE,
            WorkflowStatusParser.parseStatus("completed", "failure")
        )
        assertEquals(
            WorkflowStatusParser.DisplayStatus.CANCELLED,
            WorkflowStatusParser.parseStatus("completed", "cancelled")
        )
    }
}
