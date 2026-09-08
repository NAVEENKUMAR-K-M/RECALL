package com.aigallery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassBottomBar
import com.aigallery.app.core.design.NavigationTab
import com.aigallery.app.core.permissions.PermissionManager
import com.aigallery.app.core.permissions.PermissionOnboardingScreen
import com.aigallery.app.core.theme.AIGalleryTheme
import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.repository.ScreenshotAIStats
import com.aigallery.app.domain.repository.ScreenshotRepository
import com.aigallery.app.presentation.albums.AlbumDetailScreen
import com.aigallery.app.presentation.albums.AlbumsScreen
import com.aigallery.app.presentation.albums.AlbumsViewModel
import com.aigallery.app.presentation.favorites.FavoritesScreen
import com.aigallery.app.presentation.favorites.FavoritesViewModel
import com.aigallery.app.presentation.home.HomeScreen
import com.aigallery.app.presentation.home.HomeViewModel
import com.aigallery.app.presentation.screenshot.AIDebugStatusScreen
import com.aigallery.app.presentation.screenshot.SmartCollectionsScreen
import com.aigallery.app.presentation.search.SearchScreen
import com.aigallery.app.presentation.search.SearchViewModel
import com.aigallery.app.presentation.viewer.MediaViewerScreen
import com.aigallery.app.presentation.viewer.MediaViewerViewModel

class MainActivity : ComponentActivity() {

    private var hasPermissions by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = PermissionManager.hasMediaPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        hasPermissions = PermissionManager.hasMediaPermission(this)

        val app = application as AIGalleryApp
        val homeViewModel by viewModels<HomeViewModel> {
            HomeViewModel.Factory(app.mediaRepository, app.favoritesRepository)
        }
        val albumsViewModel by viewModels<AlbumsViewModel> {
            AlbumsViewModel.Factory(app.mediaRepository)
        }
        val searchViewModel by viewModels<SearchViewModel> {
            SearchViewModel.Factory(app.mediaRepository, app.screenshotRepository)
        }
        val favoritesViewModel by viewModels<FavoritesViewModel> {
            FavoritesViewModel.Factory(app.favoritesRepository)
        }
        val viewerViewModel by viewModels<MediaViewerViewModel> {
            MediaViewerViewModel.Factory(app.mediaRepository, app.favoritesRepository, app.screenshotRepository)
        }
        val organizationViewModel by viewModels<com.aigallery.app.presentation.ai.organization.OrganizationDashboardViewModel> {
            com.aigallery.app.presentation.ai.organization.OrganizationDashboardViewModel.Factory(
                app.organizationRepository,
                app.organizationSettingsRepository,
                app.mediaRepository
            )
        }

