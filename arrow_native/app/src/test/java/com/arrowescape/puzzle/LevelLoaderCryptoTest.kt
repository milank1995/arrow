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

        // Test Level 1 (Tutorial Intro)
        val l1 = levels.getJSONObject(0)
        assertEquals("Level 1 width must be 4", 4, l1.getInt("XSize"))
        assertEquals("Level 1 height must be 5", 5, l1.getInt("YSize"))
        assertEquals("Level 1 must have 4 arrows", 4, l1.getJSONArray("Arrows").length())

        // Test Level 4 (Milestone Shape 1)
        val l4 = levels.getJSONObject(3)
        assertEquals("Level 4 must have 15 arrows", 15, l4.getJSONArray("Arrows").length())

        // Test Level 9 (Milestone Shape 2)
        val l9 = levels.getJSONObject(8)
        assertEquals("Level 9 must have 20 arrows", 20, l9.getJSONArray("Arrows").length())

        // Test Level 14 (Milestone Shape 3)
        val l14 = levels.getJSONObject(13)
        assertEquals("Level 14 must have 25 arrows", 25, l14.getJSONArray("Arrows").length())

        // Test Level 19 (Milestone Shape 4)
        val l19 = levels.getJSONObject(18)
        assertEquals("Level 19 must have 32 arrows", 32, l19.getJSONArray("Arrows").length())

        // Test Level 30 (Chapter 1 Graduation)
        val l30 = levels.getJSONObject(29)
        assertEquals("Level 30 must have 50 arrows", 50, l30.getJSONArray("Arrows").length())

        println("Pack 00 verified successfully! Onboarding progression Levels 1 to 30 intact.")
    }

    @Test
    fun testDailyPackDecryption() {
        val dailyFile = File("src/main/assets/levels/daily_pack.dat")
        val dailyLevels = decryptPack(dailyFile)
        assertEquals("Daily pack must contain 300 levels", 300, dailyLevels.length())
        println("Daily pack verified successfully with 300 levels.")
    }

    @Test
    fun testPack04AndGrandFinale() {
        val pack4File = File("src/main/assets/levels/level_pack_04.dat")
        val levels4 = decryptPack(pack4File)
        assertEquals("Pack 04 must contain 1000 levels", 1000, levels4.length())

        val l5000 = levels4.getJSONObject(999) // Level 5,000
        assertEquals("Level 5000 must have 229 arrows", 229, l5000.getJSONArray("Arrows").length())
        assertEquals("Level 5000 width", 35, l5000.getInt("XSize"))
        assertEquals("Level 5000 height", 58, l5000.getInt("YSize"))

        val parsed = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(5000, l5000)
        assertNotNull("Parsed Level 5000 should not be null", parsed)
        assertEquals("Parsed arrows count for Level 5000 must match", 229, parsed!!.arrows.size)
        println("Pack 04 and Level 5000 Grand Finale verified successfully!")
    }
}
