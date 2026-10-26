with open('arrow_native/app/src/test/java/com/arrowescape/puzzle/PuzzleEngineTest.kt', 'r', encoding='utf-8') as f:
    txt = f.read()

idx = txt.find("fun testDailyLevelHintCalculation()")
assert idx != -1, "testDailyLevelHintCalculation not found"

prefix = txt[:txt.rfind("    @Test", 0, idx)]

new_tests = """    @Test
    fun testDailyLevelHintCalculation() {
        val file = java.io.File("src/main/assets/levels/daily_pack.dat")
        assertTrue("daily_pack.dat must exist", file.exists())
        val jsonStr = com.arrowescape.puzzle.core.engine.LevelLoader.decryptAndDecompress(java.io.FileInputStream(file))
        val dailyArray = org.json.JSONArray(jsonStr)
        val levelObj = dailyArray.getJSONObject(96) // Level 97
        val level = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(97, levelObj)
        assertNotNull("Level should parse", level)
        val occupancy = GridOccupancyMap(level!!.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }
        val hint = PuzzleSolver.getBestHint(level.arrows, occupancy)
        println("Best hint for daily 97: ${hint?.id} at head ${hint?.head}")
        assertNotNull("Daily challenge must have a valid movable hint", hint)
    }

    @Test
    fun testAuthenticSequentialNormalLevels() {
        val pack0File = java.io.File("src/main/assets/levels/level_pack_00.dat")
        assertTrue("level_pack_00.dat must exist", pack0File.exists())
        val jsonStr0 = com.arrowescape.puzzle.core.engine.LevelLoader.decryptAndDecompress(java.io.FileInputStream(pack0File))
        val pack0Array = org.json.JSONArray(jsonStr0)

        val pack1File = java.io.File("src/main/assets/levels/level_pack_01.dat")
        assertTrue("level_pack_01.dat must exist", pack1File.exists())
        val jsonStr1 = com.arrowescape.puzzle.core.engine.LevelLoader.decryptAndDecompress(java.io.FileInputStream(pack1File))
        val pack1Array = org.json.JSONArray(jsonStr1)

        val testLevels = listOf(1, 2, 3, 4, 5, 10, 20, 21, 25, 50, 100, 500, 1000, 1533)
        for (lvlNum in testLevels) {
            val levelObj = if (lvlNum <= 1000) {
                pack0Array.getJSONObject(lvlNum - 1)
            } else {
                pack1Array.getJSONObject(lvlNum - 1001)
            }
            val level = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJsonObject(lvlNum, levelObj)
            assertNotNull("Level $lvlNum must parse successfully", level)
            assertTrue("Level $lvlNum must have arrows", level!!.arrows.isNotEmpty())
            assertTrue("Level $lvlNum grid must be positive", level.xSize > 0 && level.ySize > 0)
            val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
                rebuild(level.arrows)
            }
            val movable = level.arrows.filter { PuzzleSolver.canMoveOut(it, occupancy) }
            assertTrue("Level $lvlNum must have at least one unblocked arrow to start", movable.isNotEmpty())
        }
    }
}
"""

updated = prefix + new_tests
with open('arrow_native/app/src/test/java/com/arrowescape/puzzle/PuzzleEngineTest.kt', 'w', encoding='utf-8') as f:
    f.write(updated)
print("Updated PuzzleEngineTest.kt successfully!")