        setContent {
            AIGalleryTheme {
                if (!hasPermissions) {
                    PermissionOnboardingScreen(
                        onContinueClick = {
                            permissionLauncher.launch(PermissionManager.getRequiredPermissions())
                        }
                    )
                } else {
                    AIGalleryAppContent(
                        homeViewModel = homeViewModel,
                        albumsViewModel = albumsViewModel,
                        searchViewModel = searchViewModel,
                        favoritesViewModel = favoritesViewModel,
                        viewerViewModel = viewerViewModel,
                        organizationViewModel = organizationViewModel,
                        screenshotRepository = app.screenshotRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AIGalleryAppContent(
    homeViewModel: HomeViewModel,
    albumsViewModel: AlbumsViewModel,
    searchViewModel: SearchViewModel,
    favoritesViewModel: FavoritesViewModel,
    viewerViewModel: MediaViewerViewModel,
    organizationViewModel: com.aigallery.app.presentation.ai.organization.OrganizationDashboardViewModel,
    screenshotRepository: ScreenshotRepository
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedCategory by remember { mutableStateOf<com.aigallery.app.domain.organization.model.ScreenshotCategory?>(null) }
    var activeViewerState by remember { mutableStateOf<Pair<List<MediaItem>, Int>?>(null) }
    var showDebugScreen by remember { mutableStateOf(false) }
    var showSmartCollections by remember { mutableStateOf(false) }

    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val favoritesList by favoritesViewModel.favorites.collectAsStateWithLifecycle()
    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val aiStats by screenshotRepository.getAIStats().collectAsStateWithLifecycle(initialValue = ScreenshotAIStats())
    val smartCollections by screenshotRepository.getAllCollections().collectAsStateWithLifecycle(initialValue = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Tab Navigation Content
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onMediaClick = { item ->
                            val index = homeUiState.allMedia.indexOfFirst { it.id == item.id }
                            activeViewerState = Pair(homeUiState.allMedia, if (index >= 0) index else 0)
                        },
                        onSearchClick = {
                            currentTab = NavigationTab.SEARCH
                        },
                        stats = aiStats,
                        onIndexingStatusClick = {
                            showDebugScreen = true
                        }
                    )
                }
                NavigationTab.ALBUMS -> {
                    if (selectedAlbum != null) {
                        AlbumDetailScreen(
                            album = selectedAlbum!!,
                            viewModel = albumsViewModel,
                            onBackClick = { selectedAlbum = null },
                            onMediaClick = { item ->
                                activeViewerState = Pair(listOf(item), 0)
                            }
                        )
                    } else {
                        AlbumsScreen(
                            viewModel = albumsViewModel,
                            onAlbumClick = { album -> selectedAlbum = album },
                            onSmartCollectionsClick = { showSmartCollections = true }
                        )
                    }
                }
                NavigationTab.AI -> {
                    if (selectedCategory != null) {
                        com.aigallery.app.presentation.ai.organization.CategoryDetailScreen(
                            category = selectedCategory!!,
                            viewModel = organizationViewModel,
                            onBackClick = { selectedCategory = null },
                            onMediaClick = { item ->
                                activeViewerState = Pair(listOf(item), 0)
                            }
                        )
                    } else {
                        com.aigallery.app.presentation.ai.organization.OrganizationDashboardScreen(
                            viewModel = organizationViewModel,
                            onCategoryClick = { cat -> selectedCategory = cat },
                            onOpenDebug = { showDebugScreen = true }
                        )
                    }
                }
                NavigationTab.SEARCH -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onMediaClick = { item ->
                            val index = searchUiState.results.indexOfFirst { it.mediaItem.id == item.id }
                            val mediaItems = searchUiState.results.map { it.mediaItem }
                            activeViewerState = Pair(mediaItems, if (index >= 0) index else 0)
                        }
                    )
                }
                NavigationTab.FAVORITES -> {
                    FavoritesScreen(
                        viewModel = favoritesViewModel,
                        onMediaClick = { item ->
                            val index = favoritesList.indexOfFirst { it.id == item.id }
                            activeViewerState = Pair(favoritesList, if (index >= 0) index else 0)
                        }
                    )
                }
            }
        }

        // Floating Liquid Glass Bottom Navigation Bar
        // Only visible when not viewing full-screen photo, debug screen, album detail, or category detail
        if (activeViewerState == null && selectedAlbum == null && selectedCategory == null && !showDebugScreen && !showSmartCollections) {
            LiquidGlassBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    selectedAlbum = null
                    selectedCategory = null
                    currentTab = tab
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Smart Collections Full-Screen Overlay
        if (showSmartCollections) {
            SmartCollectionsScreen(
                collections = smartCollections,
                onCollectionClick = { _, name ->
                    showSmartCollections = false
                    searchViewModel.onQueryChange(name)
                    currentTab = NavigationTab.SEARCH
                },
                onBack = { showSmartCollections = false }
            )
        }

        // AI Debug & Status Full-Screen Overlay
        if (showDebugScreen) {
            val orgUiState by organizationViewModel.uiState.collectAsStateWithLifecycle()
            AIDebugStatusScreen(
                stats = aiStats,
                orgStats = orgUiState.stats,
                onBack = { showDebugScreen = false },
                onReprocessAll = {
                    screenshotRepository.reprocessAll()
                }
            )
        }

        // Immersive Full-Screen Photo Viewer (Overlays entire screen)
        if (activeViewerState != null) {
            val (items, initialIndex) = activeViewerState!!
            MediaViewerScreen(
                items = items,
                initialIndex = initialIndex,
                viewModel = viewerViewModel,
                onBackClick = { activeViewerState = null }
            )
        }
    }
}
