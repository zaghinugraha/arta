package id.shoko.arta.core.money

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Semua nilai uang & kuantitas memakai BigDecimal. Jangan pernah Double/Float untuk uang. */
object Dec {
    val MC: MathContext = MathContext.DECIMAL128
    val ZERO: BigDecimal = BigDecimal.ZERO
    val ONE: BigDecimal = BigDecimal.ONE
    val HUNDRED: BigDecimal = BigDecimal(100)

    /** Gram per troy ounce. */
    val GRAMS_PER_TROY_OUNCE: BigDecimal = BigDecimal("31.1034768")

    fun of(s: String): BigDecimal = BigDecimal(s.trim())
    fun of(i: Int): BigDecimal = BigDecimal(i)
    fun of(l: Long): BigDecimal = BigDecimal(l)

    /** Parse input pengguna format Indonesia ("1.234,5") maupun format mesin ("1234.5"). */
    fun parseUser(input: String): BigDecimal? {
        val s = input.trim().replace(" ", "").replace("Rp", "", ignoreCase = true)
        if (s.isEmpty()) return null
        val normalized = when {
            s.contains(',') -> s.replace(".", "").replace(',', '.')
            s.count { it == '.' } > 1 -> s.replace(".", "")
            // "1.500" pada konteks Indonesia = seribu lima ratus; "1.5" = satu koma lima.
            s.contains('.') && s.substringAfter('.').length == 3 && s.substringBefore('.').isNotEmpty() -> s.replace(".", "")
            else -> s
        }
        return normalized.toBigDecimalOrNull()
    }
}

fun BigDecimal.safeDiv(other: BigDecimal): BigDecimal =
    if (other.signum() == 0) BigDecimal.ZERO else this.divide(other, Dec.MC)

/** Persentase (0..100) dari part/total. */
fun percentOf(part: BigDecimal, total: BigDecimal): BigDecimal =
    if (total.signum() == 0) BigDecimal.ZERO else part.multiply(Dec.HUNDRED).divide(total, Dec.MC)

fun BigDecimal.roundTo(scale: Int): BigDecimal = setScale(scale, RoundingMode.HALF_UP)

fun Iterable<BigDecimal>.sumDec(): BigDecimal = fold(BigDecimal.ZERO) { a, b -> a.add(b) }

inline fun <T> Iterable<T>.sumOfDec(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { a, t -> a.add(selector(t)) }
