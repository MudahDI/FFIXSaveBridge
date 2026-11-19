package com.mudahdi.ffixsavebridge.core

import java.nio.charset.StandardCharsets

data class Ps1SaveEntry(
    val blockNumber: Int,
    val directoryName: String,
    val region: String,
    val directoryChecksumValid: Boolean,
    val saveHeaderValid: Boolean,
    val saveCrcValid: Boolean
) {
    val fullyValidated: Boolean
        get() = directoryChecksumValid && saveHeaderValid && saveCrcValid
}

data class MemoryCardInspection(
    val isMemoryCard: Boolean,
    val saves: List<Ps1SaveEntry>,
    val message: String
)

/**
 * Read-only inspector for standard 128 KiB PS1 memory-card images.
 *
 * Layout and checksum details are based on the documented FFIX PS1 format.
 * This class only identifies and validates candidate save blocks; it does not
 * perform the PS1-to-PC conversion.
 */
object Ps1MemoryCardInspector {
    private const val CARD_SIZE = 131_072
    private const val FRAME_SIZE = 128
    private const val BLOCK_SIZE = 8_192
    private const val DIRECTORY_START = 0x80
    private const val BLOCK_START = 0x2000
    private const val SAVE_CRC_OFFSET = 0x13FE

    private val productCodes = listOf(
        "BASLUS-01251" to "NTSC-U",
        "BASLUS-01295" to "NTSC-U",
        "BASLUS-01296" to "NTSC-U",
        "BASLUS-01297" to "NTSC-U",
        "BESLES-x2965" to "PAL",
        "BESLES-x2966" to "PAL",
        "BESLES-x2967" to "PAL",
        "BESLES-x2968" to "PAL",
        "BESLES-x2969" to "PAL",
        "BISLPS-02000" to "NTSC-J",
        "BISLPS-02001" to "NTSC-J",
        "BISLPS-02002" to "NTSC-J",
        "BISLPS-02003" to "NTSC-J"
    )

    fun inspect(bytes: ByteArray): MemoryCardInspection {
        if (bytes.size != CARD_SIZE) {
            return MemoryCardInspection(
                isMemoryCard = false,
                saves = emptyList(),
                message = "Ukuran bukan image kartu memori PS1 standar 128 KiB."
            )
        }

        if (bytes[0] != 'M'.code.toByte() || bytes[1] != 'C'.code.toByte()) {
            return MemoryCardInspection(
                isMemoryCard = false,
                saves = emptyList(),
                message = "Header kartu memori tidak cocok (signature MC tidak ditemukan)."
            )
        }

        val entries = mutableListOf<Ps1SaveEntry>()

        // PS1 memory-card data blocks are numbered 1 through 15.
        for (blockNumber in 1..15) {
            val directoryOffset = DIRECTORY_START + FRAME_SIZE * blockNumber
            val state = readUInt32LE(bytes, directoryOffset)
            if (state != 0x51L) continue

            val nameBytes = bytes.copyOfRange(directoryOffset + 0x0A, directoryOffset + 0x1E)
            val name = nameBytes
                .takeWhile { it != 0.toByte() && it != 0xFF.toByte() }
                .toByteArray()
                .toString(StandardCharsets.US_ASCII)
                .trim()

            val region = productCodes.firstOrNull { (code, _) -> name.startsWith(code) }?.second
                ?: continue

            val directoryChecksumValid = xorFrame(bytes, directoryOffset)
            val dataOffset = BLOCK_START * blockNumber
            val saveHeaderValid =
                bytes[dataOffset] == 'S'.code.toByte() &&
                bytes[dataOffset + 1] == 'C'.code.toByte() &&
                bytes[dataOffset + 2] == 0x11.toByte() &&
                bytes[dataOffset + 3] == 0x01.toByte()

            val storedCrc = readUInt16LE(bytes, dataOffset + SAVE_CRC_OFFSET)
            val calculatedCrc = crc16(bytes, dataOffset, SAVE_CRC_OFFSET)
            val saveCrcValid = storedCrc == calculatedCrc

            entries += Ps1SaveEntry(
                blockNumber = blockNumber,
                directoryName = name,
                region = region,
                directoryChecksumValid = directoryChecksumValid,
                saveHeaderValid = saveHeaderValid,
                saveCrcValid = saveCrcValid
            )
        }

        val message = when {
            entries.isEmpty() -> "Kartu terbaca, tetapi tidak ditemukan entri save FFIX yang dikenali."
            entries.all { it.fullyValidated } ->
                "${entries.size} save FFIX ditemukan. Header, checksum direktori, dan CRC save lolos pemeriksaan."
            else ->
                "${entries.size} kandidat save FFIX ditemukan. Sebagian pemeriksaan validasi gagal; jangan konversi entri tersebut."
        }

        return MemoryCardInspection(true, entries, message)
    }

    private fun xorFrame(bytes: ByteArray, offset: Int): Boolean {
        var checksum = 0
        for (i in 0 until FRAME_SIZE - 1) {
            checksum = checksum xor (bytes[offset + i].toInt() and 0xFF)
        }
        return checksum == (bytes[offset + FRAME_SIZE - 1].toInt() and 0xFF)
    }

    private fun crc16(bytes: ByteArray, offset: Int, length: Int): Int {
        var crc = 0xFFFF
        for (index in offset until offset + length) {
            crc = crc xor (bytes[index].toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) {
                    (crc ushr 1) xor 0x8408
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    private fun readUInt16LE(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readUInt32LE(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
}
