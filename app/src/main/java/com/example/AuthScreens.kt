package com.example

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.providers.builtin.Email as SupabaseEmail
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CompactTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgLight)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextDark,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val supabase = Supabase.client
    val scope = rememberCoroutineScope()
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish

    Scaffold(
        topBar = {
            CompactTopBar(
                title = if (isEnglish) "Sign In" else "লগইন করুন",
                onBack = onBack
            )
        },
        containerColor = BgLight,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Premium scanning shield visual header
            CyberSecurityAnimation()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isEnglish) "Welcome Back" else "স্বাগতম",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Text(
                text = if (isEnglish) "Sign in to access your Islamic productivity features" else "আপনার ইমেইল ব্যবহার করে অ্যাকাউন্টে প্রবেশ করুন",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Slim & Premium Email Field with floating label
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (isEnglish) "Email Address" else "ইমেইল এড্রেস", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Email, 
                        contentDescription = null, 
                        tint = if (email.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Slim & Premium Password Field with floating label
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isEnglish) "Password" else "পাসওয়ার্ড", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                            null,
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        tint = if (password.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand new modern Login Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = if (isEnglish) "Please fill all fields" else "সবগুলো ঘর পূরণ করুন"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            supabase.auth.signInWith(SupabaseEmail) {
                                this.email = email.trim()
                                this.password = password
                            }
                            isLoading = false
                            onLoginSuccess()
                        } catch (e: Exception) {
                            isLoading = false
                            val msg = e.localizedMessage ?: ""
                            errorMessage = when {
                                msg.contains("credential", ignoreCase = true) || 
                                msg.contains("invalid", ignoreCase = true) || 
                                msg.contains("incorrect", ignoreCase = true) || 
                                msg.contains("password", ignoreCase = true) ||
                                msg.contains("user not found", ignoreCase = true) ||
                                msg.contains("400", ignoreCase = true) || 
                                msg.contains("401", ignoreCase = true) -> {
                                    if (isEnglish) "The email address or password you entered is incorrect." 
                                    else "আপনার দেওয়া ইমেইল অথবা পাসওয়ার্ডটি ভুল।"
                                }
                                else -> {
                                    if (isEnglish) "Authentication failed. Please check network and try again." 
                                    else "লগইন করা সম্ভব হয়নি। সংযোগ পরীক্ষা করুন এবং আবার চেষ্টা করুন।"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = !isLoading,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (isEnglish) "Login" else "লগইন করুন", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Footer navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isEnglish) "Don't have an account? " else "নতুন অ্যাকাউন্ট প্রয়োজন? ",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isEnglish) "Register Now" else "তৈরি করুন",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToRegister() }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CyberSecurityAnimation() {
    var rotation by remember { mutableStateOf(0f to 0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "ring_anim")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "rot"
    )

    Box(
        modifier = Modifier
            .size(170.dp)
            .graphicsLayer(
                rotationX = rotation.first,
                rotationY = rotation.second,
                cameraDistance = 12f
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    rotation = (rotation.first - dragAmount.y / 2f) to (rotation.second + dragAmount.x / 2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(170.dp).graphicsLayer(rotationZ = rotationAnim)) {
            drawCircle(
                color = PrimaryGreen.copy(alpha = 0.3f), 
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
        }
        Canvas(modifier = Modifier.size(136.dp).graphicsLayer(rotationZ = -rotationAnim)) {
            drawArc(color = PrimaryGreen.copy(alpha = 0.6f), startAngle = 0f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 5f))
            drawArc(color = PrimaryGreen.copy(alpha = 0.8f), startAngle = 180f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 5f))
        }
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, PrimaryGreen.copy(alpha = 0.15f), CircleShape), 
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = Icons.Default.Fingerprint, 
                 contentDescription = null, 
                 tint = PrimaryGreen, 
                 modifier = Modifier.size(44.dp)
             )
        }
        val laserTransition = rememberInfiniteTransition(label = "laser")
        val laserOffset by laserTransition.animateFloat(
            initialValue = -50f, 
            targetValue = 50f, 
            animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), 
            label = "laser"
        )
        Box(
            modifier = Modifier
                .size(150.dp, 3.dp)
                .offset(y = laserOffset.dp)
                .background(PrimaryGreen)
                .graphicsLayer(shadowElevation = 8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val supabase = Supabase.client
    val scope = rememberCoroutineScope()
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish

    Scaffold(
        topBar = {
            CompactTopBar(
                title = if (isEnglish) "Create Account" else "নতুন অ্যাকাউন্ট তৈরি করুন",
                onBack = onBack
            )
        },
        containerColor = BgLight,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            CyberSecurityAnimation()
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isEnglish) "Get Started" else "রেজিস্ট্রেশন ফরম",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Text(
                text = if (isEnglish) "Complete registration securely to join Halal Circle" else "আপনার সঠিক বিবরণ দিয়ে ফরমটি পূরণ করুন",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. First Name Field
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(if (isEnglish) "First Name" else "নামের প্রথম অংশ", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Person, 
                        contentDescription = null, 
                        tint = if (firstName.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Last Name Field
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(if (isEnglish) "Last Name" else "নামের শেষ অংশ", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Person, 
                        contentDescription = null, 
                        tint = if (lastName.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (isEnglish) "Email Address" else "ইমেইল এড্রেস", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Email, 
                        contentDescription = null, 
                        tint = if (email.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isEnglish) "Password" else "পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                            null,
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        tint = if (password.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(if (isEnglish) "Confirm Password" else "পাসওয়ার্ড নিশ্চিত করুন", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                            null,
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        tint = if (confirmPassword.isNotEmpty()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    text = it, 
                    color = Color.Red, 
                    fontSize = 12.sp, 
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Single page Register Form action button
            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = if (isEnglish) "Please fill all fields" else "সবগুলো ঘর সঠিকভাবে পূরণ করুন"
                        return@Button
                    }
                    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
                    if (!email.trim().matches(emailPattern.toRegex())) {
                        errorMessage = if (isEnglish) "Please enter a valid email address" else "দয়া করে একটি সঠিক ইমেইল এড্রেস প্রদান করুন"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = if (isEnglish) "Password must be at least 6 characters" else "পাসওয়ার্ড অন্তত ৬টি অক্ষরের হতে হবে"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = if (isEnglish) "Passwords do not match" else "পাসওয়ার্ড দুটি মেলেনি"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    val emailToRegister = email.trim()

                    scope.launch {
                        try {
                            supabase.auth.signUpWith(SupabaseEmail) {
                                this.email = emailToRegister
                                this.password = password
                                data = buildJsonObject {
                                    put("full_name", "$firstName $lastName")
                                }
                            }
                            
                            // Sign user in directly to obtain the valid token right after sign up
                            try {
                                supabase.auth.signInWith(SupabaseEmail) {
                                    this.email = emailToRegister
                                    this.password = password
                                }
                            } catch (signInErr: Exception) {
                                // Silent fallback if email verification is enabled on Supabase, 
                                // but if auto-signin works, it updates our local state correctly.
                            }

                            // Try saving user details in Supabase postgrest profile table if initialized
                            supabase.auth.currentUserOrNull()?.id?.let { userId ->
                                val initialProfile = UserProfile(
                                    id = userId,
                                    queue = 0,
                                    data1 = "$firstName $lastName"
                                )
                                try {
                                    supabase.postgrest["profiles"].insert(initialProfile)
                                } catch (dbError: Exception) {
                                    // Profile insert error is secondary, auth has succeeded
                                }
                            }

                            isLoading = false
                            onRegisterSuccess()
                        } catch (e: Exception) {
                            isLoading = false
                            val msg = e.localizedMessage ?: ""
                            errorMessage = when {
                                msg.contains("already", ignoreCase = true) || 
                                msg.contains("exists", ignoreCase = true) || 
                                msg.contains("registered", ignoreCase = true) || 
                                msg.contains("conflict", ignoreCase = true) ||
                                msg.contains("422", ignoreCase = true) -> {
                                    if (isEnglish) "This email address has already been registered." 
                                    else "এই ইমেইল এড্রেসটি পূর্বে রেজিস্টার করা হয়েছে।"
                                }
                                else -> {
                                    if (isEnglish) "Registration failed. Try checking details and connection." 
                                    else "রেজিস্ট্রেশন সম্পন্ন করা সম্ভব হয়নি। সংযোগ তথ্য পরীক্ষা করে পুনরায় চেষ্টা করুন।"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = !isLoading,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (isEnglish) "Register" else "রেজিস্ট্রেশন করুন", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isEnglish) "Already have an account? " else "ইতিমধ্যে একাউন্ট আছে? ",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isEnglish) "Log In" else "লগইন করুন",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}
