path = 'arrow_native/app/src/test/java/com/arrowescape/puzzle/LevelLoaderCryptoTest.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

target = """    @Test
    fun testProceduralFallbackArrowCount() {
        val lvl = com.arrowescape.puzzle.core.engine.LevelGenerator.generateProceduralLevel(14040, 14040L)
        println("Procedural 14040 arrow count: " + lvl.arrows.size)
        println("Procedural 14040 grid: " + lvl.xSize + "x" + lvl.ySize)
    }
}"""

replacement = """    @Test
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
}"""

c_normalized = c.replace('\r\n', '\n')
target_normalized = target.replace('\r\n', '\n')
replacement_normalized = replacement.replace('\r\n', '\n')

if target_normalized in c_normalized:
    res = c_normalized.replace(target_normalized, replacement_normalized)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(res)
    print("Updated LevelLoaderCryptoTest.kt successfully!")
else:
    print("Target not found in c_normalized")
