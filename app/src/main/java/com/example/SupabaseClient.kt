package com.example

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    // Obfuscated config
    private val endpointHex = "68747470733a2f2f61636e6c687675717873746e6f636462797567632e73757061626173652e636f"
    private val tokenHex = "73625f7075626c69736861626c655f67374c754c43667a794d6b54646279595141597256775f554f756d655f3642"

    private fun decode(hex: String): String {
        require(hex.length % 2 == 0) { "Must have an even length" }
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
    }

    val client = createSupabaseClient(
        supabaseUrl = decode(endpointHex),
        supabaseKey = decode(tokenHex)
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
