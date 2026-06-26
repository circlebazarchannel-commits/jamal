package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.ui.theme.PrimaryGreen

@Composable
fun MapLocationPicker(
    initialLocation: LatLng = LatLng(23.8103, 90.4125), // Dhaka
    onLocationSelected: (LatLng, String) -> Unit,
    onDismiss: () -> Unit
) {
    var markerPosition by remember { mutableStateOf(initialLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { markerPosition = it },
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            Marker(
                state = MarkerState(position = markerPosition),
                title = "Selected Location",
                draggable = true
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "পিন ড্রপ করে এলাকা নির্বাচন করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lat: ${String.format("%.4f", markerPosition.latitude)}, Lng: ${String.format("%.4f", markerPosition.longitude)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("বাতিল")
                        }
                        Button(
                            onClick = {
                                onLocationSelected(markerPosition, "Custom Location")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("নিশ্চিত করুন")
                        }
                    }
                }
            }
        }
    }
}
