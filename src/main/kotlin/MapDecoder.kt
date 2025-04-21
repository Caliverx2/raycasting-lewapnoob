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
        5511111111111111
        5000000001000001
        1001111111111001
        1000000001000001
        1111111001001111
        1000000001001001
        1001001111001001
        1001000000000001
        1111111001001001
        1000000001001005
        1111111111111155

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