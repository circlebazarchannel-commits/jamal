package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

data class Mosque(val id: Long, val name: String, val lat: Double, val lon: Double, val distance: Double, val address: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueFinderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapCenter by remember { mutableStateOf<LatLng?>(null) }
    var mosques by remember { mutableStateOf<List<Mosque>>(emptyList()) }
    var selectedMosque by remember { mutableStateOf<Mosque?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocation(context) { loc -> 
                userLocation = loc
                mapCenter = loc
                cameraPositionState.position = CameraPosition.fromLatLngZoom(loc, 14f)
                fetchMosques(loc.latitude, loc.longitude, context) {
                    mosques = it
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mosque Finder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = userLocation != null),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                mosques.forEach { mosque ->
                    Marker(
                        state = MarkerState(position = LatLng(mosque.lat, mosque.lon)),
                        title = mosque.name,
                        snippet = mosque.address,
                        onClick = {
                            selectedMosque = mosque
                            false
                        }
                    )
                }
            }

            // Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search location or mosque...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            coroutineScope.launch {
                                isLoading = true
                                val loc = searchLocation(context, searchQuery)
                                if (loc != null) {
                                    mapCenter = loc
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 14f))
                                    fetchMosques(loc.latitude, loc.longitude, context) {
                                        mosques = it
                                        isLoading = false
                                    }
                                } else {
                                    isLoading = false
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryGreen)
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryGreen)
            }

            // Selected Mosque Info Bottom Sheet
            selectedMosque?.let { mosque ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(mosque.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(mosque.address, fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Distance: ${String.format(Locale.US, "%.2f", mosque.distance)} km", fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${mosque.lat},${mosque.lon}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${mosque.lat},${mosque.lon}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigate")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocationResult: (LatLng) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationResult(LatLng(location.latitude, location.longitude))
        } else {
            // Request fresh location
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        onLocationResult(LatLng(it.latitude, it.longitude))
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
    }
}

private suspend fun searchLocation(context: Context, query: String): LatLng? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(query, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            return@withContext LatLng(addr.latitude, addr.longitude)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}

private fun fetchMosques(lat: Double, lon: Double, context: Context, onResult: (List<Mosque>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val radius = 5000 // 5km
            val query = "[out:json];node(around:$radius,$lat,$lon)[amenity=place_of_worship][religion=muslim];out;"
            val url = URL("https://overpass-api.de/api/interpreter?data=${URLEncoder.encode(query, "UTF-8")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val elements = jsonObject.getJSONArray("elements")
                val list = mutableListOf<Mosque>()
                
                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    val id = element.getLong("id")
                    val mLat = element.getDouble("lat")
                    val mLon = element.getDouble("lon")
                    val tags = if (element.has("tags")) element.getJSONObject("tags") else null
                    
                    var name = "Unknown Mosque"
                    var address = ""
                    if (tags != null) {
                        if (tags.has("name:bn")) name = tags.getString("name:bn")
                        else if (tags.has("name")) name = tags.getString("name")
                        else if (tags.has("name:en")) name = tags.getString("name:en")
                        
                        if (tags.has("addr:full")) address = tags.getString("addr:full")
                        else if (tags.has("addr:street")) address = tags.getString("addr:street")
                    }
                    
                    val distance = calculateDistance(lat, lon, mLat, mLon)
                    
                    list.add(Mosque(id, name, mLat, mLon, distance, address.ifEmpty { "Address not available" }))
                }
                
                // Sort by distance
                list.sortBy { it.distance }
                withContext(Dispatchers.Main) {
                    onResult(list)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Radius of earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
