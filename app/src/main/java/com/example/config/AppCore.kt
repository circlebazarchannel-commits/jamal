package com.example.config

import android.util.Base64

object AppCore {
    // Pseudonym variables for Firebase configuration
    private val aX = "ODFNN3hXSnNDQU40UFBfRE54MmZEdTNrTERYSklRbFFTQXlTYXpJQQ=="
    private val mB = "ZjliMjhmNmJlZWZlMTMzM2MxYzM2ZTpkaW9yZG5hOjI4NzIzODc0NzQ3Nzox"
    private val pI = "ZWxjcmljLWxhbGFo"
    private val sB = "cHBhLmVnYXJvdHNlcmlmLmVsY3JpYy1sYWxhaA=="

    fun k1(): String = r(aX)
    fun k2(): String = r(mB)
    fun k3(): String = r(pI)
    fun k4(): String = r(sB)

    private fun r(p: String): String {
        return String(Base64.decode(p, Base64.DEFAULT)).reversed()
    }
}
