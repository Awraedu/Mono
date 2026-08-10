package com.awras.mono.features.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.core.ToolManager
import com.awras.mono.core.model.ToolCategory
import com.awras.mono.ui.components.AppBarButton
import com.awras.mono.ui.components.ToolCard
import com.awras.mono.ui.theme.LocalTactileFeedback
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Search
import compose.icons.tablericons.X

@Composable
fun ToolLibraryScreen(
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ToolCategory.ALL) }
    val focusManager = LocalFocusManager.current
    val haptics = LocalTactileFeedback.current

    val tools by ToolManager.tools.collectAsState()
    
    val filteredTools = remember(searchQuery, selectedCategory, tools) {
        tools.filter { tool ->
            val matchesSearch = tool.name.contains(searchQuery, ignoreCase = true) ||
                    tool.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == ToolCategory.ALL || tool.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppBarButton(
                        icon = TablerIcons.ArrowLeft,
                        onClick = {
                            if (isSearchActive) {
                                focusManager.clearFocus()
                                searchQuery = ""
                            } else {
                                onBackClick()
                            }
                        }
                    )

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .onFocusChanged { isSearchActive = it.isFocused },
                        placeholder = {
                            Text(
                                "Search tools...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = TablerIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { 
                                    haptics.tick()
                                    searchQuery = "" 
                                }) {
                                    Icon(
                                        imageVector = TablerIcons.X,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    AnimatedVisibility(
                        visible = !isSearchActive,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        AppBarButton(
                            icon = TablerIcons.Bell,
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Count and Category Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${filteredTools.size} tools available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Edge-to-edge category chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ToolCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            haptics.tick()
                            selectedCategory = category 
                        },
                        label = { 
                            Text(
                                category.displayName,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface,
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tool Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTools) { tool ->
                    ToolCard(
                        category = tool.category.displayName,
                        title = tool.name,
                        icon = tool.icon,
                        onClick = { /* TODO: Open Tool */ }
                    )
                }
            }
        }
    }
}
