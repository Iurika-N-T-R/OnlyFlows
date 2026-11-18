package com.onlyflows.data.lan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanCryptoTest {
    private val secret = LanCrypto.newSecret()
    private val msg = """{"p":"delta"} é""".toByteArray()

    @Test fun roundTrip() = assertArrayEquals(msg, LanCrypto.open(secret, LanCrypto.seal(secret, msg)))

    @Test fun otherKeyIsRefused() = assertNull(LanCrypto.open(LanCrypto.newSecret(), LanCrypto.seal(secret, msg)))

    @Test fun tamperingIsRefused() {
        val sealed = LanCrypto.seal(secret, msg).also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(LanCrypto.open(secret, sealed))
    }

    @Test fun garbageIsRefused() = assertNull(LanCrypto.open(secret, ByteArray(5)))
}
