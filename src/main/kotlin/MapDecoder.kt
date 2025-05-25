fun parseMap(input: String): Array<IntArray> {
    val lines = input.trim().split("\n")
    return lines.map { line ->
        line.map { char ->
            when (char) {
                'S' -> 5
                'E' -> 5
                '5' -> 5

                ' ' -> 0
                '0' -> 0

                '█' -> 1
                '1' -> 1
                else -> throw IllegalArgumentException("Nieprawidłowy znak: $char")
            }
        }.toIntArray()
    }.toTypedArray()
}

fun main() {
    val input = """
    15111111111
    10100000001
    10101110101
    10000010101
    10111111111
    10000000101
    10111111101
    10001000001
    11101011111
    10000000001
    11111111101
    """.trimIndent()

    val map = parseMap(input)

    println("RoomTemplate(")
    println("    grid = arrayOf(")
    map.forEachIndexed { index, row ->
        print("        intArrayOf(${row.joinToString(",")})")
        if (index < map.size - 1) print(",")
        println()
    }
    println("    ),")
    println("    scale = ${map.size}")
    println("),")
}