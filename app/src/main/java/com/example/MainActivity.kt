package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.toBengali
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.AppLanguage
import com.example.ui.LocalAppStrings
import com.example.ui.theme.*
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.permissions.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*


class MainActivity : ComponentActivity() {
    var interceptedPlatformName by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("action_intercept_social", false)) {
            interceptedPlatformName = intent.getStringExtra("intercepted_platform_name")
        } else if (intent != null && intent.action == Intent.ACTION_MAIN) {
            // If the user manually opens the app, clear any previous interception state
            interceptedPlatformName = null
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val activePlatform = interceptedPlatformName

                // Handle starting/stopping service reactively based on preferences
                val sharedPrefs = remember(context) { context.getSharedPreferences("profile_prefs", Activity.MODE_PRIVATE) }
                
                val settingsViewModel: com.example.viewmodel.SettingsViewModel = viewModel(
                    factory = remember(context) {
                        object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return com.example.viewmodel.SettingsViewModel(context) as T
                            }
                        }
                    }
                )
                val appLanguage by settingsViewModel.language.collectAsState()
                val strings = com.example.ui.getString(appLanguage)
                
                val onboardingPrefs = remember(context) { context.getSharedPreferences("onboarding_prefs", Activity.MODE_PRIVATE) }
                var isOnboardingNeeded by remember { mutableStateOf(!onboardingPrefs.getBoolean("onboarding_completed", false)) }
                var showSplash by remember { mutableStateOf(true) }

                // Reactive state for social blocked status
                val isSocialBlockedState = remember { mutableStateOf(sharedPrefs.getBoolean("social_blocked", false)) }
                
                // Listener to update state when preferences change
                DisposableEffect(sharedPrefs) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                        if (key == "social_blocked") {
                            isSocialBlockedState.value = prefs.getBoolean("social_blocked", false)
                        }
                    }
                    sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                // Notification Action Permissions for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                    LaunchedEffect(Unit) {
                        if (notificationPermissionState.status is PermissionStatus.Denied) {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    }
                }
                
                // Exact Alarm check for Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        LaunchedEffect(Unit) {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                }

                CompositionLocalProvider(com.example.ui.LocalAppStrings provides strings) {
                    LaunchedEffect(isSocialBlockedState.value, context) {
                        if (isSocialBlockedState.value && SocialBlockerService.isPermissionGranted(context)) {
                            SocialBlockerService.startService(context)
                        } else {
                            SocialBlockerService.stopService(context)
                        }
                    }

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
                        label = "splash_fade_transition"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            if (isOnboardingNeeded) {
                                OnboardingScreen(
                                    prayerViewModel = viewModel() , // We'll get it from ViewModelProvider if needed or just use default here
                                    settingsViewModel = settingsViewModel,
                                    onComplete = {
                                        onboardingPrefs.edit().putBoolean("onboarding_completed", true).apply()
                                        isOnboardingNeeded = false
                                    }
                                )
                            } else if (activePlatform != null) {
                        SocialBlockerOverlay(
                            platformName = activePlatform,
                            onDismissToHome = {
                                interceptedPlatformName = null
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            }
                        )
                    } else {
                        // Real-time Circle Alert Regional Notification Listener
                        LaunchedEffect(context) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val sharedPrefs = context.getSharedPreferences("profile_prefs", Activity.MODE_PRIVATE)
                            val userDistrict = sharedPrefs.getString("location", "All Bangladesh") ?: "All Bangladesh"
                            val trackerDb = com.example.database.TrackerDatabase.getDatabase(context)
                            val notificationDao = trackerDb.notificationDao()
                            
                            db.collection("videos")
                                .whereEqualTo("isCircleAlert", true)
                                .whereEqualTo("status", "APPROVED")
                                .addSnapshotListener { snapshots, e ->
                                    if (e != null) return@addSnapshotListener
                                    if (snapshots != null) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            for (doc in snapshots.documentChanges) {
                                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                                    val data = doc.document.data
                                                    val alertLocation = data["location"] as? String ?: "All Bangladesh"
                                                    val title = data["title"] as? String ?: "Alert"
                                                    val category = data["alertCategory"] as? String ?: ""
                                                    val docId = doc.document.id
                                                    
                                                    // Only notify if location matches or it's nationwide, and not already notified
                                                    if ((alertLocation == "All Bangladesh" || alertLocation == userDistrict) && 
                                                        notificationDao.countByRemoteId("alert_$docId") == 0) {
                                                        
                                                        val isEnglish = GlobalLanguage.isEnglish
                                                        val notifTitle = if (isEnglish) "🔴 Real-time Alert: $title" else "🔴 রিয়েল-টাইম অ্যালার্ট: $title"
                                                        val notifBody = if (isEnglish) "Area: $alertLocation | Type: $category" else "এলাকা: $alertLocation | ধরন: $category"
                                                        
                                                        val entity = com.example.database.NotificationEntity(
                                                            title = notifTitle,
                                                            body = notifBody,
                                                            timestamp = System.currentTimeMillis(),
                                                            type = "GENERAL",
                                                            actorName = data["author"] as? String ?: "Halal Circle",
                                                            remoteId = "alert_$docId"
                                                        )
                                                        notificationDao.insertNotification(entity)
                                                        
                                                        // Show push notification
                                                        val ctx = context
                                                        val notifManager = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                                        val builder = androidx.core.app.NotificationCompat.Builder(ctx, "halal_circle_notifs")
                                                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                            .setContentTitle(notifTitle)
                                                            .setContentText(notifBody)
                                                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                                            .setAutoCancel(true)
                                                        notifManager.notify("alert_$docId".hashCode(), builder.build())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        val viewModel: PrayerViewModel = viewModel()
                        val state by viewModel.state.collectAsState()
                        val currentCountryCode by settingsViewModel.selectedCountryCode.collectAsState()
                        
                        // Load saved settings (like Madhab) and update when country changes
                        LaunchedEffect(currentCountryCode) {
                            viewModel.loadSettings(context)
                        }

                        // Notification Sync Logic
                        LaunchedEffect(context) {
                            val supabase = Supabase.client
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val trackerDb = com.example.database.TrackerDatabase.getDatabase(context)
                            val notificationDao = trackerDb.notificationDao()

                            supabase.auth.sessionStatus.collect { status ->
                                if (status is SessionStatus.Authenticated) {
                                    val user = status.session.user
                                    if (user != null) {
                                        db.collection("remote_notifications")
                                            .whereEqualTo("toId", user.id)
                                            .addSnapshotListener { snapshots, e ->
                                            if (e != null) return@addSnapshotListener
                                            if (snapshots != null) {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    for (doc in snapshots.documentChanges) {
                                                        if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                                            val remoteId = doc.document.id
                                                            if (notificationDao.countByRemoteId(remoteId) == 0) {
                                                                val data = doc.document.data
                                                                val type = data["type"] as? String ?: "GENERAL"
                                                                val isEnglish = GlobalLanguage.isEnglish
                                                                
                                                                var title = data["title"] as? String ?: "Notification"
                                                                var body = data["body"] as? String ?: ""
                                                                
                                                                // Localize specific video status update notifications from Admin
                                                                when (type) {
                                                                    "APPROVE", "APPROVED" -> {
                                                                        title = if (isEnglish) "Video Approved" else "ভিডিও অ্যাপ্রুভ হয়েছে"
                                                                        body = if (isEnglish) "Your video has been approved." else "আপনার ভিডিওটি এডমিন কর্তৃক অ্যাপ্রুভ করা হয়েছে।"
                                                                    }
                                                                    "REJECT", "REJECTED" -> {
                                                                        title = if (isEnglish) "Video Rejected" else "ভিডিও রিজেক্ট হয়েছে"
                                                                        body = if (isEnglish) "Your video submission was not approved." else "আপনার ভিডিওটি এডমিন কর্তৃক রিজেক্ট করা হয়েছে।"
                                                                    }
                                                                    "DELETE", "DELETED" -> {
                                                                        title = if (isEnglish) "Video Deleted" else "ভিডিও মুছে ফেলা হয়েছে"
                                                                        body = if (isEnglish) "A video was deleted by the admin." else "আপনার ভিডিওটি এডমিন কর্তৃক ডিলেট করা হয়েছে।"
                                                                    }
                                                                }

                                                                val entity = com.example.database.NotificationEntity(
                                                                    title = title,
                                                                    body = body,
                                                                    timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
                                                                    type = type,
                                                                    actorName = data["actorName"] as? String ?: "System",
                                                                    remoteId = remoteId
                                                                )
                                                                notificationDao.insertNotification(entity)
                                                                
                                                                // Show local push notification
                                                                val ctx = context
                                                                val notifManager = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                                    val channel = android.app.NotificationChannel("halal_circle_notifs", "General Notifications", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                                                                    notifManager.createNotificationChannel(channel)
                                                                }
                                                                val notifyIntent = Intent(ctx, MainActivity::class.java).apply {
                                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                    putExtra("open_notifications", true)
                                                                }
                                                                val notifyPendingIntent = android.app.PendingIntent.getActivity(
                                                                    ctx, 0, notifyIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                                )
                                                                val builder = androidx.core.app.NotificationCompat.Builder(ctx, "halal_circle_notifs")
                                                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                                    .setContentTitle(entity.title)
                                                                    .setContentText(entity.body)
                                                                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                                                                    .setAutoCancel(true)
                                                                    .setContentIntent(notifyPendingIntent)
                                                                notifManager.notify(remoteId.hashCode(), builder.build())
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

                    val permissions = remember {
                            val list = mutableListOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                list.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            // Access to set exact alarms is usually handled separately via Intent if not granted
                            list
                        }

                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                }
                            }
                        }

                        val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
                        var selectedTab by remember { mutableStateOf("home") }
                        
                        var isAlarmPageOpen by remember { mutableStateOf(false) }
                        var isZakatPageOpen by remember { mutableStateOf(false) }
                        var isCalendarPageOpen by remember { mutableStateOf(false) }
                        var isQiblaPageOpen by remember { mutableStateOf(false) }
                        var isNotificationsPageOpen by remember { mutableStateOf(false) }
                        var isAddAlarmPageOpen by remember { mutableStateOf(false) }
                        var isParentalPageOpen by remember { mutableStateOf(false) }
                        var isPrayerPageOpen by remember { mutableStateOf(false) }
                        var isCreateCircleAlertOpen by remember { mutableStateOf(false) }
                        var isCreatePostOpen by remember { mutableStateOf(false) }
                        var isFoundationPageOpen by remember { mutableStateOf(false) }
                        var isProfileSubScreenOpen by remember { mutableStateOf(false) }
                        
                        val alarmViewModel: com.example.viewmodel.AlarmViewModel = remember { 
                            com.example.viewmodel.AlarmViewModel(context) 
                        }
                        val userAlarms by alarmViewModel.alarms.collectAsState()

                        val activity = LocalContext.current as? androidx.activity.ComponentActivity
                        LaunchedEffect(activity?.intent) {
                            if (activity?.intent?.getBooleanExtra("open_notifications", false) == true) {
                                isNotificationsPageOpen = true
                                activity?.intent?.removeExtra("open_notifications")
                            }
                        }
                        DisposableEffect(activity) {
                            val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                                if (newIntent.getBooleanExtra("open_notifications", false)) {
                                    isNotificationsPageOpen = true
                                    newIntent.removeExtra("open_notifications")
                                }
                            }
                            activity?.addOnNewIntentListener(listener)
                            onDispose {
                                activity?.removeOnNewIntentListener(listener)
                            }
                        }

                        val view = LocalView.current
                        val isProfileOverlayOpen = isAlarmPageOpen || isZakatPageOpen || isCalendarPageOpen || isQiblaPageOpen || isNotificationsPageOpen || isAddAlarmPageOpen || isParentalPageOpen || isPrayerPageOpen || isCreateCircleAlertOpen || isCreatePostOpen || isFoundationPageOpen
                        val showBottomBar = !isProfileOverlayOpen && !isProfileSubScreenOpen
                        val isDarkStatusBar = false
                        val isAuthPage = false
                        
                        LaunchedEffect(isDarkStatusBar, isProfileOverlayOpen, isAuthPage, view) {
                            val window = (view.context as Activity).window
                            window.statusBarColor = android.graphics.Color.TRANSPARENT
                            window.navigationBarColor = android.graphics.Color.WHITE
                            // If profile/overlay is open, or on Auth page, we want dark icons on white/transparent status bar
                            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isProfileOverlayOpen || isAuthPage || !isDarkStatusBar
                            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
                        }

                        LaunchedEffect(multiplePermissionsState.allPermissionsGranted, context) {
                            if (multiplePermissionsState.allPermissionsGranted) {
                                viewModel.startLocationUpdates(context)
                            } else {
                                viewModel.setPermissionDenied()
                            }
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = if (isProfileOverlayOpen) Color.White else (if (isDarkStatusBar) Color.Black else BgLight),
                            bottomBar = { 
                                if (showBottomBar) {
                                    AppBottomNavigation(selectedTab, isDark = isDarkStatusBar) { 
                                        selectedTab = it 
                                        if (isProfileOverlayOpen) {
                                            isAlarmPageOpen = false
                                            isZakatPageOpen = false
                                            isCalendarPageOpen = false
                                            isQiblaPageOpen = false
                                            isNotificationsPageOpen = false
                                            isAddAlarmPageOpen = false
                                            isParentalPageOpen = false
                                            isPrayerPageOpen = false
                                            isCreateCircleAlertOpen = false
                                            isCreatePostOpen = false
                                        }
                                    } 
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isProfileOverlayOpen) {
                                            Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                                        } else {
                                            Modifier.padding(innerPadding)
                                        }
                                    )
                            ) {
                                LaunchedEffect(Unit) {
                                    if (!multiplePermissionsState.allPermissionsGranted && state.isAutoLocation) {
                                        multiplePermissionsState.launchMultiplePermissionRequest()
                                    }
                                }

                                if (selectedTab == "home") {
                                        HomeScreen(
                                            state = state,
                                            onToggleAlarm = { viewModel.toggleAlarm(context, it) },
                                            onNavigateToPrayerDetails = { isPrayerPageOpen = true },
                                            onNavigateToTracker = { selectedTab = "tracker" },
                                            onNavigateToTasbih = { selectedTab = "tasbih" },
                                            onNavigateToQuran = { selectedTab = "quran" },
                                            onNavigateToLocation = { selectedTab = "location" },
                                            onOpenAlarmPage = { isAlarmPageOpen = true },
                                            onNavigateToZakat = { isZakatPageOpen = true },
                                            onNavigateToCalendar = { isCalendarPageOpen = true },
                                            onNavigateToQibla = { isQiblaPageOpen = true },
                                            onNavigateToTools = { selectedTab = "tools" },
                                            onNavigateToAllahNames = { selectedTab = "allah_names" },
                                            onNavigateToRamadan = { selectedTab = "ramadan" },
                                            onNavigateToDua = { selectedTab = "dua" },
                                            onNavigateToHadith = { selectedTab = "hadith" },
                                            onOpenNotificationsPage = { isNotificationsPageOpen = true },
                                            onNavigateToCreatePost = { isCreatePostOpen = true },
                                            onOpenFoundationPage = { isFoundationPageOpen = true }
                                        )
                                    } else if (selectedTab == "location") {
                                        LocationSelectionScreen(
                                            viewModel = viewModel,
                                            onBack = { selectedTab = "home" }
                                        )
                                    } else if (selectedTab == "quran") {
                                        QuranScreen(onBack = { selectedTab = "home" })
                                    } else if (selectedTab == "dua") {
                                        DuaScreen(onBack = { selectedTab = "home" })
                                    } else if (selectedTab == "hadith") {
                                        HadithScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "tracker") {
                                        TrackerScreen()
                                    } else if (selectedTab == "tools") {
                                        ToolsScreen(
                                            onNavigateToTracker = { selectedTab = "tracker" },
                                            onNavigateToTasbih = { selectedTab = "tasbih" },
                                            onNavigateToQuran = { selectedTab = "quran" },
                                            onNavigateToZakat = { isZakatPageOpen = true },
                                            onNavigateToCalendar = { isCalendarPageOpen = true },
                                            onNavigateToQibla = { isQiblaPageOpen = true },
                                            onNavigateToAllahNames = { selectedTab = "allah_names" },
                                            onNavigateToRamadan = { selectedTab = "ramadan" },
                                            onNavigateToDuroodReminder = { selectedTab = "durood_reminder" },
                                            onNavigateToDua = { selectedTab = "dua" },
                                            onNavigateToHadith = { selectedTab = "hadith" },
                                            onNavigateToWidgets = { selectedTab = "widgets" },
                                            onNavigateToIslamicNames = { selectedTab = "islamic_names" },
                                            onNavigateToSocialVideos = { selectedTab = "social_videos" }
                                        )
                                    } else if (selectedTab == "social_videos") {
                                        com.example.social.SocialVideosScreen(
                                            onBack = { selectedTab = "tools" }
                                        )
                                    } else if (selectedTab == "tasbih") {
                                        TasbihScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "widgets") {
                                        HomeScreenWidgetsScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "durood_reminder") {
                                        DuroodReminderScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "islamic_names") {
                                        IslamicNamesScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "ramadan") {
                                        RamadanScreen(state = state, onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "allah_names") {
                                        NamesOfAllahScreen(onBack = { selectedTab = "tools" })
                                    } else if (selectedTab == "profile") {
                                        ProfileScreen(
                                            onNavigateToTracker = { selectedTab = "tracker" },
                                            onNavigateToSettings = { selectedTab = "settings" },
                                            onNavigateToParentalControl = { isParentalPageOpen = true },
                                            onNavigateToSavedDuas = { selectedTab = "saved_duas" },
                                            onNavigateToSavedAyahs = { selectedTab = "saved_ayahs" },
                                            onNavigateToSavedHadiths = { selectedTab = "saved_hadiths" },
                                            onToggleBottomBar = { isProfileSubScreenOpen = !it }
                                        )
                                    } else if (selectedTab == "saved_duas") {
                                        SavedDuasScreen(onBack = { selectedTab = "profile" })
                                    } else if (selectedTab == "saved_ayahs") {
                                        SavedAyahsScreen(onBack = { selectedTab = "profile" })
                                    } else if (selectedTab == "saved_hadiths") {
                                        SavedHadithsScreen(onBack = { selectedTab = "profile" })
                                    } else if (selectedTab == "settings") {
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            prayerAlarms = state.alarms,
                                            onTogglePrayerAlarm = { alarmId -> viewModel.toggleAlarm(context, alarmId) },
                                            onBack = { selectedTab = "profile" }
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("শীঘ্রই আসছে...", fontSize = 24.sp, color = TextGray)
                                        }
                                    }
                                
                                // Overlay status bar header purely for visual gradient, without padding the content below
                                if (!isDarkStatusBar) {
                                    GlassStatusBarHeader()
                                }

                                if (isParentalPageOpen) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isParentalPageOpen,
                                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                                    ) {
                                        ParentalControlScreen(
                                            onBack = { isParentalPageOpen = false }
                                        )
                                    }
                                }

                                if (isAlarmPageOpen) {
                                    AlarmSetupScreen(
                                        onBack = { isAlarmPageOpen = false },
                                        onAddAlarmClick = { isAddAlarmPageOpen = true },
                                        alarms = userAlarms,
                                        onToggleAlarm = { alarmViewModel.toggleAlarm(it) },
                                        onDeleteAlarm = { alarmViewModel.deleteAlarm(it) }
                                    )
                                }
                                if (isAddAlarmPageOpen) {
                                    AddAlarmScreen(
                                        onBack = { isAddAlarmPageOpen = false },
                                        onSave = { alarmViewModel.addAlarm(it) }
                                    )
                                }
                                if (isZakatPageOpen) {
                                    ZakatCalculatorScreen(
                                        onBack = { isZakatPageOpen = false }
                                    )
                                }
                                if (isCalendarPageOpen) {
                                    CalendarScreen(
                                        onBack = { isCalendarPageOpen = false }
                                    )
                                }
                                if (isNotificationsPageOpen) {
                                     NotificationsScreen(
                                         onBack = { isNotificationsPageOpen = false }
                                     )
                                 }
                                 if (isFoundationPageOpen) {
                                     FoundationScreen(
                                         onBack = { isFoundationPageOpen = false }
                                     )
                                 }
                                 if (isQiblaPageOpen) {
                                    QiblaCompassScreen(
                                        onBack = { isQiblaPageOpen = false }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isPrayerPageOpen,
                                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)),
                                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400))
                                ) {
                                    PrayerScreen(
                                        state = state,
                                        onBack = { isPrayerPageOpen = false },
                                        onToggleAlarm = { alarmId -> viewModel.toggleAlarm(context, alarmId) },
                                        onOpenAlarmPage = { isAlarmPageOpen = true }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isCreateCircleAlertOpen,
                                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)),
                                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400))
                                ) {
                                    CreateCircleAlertScreen(
                                        savedLocation = state.locationName.ifEmpty { "All Bangladesh" }, // Get location from state
                                        onBack = { isCreateCircleAlertOpen = false },
                                        onSubmit = { alert ->
                                               // Optional: submit to server
                                        }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isCreatePostOpen,
                                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)),
                                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400))
                                ) {
                                    com.example.social.CreatePostScreen(
                                        onNavigateBack = { isCreatePostOpen = false }
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
}

@Composable
fun GlassStatusBarHeader() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
    )
}

