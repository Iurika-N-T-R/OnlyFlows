package com.onlyflows.data.lan

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Every sync request and answer is sealed with AES-256-GCM under the key two devices agreed on
 * when pairing: only a paired device can read the data, and a request that doesn't open with a
 * paired device's key is refused (GCM authenticates as well as encrypts).
 */
object LanCrypto {
    private val random = SecureRandom()

    fun newSecret(): String = Base64.getEncoder().encodeToString(ByteArray(32).also(random::nextBytes))

    private fun key(secret: String) = SecretKeySpec(Base64.getDecoder().decode(secret), "AES")

    fun seal(secret: String, plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also(random::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(secret), GCMParameterSpec(128, iv)) }
        return iv + c.doFinal(plain)
    }

    /** The plain bytes, or null when [sealed] wasn't made with [secret] or was altered. */
    fun open(secret: String, sealed: ByteArray): ByteArray? = runCatching {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(secret), GCMParameterSpec(128, sealed, 0, 12)) }
        c.doFinal(sealed, 12, sealed.size - 12)
    }.getOrNull()
}
