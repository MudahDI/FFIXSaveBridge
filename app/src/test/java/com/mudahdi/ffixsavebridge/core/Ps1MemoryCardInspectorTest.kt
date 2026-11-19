package com.mudahdi.ffixsavebridge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ps1MemoryCardInspectorTest {
    @Test
    fun rejectsWrongCardSize() {
        val result = Ps1MemoryCardInspector.inspect(ByteArray(1024))
        assertFalse(result.isMemoryCard)
        assertTrue(result.saves.isEmpty())
    }

    @Test
    fun recognizesCardHeaderAndReportsNoFfixSaves() {
        val card = ByteArray(131_072)
        card[0] = 'M'.code.toByte()
        card[1] = 'C'.code.toByte()

        val result = Ps1MemoryCardInspector.inspect(card)

        assertTrue(result.isMemoryCard)
        assertTrue(result.saves.isEmpty())
        assertTrue(result.message.contains("tidak ditemukan"))
    }

    @Test
    fun validatesSyntheticFfixSaveEntry() {
        val card = ByteArray(131_072)
        card[0] = 'M'.code.toByte()
        card[1] = 'C'.code.toByte()

        val directoryOffset = 0x80 + 128
        card[directoryOffset] = 0x51
        val fileName = "BASLUS-0125100000-00".toByteArray(Charsets.US_ASCII)
        fileName.copyInto(card, directoryOffset + 0x0A)

        var xor = 0
        for (i in 0 until 127) xor = xor xor (card[directoryOffset + i].toInt() and 0xFF)
        card[directoryOffset + 127] = xor.toByte()

        val blockOffset = 0x2000
        card[blockOffset] = 'S'.code.toByte()
        card[blockOffset + 1] = 'C'.code.toByte()
        card[blockOffset + 2] = 0x11
        card[blockOffset + 3] = 0x01

        val crc = crc16(card, blockOffset, 0x13FE)
        card[blockOffset + 0x13FE] = (crc and 0xFF).toByte()
        card[blockOffset + 0x13FF] = ((crc ushr 8) and 0xFF).toByte()

        val result = Ps1MemoryCardInspector.inspect(card)

        assertEquals(1, result.saves.size)
        assertEquals(1, result.saves.single().blockNumber)
        assertEquals("NTSC-U", result.saves.single().region)
        assertTrue(result.saves.single().fullyValidated)
    }

    private fun crc16(bytes: ByteArray, offset: Int, length: Int): Int {
        var crc = 0xFFFF
        for (index in offset until offset + length) {
            crc = crc xor (bytes[index].toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0x8408 else crc ushr 1
            }
        }
        return crc and 0xFFFF
    }
}
