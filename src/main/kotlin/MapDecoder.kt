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

    // Wypisz wynik w formacie intArrayOf
    println("var grid: Array<IntArray> = arrayOf(")
    map.forEachIndexed { index, row ->
        print("    intArrayOf(${row.joinToString(",")})")
        if (index < map.size - 1) print(",")
        println()
    }
    println(")")
}