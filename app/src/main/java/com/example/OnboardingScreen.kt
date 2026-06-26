package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.PrayerViewModel

@Composable
fun OnboardingScreen(
    prayerViewModel: PrayerViewModel,
    settingsViewModel: com.example.viewmodel.SettingsViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var showAuthOption by remember { mutableStateOf<String?>(null) }
    
    if (showAuthOption == "login") {
        LoginScreen(
            onBack = { showAuthOption = null },
            onNavigateToRegister = { showAuthOption = "signup" },
            onLoginSuccess = { 
                showAuthOption = null
                step = 2 
            }
        )
        return
    } else if (showAuthOption == "signup") {
        RegisterScreen(
            onBack = { showAuthOption = null },
            onNavigateToLogin = { showAuthOption = "login" },
            onRegisterSuccess = {
                showAuthOption = null
                step = 2 
            }
        )
        return
    }
    
    val premiumGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF6FAF7), Color(0xFFFFFFFF), Color(0xFFF0FDF4))
    )
    
    Box(modifier = Modifier.fillMaxSize().background(premiumGradient)) {
        // Subtle premium background decoration
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PrimaryGreen.copy(alpha = 0.04f),
                radius = size.width * 1.2f,
                center = androidx.compose.ui.geometry.Offset(size.width, -100f)
            )
            drawCircle(
                color = PrimaryGreen.copy(alpha = 0.02f),
                radius = size.width * 0.8f,
                center = androidx.compose.ui.geometry.Offset(0f, size.height + 100f)
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            modifier = Modifier.fillMaxSize(),
            label = "StepAnimation"
        ) { currentStep ->
            when (currentStep) {
                1 -> WelcomeStep(
                    settingsViewModel = settingsViewModel,
                    onLogin = { showAuthOption = "login" },
                    onSignUp = { showAuthOption = "signup" },
                    onGuest = { step = 2 }
                )
                2 -> LanguageStep(
                    settingsViewModel = settingsViewModel,
                    prayerViewModel = prayerViewModel,
                    onNext = { step = 3 }
                )
                3 -> MadhabStep(
                    prayerViewModel = prayerViewModel,
                    onNext = { step = 4 }
                )
                4 -> LocationStep(
                    prayerViewModel = prayerViewModel,
                    onComplete = onComplete
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(settingsViewModel: com.example.viewmodel.SettingsViewModel, onLogin: () -> Unit, onSignUp: () -> Unit, onGuest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // High-quality visual icon area
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = PrimaryGreen.copy(alpha = 0.1f)
            ) {}
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = PrimaryGreen.copy(alpha = 0.15f)
            ) {}
            com.example.ui.components.DynamicAppLogo(
                settingsViewModel = settingsViewModel,
                size = 100.dp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "স্বাগতম!",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "আপনার আত্মিক উন্নয়নের পথে\nএক ধাপ এগিয়ে",
            textAlign = TextAlign.Center,
            color = TextGray,
            fontSize = 18.sp,
            lineHeight = 26.sp
        )
        
        Spacer(modifier = Modifier.weight(1.2f))
        
        // Buttons at the bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("লগইন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, PrimaryGreen)
            ) {
                Text("সাইন আপ", fontSize = 18.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onGuest,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("গেস্ট ইউজার হিসেবে প্রবেশ করুন", color = TextGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageStep(
    settingsViewModel: com.example.viewmodel.SettingsViewModel,
    prayerViewModel: com.example.viewmodel.PrayerViewModel,
    onNext: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val language by settingsViewModel.language.collectAsState()
    val selectedCountryCode by settingsViewModel.selectedCountryCode.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    val isBengali = language == com.example.viewmodel.AppLanguage.BENGALI
    
    val titleText = if (isBengali) "দেশ ও ভাষা নির্বাচন করুন" else "Select Country & Language"
    val subtitleText = if (isBengali) "তালিকা থেকে আপনার দেশ বা ভাষা নির্বাচন করুন" else "Select your country or language from the list"
    val searchPlaceholder = if (isBengali) "দেশ বা ভাষা অনুসন্ধান করুন..." else "Search country or language..."
    val buttonText = if (isBengali) "পরবর্তী" else "Next"
    
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            com.example.model.CountryData.countries
        } else {
            com.example.model.CountryData.countries.filter { country ->
                country.name.contains(searchQuery, ignoreCase = true) ||
                country.code.contains(searchQuery, ignoreCase = true) ||
                country.language.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = titleText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitleText,
            fontSize = 14.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar at the very top
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text(searchPlaceholder, color = TextGray.copy(alpha = 0.7f), fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryGreen) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray)
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )
        
        // Scrollable List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredCountries) { country ->
                val isSelected = country.code == selectedCountryCode
                CountryLanguageOption(
                    country = country,
                    isSelected = isSelected,
                    onClick = {
                        settingsViewModel.setSelectedCountryAndLanguage(country.code)
                        prayerViewModel.loadSettings(context)
                    }
                )
            }
            if (filteredCountries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBengali) "কোনো ফলাফল পাওয়া যায়নি" else "No matching results found",
                            color = TextGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom Next Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun CountryLanguageOption(
    country: com.example.model.Country,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else Color.White,
        border = if (isSelected) BorderStroke(1.5.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shadowElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag display
                Text(
                    text = country.flag,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Column {
                    Text(
                        text = country.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (country.code == "BD") "বাংলা (Bengali)" else "English (US)",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
            
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) PrimaryGreen else Color(0xFFD1D5DB),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MadhabStep(prayerViewModel: PrayerViewModel, onNext: () -> Unit) {
    val state by prayerViewModel.state.collectAsState()
    val isEnglish = GlobalLanguage.isEnglish
    val context = LocalContext.current
    var uiSelectedMadhab by remember { mutableStateOf(if(state.madhab == 2) "hanafi" else "shafii") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            if (isEnglish) "Select Madhab" else "মাযহাব নির্বাচন করুন", 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Bold, 
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (isEnglish) "This affects Asr prayer time calculation" else "এটি আসরের নামাজের সময় গণনায় ব্যবহৃত হবে", 
            fontSize = 15.sp, 
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        MadhabOption(
            title = if (isEnglish) "Hanafi" else "হানাফী",
            description = if (isEnglish) "Asr starts when shadow is twice the length" else "ছায়ার দৈর্ঘ্য দ্বিগুণ হলে আসর শুরু হয়",
            isSelected = uiSelectedMadhab == "hanafi",
            onClick = { 
                uiSelectedMadhab = "hanafi"
                prayerViewModel.setMadhab(context, 2) 
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        MadhabOption(
            title = if (isEnglish) "Shafi'i" else "শাফেয়ী",
            description = if (isEnglish) "Asr starts when shadow equals length" else "ছায়ার দৈর্ঘ্য একগুণ হলে আসর শুরু হয়",
            isSelected = uiSelectedMadhab == "shafii",
            onClick = { 
                uiSelectedMadhab = "shafii"
                prayerViewModel.setMadhab(context, 1) 
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        MadhabOption(
            title = if (isEnglish) "Maliki" else "মালিকী",
            description = if (isEnglish) "Asr starts when shadow equals length" else "ছায়ার দৈর্ঘ্য একগুণ হলে আসর শুরু হয়",
            isSelected = uiSelectedMadhab == "maliki",
            onClick = { 
                uiSelectedMadhab = "maliki"
                prayerViewModel.setMadhab(context, 1) 
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        MadhabOption(
            title = if (isEnglish) "Hanbali" else "হাম্বলী",
            description = if (isEnglish) "Asr starts when shadow equals length" else "ছায়ার দৈর্ঘ্য একগুণ হলে আসর শুরু হয়",
            isSelected = uiSelectedMadhab == "hanbali",
            onClick = { 
                uiSelectedMadhab = "hanbali"
                prayerViewModel.setMadhab(context, 1) 
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(if (isEnglish) "Next" else "পরবর্তী", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MadhabOption(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) BorderStroke(1.5.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shadowElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextDark)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = TextGray)
            }
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) PrimaryGreen else Color(0xFFDDDDDD),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LocationStep(prayerViewModel: PrayerViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val isEnglish = GlobalLanguage.isEnglish
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = PrimaryGreen.copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null, 
                tint = PrimaryGreen, 
                modifier = Modifier.padding(32.dp).fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            if (isEnglish) "Set Your Location" else "লোকেশন সেট করুন", 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Bold, 
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            if (isEnglish) "Automatically detect location for accurate prayer times" else "সঠিক নামাজের সময়সূচির জন্য অটোমেটিক লোকেশন সেট করুন", 
            fontSize = 16.sp, 
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1.2f))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { 
                    prayerViewModel.setAutoLocation(context) 
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isEnglish) "Auto Location" else "অটোমেটিক লোকেশন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFDDDDDD))
            ) {
                Text(if (isEnglish) "Later" else "পরে করব", color = TextGray, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
