package com.nanascode.app.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTemplateTest {

    @Test
    fun `all templates contain required metadata and files`() {
        val templates = ProjectTemplate.ALL_TEMPLATES
        assertTrue("Harus menyediakan minimal 4 template bawaan", templates.size >= 4)

        for (template in templates) {
            assertTrue("Template ID tidak boleh kosong", template.id.isNotBlank())
            assertTrue("Template Name tidak boleh kosong", template.name.isNotBlank())
            assertTrue("Template Description tidak boleh kosong", template.description.isNotBlank())
            assertFalse("Template file map tidak boleh kosong", template.files.isEmpty())
        }
    }

    @Test
    fun `tailwind v4 template contains modern CSS import and HTML`() {
        val tailwind = ProjectTemplate.ALL_TEMPLATES.find { it.id == "html-tailwind" }
        assertNotNull(tailwind)

        val files = tailwind!!.files
        assertTrue(files.containsKey("index.html"))
        assertTrue(files.containsKey("style.css"))
        assertTrue(files.containsKey("package.json"))

        val cssContent = files["style.css"]!!
        assertTrue(cssContent.contains("@import \"tailwindcss\";"))
    }

    @Test
    fun `nodejs express template contains index js and package json`() {
        val express = ProjectTemplate.ALL_TEMPLATES.find { it.id == "nodejs-express" }
        assertNotNull(express)

        val files = express!!.files
        assertTrue(files.containsKey("index.js"))
        assertTrue(files.containsKey("package.json"))

        val indexJs = files["index.js"]!!
        assertTrue(indexJs.contains("express"))
        assertTrue(indexJs.contains("app.listen"))
    }
}
