package com.mudahdi.ffixsavebridge

import javax.crypto.spec.PBEKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implements the vanilla rr2016 AES-256-CBC envelope using Android's JCA.
 * Key derivation constants are documented by the MIT-licensed FFIX converter.
 */
object RrCryptoBridge {
    private val salt = byteArrayOf(3, 3, 1, 4, 7, 0, 9, 7)
    private val password = "System.Security.SecureString".toCharArray()

    private val keyAndIv: Pair<ByteArray, ByteArray> by lazy {
        val spec = PBEKeySpec(password, salt, 1000, 384)
        try {
            val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec).encoded
            derived.copyOfRange(0, 32) to derived.copyOfRange(32, 48)
        } finally {
            spec.clearPassword()
        }
    }

    @JvmStatic
    fun encrypt(plain: ByteArray): ByteArray = cipher(Cipher.ENCRYPT_MODE, plain)

    @JvmStatic
    fun decrypt(encrypted: ByteArray): ByteArray = cipher(Cipher.DECRYPT_MODE, encrypted)

    private fun cipher(mode: Int, input: ByteArray): ByteArray {
        val (key, iv) = keyAndIv
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(input)
    }

    @JvmStatic
    fun keyForTests(): ByteArray = keyAndIv.first.copyOf()

    @JvmStatic
    fun ivForTests(): ByteArray = keyAndIv.second.copyOf()
}
