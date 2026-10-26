package com.arrowescape.puzzle

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class LevelLoaderCryptoTest {

    private val aesKey = "AmazeGo_Arrow_Security_Key_2026!".toByteArray(Charsets.UTF_8)

    private fun decryptPack(file: File): JSONArray {
        assertTrue("Pack file must exist: ${file.path}", file.exists())
        val bytes = file.readBytes()
        assertTrue("Pack must have at least 16 bytes IV", bytes.size > 16)
        val iv = bytes.copyOfRange(0, 16)
        val ciphertext = bytes.copyOfRange(16, bytes.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(ciphertext)

        val gzipIn = GZIPInputStream(ByteArrayInputStream(decrypted))
        val jsonStr = gzipIn.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONArray(jsonStr)
    }

    @Test
    fun testPack00DecryptionAndLevels() {
        val packFile = File("src/main/assets/levels/level_pack_00.dat")
        val levels = decryptPack(packFile)
        assertEquals("Pack 00 must contain 1000 levels", 1000, levels.length())

        // Test Level 1 (Tutorial)
        val l1 = levels.getJSONObject(0)
        assertEquals("Level 1 width must be 4", 4, l1.getInt("XSize"))
        assertEquals("Level 1 height must be 5", 5, l1.getInt("YSize"))
        assertEquals("Level 1 must have 4 arrows", 4, l1.getJSONArray("Arrows").length())

        // Test Level 25 (SuperHard Milestone)
        val l25 = levels.getJSONObject(24)
        assertEquals("Level 25 hash must match authentic SuperHard", "35ad40df6826a14f", l25.getString("Hash"))
        assertEquals("Level 25 must have 74 arrows", 74, l25.getJSONArray("Arrows").length())

        println("Pack 00 verified successfully! Level 1: 4 arrows, Level 25: 74 arrows.")
    }

    @Test
    fun testDailyPackDecryption() {
        val dailyFile = File("src/main/assets/levels/daily_pack.dat")
        val dailyLevels = decryptPack(dailyFile)
        assertEquals("Daily pack must contain 300 levels", 300, dailyLevels.length())
        println("Daily pack verified successfully with 300 levels.")
    }
}
