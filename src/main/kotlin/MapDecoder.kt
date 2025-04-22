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
        50000000001000000000001
        10111011101011111110101
        10100000101000000010101
        11111010101010111111101
        10000010101010100000001
        10101011111111101011101
        10101010100010001010001
        10101110111010111011101
        10100010000000001010101
        10111010101110111010101
        10001010100010101000101
        11101111111011101011101
        10000000000000100000101
        10111111111010111011111
        10001010100010100000101
        10101010101010111111101
        10101000001010100000001
        11111011101111111110101
        10101010100010101000101
        10101110111010101111101
        10000000000000000000005
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