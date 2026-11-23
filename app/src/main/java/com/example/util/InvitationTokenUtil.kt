package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

object InvitationTokenUtil {

    const val INVITE_WEB_BASE_URL = "https://famspend.app/invite"
    const val INVITE_DEEP_LINK_SCHEME = "famspend"
    const val INVITE_DEEP_LINK_HOST = "invite"
    const val DEFAULT_EXPIRATION_DAYS = 7L
    const val DEFAULT_EXPIRATION_MILLIS = DEFAULT_EXPIRATION_DAYS * 24L * 60L * 60L * 1000L

    private val secureRandom = SecureRandom()
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val tokenPattern = Regex("^[A-Za-z0-9_-]{16,128}$")
    private val base64UrlAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /**
     * Generates a cryptographically secure, unpredictable 256-bit URL-safe token.
     */
    fun generateSecureToken(numBytes: Int = 32): String {
        val bytes = ByteArray(numBytes)
        secureRandom.nextBytes(bytes)
        return encodeBase64UrlNoPadding(bytes)
    }

    /**
     * Computes a 64-character lowercase SHA-256 hex digest of the raw invitation token.
     * Only this hash is stored in Firestore and local SQLite; raw bearer tokens are never persisted.
     */
    fun sha256Hex(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val v = byte.toInt() and 0xFF
            hex.append(Character.forDigit(v ushr 4, 16))
            hex.append(Character.forDigit(v and 0x0F, 16))
        }
        return hex.toString()
    }

    fun buildInvitationLink(rawToken: String): String {
        return "$INVITE_WEB_BASE_URL?token=$rawToken"
    }

    fun buildAppDeepLink(rawToken: String): String {
        return "$INVITE_DEEP_LINK_SCHEME://$INVITE_DEEP_LINK_HOST?token=$rawToken"
    }

    /**
     * Extracts a valid raw invitation token from an HTTPS invite URL, custom scheme deep link,
     * or pasted token string. Returns null if malformed.
     */
    fun extractTokenFromInput(rawInput: String?): String? {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        // Check query parameter ?token=... or &token=...
        val queryIndex = trimmed.indexOf("token=")
        val candidate = if (queryIndex >= 0) {
            val afterParam = trimmed.substring(queryIndex + "token=".length)
            val endIndex = afterParam.indexOfFirst { it == '&' || it == '#' || it.isWhitespace() }
            if (endIndex >= 0) afterParam.substring(0, endIndex) else afterParam
        } else {
            trimmed
        }

        val cleaned = candidate.trim()
        return if (tokenPattern.matches(cleaned)) cleaned else null
    }

    fun isValidEmail(email: String): Boolean {
        val normalized = email.trim()
        if (normalized.isEmpty() || normalized.length > 254) return false
        return emailRegex.matches(normalized)
    }

    fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.US)
    }

    fun buildShareMessage(
        householdName: String,
        inviterName: String,
        inviteLink: String,
        personalMessage: String = ""
    ): String {
        val base = buildString {
            append("$inviterName invited you to join \"$householdName\" on FamSpend — manage money together with your family.\n\n")
            if (personalMessage.isNotBlank()) {
                append("\"$personalMessage\"\n\n")
            }
            append("Tap to accept your invitation:\n$inviteLink")
        }
        return base
    }

    private fun encodeBase64UrlNoPadding(data: ByteArray): String {
        val sb = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val hasB1 = i + 1 < data.size
            val hasB2 = i + 2 < data.size
            val b1 = if (hasB1) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) data[i + 2].toInt() and 0xFF else 0

            sb.append(base64UrlAlphabet[b0 ushr 2])
            sb.append(base64UrlAlphabet[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (hasB1) {
                sb.append(base64UrlAlphabet[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
            }
            if (hasB2) {
                sb.append(base64UrlAlphabet[b2 and 0x3F])
            }
            i += 3
        }
        return sb.toString()
    }
}
