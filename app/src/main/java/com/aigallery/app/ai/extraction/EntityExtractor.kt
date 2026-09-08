package com.aigallery.app.ai.extraction

import com.aigallery.app.data.database.ScreenshotEntityItem
import java.util.regex.Pattern

object EntityExtractor {

    // Strict URL regex requiring either explicit protocol/www or recognized valid TLD
    private val URL_PATTERN = Pattern.compile(
        "\\b(?:https?://|www\\.)[a-zA-Z0-9][-a-zA-Z0-9]*(?:\\.[a-zA-Z0-9][-a-zA-Z0-9]*)+(?:/[^\\s]*)?|\\b[a-zA-Z0-9][-a-zA-Z0-9]*\\.(?:com|org|net|in|io|ai|co|edu|gov|app|dev|me|tech|xyz|info|biz|co\\.in|org\\.in)(?:/[^\\s]*)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "(?:\\+91[-\\s]?)?[6-9]\\d{4}[-\\s]?\\d{5}\\b|\\b\\d{3,4}[-\\s]?\\d{3,4}[-\\s]?\\d{4}\\b"
    )

    private val DATE_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,2}\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{2,4}|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{1,2},?\\s+\\d{2,4}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\b(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun),\\s*\\d{1,2}\\s*(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\b)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val TIME_PATTERN = Pattern.compile(
        "\\b(?:1[0-2]|0?[1-9]):[0-5][0-9]\\s*(?:AM|PM|am|pm)\\b|\\b(?:at|by|onwards|around)\\s+(?:[01]?[0-9]|2[0-3]):[0-5][0-9]\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val MONEY_PATTERN = Pattern.compile(
        "(?:[₹$€£]|Rs\\.?|INR|USD|EUR)\\s*[0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?(?:\\s*(?:Crore|Lakh|k|M|mo|month))?|[0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?\\s*(?:[₹$€£]|Rs\\.?|INR|USD|EUR|/\\s*(?:mo|month))\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val PERCENTAGE_PATTERN = Pattern.compile(
        "\\b\\d{1,3}(?:\\.\\d+)?%\\s*(?:off|discount|cashback|save|savings)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val HASHTAG_PATTERN = Pattern.compile(
        "#[a-zA-Z0-9_]{2,}\\b"
    )

    private val USERNAME_PATTERN = Pattern.compile(
        "@[a-zA-Z0-9_.]{3,}\\b"
    )

    // Curated organization keywords
    private val KNOWN_ORGS = listOf(
        "Google", "Microsoft", "Amazon", "Apple", "Meta", "Netflix", "Uber",
        "iQOO", "vivo", "Samsung", "Nothing", "OnePlus", "Xiaomi",
        "Swiggy", "Zomato", "Instamart", "Flipkart", "Myntra", "Zepto", "Blinkit",
        "LinkedIn", "GitHub", "YouTube", "Spotify", "OpenAI", "Twitter",
        "Adobe", "Intel", "Nvidia", "AMD", "TCS", "Infosys", "Wipro", "HDFC",
        "SBI", "ICICI", "Axis Bank", "Paytm", "PhonePe", "Google Pay", "Razorpay",
        "EliteHubs", "BikeWale", "Truecaller", "Discord", "INE Security"
    )

    // Topic keywords
    private val KNOWN_TOPICS = listOf(
        "Software Engineering", "Software Engineer", "Internship", "Intern",
        "Full Stack", "Frontend", "Backend", "Machine Learning", "Data Science",
        "Product Management", "Cybersecurity", "Cloud Computing", "Penetration Tester",
        "eJPT", "Junior Penetration Tester", "Browser Security", "Security Awareness",
        "Flight Booking", "Hotel Booking", "Boarding Pass", "Train Ticket", "Concert Pass",
        "Invoice", "Salary Slip", "Order Confirmed", "Bank Statement",
        "Interview Scheduled", "Offer Letter", "Subscription", "Scam-Call Detection",
        "Threadripper", "Ryzen", "RTX 6000", "Answer Script"
    )

    fun extract(screenshotId: Long, text: String): List<ScreenshotEntityItem> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<ScreenshotEntityItem>()
        val seenKeys = mutableSetOf<String>()

        fun addEntity(type: String, value: String, confidence: Float = 0.95f) {
            val trimmed = value.trim()
            if (trimmed.length < 2) return
            val key = "$type:$trimmed".lowercase()
            if (seenKeys.add(key)) {
                results.add(
                    ScreenshotEntityItem(
                        screenshotId = screenshotId,
                        type = type,
                        value = trimmed,
                        confidence = confidence,
                        source = "deterministic"
                    )
                )
            }
        }

        // 1. URLs
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            val rawUrl = urlMatcher.group(0) ?: continue
            if (rawUrl.contains(".") && !rawUrl.contains("@") && !rawUrl.endsWith(".")) {
                // Extract clean domain if possible
                val cleanDomain = rawUrl
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .removePrefix("www.")
                    .substringBefore("/")
                if (cleanDomain.length > 3) {
                    addEntity("URL", cleanDomain, 0.95f)
                }
            }
        }

        // 2. Emails
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        while (emailMatcher.find()) {
            emailMatcher.group(0)?.let { addEntity("EMAIL", it, 0.98f) }
        }

        // 3. Phones (filter out dates/numbers that might clash)
        val phoneMatcher = PHONE_PATTERN.matcher(text)
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group(0)?.trim() ?: continue
            val digitsOnly = phone.filter { it.isDigit() }
            if (digitsOnly.length in 10..13) {
                addEntity("PHONE", phone, 0.90f)
            }
        }

        // 4. Dates
        val dateMatcher = DATE_PATTERN.matcher(text)
        while (dateMatcher.find()) {
            dateMatcher.group(0)?.let { addEntity("DATE", it, 0.92f) }
        }

        // 5. Times
        val timeMatcher = TIME_PATTERN.matcher(text)
        while (timeMatcher.find()) {
            val start = timeMatcher.start()
            val matchedTime = timeMatcher.group(0) ?: continue
            // Filter out phone status bar clock at the beginning of OCR
            if (start < 25 && !matchedTime.contains("PM", ignoreCase = true) && !matchedTime.contains("AM", ignoreCase = true) && !matchedTime.contains("onwards", ignoreCase = true)) {
                continue
            }
            addEntity("TIME", matchedTime, 0.88f)
        }

        // 6. Money / Currency
        val moneyMatcher = MONEY_PATTERN.matcher(text)
        while (moneyMatcher.find()) {
            moneyMatcher.group(0)?.let { addEntity("MONEY", it, 0.95f) }
        }

        // 7. Percentages
        val pctMatcher = PERCENTAGE_PATTERN.matcher(text)
        while (pctMatcher.find()) {
            pctMatcher.group(0)?.let { addEntity("PERCENTAGE", it, 0.90f) }
        }

        // 8. Hashtags
        val hashMatcher = HASHTAG_PATTERN.matcher(text)
        while (hashMatcher.find()) {
            hashMatcher.group(0)?.let { addEntity("HASHTAG", it, 0.95f) }
        }

        // 9. Usernames / Handles
        val userMatcher = USERNAME_PATTERN.matcher(text)
        while (userMatcher.find()) {
            userMatcher.group(0)?.let { addEntity("USERNAME", it, 0.90f) }
        }

        // 10. Organizations
        for (org in KNOWN_ORGS) {
            val regex = Regex("\\b${Regex.escape(org)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                addEntity("ORGANIZATION", org, 0.95f)
            }
        }

        // 11. Topics
        for (topic in KNOWN_TOPICS) {
            val regex = Regex("\\b${Regex.escape(topic)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                addEntity("TOPIC", topic, 0.90f)
            }
        }

        return results
    }
}
