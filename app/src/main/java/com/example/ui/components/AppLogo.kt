package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.viewmodel.SettingsViewModel

@Composable
fun DynamicAppLogo(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val customLogoPath by settingsViewModel.customLogoPath.collectAsState()

    if (customLogoPath.isNotEmpty()) {
        AsyncImage(
            model = customLogoPath,
            contentDescription = "App Logo",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo_custom),
            contentDescription = "App Logo",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}
