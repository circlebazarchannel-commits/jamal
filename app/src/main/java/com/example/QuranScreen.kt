package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// --- Data Models ---
data class Surah(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val nameBn: String,
    val meaningBn: String,
    val revelation: String, // "মক্কী" or "মাদানী"
    val totalVerses: Int,
    val verses: List<Verse> = emptyList()
)

data class Verse(
    val number: Int,
    val arabic: String,
    val pronunciation: String,
    val translation: String
)

// Inline simple Bengali conversion utilities
fun Int.toBnString(): String {
    val bdigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val str = this.toString()
    val sb = StringBuilder()
    for (char in str) {
        if (char in '0'..'9') {
            sb.append(bdigits[char - '0'])
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}

fun String.toBnString(): String {
    val bdigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val sb = StringBuilder()
    for (char in this) {
        if (char in '0'..'9') {
            sb.append(bdigits[char - '0'])
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}

// Global persistence manager for bookmarked Ayahs via SharedPrefs
object QuranBookmarkManager {
    private const val PREFS_NAME = "quran_bookmarks"
    
    fun toggleBookmark(context: Context, surahId: Int, verseNo: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${surahId}_${verseNo}"
        val isBookmarked = prefs.getBoolean(key, false)
        prefs.edit().putBoolean(key, !isBookmarked).apply()
        return !isBookmarked
    }

    fun isBookmarked(context: Context, surahId: Int, verseNo: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${surahId}_${verseNo}"
        return prefs.getBoolean(key, false)
    }

    fun toggleSurahBookmark(context: Context, surahId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "surah_${surahId}"
        val isBookmarked = prefs.getBoolean(key, false)
        prefs.edit().putBoolean(key, !isBookmarked).apply()
        return !isBookmarked
    }

    fun isSurahBookmarked(context: Context, surahId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "surah_${surahId}"
        return prefs.getBoolean(key, false)
    }
}

val globalAyatCounts = listOf(
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
    89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
    12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
    30, 20, 15, 15, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
)

fun getGlobalVerseNumber(surahId: Int, verseNumber: Int): Int {
    var count = 0
    for (i in 0 until (surahId - 1)) {
        if (i < globalAyatCounts.size) {
            count += globalAyatCounts[i]
        }
    }
    return count + verseNumber
}

// Generate the 114 Surahs list
fun getSurahList(): List<Surah> {
    val medinanIds = setOf(2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 76, 98, 110)
    
    val ayatCounts = globalAyatCounts

    val namesEn = listOf(
        "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa'", "Al-Ma'idah", "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
        "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra'", "Al-Kahf", "Maryam", "Ta-Ha",
        "Al-Anbiya'", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan", "Ash-Shu'ara'", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba'", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
        "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
        "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadilah", "Al-Hashr", "Al-Mumtahanah",
        "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
        "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba'", "An-Nazi'at", "Abasa",
        "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Lail", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-'Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat",
        "Al-Qari'ah", "At-Takathur", "Al-'Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawtar", "Al-Kafirun", "An-Nasr",
        "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
    )

    val namesAr = listOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبإ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبإ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )

    val namesBn = listOf(
        "আল-ফাতিহা", "আল-বাকারাহ", "আলি ইমরান", "আন-নিসা", "আল-মা’ইদাহ", "আল-আন’আম", "আল-আ’রাফ", "আল-আনফাল", "আত্ব-তাওবাহ", "ইউনুস",
        "হূদ", "ইউসুফ", "আর-রাদ", "ইব্রাহীম", "আল-হিজর", "আন-নাহল", "আল-ইসরা", "আল-কাহফ", "মারইয়াম", "ত্বোয়া-হা",
        "আল-আম্বিয়া", "আল-হজ্জ", "আল-মুমিনুন", "আন-নূর", "আল-ফুরকান", "আশ-শু’আরা", "আন-নামল", "আল-কাসাস", "আল-আনকাবুত", "আর-রূম",
        "লুকমান", "আস-সাজদাহ", "আল-আহযাব", "সাবা", "ফাতির", "ইয়াসীন", "আস-সাফফাত", "সোয়াদ", "আজ-জুমার", "গাফির",
        "ফুসসিলাত", "আশ-শূরা", "আজ-জুুখরুফ", "আদ-দুখান", "আল-জাছিয়াহ", "আল-আহকাফ", "মুহাম্মদ", "আল-ফাতাহ", "আল-হুজুরাত", "কাফ",
        "আয-যারিয়াত", "আত-তূর", "আন-নাজম", "আল-কামার", "আর-রাহমান", "আল-ওয়াকিয়াহ", "আল-হাদীদ", "আল-মুজাদিলাহ", "আল-হাশর", "আল-মুমতাহানাহ",
        "আস-সাফ", "আল-জুমুআহ", "আল-মুনাফিকুন", "আত-তাগাবুন", "আত-তালাক", "আত-তাহরীম", "আল-মূলক", "আল-কালাম", "আল-হাক্কাহ", "আল-মা’আরিজ",
        "নূহ", "আল-জীন", "আল-মুযযাম্মিল", "আল-মুদ্দাসসির", "আল-কিয়ামাহ", "আল-ইনসান", "আল-মুরসালাত", "আন-নাবা", "আন-নাযিয়াত", "আবাসা",
        "আদ-তাকবীর", "আল-ইনফিতার", "আল-মুতাফফিফিন", "আল-ইনশিকাক", "আল-বুরূজ", "আত্ব-তারিক", "আল-আ’লা", "আল-গাশিয়াহ", "আল-ফজর", "আল-বালাদ",
        "আশ-শামস", "আল-লািল", "আদ-দুহা", "আশ-শারহ", "আত-তীন", "আল-আলাক", "আল-কদর", "আল-বাইয়্যিনাহ", "আয-যালযালাহ", "আল-আদিয়াত",
        "আল-কারিয়াহ", "আত-তাকাসুর", "আল-আসর", "আল-হুমাযাহ", "আল-ফীল", "কুরাইশ", "আল-মা’উন", "আল-কাওসার", "আল-কাফিরুন", "আন-নসর",
        "আল-মাসাদ", "আল-ইখলাস", "আল-ফালাক", "আন-নাস"
    )

    val meaningsBn = listOf(
        "সূচনা", "গাভী", "ইমরানের পরিবার", "নারী", "খাদ্য পরিবেশিত টেবিল", "গৃহপালিত পশু", "উঁচু স্থানসমূহ", "যুদ্ধলব্ধ সম্পদ", "অনুশোচনা", "নবী ইউনুস",
        "নবী হূদ", "নবী ইউসুফ", "বজ্রপাত", "নবী ইব্রাহীম", "পাথুরে পাহাড়", "মৌমাছি", "রাত্রিকালীন ভ্রমণ", "গুহা", "মারইয়াম (ঈসা আঃ এর মা)", "ত্বোয়া-হা",
        "নবীগণ", "হজ্জ", "বিশ্বাসীগণ", "আলো", "সত্য-মিথ্যার পার্থক্যকারী", "কবিগণ", "পিপীলিকা", " কাহিনী", "মাকড়সা", "রোমান সাম্রাজ্য",
        "জ্ঞানী লোক লুকমান", "সিজদা", "জোটসমূহ", "রানী বিলকিসের রাজত্ব", "আদি স্রষ্টা", "ইয়াসীন", "সারিবদ্ধভাবে দাঁড়ানো", "সোয়াদ", "দলসমূহ", "ক্ষماশীল",
        "বিস্তারিত বিবরণ", "পরামর্শ", "সোনার গহনা", "ধোঁয়া", "নতজানু", "বালুর পাহাড়", "নবী মুহাম্মদ (সাঃ)", "বিজয়", "কক্ষসমূহ", "কাফ",
        "বিক্ষেপকারী বাতাস", "তূর পাহাড়", "নক্ষত্র", "চন্দ্র", "পরম দয়ালu", "মহাবিপদ", "লোহা", "বাদানুবাদকারী নারী", "সমাবেশ", "পরীক্ষিতা নারী",
        "সারিবদ্ধ সৈন্যদল", "শুক্রবার", "মুনাফিকগণ", "ক্ষতিগ্রস্ত হওয়া", "তালাক", "নিষিদ্ধকরণ", "সার্বভৌমত্ব", "কলম", "অনিবার্য সত্য কেয়ামত", "উত্থানের সিঁড়ি",
        "নূহ", "জীন জাতি", "বস্ত্রাবৃত", "পোশাক পরিহিত", "পুনরুত্থান", "মানবজাতি", "প্রেরিত বায়ু", "মহা সংবাদ", "আত্মা হরণকারী", "তিনি ভ্রূকুটি করলেন",
        "অন্ধকারাচ্ছন্ন করা", "বিদীর্ণ করা", "মাপে কমদানকারী", "ফেটে যাওয়া", "নক্ষত্রপুঞ্জ", "রাতের আগমনকারী", "সর্বোচ্চ", "আচ্ছন্নকারী বিপদ", "ভোরবেলা", "নগরী",
        "সূর্য", "রাত", "পূর্বাহ্নের রোদ", "বক্ষ প্রশস্ত করা", "ডুমুর ফল", "রক্তপিণ্ড", "সম্মান ও মহিমান্বিত রাত", "সুস্পষ্ট প্রমাণ", "মহাকম্পন", "অভিযানকারী ঘোড়াসমূহ",
        "মহা দুর্ঘটনা", "প্রাচুর্যের প্রতিযোগিতা", "সময় / অপরাহ্ন", "পরনিন্দাকারী", "হাতি", "কুরাইশ গোত্র", "নিত্যপ্রয়োজনীয় সাহায্য", "অফুরন্ত নেয়ামত", "অবিশ্বাসী কাফেরগণ", "সাহায্য ও বিজয়",
        "খেজুরের পাকানো রশি", "একত্ববাদ ও বিশুদ্ধতা", "ঊষা / নিশিভোর", "মানবজাতি"
    )

    val list = ArrayList<Surah>()
    for (i in 0 until 114) {
        val id = i + 1
        val isMedinan = medinanIds.contains(id)
        val totalVerses = if (i < ayatCounts.size) ayatCounts[i] else 7
        val enN = if (i < namesEn.size) namesEn[i] else "Surah"
        val arN = if (i < namesAr.size) namesAr[i] else "سورة"
        val bnN = if (i < namesBn.size) namesBn[i] else "সূরা"
        val bnM = if (i < meaningsBn.size) meaningsBn[i] else "অর্থ"

        val customVerses = when (id) {
            1 -> getFatihahVerses()
            103 -> getAsrVerses()
            108 -> getKautharVerses()
            112 -> getIkhlasVerses()
            113 -> getFalaqVerses()
            114 -> getNasVerses()
            else -> generateGenericVerses(id, bnN, totalVerses)
        }

        list.add(
            Surah(
                id = id,
                nameEn = enN,
                nameAr = arN,
                nameBn = bnN,
                meaningBn = bnM,
                revelation = if (isMedinan) "মাদানী" else "মক্কী",
                totalVerses = totalVerses,
                verses = customVerses
            )
        )
    }

    return list
}

// Full Authentic Quran Surahs data structures:
fun getFatihahVerses(): List<Verse> = listOf(
    Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "বিসমিল্লাহির রহমানির রাহিম", "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে শুরু করছি।"),
    Verse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "আলহামদু লিল্লাহি রাব্বিল আলামিন", "যাবতীয় প্রশংসা জগৎসমূহের প্রতিপালক আল্লাহর জন্য।"),
    Verse(3, "الرَّحْمَٰنِ الرَّحِيمِ", "আর-রহমানির রাহিম", "যিনি পরম করুণাময় ও অতি দয়ালু।"),
    Verse(4, "مَالِكِ يَوْمِ الدِّينِ", "মালিকি ইয়াওমিদ্দিন", "যিনি বিচার দিনের একমাত্র মালিক।"),
    Verse(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "ইয়্যাকা নাবুদু ওয়া ইয়্যাকা নাস্তায়িন", "আমরা কেবল আপনারই ইবাদত করি এবং কেবল আপনারই সাহায্য প্রার্থনা করি।"),
    Verse(6, "اهْدِنَا الصِّراطَ الْمُسْتَقِيمَ", "ইহদিনাস সিরাতাল মুস্তাকিম", "আমাদের সরল ও সঠিক দ্বীনের পথ প্রদর্শন করুন।"),
    Verse(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "সিরাতাল্লাজিনা আনআমতা আলাইহিম গাইরিল মাগদুবি আলাইহিম ওয়ালাদ্দাল্লিন", "তাদের পথ, যাদের আপনি পুরস্কৃত ও নেয়ামত দান করেছেন। তাদের পথ নয় যারা ক্রুদ্ধ হয়েছে এবং যারা পথভ্রষ্ট হয়েছে।")
)

fun getAsrVerses(): List<Verse> = listOf(
    Verse(1, "وَالْعَصْرِ", "ওয়াল আসর", "শপথ সময়ের (অপরাহ্নের)।"),
    Verse(2, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "ইন্নাল ইনসানা লাফী খুসর", "নিশ্চয়ই সমস্ত মানবজাতি চরম ক্ষতির মধ্যে নিমজ্জিত রয়েছে।"),
    Verse(3, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ", "ইল্লাল্লাযীনা আমানূ ওয়া আমিলুছ ছালিহাতি ওয়া তাওয়া ছাও বিল হাক্কি ওয়া তাওয়া ছাও বিচ্ছাবর", "কিন্তু তারা ছাড়া, যারা ঈমান এনেছে, সৎকাজ করেছে এবং পরস্পরকে হকের (সত্যের) ও ধৈর্যের উপদেশ দিয়েছে।")
)

fun getKautharVerses(): List<Verse> = listOf(
    Verse(1, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "ইন্না আতাইনাকাল কাওসার", "নিশ্চয়ই আমি আপনাকে কাওসার (অফুরন্ত কল্যাণ ও প্রশংসিত ঝর্ণা) দান করেছি।"),
    Verse(2, "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "ফাসাল্লি লিরাব্বিকা ওয়ানহার", "অতএব আপনার রবের উদ্দেশ্যেই সালাত আদায় করুন ও কোরবানি সম্পাদন করুন।"),
    Verse(3, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "ইন্না শানিআকা হুয়াল আবতার", "নিশ্চয় আপনার কুৎসা রটনাকারী শত্রুই হচ্ছে নির্বংশ, লেজকাটা।")
)

fun getIkhlasVerses(): List<Verse> = listOf(
    Verse(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "কুল হুয়াল্লাহু আহাদ", "বলুন, তিনিই মহান আল্লাহ, এক ও অদ্বিতীয়।"),
    Verse(2, "اللَّهُ الصَّمَدُ", "আল্লাহুচ্ছামাদ", "আল্লাহ অমুখাপেক্ষী সর্বত্র সাহায্যদাতা (সবাই তাঁর মুখাপেক্ষী)।"),
    Verse(3, "لَمْ يَلِد * وَلَمْ يُولَدْ", "লাম ইয়ালিদ ওয়া লাম ইউলাদ", "তিনি কাউকেও জন্ম দেননি এবং তাঁকেও জন্ম দেওয়া হয়নি।"),
    Verse(4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "ওয়া লাম ইয়াকুল্লাহু কুফুয়ান আহাদ", "এবং তাঁর সমকক্ষ বা সমতুল্য কেউই হতে পারে না।")
)

fun getFalaqVerses(): List<Verse> = listOf(
    Verse(1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "কুল আউযু বিরাব্বিল ফালাক", "বলুন, আমি আশ্রয় প্রার্থনা করছি উদীয়মান প্রত্যুষের রবের।"),
    Verse(2, "مِن شَرِّ مَا خَلَقَ", "মিন শাররি মা খালাক", "তিনি যা কিছু সৃষ্টি করেছেন তার যাবতীয় অনিষ্ট থেকে।"),
    Verse(3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "ওয়া মিন শাররি গাসিকিন ইযা ওয়া কাব", "এবং গভীর অন্ধকারাচ্ছন্ন রাত্রির অনিষ্ট থেকে যখন তা আচ্ছন্ন করে।"),
    Verse(4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "ওয়া মিন শাররিন নাফফাসাতি ফিল উকাদ", "আর গ্রন্থিসমূহে ফুঁৎকার দিয়ে জাদুকারিনীদের অনিষ্ট থেকে।"),
    Verse(5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "ওয়া মিন শাররি হাসিদিন ইযা হাসাদ", "এবং হিংসুকের অনিষ্ট থেকে যখন সে চরম হিংসা করে।")
)

fun getNasVerses(): List<Verse> = listOf(
    Verse(1, "قُل * أَعُوذُ بِرَبِّ النَّاسِ", "কুল আউযু বিরাব্বিন নাস", "বলুন, আমি আশ্রয় প্রার্থনা করছি গোটা মানবজাতির রবের কাছে।"),
    Verse(2, "مَلِكِ النَّاسِ", "মালিকিন নাস", "যিনি মানবজাতির অধিপতি ও প্রকৃত বাদশাহ।"),
    Verse(3, "إِلَٰهِ النَّاسِ", "ইলাহিন নাস", "যিনি মানবজাতির পরম উপাস্য ও মাবুদ।"),
    Verse(4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "মিন শাররিল ওয়াসওয়াসিল খান্নাস", "সন্দেহ সৃষ্টিকারী ও আত্মগোপনকারী শয়তানের প্ররোচনার অনিষ্ট হতে।"),
    Verse(5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "আল্লাযী ইউওয়াসউইসু ফী ছুদূরিন নাস", "যে মানুষের অন্তরে প্রতিনিয়ত কুপ্ররোচনা ও সন্দেহ সৃষ্টি করে।"),
    Verse(6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "মিনাল জিন্নাতি ওয়ান নাস", "সে শয়তান জ্বিন জাতি ও মানুষের মধ্য হতে অন্তর্ভুক্ত।")
)

suspend fun fetchSurahVerses(surahId: Int): List<Verse> = withContext(Dispatchers.IO) {
    val result = mutableListOf<Verse>()
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,bn.bengali")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            
            val jsonResponse = JSONObject(sb.toString())
            val status = jsonResponse.optString("status", "")
            if (status == "OK") {
                val dataArray = jsonResponse.getJSONArray("data")
                if (dataArray.length() >= 2) {
                    val arabicEdition = dataArray.getJSONObject(0)
                    val bengaliEdition = dataArray.getJSONObject(1)
                    
                    val arabicAyahs = arabicEdition.getJSONArray("ayahs")
                    val bengaliAyahs = bengaliEdition.getJSONArray("ayahs")
                    
                    val length = minOf(arabicAyahs.length(), bengaliAyahs.length())
                    for (i in 0 until length) {
                        val arObj = arabicAyahs.getJSONObject(i)
                        val bnObj = bengaliAyahs.getJSONObject(i)
                        
                        val numberInSurah = arObj.getInt("numberInSurah")
                        var arText = arObj.getString("text")
                        val bnText = bnObj.getString("text")
                        
                        val bismillahPrefix = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                        if (surahId != 1 && surahId != 9 && numberInSurah == 1 && arText.startsWith(bismillahPrefix)) {
                            val candidate = arText.substring(bismillahPrefix.length).trim()
                            if (candidate.isNotEmpty()) {
                                arText = candidate
                            }
                        }
                        
                        result.add(
                            Verse(
                                number = numberInSurah,
                                arabic = arText,
                                pronunciation = "",
                                translation = bnText
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.disconnect()
    }
    result
}

fun generateGenericVerses(surahId: Int, surahName: String, count: Int): List<Verse> {
    val versesList = mutableListOf<Verse>()
    versesList.add(Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "বিসমিল্লাহির রহমানির রাহিম", "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে শুরু করছি।"))
    
    val famousAyats = listOf(
        Triple(
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ",
            "আল্লাহু লা ইলাহা ইল্লা হুয়াল হাইয়্যুল কাইয়্যুম, লা তা’খুযুহু সিনাতুও ওয়ালা নাওম...",
            "আল্লাহ, তিনি ছাড়া কোনো সত্যিকারের উপাস্য নেই। তিনি চিরঞ্জীব, সর্বসত্তার ধারক। তন্দ্রা বা নিদ্রা তাঁকে স্পর্শ করতে পারে না।"
        ),
        Triple(
            "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
            "ইন্নাল্লাহা মা’আছ ছাবিরীন",
            "নিশ্চয়ই পরম দয়ালু আল্লাহ বিপদে ধৈর্যশীল ব্যক্তিদের সাথে রয়েছেন।"
        ),
        Triple(
            "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ",
            "ইয়া আইয়্যুহাল্লাযীনা আমানুসতায়িনু বিচ্ছাবরি ওয়াচ্ছালাহ",
            "হে মুমিনগণ! তোমরা ধৈর্য ও সালাতের মাধ্যমে আল্লাহর নিকট সাহায্য প্রার্থনা করো।"
        ),
        Triple(
            "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            "ওয়া কুর রব্বি যিদনী ইলমা",
            "এবং হে নবী আপনি দোয়া করুন: হে আমার রব! আমার জ্ঞান বৃদ্ধি করে দিন।"
        ),
        Triple(
            "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            "ইন্না মা’আল উসরি উসরা",
            "নিশ্চয়ই কষ্টের সাথেই স্বস্তি ও সহজতা রয়েছে।"
        ),
        Triple(
            "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُون",
            "ফাযকুরুনি আযকুরকুম ওয়াশ কুরু লি ওয়ালা তাকফুরুন",
            "অতএব তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করব। আমার প্রতি কৃতজ্ঞতা প্রকাশ করো এবং অকৃতজ্ঞ হয়ো না।"
        )
    )

    for (i in 2..count) {
        val selected = famousAyats[(i + surahId) % famousAyats.size]
        versesList.add(
            Verse(
                number = i,
                arabic = selected.first,
                pronunciation = selected.second,
                translation = selected.third
            )
        )
    }
    return versesList
}

@Composable
fun QuranScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("সকল") } // "সকল", "মক্কী", "মাদানী"
    var activeSurah by remember { mutableStateOf<Surah?>(null) }
    
    val allSurahs = remember { getSurahList() }
    val filteredSurahs = remember(searchQuery, selectedFilter) {
        allSurahs.filter { surah ->
            val matchesSearch = surah.nameBn.contains(searchQuery, ignoreCase = true) || 
                    surah.nameEn.contains(searchQuery, ignoreCase = true) ||
                    surah.id.toString() == searchQuery
            val matchesFilter = when (selectedFilter) {
                "মক্কী" -> surah.revelation == "মক্কী"
                "মাদানী" -> surah.revelation == "মাদানী"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    AnimatedContent(
        targetState = activeSurah,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        }, label = "QuranNavigation"
    ) { currentSurah ->
        if (currentSurah != null) {
            SurahReadScreen(
                surah = currentSurah,
                onBack = { activeSurah = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PrimaryGreen, Color(0xFF064E3B))
                            )
                        )
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "আল-কুরআন মাজীদ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OverviewStatItem("১১৪ টি", "মোট সূরা")
                            VerticalDivider()
                            OverviewStatItem("মক্কী ৮৬", "অবতরণ")
                            VerticalDivider()
                            OverviewStatItem("মাদানী ২৮", "অবতরণ")
                        }
                    }
                }

                // Slim Premium Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "সূরা খুঁজুন (যেমন: ফাতিহা বা ১)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Segmented control style filter section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filters = listOf("সকল", "মক্কী", "মাদানী")
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) PrimaryGreen else Color.Transparent
                                )
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }

                if (filteredSurahs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "কোনো সূরা খুঁজে পাওয়া যায়নি!",
                                color = TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            SurahListItem(
                                surah = surah,
                                onClick = { activeSurah = surah }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurahListItem(surah: Surah, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF0FDF4), CircleShape)
                    .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.id.toBnString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.nameBn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${surah.nameEn})",
                        fontSize = 10.sp,
                        color = TextGray.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = "অর্থ: ${surah.meaningBn}",
                    fontSize = 10.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = surah.nameAr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (surah.revelation == "মক্কী") Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = surah.revelation,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (surah.revelation == "মক্কী") Color(0xFFB45309) else PrimaryGreen
                        )
                    }
                    Text(
                        text = "${surah.totalVerses.toBnString()} আয়াত",
                        fontSize = 9.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun SurahReadScreen(surah: Surah, onBack: () -> Unit) {
    val context = LocalContext.current
    var arabicFontSize by remember { mutableFloatStateOf(24f) }
    var banglaFontSize by remember { mutableFloatStateOf(14f) }
    
    var showArabic by remember { mutableStateOf(true) }
    var showPronunciation by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    
    var loadedVerses by remember { mutableStateOf(surah.verses) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var triggerFetchCount by remember { mutableStateOf(0) }

    LaunchedEffect(surah.id, triggerFetchCount) {
        val hardcodedIds = setOf(1, 103, 108, 112, 113, 114)
        if (surah.id in hardcodedIds) {
            loadedVerses = surah.verses
            isLoading = false
            isError = false
            return@LaunchedEffect
        }
        
        isLoading = true
        isError = false
        try {
            val verses = fetchSurahVerses(surah.id)
            if (verses.isNotEmpty()) {
                loadedVerses = verses
                isError = false
            } else {
                isError = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isError = true
        } finally {
            isLoading = false
        }
    }

    var verseSearchQuery by remember { mutableStateOf("") }
    val filteredVerses = remember(verseSearchQuery, loadedVerses) {
        loadedVerses.filter { v ->
            v.translation.contains(verseSearchQuery, ignoreCase = true) ||
                    v.pronunciation.contains(verseSearchQuery, ignoreCase = true) ||
                    v.number.toString() == verseSearchQuery
        }
    }

    var isPlayingAll by remember { mutableStateOf(false) }
    var activePlaybackVerseNo by remember { mutableStateOf(-1) }

    val mediaPlayerHolder = remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun stopAudio() {
        mediaPlayerHolder.value?.let { gp ->
            try {
                if (gp.isPlaying) {
                     gp.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                gp.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayerHolder.value = null
    }

    fun playAudio(surahId: Int, verseNumber: Int, onComplete: () -> Unit) {
        stopAudio()

        val surahPadded = surahId.toString().padStart(3, '0')
        val versePadded = verseNumber.toString().padStart(3, '0')
        val primaryUrl = "https://www.everyayah.com/data/Alafasy_128kbps/$surahPadded$versePadded.mp3"
        
        val mp = android.media.MediaPlayer()
        mediaPlayerHolder.value = mp
        
        try {
            mp.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(primaryUrl)
            mp.setOnPreparedListener { player ->
                player.start()
            }
            mp.setOnCompletionListener { player ->
                player.release()
                if (mediaPlayerHolder.value == player) {
                    mediaPlayerHolder.value = null
                }
                onComplete()
            }
            mp.setOnErrorListener { player, what, extra ->
                player.release()
                if (mediaPlayerHolder.value == player) {
                    mediaPlayerHolder.value = null
                }
                
                // fallback audio stream logic
                val cumulativeCount = getGlobalVerseNumber(surahId, verseNumber)
                val fallbackUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$cumulativeCount.mp3"
                try {
                    val fallbackMp = android.media.MediaPlayer()
                    mediaPlayerHolder.value = fallbackMp
                    fallbackMp.setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    fallbackMp.setDataSource(fallbackUrl)
                    fallbackMp.setOnPreparedListener { fPlayer ->
                        fPlayer.start()
                    }
                    fallbackMp.setOnCompletionListener { fPlayer ->
                        fPlayer.release()
                        if (mediaPlayerHolder.value == fPlayer) {
                            mediaPlayerHolder.value = null
                        }
                        onComplete()
                    }
                    fallbackMp.setOnErrorListener { fPlayer, fWhat, fExtra ->
                        fPlayer.release()
                        if (mediaPlayerHolder.value == fPlayer) {
                            mediaPlayerHolder.value = null
                        }
                        Toast.makeText(context, "তিলাওয়াত অডিও লোড করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                        onComplete()
                        true
                    }
                    fallbackMp.prepareAsync()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    onComplete()
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAudio()
        }
    }

    LaunchedEffect(activePlaybackVerseNo, isPlayingAll) {
        if (activePlaybackVerseNo != -1) {
            playAudio(surah.id, activePlaybackVerseNo) {
                if (isPlayingAll) {
                    val nextVerse = activePlaybackVerseNo + 1
                    if (nextVerse <= surah.totalVerses) {
                        activePlaybackVerseNo = nextVerse
                    } else {
                        isPlayingAll = false
                        activePlaybackVerseNo = -1
                        Toast.makeText(context, "সূরা তিলাওয়াত সমাপ্ত হয়েছে", Toast.LENGTH_LONG).show()
                    }
                } else {
                    activePlaybackVerseNo = -1
                }
            }
        } else {
            stopAudio()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryGreen, Color(0xFF115E59))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "সূরা ${surah.nameBn}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "অর্থ: ${surah.meaningBn} • ${surah.totalVerses.toBnString()} আয়াত",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isPlayingAll = !isPlayingAll
                            if (isPlayingAll) {
                                activePlaybackVerseNo = 1
                                Toast.makeText(context, "সূরা তিলাওয়াত অডিও চালু হয়েছে", Toast.LENGTH_SHORT).show()
                            } else {
                                activePlaybackVerseNo = -1
                                Toast.makeText(context, "সূরা তিলাওয়াত বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isPlayingAll) PrimaryGreen else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlayingAll) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Surah audio",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("আরবিঃ ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { if (arabicFontSize > 18) arabicFontSize -= 2 },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("A-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        TextButton(
                            onClick = { if (arabicFontSize < 40) arabicFontSize += 2 },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("A+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("বাংলাঃ ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { if (banglaFontSize > 11) banglaFontSize -= 1 },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("ক-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        TextButton(
                            onClick = { if (banglaFontSize < 24) banglaFontSize += 1 },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("ক+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showPronunciation = !showPronunciation },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showPronunciation) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Bengali Pronunciation",
                                tint = if (showPronunciation) Color(0xFF38BDF8) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { showTranslation = !showTranslation },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showTranslation) Icons.Default.Translate else Icons.Default.GTranslate,
                                contentDescription = "Toggle Bengali Translation",
                                tint = if (showTranslation) PrimaryGreen else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (surah.id != 1 && surah.id != 9) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    color = PrimaryGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        OutlinedTextField(
            value = verseSearchQuery,
            onValueChange = { verseSearchQuery = it },
            placeholder = { Text("আয়াতের অনুবাদ বা নম্বর দিয়ে খুঁজুন...", fontSize = 12.sp) },
            prefix = {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator(color = PrimaryGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "পবিত্র সুরার আয়াতসমূহ লোড হচ্ছে...",
                        color = PrimaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (isError) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "আয়ারসমূহ লোড করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।",
                        color = TextGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { triggerFetchCount++ },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("পুনরায় চেষ্টা করুন", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        } else {
            if (filteredVerses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("কোনো আয়াত পাওয়া যায়নি!", color = TextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVerses) { verse ->
                        val isPlaying = activePlaybackVerseNo == verse.number
                        VerseCardItem(
                            surahId = surah.id,
                            verse = verse,
                            showArabic = showArabic,
                            showPronunciation = showPronunciation,
                            showTranslation = showTranslation,
                            arabicFontSize = arabicFontSize,
                            banglaFontSize = banglaFontSize,
                            isPlaying = isPlaying,
                            onPlayClick = {
                                if (activePlaybackVerseNo == verse.number) {
                                    activePlaybackVerseNo = -1
                                    Toast.makeText(context, "তিলাওয়াত স্থগিত করা হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    activePlaybackVerseNo = verse.number
                                    Toast.makeText(context, "আয়াত ${verse.number.toBnString()} তিলাওয়াত শুরু হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerseCardItem(
    surahId: Int,
    verse: Verse,
    showArabic: Boolean,
    showPronunciation: Boolean,
    showTranslation: Boolean,
    arabicFontSize: Float,
    banglaFontSize: Float,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(QuranBookmarkManager.isBookmarked(context, surahId, verse.number)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPlaying) 1.5.dp else 0.dp,
                color = if (isPlaying) PrimaryGreen else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFF0FDF4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(PrimaryGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = verse.number.toBnString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play Verse Audio",
                            tint = if (isPlaying) PrimaryGreen else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val state = QuranBookmarkManager.toggleBookmark(context, surahId, verse.number)
                            isBookmarked = state
                            val msg = if (state) "আয়াত বুকমার্ক সংরক্ষণ করা হয়েছে" else "বুকমার্ক মুছে ফেলা হয়েছে"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFF59E0B) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val pronunciationText = if (verse.pronunciation.isNotBlank()) "\n\nউচ্চারণ: ${verse.pronunciation}" else ""
                            val clip = android.content.ClipData.newPlainText(
                                "Quran Verse", 
                                "${verse.arabic}${pronunciationText}\n\nঅনুবাদ: ${verse.translation}"
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "আয়াত অনুলিপি (Copy) করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Verse",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                val pronunciationText = if (verse.pronunciation.isNotBlank()) "\n\nউচ্চারণ: ${verse.pronunciation}" else ""
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "আল-কুরআন: সূরা ${surahId}, আয়াত ${verse.number}\n\n${verse.arabic}${pronunciationText}\n\nঅনুবাদ: ${verse.translation}"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "শেয়ার করুন"))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showArabic) {
                Text(
                    text = verse.arabic,
                    fontSize = arabicFontSize.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.End,
                    lineHeight = (arabicFontSize * 1.5).sp
                )
            }

            if (showPronunciation && verse.pronunciation.isNotBlank()) {
                Text(
                    text = "উচ্চারণ: " + verse.pronunciation,
                    fontSize = (banglaFontSize + 1).sp,
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    lineHeight = (banglaFontSize * 1.4).sp
                )
            }

            if (showTranslation) {
                Text(
                    text = "অনুবাদ: " + verse.translation,
                    fontSize = banglaFontSize.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = (banglaFontSize * 1.4).sp
                )
            }
        }
    }
}

@Composable
fun OverviewStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toBnString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.2f))
    )
}
