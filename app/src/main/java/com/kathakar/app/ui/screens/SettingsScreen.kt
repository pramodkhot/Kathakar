package com.kathakar.app.ui.screens

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.kathakar.app.R

// Language data — code + native name + English name
data class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en", "English",    "English"),
    AppLanguage("hi", "हिन्दी",     "Hindi"),
    AppLanguage("mr", "मराठी",      "Marathi"),
    AppLanguage("ta", "தமிழ்",      "Tamil"),
    AppLanguage("te", "తెలుగు",     "Telugu"),
    AppLanguage("bn", "বাংলা",      "Bengali"),
    AppLanguage("gu", "ગુજરાતી",    "Gujarati"),
    AppLanguage("kn", "ಕನ್ನಡ",      "Kannada"),
    AppLanguage("pa", "ਪੰਜਾਬੀ",     "Punjabi")
)

fun getCurrentLanguageCode(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) "en"
    else locales[0]?.language ?: "en"
}

fun applyLanguage(code: String) {
    val localeList = if (code == "en") LocaleListCompat.getEmptyLocaleList()
                     else LocaleListCompat.forLanguageTags(code)
    AppCompatDelegate.setApplicationLocales(localeList)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentLangCode by remember { mutableStateOf(getCurrentLanguageCode()) }
    var showLangDialog   by remember { mutableStateOf(false) }
    var pendingLangCode  by remember { mutableStateOf(currentLangCode) }

    val currentLang = SUPPORTED_LANGUAGES.find { it.code == currentLangCode }
        ?: SUPPORTED_LANGUAGES[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title),
                    fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Language Section ─────────────────────────────────────────────
            SettingsSectionHeader(text = stringResource(R.string.language_settings))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showLangDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Globe icon
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.language_settings),
                            fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(text = stringResource(R.string.language_settings_sub),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Current language badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = currentLang.nativeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            // Language change note
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.language_change_note),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 18.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── About Section ─────────────────────────────────────────────────
            SettingsSectionHeader(text = stringResource(R.string.about_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.app_name),
                        value = "KathaKar"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsInfoRow(
                        icon = Icons.Default.Star,
                        label = "Version",
                        value = "1.0.0"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsInfoRow(
                        icon = Icons.Default.Place,
                        label = "Languages",
                        value = "9 Indian languages"
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Language Picker Dialog ────────────────────────────────────────────────
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(text = stringResource(R.string.language_settings),
                fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isSelected = lang.code == pendingLangCode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingLangCode = lang.code },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp, 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { pendingLangCode = lang.code },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.nativeName,
                                        fontWeight = if (isSelected) FontWeight.SemiBold
                                                     else FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (lang.code != "en") {
                                        Text(
                                            text = lang.englishName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showLangDialog  = false
                    currentLangCode = pendingLangCode
                    // Set the locale first, then recreate after a short delay
                    // so AppCompatDelegate has time to apply the change
                    applyLanguage(pendingLangCode)
                    (context as? Activity)?.let { activity ->
                        activity.window.decorView.post {
                            activity.recreate()
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingLangCode = currentLangCode
                    showLangDialog  = false
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
                             label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text = label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
