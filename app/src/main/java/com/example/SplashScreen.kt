package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    
    // Core animation values for Logo
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutQuad),
        label = "logo_alpha"
    )

    // Pulse/Glow animation for the logo container
    val infiniteTransition = rememberInfiniteTransition(label = "infinite_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    // Brand text fade/slide animations
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 600, easing = EaseOutCubic),
        label = "text_alpha"
    )
    
    val textOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 800, delayMillis = 600, easing = EaseOutCubic),
        label = "text_offset"
    )

    val footerAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.8f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 1000, easing = EaseOut),
        label = "footer_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // Keep splash screen visible for 2.4 seconds to make it snappy but readable
        delay(2400)
        onFinished()
    }

    // Pure White Background to blend seamlessly with the system launch screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant Animated App Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scaleAnim * pulseGlow)
                    .alpha(alphaAnim),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_custom),
                    contentDescription = "Halal Circle Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name & subtitle container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset)
                    .alpha(textAlpha)
            ) {
                // App Name with deep premium green color
                Text(
                    text = "Halal Circle",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332), // Deep, elegant Islamic dark green
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Pure elegant spiritual subtext in neutral gray
                Text(
                    text = "আপনার দ্বীনি সার্কেল ও আধ্যাত্মিক সঙ্গী",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569), // Beautiful slate gray
                    textAlign = TextAlign.Center
                )
            }
        }

        // Facebook style branding "from CIRCLE" centered at the bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(footerAlpha)
        ) {
            Text(
                text = "from",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8), // slate-400
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Small green circle indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF15803D), CircleShape) // Primary Green Circle
                )
                Text(
                    text = "CIRCLE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B), // slate-800
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
