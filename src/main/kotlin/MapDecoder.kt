fun parseMap(input: String): Array<IntArray> {
    // Podziel input na linie
    val lines = input.trim().split("\n")

    // Przetwórz każdą linię na IntArray
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
        55111111111111111111111
        50001000000010001000001
        10111010111110111111101
        10001010001000001010001
        10111011101011111011101
        10001010001000100000001
        11101111101010111111101
        10000010000010000010001
        10111110111111101111101
        10100000101010001010001
        10111011101011111010101
        10000010000010000010101
        11101110101110101110111
        10100000101000100000001
        10111110111110111011101
        10000000100000100000101
        11101111101111111110111
        10000000001000100000101
        11111110111110101010101
        10001010001000001010101
        10111011101110111111101
        10000000000010000000005
        11111111111111111111155

    """.trimIndent()

    val map = parseMap(input)

    // Wypisz wynik w formacie intArrayOf
    println("val grid: Array<IntArray> = arrayOf(")
    map.forEachIndexed { index, row ->
        print("    intArrayOf(${row.joinToString(",")})")
        if (index < map.size - 1) print(",")
        println()
    }
    println(")")
}