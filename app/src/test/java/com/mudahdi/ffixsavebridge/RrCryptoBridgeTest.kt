package com.mudahdi.ffixsavebridge

import org.junit.Assert.assertEquals
import org.junit.Test

class RrCryptoBridgeTest {
    @Test
    fun derivesTheKnownVanillaRr2016KeyAndIv() {
        assertEquals(
            "10b45d06bbc66bedb11a2f44f1911e072e97ffc29ce44b92f97474a710b8a5d5",
            RrCryptoBridge.keyForTests().joinToString("") { "%02x".format(it) }
        )
        assertEquals(
            "a83e99c0c895e1acaf11ccd982f9715f",
            RrCryptoBridge.ivForTests().joinToString("") { "%02x".format(it) }
        )
    }

    @Test
    fun encryptDecryptRoundTrips() {
        val input = "FFIX Save Bridge test".toByteArray()
        assertEquals(input.toList(), RrCryptoBridge.decrypt(RrCryptoBridge.encrypt(input)).toList())
    }
}
