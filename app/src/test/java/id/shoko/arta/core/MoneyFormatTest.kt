package id.shoko.arta.core

import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {
    @Test fun full() {
        assertEquals("Rp 1.284.560.000", MoneyFormat.full(BigDecimal("1284560000")))
        assertEquals("+Rp 6.294.300", MoneyFormat.signedFull(BigDecimal("6294300")))
        assertEquals("−Rp 1.200", MoneyFormat.signedFull(BigDecimal("-1200")))
    }

    @Test fun compact() {
        assertEquals("Rp 411,1 jt", MoneyFormat.compact(BigDecimal("411059200")))
        assertEquals("Rp 1,28 M", MoneyFormat.compact(BigDecimal("1284560000")))
        assertEquals("Rp 950 rb", MoneyFormat.compact(BigDecimal("950000")))
        assertEquals("−Rp 69,4 jt", MoneyFormat.compact(BigDecimal("-69366240")))
        assertEquals("+Rp 6,3 jt", MoneyFormat.compact(BigDecimal("6294300"), signed = true))
    }

    @Test fun percent() {
        assertEquals("+1,5%", MoneyFormat.signedPct(BigDecimal("1.5")))
        assertEquals("−1,4%", MoneyFormat.signedPct(BigDecimal("-1.4")))
        assertEquals("0,0%", MoneyFormat.signedPct(BigDecimal("0.01")))
        assertEquals("32%", MoneyFormat.pct(BigDecimal("32.0004")))
        assertEquals("0,49%", MoneyFormat.pct(BigDecimal("0.49"), 2))
    }

    @Test fun quantity() {
        assertEquals("0,05", MoneyFormat.quantity(BigDecimal("0.0500")))
        assertEquals("150", MoneyFormat.quantity(BigDecimal("150")))
        assertEquals("1.500", MoneyFormat.quantity(BigDecimal("1500")))
    }

    @Test fun parseUser() {
        assertEquals(BigDecimal("1234.5"), Dec.parseUser("1.234,5"))
        assertEquals(BigDecimal("0.05"), Dec.parseUser("0,05"))
        assertEquals(BigDecimal("1500"), Dec.parseUser("1.500"))
        assertEquals(BigDecimal("1.5"), Dec.parseUser("1.5"))
        assertEquals(BigDecimal("47009200"), Dec.parseUser("Rp 47.009.200"))
        assertNull(Dec.parseUser("abc"))
    }
}
