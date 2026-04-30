package com.kathakar.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Language Onboarding Screen ────────────────────────────────────────────────
// Shown once after first registration.
// User picks which languages they want to read content in.
// Multi-select — can pick 1 or more.
@Composable
fun LanguageOnboardingScreen(
    isSaving: Boolean,
    onDone: (List<String>) -> Unit   // called with selected language codes
) {
    // Pre-select English by default
    val selected = remember { mutableStateListOf("en") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Header
        Text(
            text = "🌏",
            fontSize = 48.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "What languages do\nyou like to read in?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Stories and poems in your chosen languages\nwill appear first in your feed.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))

        // Language grid — 2 columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SUPPORTED_LANGUAGES) { lang ->
                val isSelected = lang.code in selected
                LanguagePickerCard(
                    lang = lang,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            // Don't allow deselecting all — keep at least 1
                            if (selected.size > 1) selected.remove(lang.code)
                        } else {
                            selected.add(lang.code)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Selected count indicator
        Text(
            text = "${selected.size} language${if (selected.size > 1) "s" else ""} selected",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(12.dp))

        // Continue button
        Button(
            onClick = { onDone(selected.toList()) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isSaving && selected.isNotEmpty()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = "Continue →", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Skip option
        TextButton(onClick = { onDone(emptyList()) }) {
            Text(text = "Skip for now", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LanguagePickerCard(
    lang: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = lang.nativeName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lang.code != "en") {
                    Text(
                        text = lang.englishName,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                )
            }
        }
    }
}
