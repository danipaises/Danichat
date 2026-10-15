package com.danichat.account.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SupabaseConfigTest {
    @Test fun `no configuration preserves the offline-only mode`() {
        assertNull(SupabaseConfig.fromBuildValues("", ""))
    }

    @Test fun `only a public key on a clean HTTPS origin is accepted`() {
        assertNotNull(SupabaseConfig.fromBuildValues("https://project.example.invalid", KEY))
        for ((url, key) in listOf(
            "http://project.example.invalid" to KEY,
            "https://name:password@project.example.invalid" to KEY,
            "https://project.example.invalid/collect" to KEY,
            "https://project.example.invalid/?key=value" to KEY,
            "https://project.example.invalid" to "sb_secret_synthetic_forbidden",
            "https://project.example.invalid" to "service_role",
            "" to KEY,
            "https://project.example.invalid" to ""
        )) {
            try {
                SupabaseConfig.fromBuildValues(url, key)
                throw AssertionError("Invalid configuration was accepted")
            } catch (_: IllegalArgumentException) { }
        }
    }

    companion object { private const val KEY = "sb_publishable_synthetic_test" }
}
