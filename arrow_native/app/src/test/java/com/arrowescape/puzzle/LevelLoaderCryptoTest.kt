package com.arrowescape.puzzle

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class LevelLoaderCryptoTest {

    private val AES_KEY = "AmazeGo_Arrow_Security_Key_2026!".toByteArray(Charsets.UTF_8)

    private fun decryptPack(file: File): JSONArray {
        assertTrue("Pack file must exist at ${file.absolutePath}", file.exists())
        val data = file.readBytes()
        assertTrue("Pack file must have at least IV (16 bytes)", data.size > 16)

        val iv = data.copyOfRange(0, 16)
        val ciphertext = data.copyOfRange(16, data.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(AES_KEY, "AES")
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
        assertEquals("Level 1 must have 4 arrows", 4, l1.getJSONArray("Arrows").length())

        // Test Level 5 (Milestone Shape 1)
        val l5 = levels.getJSONObject(4)
        assertEquals("Level 5 must have 54 arrows", 54, l5.getJSONArray("Arrows").length())

        // Test Level 10 (Milestone Shape 2)
        val l10 = levels.getJSONObject(9)
        assertEquals("Level 10 must have 59 arrows", 59, l10.getJSONArray("Arrows").length())

        // Test Level 15 (Milestone Shape 3)
        val l15 = levels.getJSONObject(14)
        assertEquals("Level 15 must have 64 arrows", 64, l15.getJSONArray("Arrows").length())

        // Test Level 20 (Milestone Shape 4)
        val l20 = levels.getJSONObject(19)
        assertEquals("Level 20 must have 67 arrows", 67, l20.getJSONArray("Arrows").length())

        // Test Level 25 (Milestone Shape 5)
        val l25 = levels.getJSONObject(24)
        assertEquals("Level 25 must have 77 arrows", 77, l25.getJSONArray("Arrows").length())

        // Test Level 30 (Milestone Shape 6)
        val l30 = levels.getJSONObject(29)
        assertEquals("Level 30 must have 86 arrows", 86, l30.getJSONArray("Arrows").length())

        // Test Level 31 (Smooth continuation, no reset drop!)
        val l31 = levels.getJSONObject(30)
        assertEquals("Level 31 must maintain momentum with 42 arrows", 42, l31.getJSONArray("Arrows").length())

        println("Pack 00 verified successfully! First 30 levels onboarding and momentum intact.")
    }

    @Test
    fun testShapePackDecryptionAndTiers() {
        val shapeFile = File("src/main/assets/levels/shape_pack.dat")
        val shapes = decryptPack(shapeFile)
        assertEquals("Shape pack must contain 1061 dynamic curated shapes", 1061, shapes.length())

        var maxArrows = 0
        val tiersSeen = mutableSetOf<Int>()
        for (i in 0 until shapes.length()) {
            val s = shapes.getJSONObject(i)
            tiersSeen.add(s.getInt("Tier"))
            val arrCount = s.getJSONArray("Arrows").length()
            if (arrCount > maxArrows) {
                maxArrows = arrCount
            }
        }

        assertEquals("Must cover all 6 difficulty tiers (0 to 5)", 6, tiersSeen.size)
        assertEquals("Pinnacle shape must reach 437 arrows (Sphinx)", 437, maxArrows)
        println("Shape pack verified successfully with 1,061 shapes across 6 tiers reaching 437 arrows!")
    }

    @Test
    fun testDynamicSelectionSimulation() {
        val shapeFile = File("src/main/assets/levels/shape_pack.dat")
        val shapes = decryptPack(shapeFile)

        // Group by tier
        val tier0 = mutableListOf<org.json.JSONObject>()
        for (i in 0 until shapes.length()) {
            val s = shapes.getJSONObject(i)
            if (s.getInt("Tier") == 0) {
                tier0.add(s)
            }
        }
        assertTrue("Tier 0 must have at least 100 shapes", tier0.size >= 100)

        // Simulate two different users
        val user1Seed = 123456789L
        val user2Seed = 987654321L

        val idx1 = Math.floorMod((user1Seed xor (5L * 1000003L)).toInt(), tier0.size)
        val idx2 = Math.floorMod((user2Seed xor (5L * 1000003L)).toInt(), tier0.size)

        val shape1 = tier0[idx1].getString("Hash")
        val shape2 = tier0[idx2].getString("Hash")

        assertTrue("Different users should receive different shapes at Level 5", shape1 != shape2)

        // Simulate retry: same user and level should get identical shape
        val retryIdx = Math.floorMod((user1Seed xor (5L * 1000003L)).toInt(), tier0.size)
        val retryShape = tier0[retryIdx].getString("Hash")
        assertEquals("Retrying same level gives same shape", shape1, retryShape)

        // Simulate completion and exclusion at next milestone (Level 10)
        val unplayedTier0 = tier0.filter { it.getString("Hash") != shape1 }
        val nextIdx = Math.floorMod((user1Seed xor (10L * 1000003L)).toInt(), unplayedTier0.size)
        val nextShape = unplayedTier0[nextIdx].getString("Hash")
        assertTrue("Played shape must not repeat at Level 10", nextShape != shape1)
        println("Dynamic per-user selection simulation verified successfully!")
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
        assertEquals("Level 2000 must have 383 arrows", 383, l2000.getJSONArray("Arrows").length())
        assertEquals("Level 2000 width", 60, l2000.getInt("XSize"))
        assertEquals("Level 2000 height", 74, l2000.getInt("YSize"))

        val parsed = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(2000, l2000)
        assertNotNull("Parsed Level 2000 should not be null", parsed)
        assertEquals("Parsed arrows count for Level 2000 must match", 383, parsed!!.arrows.size)
        println("Pack 01 and Level 2000 Grand Finale verified successfully!")
    }
}
