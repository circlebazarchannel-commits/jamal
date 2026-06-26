package com.example

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PrayerViewModel
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocationSearchResult(val name: String, val lat: Double, val lng: Double)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationSelectionScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val isEng = com.example.viewmodel.GlobalLanguage.isEnglish

    // Setup Accompanist Location Permissions State
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted && state.isAutoLocation) {
            viewModel.startLocationUpdates(context)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            isSearching = true
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(searchQuery, 8)
                    val results = addresses?.mapNotNull { address ->
                        val name = address.getAddressLine(0) ?: listOfNotNull(address.featureName, address.locality, address.adminArea, address.countryName).distinct().joinToString(", ")
                        if (name.isNotEmpty() && address.hasLatitude() && address.hasLongitude()) {
                            LocationSearchResult(name, address.latitude, address.longitude)
                        } else null
                    } ?: emptyList()
                    searchResults = results
                } catch (e: Exception) {
                    searchResults = emptyList()
                }
            }
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isEng) "Set Location" else "লোকেশন সেট করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                }
                
                // Unified Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 8.dp),
                    placeholder = { Text(if (isEng) "Search city, country, or place..." else "যেকোনো শহর, দেশ বা স্থান খুঁজুন...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextGray
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextGray
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedLabelColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (searchQuery.length > 2) {
            // Search Results Overlay
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (isEng) "No location found" else "কোনো স্থান পাওয়া যায়নি", color = TextGray)
                        }
                    }
                } else {
                    items(searchResults) { result ->
                        Card(
                            onClick = {
                                val shortName = result.name.split(",").firstOrNull() ?: result.name
                                viewModel.setLocationManually(context, shortName, result.lat, result.lng)
                                searchQuery = "" // Clear search and go back to dashboard mode
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(PrimaryGreen.copy(alpha=0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryGreen)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = result.name.split(",").firstOrNull() ?: result.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = result.name,
                                        color = TextGray,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Main Dashboard View (Map + Controls)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Interactive Dynamic Google Map Container
                item {
                    val mapCenter = LatLng(state.latitude, state.longitude)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(mapCenter, 11f)
                    }

                    // Fluidly animate camera when selection modifications happen
                    LaunchedEffect(state.latitude, state.longitude) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(mapCenter, 11f)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val playServicesAvailable = remember {
                                try {
                                    val availability = GoogleApiAvailability.getInstance()
                                    availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
                                } catch (e: Throwable) {
                                    false
                                }
                            }

                            if (playServicesAvailable) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    onMapClick = { latLng ->
                                        // Clicking on the map places a pin and updates location manually
                                        viewModel.setLocationManually(
                                            context = context,
                                            districtName = if(isEng) "Selected Location" else "নির্দিষ্ট এলাকা",
                                            lat = latLng.latitude,
                                            lng = latLng.longitude
                                        )
                                    },
                                    uiSettings = MapUiSettings(
                                        myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted,
                                        zoomControlsEnabled = true,
                                        compassEnabled = true,
                                        mapToolbarEnabled = false
                                    ),
                                    properties = MapProperties(
                                        isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
                                    )
                                ) {
                                    Marker(
                                        state = MarkerState(position = mapCenter),
                                        title = state.locationName,
                                        snippet = if (isEng) "Current Selected Area" else "বর্তমান নির্বাচিত জায়গা"
                                    )
                                }
                            } else {
                                // Beautiful stylized offline fallback view
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (isEng) "Interactive Map View" else "ইন্টারেক্টিভ ম্যাপ ভিউ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${state.locationName} (${String.format(java.util.Locale.US, "%.4f", state.latitude)}, ${String.format(java.util.Locale.US, "%.4f", state.longitude)})",
                                            fontSize = 12.sp,
                                            color = TextGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isEng) "Play Services unavailable" else "প্লে সার্ভিস পাওয়া যায়নি",
                                            fontSize = 10.sp,
                                            color = TextGray.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Small overlay badge inside map showing coordinates
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Lat: ${String.format(java.util.Locale.US, "%.4f", state.latitude)}, Lng: ${String.format(java.util.Locale.US, "%.4f", state.longitude)}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Current Location Badge & Info Bar
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).background(PrimaryGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryGreen)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEng) "Selected Location" else "নির্বাচিত স্থান",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                                Text(
                                    text = state.locationName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                            }
                            if (state.isAutoLocation) {
                                Surface(
                                    color = PrimaryGreen.copy(0.1f),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "GPS",
                                        color = PrimaryGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // OPTION 1: GPS Automatic Location Detection
                item {
                    val hasGpsPermission = locationPermissionsState.allPermissionsGranted
                    Card(
                        onClick = {
                            if (hasGpsPermission) {
                                viewModel.setAutoLocation(context)
                            } else {
                                viewModel.setAutoLocation(context) // triggers flag
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isAutoLocation) PrimaryGreen.copy(alpha = 0.05f) else Color.White
                        ),
                        border = if (state.isAutoLocation) BorderStroke(1.5.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (state.isAutoLocation) PrimaryGreen.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = if (state.isAutoLocation) PrimaryGreen else TextGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEng) "Auto Detect GPS Location" else "জিপিএস লোকেশন সনাক্তকরণ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hasGpsPermission) {
                                        if(isEng) "Use GPS to track current location automatically" else "জিপিএস ব্যবহার করে বর্তমান অবস্থান অটো খুঁজুন"
                                    } else {
                                        if(isEng) "Grant permission to use auto detection" else "লোকেশন পারমিশন দিয়ে অটো সনাক্ত করুন"
                                    },
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }

                            if (state.isAutoLocation) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
