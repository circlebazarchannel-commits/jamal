package com.example.viewmodel

data class District(val name: String, val englishName: String, val lat: Double, val lng: Double)

val bangladeshDistricts = listOf(
    // Dhaka Division
    District("ঢাকা", "Dhaka", 23.8103, 90.4125),
    District("গাজীপুর", "Gazipur", 23.9999, 90.4203),
    District("নারায়ণগঞ্জ", "Narayanganj", 23.6238, 90.5000),
    District("টাঙ্গাইল", "Tangail", 24.2513, 89.9231),
    District("ফরিদপুর", "Faridpur", 23.6071, 89.8429),
    District("মুন্সীগঞ্জ", "Munshiganj", 23.5422, 90.5305),
    District("মানিকগঞ্জ", "Manikganj", 23.8644, 90.0047),
    District("নরসিংদী", "Narsingdi", 23.9322, 90.7154),
    District("রাজবাড়ী", "Rajbari", 23.7574, 89.6444),
    District("শরীয়তপুর", "Shariatpur", 23.2423, 90.3542),
    District("মাদারীপুর", "Madaripur", 23.1641, 90.1896),
    District("গোপালগঞ্জ", "Gopalganj", 23.0050, 89.8267),
    District("কিশোরগঞ্জ", "Kishoreganj", 24.4349, 90.7851),

    // Chittagong Division
    District("চট্টগ্রাম", "Chittagong", 22.3569, 91.7832),
    District("কুমিল্লা", "Comilla", 23.4607, 91.1809),
    District("নোয়াখালী", "Noakhali", 22.8696, 91.0994),
    District("কক্সবাজার", "Cox's Bazar", 21.4272, 92.0058),
    District("ব্রাহ্মণবাড়িয়া", "Brahmanbaria", 23.9571, 91.1119),
    District("চাঁদপুর", "Chandpur", 23.2321, 90.6631),
    District("ফেনী", "Feni", 23.0116, 91.3963),
    District("লক্ষ্মীপুর", "Lakshmipur", 22.9426, 90.8417),
    District("রাঙ্গামাটি", "Rangamati", 22.6559, 92.1794),
    District("বান্দরবান", "Bandarban", 22.1953, 92.2184),
    District("খাগড়াছড়ি", "Khagrachari", 23.1115, 91.9964),

    // Rajshahi Division
    District("রাজশাহী", "Rajshahi", 24.3636, 88.6241),
    District("বগুড়া", "Bogra", 24.8481, 89.3730),
    District("পাবনা", "Pabna", 24.0158, 89.2505),
    District("নওগাঁ", "Naogaon", 24.7936, 88.9318),
    District("নাটোর", "Natore", 24.4201, 88.9818),
    District("জয়পুরহাট", "Joypurhat", 25.1010, 89.0267),
    District("চাঁপাইনবাবগঞ্জ", "Chapainawabganj", 24.5965, 88.2718),
    District("সিরাজগঞ্জ", "Sirajganj", 24.4577, 89.7080),

    // Khulna Division
    District("খুলনা", "Khulna", 22.8456, 89.5403),
    District("কুষ্টিয়া", "Kushtia", 23.9013, 89.1204),
    District("যশোর", "Jessore", 23.1664, 89.2081),
    District("বাগেরহাট", "Bagerhat", 22.6516, 89.7859),
    District("সাতক্ষীরা", "Satkhira", 22.7185, 89.0705),
    District("ঝিনাইদহ", "Jhenaidah", 23.5450, 89.1726),
    District("মাগুরা", "Magura", 23.4873, 89.4199),
    District("মেহেরপুর", "Meherpur", 23.7622, 88.6318),
    District("নড়াইল", "Narail", 23.1725, 89.5126),
    District("চুয়াডাঙ্গা", "Chuadanga", 23.6427, 88.8525),

    // Sylhet Division
    District("সিলেট", "Sylhet", 24.8949, 91.8687),
    District("মৌলভীবাজার", "Moulvibazar", 24.4829, 91.7705),
    District("হবিগঞ্জ", "Habiganj", 24.3749, 91.4111),
    District("সুনামগঞ্জ", "Sunamganj", 25.0658, 91.3950),

    // Barisal Division
    District("বরিশাল", "Barisal", 22.7010, 90.3535),
    District("পটুয়াখালী", "Patuakhali", 22.3597, 90.3298),
    District("ভোলা", "Bhola", 22.6859, 90.6481),
    District("পিরোজপুর", "Pirojpur", 22.5781, 90.0041),
    District("বরগুনা", "Barguna", 22.1557, 90.1257),
    District("ঝালকাঠি", "Jhalokati", 22.6419, 90.1983),

    // Rangpur Division
    District("রংপুর", "Rangpur", 25.7439, 89.2752),
    District("দিনাজপুর", "Dinajpur", 25.6217, 88.6470),
    District("গাইবান্ধা", "Gaibandha", 25.3287, 89.5422),
    District("কুড়িগ্রাম", "Kurigram", 25.8054, 89.6361),
    District("লালমনিরহাট", "Lalmonirhat", 25.9122, 89.4489),
    District("নীলফামারী", "Nilphamari", 25.9317, 88.8560),
    District("পঞ্চগড়", "Panchagarh", 26.3331, 88.5614),
    District("ঠাকুরগাঁও", "Thakurgaon", 26.0333, 88.4667),

    // Mymensingh Division
    District("ময়মনসিংহ", "Mymensingh", 24.7471, 90.4203),
    District("জামালপুর", "Jamalpur", 24.9375, 89.9377),
    District("নেত্রকোণা", "Netrokona", 24.8781, 90.7278),
    District("শেরপুর", "Sherpur", 25.0188, 90.0175)
)

