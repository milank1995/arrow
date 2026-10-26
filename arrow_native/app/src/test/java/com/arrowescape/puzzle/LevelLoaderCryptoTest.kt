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
    @Test
    fun testLevel14040AndMilestones() {
        // Test Pack 14 (Level 14040)
        val pack14File = File("src/main/assets/levels/level_pack_14.dat")
        val levels14 = decryptPack(pack14File)
        val l14040Json = levels14.getJSONObject(39) // index 39 is Level 14,040
        assertEquals("Level 14040 hash", "73d41d2c55a17604", l14040Json.getString("Hash"))
        assertEquals("Level 14040 arrows count", 89, l14040Json.getJSONArray("Arrows").length())
        assertEquals("Level 14040 XSize", 49, l14040Json.getInt("XSize"))
        assertEquals("Level 14040 YSize", 47, l14040Json.getInt("YSize"))

        val parsed14040 = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(14040, l14040Json)
        assertNotNull("Parsed Level 14040 should not be null", parsed14040)
        assertEquals("Parsed arrows count for Level 14040 must be 89", 89, parsed14040!!.arrows.size)

        // Test Pack 00 Milestones
        val pack0File = File("src/main/assets/levels/level_pack_00.dat")
        val levels0 = decryptPack(pack0File)
        assertEquals("Level 5 (Binoculars) arrow count", 54, levels0.getJSONObject(4).getJSONArray("Arrows").length())
        assertEquals("Level 10 (Lightbulb) arrow count", 99, levels0.getJSONObject(9).getJSONArray("Arrows").length())
        assertEquals("Level 20 (Cat Head) arrow count", 49, levels0.getJSONObject(19).getJSONArray("Arrows").length())
        assertEquals("Level 26 (Cactus) arrow count", 130, levels0.getJSONObject(25).getJSONArray("Arrows").length())
        assertEquals("Level 50 arrow count", 48, levels0.getJSONObject(49).getJSONArray("Arrows").length())
        assertEquals("Level 100 arrow count", 43, levels0.getJSONObject(99).getJSONArray("Arrows").length())
        println("All milestone arrow counts and Level 14040 (89 arrows) verified successfully!")
    }
}
