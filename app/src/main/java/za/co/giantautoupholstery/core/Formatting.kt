package za.co.giantautoupholstery.core

import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/** 120000 -> "R1 200"; 35050 -> "R350,50". Prices are "from" estimates, not quotations. */
fun formatRand(cents: Long): String {
    val rand = cents / 100
    val rem = (cents % 100).toInt()
    val grouped = rand.toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (rem == 0) "R$grouped" else "R$grouped,${rem.toString().padStart(2, '0')}"
}

private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("en-ZA"))
fun formatDate(date: LocalDate): String = date.format(dateFmt)

fun firstName(fullName: String?): String =
    fullName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "there"

fun initials(fullName: String?): String {
    val parts = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private const val REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
fun generateReference(random: Random = Random(SecureRandom().nextLong())): String =
    "GAU-" + (1..6).map { REF_ALPHABET[random.nextInt(REF_ALPHABET.length)] }.joinToString("")
