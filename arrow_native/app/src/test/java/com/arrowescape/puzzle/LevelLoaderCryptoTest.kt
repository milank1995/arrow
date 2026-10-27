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

        // Test Level 5 (Milestone Shape 1)
        val l5 = levels.getJSONObject(4)
        assertEquals("Level 5 must have 54 arrows", 54, l5.getJSONArray("Arrows").length())

        // Test Level 10 (Milestone Showstopper)
        val l10 = levels.getJSONObject(9)
        assertEquals("Level 10 must have 99 arrows", 99, l10.getJSONArray("Arrows").length())

        // Test Level 15 (Milestone Shape 2)
        val l15 = levels.getJSONObject(14)
        assertEquals("Level 15 must have 53 arrows", 53, l15.getJSONArray("Arrows").length())

        // Test Level 20 (Milestone Shape 3)
        val l20 = levels.getJSONObject(19)
        assertEquals("Level 20 must have 49 arrows", 49, l20.getJSONArray("Arrows").length())

        // Test Level 25 (SuperHard Milestone)
        val l25 = levels.getJSONObject(24)
        assertEquals("Level 25 must have 74 arrows", 74, l25.getJSONArray("Arrows").length())

        // Test Level 26 (Cactus Heavy Milestone)
        val l26 = levels.getJSONObject(25)
        assertEquals("Level 26 must have 130 arrows", 130, l26.getJSONArray("Arrows").length())

        // Test Level 30 (Milestone Shape 5)
        val l30 = levels.getJSONObject(29)
        assertEquals("Level 30 must have 68 arrows", 68, l30.getJSONArray("Arrows").length())

        // Test Level 31 (Smooth continuation, no 3-arrow drop!)
        val l31 = levels.getJSONObject(30)
        assertEquals("Level 31 must maintain momentum with 28 arrows", 28, l31.getJSONArray("Arrows").length())

        println("Pack 00 verified successfully! Amaze Go momentum progression intact.")
    }

    @Test
    fun testDailyPackDecryption() {
        val dailyFile = File("src/main/assets/levels/daily_pack.dat")
        val dailyLevels = decryptPack(dailyFile)
        assertEquals("Daily pack must contain 300 levels", 300, dailyLevels.length())
        println("Daily pack verified successfully with 300 levels.")
    }

    @Test
    fun testPack01AndGrandFinale() {
        val pack1File = File("src/main/assets/levels/level_pack_01.dat")
        val levels1 = decryptPack(pack1File)
        assertEquals("Pack 01 must contain 1000 levels", 1000, levels1.length())

        val l2000 = levels1.getJSONObject(999) // Level 2,000
        assertEquals("Level 2000 must have 80 arrows", 80, l2000.getJSONArray("Arrows").length())
        assertEquals("Level 2000 width", 24, l2000.getInt("XSize"))
        assertEquals("Level 2000 height", 35, l2000.getInt("YSize"))

        val parsed = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(2000, l2000)
        assertNotNull("Parsed Level 2000 should not be null", parsed)
        assertEquals("Parsed arrows count for Level 2000 must match", 80, parsed!!.arrows.size)
        println("Pack 01 and Level 2000 Grand Finale verified successfully!")
    }
}
