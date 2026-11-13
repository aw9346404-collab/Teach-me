package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AudioAndSpeechManager
import com.example.core.configureAppCheck
import com.example.data.model.LearnerProfile
import com.example.data.model.ReadingThemeMode
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.LearningRepository
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.ArticlesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProgressAndPortalScreen
import com.example.ui.screens.VoiceAndAiScreen
import com.example.ui.theme.BayanEnglishTheme
import com.example.ui.viewmodel.DataState
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val DASHBOARD = "dashboard"
    const val ARTICLES = "articles"
    const val VOICE = "voice"
    const val LIBRARY = "library"
    const val PROGRESS = "progress"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            BayanEnglishTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
    } else {
        key(user.uid) {
            MainFeatureShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Scholar"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureShell(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = LearningRepository(db),
                    aiRepository = AiTutorRepository(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val audioManager = remember(context) { AudioAndSpeechManager(context) }
    DisposableEffect(audioManager) {
        onDispose { audioManager.release() }
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val dashboardModel by viewModel.studentDashboardState.collectAsStateWithLifecycle()
    val mistakes by viewModel.mistakesState.collectAsStateWithLifecycle()
    val assessments by viewModel.assessmentsState.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val articleGenState by viewModel.articleGenState.collectAsStateWithLifecycle()
    val speechEvalState by viewModel.speechEvalState.collectAsStateWithLifecycle()
    val smartSearchState by viewModel.smartSearchState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val initError by viewModel.initError.collectAsStateWithLifecycle()

    val isSpeaking by audioManager.isSpeaking.collectAsStateWithLifecycle()
    val speakingRate by audioManager.speakingRate.collectAsStateWithLifecycle()
    val isListening by audioManager.isListening.collectAsStateWithLifecycle()
    val isRecordingAudio by audioManager.isRecordingAudio.collectAsStateWithLifecycle()
    val recognizedText by audioManager.recognizedText.collectAsStateWithLifecycle()
    val speechError by audioManager.speechError.collectAsStateWithLifecycle()

    val activeProfile = (profileState as? DataState.Success)?.data ?: LearnerProfile(
        userId = currentUserId,
        displayName = displayName
    )
    val themeMode = ReadingThemeMode.fromId(activeProfile.readingTheme)
    val strings = stringsFor(activeProfile.preferredLang)
    val layoutDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: Routes.DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    fun navigateToTab(route: String) {
        val targetRoute = when (route) {
            "articles", Routes.ARTICLES -> Routes.ARTICLES
            "voice", Routes.VOICE -> Routes.VOICE
            "library", Routes.LIBRARY -> Routes.LIBRARY
            "mistakes", "progress", Routes.PROGRESS -> Routes.PROGRESS
            else -> Routes.DASHBOARD
        }
        navController.navigate(targetRoute) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun performSignOut() {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                }
            }
        }
    }

    val navItems = listOf(
        Triple(Routes.DASHBOARD, strings.navDashboard, Icons.Rounded.Home),
        Triple(Routes.ARTICLES, strings.navArticles, Icons.AutoMirrored.Rounded.MenuBook),
        Triple(Routes.VOICE, strings.navVoiceAi, Icons.Rounded.GraphicEq),
        Triple(Routes.LIBRARY, strings.navLibrary, Icons.Rounded.AutoStories),
        Triple(Routes.PROGRESS, strings.navProgress, Icons.Rounded.Analytics)
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        BayanEnglishTheme(themeMode = themeMode) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 680.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "${strings.appTitle} • ${activeProfile.cefrLevel}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                                navItems.forEach { (route, label, icon) ->
                                    NavigationBarItem(
                                        selected = currentRoute == route,
                                        onClick = { navigateToTab(route) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_$route")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail {
                                navItems.forEach { (route, label, icon) ->
                                    NavigationRailItem(
                                        selected = currentRoute == route,
                                        onClick = { navigateToTab(route) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("rail_$route")
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (profileState is DataState.Loading && initError == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                NavHost(
                                    navController = navController,
                                    startDestination = Routes.DASHBOARD
                                ) {
                                    composable(Routes.DASHBOARD) {
                                        DashboardScreen(
                                            profile = activeProfile,
                                            dashboard = dashboardModel,
                                            strings = strings,
                                            onSelectTheme = viewModel::updateTheme,
                                            onToggleLanguage = viewModel::updateLanguage,
                                            onNavigateTab = ::navigateToTab
                                        )
                                    }

                                    composable(Routes.ARTICLES) {
                                        BackHandler { navigateToTab(Routes.DASHBOARD) }
                                        ArticlesScreen(
                                            profile = activeProfile,
                                            articles = articles,
                                            articleGenState = articleGenState,
                                            isSpeaking = isSpeaking,
                                            speakingRate = speakingRate,
                                            strings = strings,
                                            onSpeakText = audioManager::speak,
                                            onStopAudio = audioManager::stopSpeaking,
                                            onChangeRate = audioManager::setSpeakingRate,
                                            onGenerateAdaptiveArticle = {
                                                viewModel.generateAdaptiveArticle(activeProfile)
                                            },
                                            onSubmitQuiz = { art, answers ->
                                                viewModel.submitArticleQuiz(activeProfile, art, answers)
                                            }
                                        )
                                    }

                                    composable(Routes.VOICE) {
                                        BackHandler { navigateToTab(Routes.DASHBOARD) }
                                        VoiceAndAiScreen(
                                            profile = activeProfile,
                                            isListening = isListening,
                                            isRecordingAudio = isRecordingAudio,
                                            recognizedText = recognizedText,
                                            speechError = speechError,
                                            speechEvalState = speechEvalState,
                                            chatMessages = chatMessages,
                                            chatError = chatError,
                                            strings = strings,
                                            onSpeakText = audioManager::speak,
                                            onStartSpeechRecognition = audioManager::startSpeechRecognition,
                                            onStopSpeechRecognition = audioManager::stopSpeechRecognition,
                                            onStartAudioRecording = audioManager::startAudioRecording,
                                            onStopRecordingAndAnalyze = { prompt, text ->
                                                val bytes = audioManager.stopAudioRecordingAndReadBytes()
                                                viewModel.evaluateSpeechOrWriting(activeProfile, prompt, text, bytes)
                                            },
                                            onAnalyzeText = { prompt, text ->
                                                viewModel.evaluateSpeechOrWriting(activeProfile, prompt, text, null)
                                            },
                                            onSendTutorMessage = { msg ->
                                                viewModel.sendTutorMessage(activeProfile, msg)
                                            }
                                        )
                                    }

                                    composable(Routes.LIBRARY) {
                                        BackHandler { navigateToTab(Routes.DASHBOARD) }
                                        LibraryScreen(
                                            profile = activeProfile,
                                            smartSearchState = smartSearchState,
                                            strings = strings,
                                            onSpeakWord = audioManager::speak,
                                            onSelectTranslationLang = viewModel::updateTranslationLanguage,
                                            onRunSmartSearch = viewModel::runSmartLibrarySearch,
                                            onClearSmartSearch = viewModel::clearSmartSearch
                                        )
                                    }

                                    composable(Routes.PROGRESS) {
                                        BackHandler { navigateToTab(Routes.DASHBOARD) }
                                        ProgressAndPortalScreen(
                                            profile = activeProfile,
                                            mistakes = mistakes,
                                            assessments = assessments,
                                            strings = strings,
                                            onSpeakText = audioManager::speak,
                                            onReviewMistake = { m, mastered ->
                                                viewModel.markMistakeReviewed(activeProfile, m, mastered)
                                            },
                                            onSelectTheme = viewModel::updateTheme,
                                            onSelectLanguage = viewModel::updateLanguage,
                                            onSaveGoalAndReminder = viewModel::updateGoalAndReminder,
                                            onUpdateGuardianPin = viewModel::updateGuardianPin,
                                            onSignOut = ::performSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