@Composable
fun AppBottomNavigation(selectedTab: String, isDark: Boolean, onTabSelected: (String) -> Unit) {
    val navContainerColor = Color.White
    val navUnselectedColor = TextGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        NavigationBar(
            containerColor = navContainerColor,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .navigationBarsPadding()
        ) {
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = { onTabSelected("home") },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (selectedTab == "home") Modifier.border(2.dp, PrimaryGreen, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == "home") Icons.Default.Home else Icons.Outlined.Home, 
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = navUnselectedColor,
                unselectedTextColor = navUnselectedColor
            )
        )
        NavigationBarItem(
            selected = selectedTab == "quran",
            onClick = { onTabSelected("quran") },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (selectedTab == "quran") Modifier.border(2.dp, PrimaryGreen, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == "quran") Icons.Filled.MenuBook else Icons.Outlined.MenuBook, 
                        contentDescription = "Quran",
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = navUnselectedColor,
                unselectedTextColor = navUnselectedColor
            )
        )
        NavigationBarItem(
            selected = selectedTab == "tracker",
            onClick = { onTabSelected("tracker") },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (selectedTab == "tracker") Modifier.border(2.dp, PrimaryGreen, RoundedCornerShape(10.dp)) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == "tracker") Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, 
                        contentDescription = "Tracker",
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = navUnselectedColor,
                unselectedTextColor = navUnselectedColor
            )
        )
        NavigationBarItem(
            selected = selectedTab == "tools",
            onClick = { onTabSelected("tools") },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (selectedTab == "tools") Modifier.border(2.dp, PrimaryGreen, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == "tools") Icons.Filled.GridView else Icons.Outlined.GridView, 
                        contentDescription = "Tools",
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = navUnselectedColor,
                unselectedTextColor = navUnselectedColor
            )
        )
        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = { onTabSelected("profile") },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (selectedTab == "profile") Modifier.border(2.dp, PrimaryGreen, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person, 
                        contentDescription = "Profile",
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = navUnselectedColor,
                unselectedTextColor = navUnselectedColor
            )
        )
    }
}
}

