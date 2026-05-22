package com.aistudio.shortsgen

import com.aistudio.shortsgen.network.FastApiClient
import org.junit.Assert.assertEquals
import org.junit.Test

class FastApiClientTest {
    private val client = FastApiClient()

    @Test
    fun testSanitizeUrl() {
        assertEquals("http://10.0.2.2:8000", client.sanitizeUrl("10.0.2.2:8000"))
        assertEquals("http://10.0.2.2:8000", client.sanitizeUrl("http://10.0.2.2:8000/"))
        assertEquals("https://my-backend.com", client.sanitizeUrl("https://my-backend.com///"))
        assertEquals("http://localhost:8000", client.sanitizeUrl("  localhost:8000  "))
        assertEquals("", client.sanitizeUrl(""))
    }
}
