package com.example

import android.util.Base64
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    // URL: https://acnlhvuqxstnocdbyugc.supabase.co
    private val alpha = "aHR0cHM6Ly9hY25saHZ1cXhzdG5vY2RieXVnYy5zdXBhYmFzZS5jbw=="
    // Anon Key: sb_publishable_g7LuLCfzyMkTdbyYQAYrVw_UOume_6B
    private val beta = "c2JfcHVibGlzaGFibGVfZzdMdUxDZnp5TWtUZGJ5WVFBWXJWd19VT3VtZV82Qg=="

    private fun reveal(secret: String): String {
        return String(Base64.decode(secret, Base64.NO_WRAP)).trim()
    }

    val client = createSupabaseClient(
        supabaseUrl = reveal(alpha),
        supabaseKey = reveal(beta)
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