@Composable
fun HomeScreen(
    state: com.example.viewmodel.ViewState, 
    onToggleAlarm: (String) -> Unit, 
    onNavigateToPrayerDetails: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToQuran: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onOpenAlarmPage: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToAllahNames: () -> Unit,
    onNavigateToRamadan: () -> Unit,
    onNavigateToDua: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onOpenNotificationsPage: () -> Unit,
    onNavigateToCreatePost: () -> Unit = {},
    onOpenFoundationPage: () -> Unit
) {
    var isPrayerExpanded by remember { mutableStateOf(false) }
    
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // App Bar (Top row: Clickable App Name with Quick Action Icons aligned perfectly on the right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToLocation() }
            ) {
                Text(
                    text = "Halal Circle",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = PrimaryGreen
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenAlarmPage) {
                    Icon(
                        imageVector = Icons.Outlined.AccessAlarm, 
                        contentDescription = "Alarms", 
                        tint = TextDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = onOpenFoundationPage) {
                    Icon(
                        imageVector = Icons.Outlined.VolunteerActivism, 
                        contentDescription = "Foundation", 
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Box {
                    val context = LocalContext.current
                    val unreadCount by remember {
                        com.example.database.TrackerDatabase.getDatabase(context).notificationDao().getUnreadCount()
                    }.collectAsState(initial = 0)

                    IconButton(
                        onClick = onOpenNotificationsPage
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications, 
                            contentDescription = "Notifications", 
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    if (unreadCount > 0) {
                        val displayCount = if (unreadCount > 99) (if (GlobalLanguage.isEnglish) "99+" else "৯৯+") else if (GlobalLanguage.isEnglish) unreadCount.toString() else unreadCount.toBengaliDigits()
                        val badgeSize = if (unreadCount > 9) 20.dp else 18.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp)
                                .defaultMinSize(minWidth = badgeSize, minHeight = badgeSize)
                                .background(Color.Red, CircleShape)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayCount,
                                color = Color.White,
                                fontSize = if (unreadCount > 9) 9.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Location Box: Thinner and positioned beautifully below the location line
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 2.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.6f))
                .border(
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(50)
                )
                .clickable { onNavigateToLocation() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Location",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = state.locationName,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Location",
                    tint = TextDark,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Hero Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNavigateToPrayerDetails() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sunrise
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.WbTwilight, contentDescription = "Sunrise", tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.prayerTimes?.sunrise?.toBengali() ?: "--", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (GlobalLanguage.isEnglish) "Sunrise" else "সূর্যোদয়", color = TextGray, fontSize = 11.sp)
            }

            // Circular Timer
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp).padding(4.dp)) {
                // Foreground filling circle
                CircularProgressIndicator(
                    progress = { state.timerProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryGreen,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayNames = if (state.rotatingNames.isNotEmpty()) state.rotatingNames else listOf(state.currentPrayerNameBen.ifEmpty { state.nextPrayerNameBen })
                    var currentIndex by remember(displayNames) { mutableIntStateOf(0) }
                    LaunchedEffect(displayNames) {
                        if (displayNames.size > 1) {
                            while(true) {
                                kotlinx.coroutines.delay(3000)
                                currentIndex = (currentIndex + 1) % displayNames.size
                            }
                        } else {
                            currentIndex = 0
                        }
                    }
                    val currentTitle = if (displayNames.isNotEmpty()) displayNames[currentIndex % displayNames.size] else "..."
                    
                    AnimatedContent(
                        targetState = currentTitle,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(600)) + slideInVertically { height -> height }).togetherWith(
                                fadeOut(animationSpec = tween(600)) + slideOutVertically { height -> -height }
                            )
                        },
                        label = "hero_title_rotation"
                    ) { title ->
                        Text(title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    
                    Text(state.nextPrayerRemaining, color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top=2.dp))
                    Text(if (GlobalLanguage.isEnglish) "Time remaining" else "শেষ হতে বাকি", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(top=2.dp))
                }
            }

            // Sunset
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.WbSunny, contentDescription = "Sunset", tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.prayerTimes?.maghrib?.toBengali() ?: "--", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (GlobalLanguage.isEnglish) "Sunset" else "সূর্যাস্ত", color = TextGray, fontSize = 11.sp)
            }
        }

        // Sub info (Sehri / Iftar Countdown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubInfoItemProgress(
                title = state.specialCountdownLabel, 
                time = state.specialCountdownTime, 
                progress = "${(state.specialCountdownProgress * 100).toInt()}%".toBengali(),
                isLarge = true
            )
            Spacer(modifier = Modifier.weight(1f))
            SubInfoItem(
                title = if (GlobalLanguage.isEnglish) "Sehri Last Time" else "সাহরির শেষ সময়", 
                time = state.prayerTimes?.fajr?.toBengali() ?: "--:--"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nafl Salat Section (Always visible by default)
        NaflSalatSection(state)

        Spacer(modifier = Modifier.height(24.dp))

        // Forbidden Times (Always visible by default)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(LocalAppStrings.current.forbidden_times, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            Icon(Icons.Outlined.Info, contentDescription = "Info", tint=TextGray, modifier=Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp), 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ForbiddenTimeCard(LocalAppStrings.current.sunrise, state.forbiddenSunrise, state.forbiddenSunriseEnd, Icons.Outlined.WbTwilight)
            ForbiddenTimeCard(LocalAppStrings.current.noon, state.forbiddenNoon, state.forbiddenNoonEnd, Icons.Outlined.WbSunny)
            ForbiddenTimeCard(LocalAppStrings.current.sunset, state.forbiddenSunset, state.forbiddenSunsetEnd, Icons.Outlined.WbTwilight)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NaflSalatSection(state: com.example.viewmodel.ViewState) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (GlobalLanguage.isEnglish) "Nafl Salat Times" else "নফল সালাতের সময়", 
                fontWeight = FontWeight.Bold, 
                fontSize = 17.sp, 
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (GlobalLanguage.isEnglish) "Reference" else "রেফারেন্স", 
                color = PrimaryGreen, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        state.prayerTimes?.let { times ->
             val sunriseHours = times.sunriseHours
             val dhuhrHours = times.dhuhrHours
             val chashtHours = sunriseHours + 15.0 / 60.0
             
             // Helper for formatting
             val formatTime = { h: Double ->
                val totalMin = (h * 60).toInt()
                val hour = (totalMin / 60) % 24
                val min = totalMin % 60
                val isEng = GlobalLanguage.isEnglish
                val p = if (hour >= 12) (if(isEng) "PM" else "পিএম") else (if(isEng) "AM" else "এএম")
                val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                String.format("%02d:%02d %s", displayHour, min, p).toBengali()
             }
             
             val formatTimeRange = { start: Double, end: Double ->
                val s = formatTime(start).replace(" এএম", "").replace(" পিএম", "").replace(" AM", "").replace(" PM", "")
                val e = formatTime(end)
                "$s - $e"
             }

             Card(
                 modifier = Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(12.dp),
                 colors = CardDefaults.cardColors(containerColor = Color.White),
                 border = BorderStroke(1.dp, BgLight.copy(alpha = 0.5f))
             ) {
                 Column(
                     modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                 ) {
                     // Duha
                     NaflSalatRow(
                        icon = Icons.Outlined.WbSunny,
                        name = if (GlobalLanguage.isEnglish) "Duha / Chasht" else "দুহা / চাশত",
                        time = formatTimeRange(chashtHours, dhuhrHours - 5.0/60.0),
                        label = if (GlobalLanguage.isEnglish) "Morning" else "সকাল"
                     )
                     
                     HorizontalDivider(color = BgLight.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                     
                     // Zawal
                     NaflSalatRow(
                        icon = Icons.Outlined.LocationOn,
                        name = if (GlobalLanguage.isEnglish) "Zawal Start" else "যাওয়াল শুরু",
                        time = formatTime(dhuhrHours),
                        label = if (GlobalLanguage.isEnglish) "Noon" else "দুপুর"
                     )
                     
                     HorizontalDivider(color = BgLight.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                     
                     // Awwabin
                     NaflSalatRow(
                        icon = Icons.Outlined.WbTwilight,
                        name = if (GlobalLanguage.isEnglish) "Awwabin" else "আওয়াবিন",
                        time = times.maghrib.toBengali(),
                        label = if (GlobalLanguage.isEnglish) "After Maghrib" else "মাগরিবের পর"
                     )
                     
                     HorizontalDivider(color = BgLight.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                     
                     // Tahajjud
                     NaflSalatRow(
                        icon = Icons.Outlined.DarkMode,
                        name = if (GlobalLanguage.isEnglish) "Tahajjud" else "তাহাজ্জুদ",
                        time = times.fajr.toBengali(),
                        label = if (GlobalLanguage.isEnglish) "Last 1/3 of Night" else "রাতের শেষ ১/৩ হেসসা",
                        subtitle = if (GlobalLanguage.isEnglish) "Starts: " + formatTime(times.fajrHours - 3.0) 
                                   else "শুরু: রাত ${formatTime(times.fajrHours - 3.0)}"
                     )
                 }
             }
        }
    }
}

@Composable
fun NaflSalatRow(icon: ImageVector, name: String, time: String, label: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PrimaryGreen.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name, 
                    color = TextDark, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1
                )
                Text(
                    text = label, 
                    color = TextGray, 
                    fontSize = 11.sp
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = time, 
                color = PrimaryGreen, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(PrimaryGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle, 
                        color = TextGray, 
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SubInfoItem(title: String, time: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(title, color = TextGray, fontSize = 11.sp, fontWeight=FontWeight.Medium)
        Spacer(modifier=Modifier.height(2.dp))
        Text(time, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SubInfoItemProgress(title: String, time: String, progress: String, isLarge: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(if (isLarge) 60.dp else 44.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                strokeWidth = if (isLarge) 4.dp else 3.dp,
                color = BgLight,
                modifier = Modifier.fillMaxSize()
            )
            val progressFactor = try { progress.replace("%", "").trim().toDouble() / 100.0 } catch(e: Exception) { 0.75 }
            CircularProgressIndicator(
                progress = { progressFactor.toFloat() },
                strokeWidth = if (isLarge) 4.dp else 3.dp,
                color = PrimaryGreen,
                modifier = Modifier.fillMaxSize()
            )
            // progress is in Bengali digits, need to handle or just display
            Text(progress, fontSize = if (isLarge) 14.sp else 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Column {
            Text(title, fontSize = if (isLarge) 11.sp else 9.sp, color = TextGray)
            Text(time, fontSize = if (isLarge) 20.sp else 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun PrayerRow(
    name: String, 
    startTime: String, 
    endTime: String,
    isAlarmOn: Boolean, 
    isActive: Boolean, 
    onToggleAlarm: () -> Unit,
    onOpenAlarmPage: () -> Unit
) {
    val cleanStart = startTime.replace(" এএম", "").replace(" পিএম", "").replace(" AM", "").replace(" PM", "").toBengali()
    val cleanEnd = endTime.replace(" এএম", "").replace(" পিএম", "").replace(" AM", "").replace(" PM", "").toBengali()
    val endSuffix = if (endTime.contains("পিএম") || endTime.contains("PM")) (if(GlobalLanguage.isEnglish) " PM" else " পিএম") else (if(GlobalLanguage.isEnglish) " AM" else " এএম")
    
    // For night time range like Isha to Fajr
    val timeDisplay = if (name == "এশা" || name == "Isha") {
        "$cleanStart - ${if (GlobalLanguage.isEnglish) "Night" else "রাত"} $cleanEnd".toBengali()
    } else {
        "$cleanStart - $cleanEnd$endSuffix".toBengali()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if(isActive) PrimaryGreen.copy(alpha=0.12f) else Color.Transparent, 
                RoundedCornerShape(12.dp)
            )
            .then(
                if(isActive) Modifier.border(1.dp, PrimaryGreen.copy(alpha=0.3f), RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable { onOpenAlarmPage() }
            .padding(horizontal=12.dp, vertical=8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if(isActive) PrimaryGreen else BgLight.copy(alpha=0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (name == "এশা" || name == "মাগরিব" || name=="ফজর" || name == "Isha" || name == "Maghrib" || name == "Fajr") 
                        Icons.Outlined.DarkMode else Icons.Outlined.LightMode, 
                    contentDescription = null, 
                    tint = if(isActive) Color.White else TextGray, 
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                name, 
                color = TextDark, 
                fontSize = 17.sp, 
                fontWeight=if(isActive) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                timeDisplay, 
                color = PrimaryGreen, 
                fontSize = 16.sp, 
                fontWeight=if(isActive) FontWeight.ExtraBold else FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onToggleAlarm, modifier = Modifier.size(24.dp)) {
                 Icon(
                     if (isAlarmOn) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                     contentDescription = "Alarm",
                     tint = if (isAlarmOn) PrimaryGreen else TextGray.copy(alpha = 0.6f),
                     modifier = Modifier.size(20.dp)
                 )
            }
        }
    }
}

data class Quad<A, B, C, D>(val id: A, val name: B, val startTime: C, val endTime: D)

@Composable
fun CategoryScrollableRow(
    onNavigateToTracker: () -> Unit,
    onNavigateToTasbih: () -> Unit = {},
    onNavigateToQuran: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToAllahNames: () -> Unit = {},
    onNavigateToRamadan: () -> Unit = {},
    onNavigateToDua: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateToIslamicNames: () -> Unit = {}
) {
    val items = if (GlobalLanguage.isEnglish) {
        listOf(
            Triple("Al Quran", Icons.Outlined.MenuBook, PrimaryGreen),
            Triple("Hadith", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("Tasbih", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("Qibla", Icons.Outlined.Explore, PrimaryGreen),
            Triple("Dua", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("Allah's Names", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("Zakat", Icons.Outlined.MonetizationOn, PrimaryGreen),
            Triple("Calendar", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("Amal Learning", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("Ramadan", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("Islamic Name", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("Salah Learning", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6))
        )
    } else {
        listOf(
            Triple("আল কুরআন", Icons.Outlined.MenuBook, PrimaryGreen),
            Triple("হাদিস", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("তাসবিহ", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("কিবলা", Icons.Outlined.Explore, PrimaryGreen),
            Triple("দোয়া", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("আল্লাহর নাম", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("যাকাত", Icons.Outlined.MonetizationOn, PrimaryGreen),
            Triple("ক্যালেন্ডার", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("আমল শিক্ষা", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("রমজান", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("ইসলামিক নাম", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("নামাজ শিক্ষা", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6))
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        when (item.first) {
                            "আমল শিক্ষা", "নামাজ শিক্ষা", "Amal Learning", "Salah Learning" -> onNavigateToTracker()
                            "তাসবিহ", "Tasbih" -> onNavigateToTasbih()
                            "আল কুরআন", "Al Quran" -> onNavigateToQuran()
                            "যাকাত", "Zakat" -> onNavigateToZakat()
                            "ক্যালেন্ডার", "Calendar" -> onNavigateToCalendar()
                            "কিবলা", "Qibla" -> onNavigateToQibla()
                            "আল্লাহর নাম", "Allah's Names" -> onNavigateToAllahNames()
                            "রমজান", "Ramadan" -> onNavigateToRamadan()
                            "দোয়া", "Dua" -> onNavigateToDua()
                            "হাদিস", "Hadith" -> onNavigateToHadith()
                            "ইসলামিক নাম", "Islamic Name" -> onNavigateToIslamicNames()
                        }
                    }
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(
                                red = (item.third.red * 0.82f).coerceIn(0f, 1f),
                                green = (item.third.green * 0.82f).coerceIn(0f, 1f),
                                blue = (item.third.blue * 0.82f).coerceIn(0f, 1f),
                                alpha = 1f
                            ), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.second, contentDescription = item.first, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.first,
                    color = TextDark,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ForbiddenTimeCard(title: String, start: String, end: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = title, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(start, color = PrimaryGreen, fontSize = 11.sp)
        Text(if (GlobalLanguage.isEnglish) "from" else "থেকে", color = TextGray, fontSize = 10.sp)
        Text(end, color = PrimaryGreen, fontSize = 11.sp)
    }
}

@Composable
fun SocialBlockerOverlay(
    platformName: String,
    onDismissToHome: () -> Unit
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableStateOf(5) }
    
    val hadiths = remember {
        listOf(
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: মানুষের ইসলামের অন্যতম সৌন্দর্য হলো নিরর্থক ও অনর্থক কথা ও কাজ পরিহার করা।” — তিরমিযী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: যে ব্যক্তি আল্লাহ ও পরকালের প্রতি ঈমান রাখে, সে যেন ভালো কথা বলে অথবা নীরব থাকে।” — বুখারী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: দুটি নিয়ামত এমন রয়েছে, যে দুটিতে অধিকাংশ মানুষ ক্ষতিগ্রস্ত; তা হলো স্বাস্থ্য এবং অবসর সময়।” — বুখারী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: নিশ্চয়ই তোমার প্রতিপালকের প্রতি তোমার অবধারিত কতর্ব্য রয়েছে, তোমার নিজের প্রতিও কতর্ব্য রয়েছে।” — বুখারী",
            "“আল্লাহতায়ালা বলেছেন: আর তোমরা অনর্থক ক্রীড়াকৌতুক থেকে নিজেদের দূরে রাখো।” — সূরা আল-মুমিনুন"
        )
    }
    val selectedHadith = remember { hadiths.random() }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
        onDismissToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Halal Circle App Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Halal Circle",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Warning Message
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "অবরুদ্ধ",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "অ্যাপ ব্যবহারে নিষেধাজ্ঞা",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "আপনি এখন $platformName ব্যবহার করতে পারবেন না। আপনার একাগ্রতা এবং আত্মিক উন্নয়ন বজায় রাখতে এই অ্যাপটি সাময়িকভাবে ব্লক করা হয়েছে।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Beautiful Islamic Hadith Section with Rose/Muted Box styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF5FF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFF3E8FF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = selectedHadith,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF581C87),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Countdown Timer Progress Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF43F5E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secondsLeft.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "সেকেন্ডের মধ্যে আপনাকে সড়িয়ে দেওয়া হবে...",
                        fontSize = 13.sp,
                        color = Color(0xFFF43F5E),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Exit immediately button
                Button(
                    onClick = onDismissToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "হোম স্কিনে ফিরে যান",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(
    onNavigateToTracker: () -> Unit,
    onNavigateToTasbih: () -> Unit = {},
    onNavigateToQuran: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToAllahNames: () -> Unit = {},
    onNavigateToRamadan: () -> Unit = {},
    onNavigateToDuroodReminder: () -> Unit = {},
    onNavigateToDua: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateToWidgets: () -> Unit = {},
    onNavigateToIslamicNames: () -> Unit = {},
    onNavigateToSocialVideos: () -> Unit = {},
    maxItems: Int? = null
) {
    val items = if (GlobalLanguage.isEnglish) {
        listOf(
            Triple("Al Quran", Icons.Outlined.MenuBook, PrimaryGreen),
            Triple("Hadith", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("Tasbih", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("Qibla", Icons.Outlined.Explore, PrimaryGreen),
            Triple("Dua", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("Allah's Names", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("Zakat", Icons.Outlined.MonetizationOn, PrimaryGreen),
            Triple("Calendar", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("Amal Learning", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("Ramadan", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("Islamic Name", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("Salah Learning", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6)),
            Triple("Durood Reminder", Icons.Outlined.Notifications, Color(0xFF8B5CF6)),
            Triple("Widgets", Icons.Outlined.Widgets, Color(0xFF10B982)),
            Triple("Social Videos", Icons.Outlined.VideoLibrary, Color(0xFFEF4444))
        )
    } else {
        listOf(
            Triple("আল কুরআন", Icons.Outlined.MenuBook, PrimaryGreen),
            Triple("হাদিস", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("তাসবিহ", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("কিবলা", Icons.Outlined.Explore, PrimaryGreen),
            Triple("দোয়া", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("আল্লাহর নাম", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("যাকাত", Icons.Outlined.MonetizationOn, PrimaryGreen),
            Triple("ক্যালেন্ডার", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("আমল শিক্ষা", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("রমজান", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("ইসলামিক নাম", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("নামাজ শিক্ষা", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6)),
            Triple("দরুদ রিমাইন্ডার", Icons.Outlined.Notifications, Color(0xFF8B5CF6)),
            Triple("উইজেট", Icons.Outlined.Widgets, Color(0xFF10B982)),
            Triple("সোশ্যাল ভিডিও", Icons.Outlined.VideoLibrary, Color(0xFFEF4444))
        )
    }

    val displayItems = if (maxItems != null) items.take(maxItems) else items

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        val numRows = (displayItems.size + 3) / 4
        for (row in 0 until numRows) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (col in 0..3) {
                    val index = row * 4 + col
                    if (index < displayItems.size) {
                        val item = displayItems[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (item.first == "আমল শিক্ষা" || item.first == "নামাজ শিক্ষা" || 
                                        item.first == "Amal Learning" || item.first == "Salah Learning") {
                                        onNavigateToTracker()
                                    } else if (item.first == "তাসবিহ" || item.first == "Tasbih") {
                                        onNavigateToTasbih()
                                    } else if (item.first == "আল কুরআন" || item.first == "Al Quran") {
                                        onNavigateToQuran()
                                    } else if (item.first == "যাকাত" || item.first == "Zakat") {
                                        onNavigateToZakat()
                                    } else if (item.first == "ক্যালেন্ডার" || item.first == "Calendar") {
                                        onNavigateToCalendar()
                                    } else if (item.first == "কিবলা" || item.first == "Qibla") {
                                        onNavigateToQibla()
                                    } else if (item.first == "আল্লাহর নাম" || item.first == "Allah's Names") {
                                        onNavigateToAllahNames()
                                    } else if (item.first == "রমজান" || item.first == "Ramadan") {
                                        onNavigateToRamadan()
                                    } else if (item.first == "দরুদ রিমাইন্ডার" || item.first == "Durood Reminder") {
                                        onNavigateToDuroodReminder()
                                    } else if (item.first == "দোয়া" || item.first == "Dua") {
                                        onNavigateToDua()
                                    } else if (item.first == "হাদিস" || item.first == "Hadith") {
                                        onNavigateToHadith()
                                    } else if (item.first == "উইজেট" || item.first == "Widgets") {
                                        onNavigateToWidgets()
                                    } else if (item.first == "ইসলামিক নাম" || item.first == "Islamic Name") {
                                        onNavigateToIslamicNames()
                                    } else if (item.first == "সোশ্যাল ভিডিও" || item.first == "Social Videos") {
                                        onNavigateToSocialVideos()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        Color(
                                            red = (item.third.red * 0.82f).coerceIn(0f, 1f),
                                            green = (item.third.green * 0.82f).coerceIn(0f, 1f),
                                            blue = (item.third.blue * 0.82f).coerceIn(0f, 1f),
                                            alpha = 1f
                                        ), 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.second, contentDescription = item.first, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.first,
                                color = TextDark,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(
    onNavigateToTracker: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToQuran: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToAllahNames: () -> Unit,
    onNavigateToRamadan: () -> Unit,
    onNavigateToDuroodReminder: () -> Unit,
    onNavigateToDua: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToIslamicNames: () -> Unit,
    onNavigateToSocialVideos: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (GlobalLanguage.isEnglish) "All Tools" else "সকল ক্যাটাগরি", 
                fontWeight = FontWeight.Bold, 
                color = TextDark, 
                fontSize = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Categories Grid
        CategoryGrid(
            onNavigateToTracker = onNavigateToTracker,
            onNavigateToTasbih = onNavigateToTasbih,
            onNavigateToQuran = onNavigateToQuran,
            onNavigateToZakat = onNavigateToZakat,
            onNavigateToCalendar = onNavigateToCalendar,
            onNavigateToQibla = onNavigateToQibla,
            onNavigateToAllahNames = onNavigateToAllahNames,
            onNavigateToRamadan = onNavigateToRamadan,
            onNavigateToDuroodReminder = onNavigateToDuroodReminder,
            onNavigateToDua = onNavigateToDua,
            onNavigateToHadith = onNavigateToHadith,
            onNavigateToWidgets = onNavigateToWidgets,
            onNavigateToIslamicNames = onNavigateToIslamicNames,
            onNavigateToSocialVideos = onNavigateToSocialVideos
        )
    }
}