val indiaDistricts = listOf(
    District("কলকাতা", "Kolkata", 22.5726, 88.3639),
    District("দিল্লি", "New Delhi", 28.6139, 77.2090),
    District("মুম্বাই", "Mumbai", 19.0760, 72.8777),
    District("চেন্নাই", "Chennai", 13.0827, 80.2707),
    District("বেঙ্গালুরু", "Bengaluru", 12.9716, 77.5946),
    District("হায়দ্রাবাদ", "Hyderabad", 17.3850, 78.4867),
    District("আহমেদাবাদ", "Ahmedabad", 23.0225, 72.5714),
    District("পুনে", "Pune", 18.5204, 73.8567),
    District("কানপুর", "Kanpur", 26.4499, 80.3319),
    District("লখনউ", "Lucknow", 26.8467, 80.9462)
)

val pakistanDistricts = listOf(
    District("করাচি", "Karachi", 24.8607, 67.0011),
    District("লাহোর", "Lahore", 31.5204, 74.3587),
    District("ইসলামাবাদ", "Islamabad", 33.6844, 73.0479),
    District("রাওয়ালপিন্ডি", "Rawalpindi", 33.5984, 73.0441),
    District("পেশোয়ার", "Peshawar", 34.0151, 71.5249),
    District("মুলতান", "Multan", 30.1978, 71.4697),
    District("ফয়সালাবাদ", "Faisalabad", 31.4504, 73.1350),
    District("কোয়েটা", "Quetta", 30.1798, 66.9750)
)

val saudiArabiaDistricts = listOf(
    District("মক্কা", "Makkah", 21.3891, 39.8579),
    District("মদিনা", "Madinah", 24.5247, 39.5692),
    District("রিয়াদ", "Riyadh", 24.7136, 46.6753),
    District("জেদ্দা", "Jeddah", 21.4858, 39.1925),
    District("দাম্মাম", "Dammam", 26.4207, 50.0888),
    District("তায়েফ", "Taif", 21.4373, 40.5127),
    District("তাবুক", "Tabuk", 28.3835, 36.5662)
)

val uaeDistricts = listOf(
    District("দুবাই", "Dubai", 25.2048, 55.2708),
    District("আবু ধাবি", "Abu Dhabi", 24.4539, 54.3773),
    District("শারজাহ", "Sharjah", 25.3573, 55.3911),
    District("আজমান", "Ajman", 25.4052, 55.5136),
    District("রাস আল খাইমাহ", "Ras Al Khaimah", 25.7895, 55.9432),
    District("ফুজাইরাহ", "Fujairah", 25.1288, 56.3265)
)

val ukDistricts = listOf(
    District("লন্ডন", "London", 51.5074, -0.1278),
    District("বার্মিংহাম", "Birmingham", 52.4862, -1.8904),
    District("ম্যানচেস্টার", "Manchester", 53.4808, -2.2426),
    District("লিভারপুল", "Liverpool", 53.4084, -2.9916),
    District("লিডস", "Leeds", 53.8008, -1.5491),
    District("গ্লাসগো", "Glasgow", 55.8642, -4.2518),
    District("এডিনবরা", "Edinburgh", 55.9533, -3.1883)
)

val usaDistricts = listOf(
    District("নিউ ইয়র্ক", "New York", 40.7128, -74.0060),
    District("লস এঞ্জেলেস", "Los Angeles", 34.0522, -118.2437),
    District("শিকাগো", "Chicago", 41.8781, -87.6298),
    District("হিউস্টন", "Houston", 29.7604, -95.3698),
    District("ফিনিক্স", "Phoenix", 33.4484, -112.0740),
    District("ফিলাডেলফিয়া", "Philadelphia", 39.9526, -75.1652),
    District("ডালাস", "Dallas", 32.7767, -96.7970)
)

val malaysiaDistricts = listOf(
    District("কুয়ালালামপুর", "Kuala Lumpur", 3.1390, 101.6869),
    District("জর্জ টাউন", "George Town", 5.4164, 100.3327),
    District("জোহর বাহরু", "Johor Bahru", 1.4927, 103.7414),
    District("ইপোহ", "Ipoh", 4.5975, 101.0901),
    District("শাহ আলম", "Shah Alam", 3.0738, 101.5183),
    District("মেলাকা", "Malacca", 2.1896, 102.2501)
)

val indonesiaDistricts = listOf(
    District("জাকার্তা", "Jakarta", -6.2088, 106.8456),
    District("সুরাবায়া", "Surabaya", -7.2504, 112.7688),
    District("বান্দুং", "Bandung", -6.9175, 107.6191),
    District("মেদান", "Medan", 3.5952, 98.6722),
    District("সেমারাং", "Semarang", -6.9667, 110.4167),
    District("মাকাসার", "Makassar", -5.1476, 119.4327)
)

fun getDistrictsForCountry(countryCode: String): List<District> {
    return when (countryCode) {
        "BD" -> bangladeshDistricts
        "IN" -> indiaDistricts
        "PK" -> pakistanDistricts
        "SA" -> saudiArabiaDistricts
        "AE" -> uaeDistricts
        "GB" -> ukDistricts
        "US" -> usaDistricts
        "MY" -> malaysiaDistricts
        "ID" -> indonesiaDistricts
        else -> bangladeshDistricts
    }
}

val countries = listOf(
    "বাংলাদেশ",
    "ভারত",
    "پاکستان",
    "Saudi Arabia",
    "United Arab Emirates",
    "Malaysia",
    "Indonesia",
    "United Kingdom",
    "United States"
)
