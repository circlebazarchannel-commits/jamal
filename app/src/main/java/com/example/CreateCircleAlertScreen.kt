package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.example.model.CircleAlert
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.viewmodel.GlobalLanguage
import com.example.database.TrackerDatabase
import com.example.database.NotificationEntity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleAlertScreen(
    savedLocation: String, // from prayer settings
    onBack: () -> Unit,
    onSubmit: (CircleAlert) -> Unit
) {
    BackHandler(enabled = true) {
        onBack()
    }
    val context = LocalContext.current
    val isEnglish = GlobalLanguage.isEnglish
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var alertCategory by remember { mutableStateOf(if (isEnglish) "Lost Child" else "হারিয়ে যাওয়া শিশু") }
    var contactNumber by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(if (isEnglish) "Bangladesh" else "বাংলাদেশ") }
    var location by remember { mutableStateOf(if (savedLocation.isEmpty()) "Dhaka" else savedLocation) }
    
    val alertCategories = if (isEnglish) {
        listOf("Lost Child", "Lost Person", "Blood Required", "Accident", "Emergency Help", "Criminal Activity", "Other")
    } else {
        listOf("হারিয়ে যাওয়া শিশু", "নিখোঁজ ব্যক্তি", "রক্তের প্রয়োজন", "দুর্ঘটনা", "জরুরি সাহায্য", "অপরাধমূলক কার্যক্রম", "অন্যান্য")
    }

    val districts = listOf(
        "All Bangladesh", "Dhaka", "Faridpur", "Gazipur", "Gopalganj", "Kishoreganj", "Madaripur", "Manikganj", "Munshiganj", "Narayanganj", "Narsingdi", "Rajbari", "Shariatpur", "Tangail",
        "Barishal", "Barguna", "Bhola", "Jhalokati", "Patuakhali", "Pirojpur",
        "Chattogram", "Bandarban", "Brahmanbaria", "Chandpur", "Cumilla", "Cox's Bazar", "Feni", "Khagrachhari", "Lakshmipur", "Noakhali", "Rangamati",
        "Khulna", "Bagerhat", "Chuadanga", "Jashore", "Jhenaidah", "Kushtia", "Magura", "Meherpur", "Narail", "Satkhira",
        "Rajshahi", "Bogura", "Joypurhat", "Naogaon", "Natore", "Chapai Nawabganj", "Pabna", "Sirajganj",
        "Rangpur", "Dinajpur", "Gaibandha", "Kurigram", "Lalmonirhat", "Nilphamari", "Panchagarh", "Thakurgaon",
        "Sylhet", "Habiganj", "Moulvibazar", "Sunamganj",
        "Mymensingh", "Jamalpur", "Netrokona", "Sherpur"
    )

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDistrictMenu by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("photo") }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            mediaUri = it
            mediaType = "photo"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Create Circle Alert" else "সার্কেল অ্যালার্ট তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isEnglish) "Alert Title (Short & Clear)" else "অ্যালার্ট সিরনাম (সংক্ষিপ্ত ও স্পষ্ট)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selection
            Box {
                OutlinedTextField(
                    value = alertCategory,
                    onValueChange = {},
                    label = { Text(if (isEnglish) "Select Alert Category" else "অ্যালার্ট ক্যাটাগরি সিলেক্ট করুন") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showCategoryMenu = true }
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                ) {
                    alertCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                alertCategory = cat
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
            
            // District Selection
            Box {
                OutlinedTextField(
                    value = location,
                    onValueChange = {},
                    label = { Text(if (isEnglish) "Target Area / District" else "লক্ষ্য এলাকা / জেলা") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDistrictMenu = true }
                )
                DropdownMenu(
                    expanded = showDistrictMenu,
                    onDismissRequest = { showDistrictMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White).heightIn(max = 300.dp)
                ) {
                    districts.forEach { dist ->
                        DropdownMenuItem(
                            text = { Text(dist) },
                            onClick = {
                                location = dist
                                showDistrictMenu = false
                            }
                        )
                    }
                }
            }

            // Map Picker Trigger
            Button(
                onClick = { showMapPicker = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9FAFB), contentColor = Color(0xFF374151)),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEnglish) "Select specific area via Map" else "ম্যাপের মাধ্যমে নির্দিষ্ট এলাকা নির্বাচন", fontSize = 14.sp)
            }

            if (showMapPicker) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showMapPicker = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    MapLocationPicker(
                        onLocationSelected = { latlng, label ->
                            location = "${String.format("%.4f", latlng.latitude)}, ${String.format("%.4f", latlng.longitude)}"
                            showMapPicker = false
                        },
                        onDismiss = { showMapPicker = false }
                    )
                }
            }

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text(if (isEnglish) "Emergency Contact Phone" else "জরুরি যোগাযোগ ফোন") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isEnglish) "Attach Photo" else "ছবি সংযুক্ত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mediaUri != null && mediaType == "photo") PrimaryGreen else BgLight, contentColor = if (mediaUri != null && mediaType == "photo") Color.White else Color.Gray)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Photo" else "ছবি")
                }
            }

            if (mediaUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isEnglish) "Photo Selected" else "ছবি নির্বাচিত হয়েছে",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val isValid = title.trim().isNotEmpty() && contactNumber.trim().isNotEmpty() && mediaUri != null
                    
                    if (isValid) {
                        val docId = UUID.randomUUID().toString()
                        val alert = CircleAlert(
                            docId = docId,
                            title = title,
                            description = "", // Removed description as per request
                            mediaUri = mediaUri?.toString() ?: "",
                            mediaType = mediaType,
                            contactNumber = contactNumber,
                            country = country,
                            location = location,
                            timestamp = System.currentTimeMillis(),
                            status = "PENDING"
                        )
                        
                        val currentUser = Supabase.client.auth.currentUserOrNull()
                        
                        // Push to Firestore
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val videoData = hashMapOf(
                            "userId" to (currentUser?.id ?: "unknown"),
                            "author" to (currentUser?.userMetadata?.get("full_name")?.toString() ?: "Unknown Author"),
                            "title" to title,
                            "description" to "",
                            "timestamp" to System.currentTimeMillis(),
                            "status" to "PENDING",
                            "mediaType" to mediaType,
                            "contactNumber" to contactNumber,
                            "country" to country,
                            "location" to location,
                            "isCircleAlert" to true,
                            "alertCategory" to alertCategory
                        )
                        firestore.collection("videos").document(docId).set(videoData)
                        
                        if (mediaUri != null) {
                            val intent = android.content.Intent(context, AlertUploadService::class.java).apply {
                                putExtra("mediaUri", mediaUri.toString())
                                putExtra("mediaType", mediaType)
                                putExtra("title", title)
                                putExtra("description", "")
                                putExtra("contactNumber", contactNumber)
                                putExtra("location", location)
                                putExtra("docId", docId)
                            }
                            context.startService(intent)
                        }
                        
                        onSubmit(alert)
                    } else {
                        val errorMessage = when {
                            title.trim().isEmpty() -> if(isEnglish) "Enter a title" else "একটি শিরনাম লিখুন"
                            contactNumber.trim().isEmpty() -> if(isEnglish) "Enter contact number" else "কন্টাক্ট নম্বর দিন"
                            mediaUri == null -> if(isEnglish) "Attach a photo" else "একটি ছবি দিন"
                            else -> if(isEnglish) "Missing fields" else "তথ্য অসম্পূর্ণ"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red) // High alert color
            ) {
                Text(if (isEnglish) "Broadcast Real-time Alert 🚨" else "অ্যালার্ট ব্রডকাস্ট করুন 🚨", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
