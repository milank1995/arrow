with open('arrow_native/app/src/test/java/com/arrowescape/puzzle/LevelLoaderCryptoTest.kt', 'r', encoding='utf-8') as f:
    txt = f.read()

test_code = """    @Test
    fun testProceduralFallbackArrowCount() {
        val lvl = com.arrowescape.puzzle.core.engine.LevelGenerator.generateProceduralLevel(14040, 14040L)
        println("Procedural 14040 arrow count: " + lvl.arrows.size)
        println("Procedural 14040 grid: " + lvl.xSize + "x" + lvl.ySize)
    }
}
"""

txt = txt[:txt.rfind("}")] + test_code
with open('arrow_native/app/src/test/java/com/arrowescape/puzzle/LevelLoaderCryptoTest.kt', 'w', encoding='utf-8') as f:
    f.write(txt)
print("Added test!")
