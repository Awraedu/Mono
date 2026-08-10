package com.awras.mono.core

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ToolManager {
    private val _tools = MutableStateFlow<List<Tool>>(ToolRegistry.allTools)
    val tools: StateFlow<List<Tool>> = _tools.asStateFlow()

    fun registerTool(tool: Tool) {
        _tools.value = _tools.value + tool
    }

    fun getToolsByCategory(category: ToolCategory): List<Tool> {
        return if (category == ToolCategory.ALL) {
            _tools.value
        } else {
            _tools.value.filter { it.category == category }
        }
    }
}