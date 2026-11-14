package com.example.okkhor.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    repository: KeyboardPreferencesRepository
) {
    val settings by repository.settings.collectAsState()
    val activeTheme = repository.getActiveTheme()
    val shortcuts by repository.shortcuts.collectAsState()
    val clipboardHistory by repository.clipboardHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(stringResource(R.string.nav_home)) },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Themes") },
                    label = { Text(stringResource(R.string.nav_themes)) },
                    modifier = Modifier.testTag("nav_tab_themes")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = "Input") },
                    label = { Text(stringResource(R.string.nav_input)) },
                    modifier = Modifier.testTag("nav_tab_input")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Tools") },
                    label = { Text(stringResource(R.string.nav_typing)) },
                    modifier = Modifier.testTag("nav_tab_tools")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Privacy & About") },
                    label = { Text(stringResource(R.string.nav_more)) },
                    modifier = Modifier.testTag("nav_tab_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> {
                    OnboardingSection(
                        settings = settings,
                        activeTheme = activeTheme,
                        onNavigateToTab = { tabIndex -> selectedTab = tabIndex }
                    )
                }
                1 -> {
                    ThemeStudioSection(
                        settings = settings,
                        activeTheme = activeTheme,
                        onSelectThemeId = { id ->
                            repository.updateSettings { it.copy(selectedThemeId = id) }
                        },
                        onUpdateTheme = { customTheme ->
                            repository.updateSettings {
                                it.copy(
                                    selectedThemeId = "custom",
                                    customThemeJson = customTheme.toJson()
                                )
                            }
                        },
                        onUpdateHeightPercent = { height ->
                            repository.updateSettings { it.copy(keyboardHeightPercent = height) }
                        }
                    )
                }
                2 -> {
                    InputSettingsSection(
                        settings = settings,
                        onUpdateSettings = { transform ->
                            repository.updateSettings(transform)
                        }
                    )
                }
                3 -> {
                    ShortcutsClipboardSection(
                        settings = settings,
                        shortcuts = shortcuts,
                        clipboardItems = clipboardHistory,
                        onToggleClipboardEnabled = { enabled ->
                            repository.updateSettings { it.copy(clipboardHistoryEnabled = enabled) }
                        },
                        onUpdateRetentionHours = { hours ->
                            repository.updateSettings { it.copy(clipboardRetentionHours = hours) }
                        },
                        onAddShortcut = { trigger, expansion ->
                            repository.addShortcut(trigger, expansion)
                        },
                        onRemoveShortcut = { trigger ->
                            repository.removeShortcut(trigger)
                        },
                        onTogglePinClipboard = { id ->
                            repository.togglePinClipboardItem(id)
                        },
                        onRemoveClipboardItem = { id ->
                            repository.removeClipboardItem(id)
                        },
                        onClearClipboard = {
                            repository.clearClipboard()
                        }
                    )
                }
                4 -> {
                    PrivacyAboutSection(
                        onResetAllSettings = {
                            repository.resetAllSettings()
                        }
                    )
                }
            }
        }
    }
}
