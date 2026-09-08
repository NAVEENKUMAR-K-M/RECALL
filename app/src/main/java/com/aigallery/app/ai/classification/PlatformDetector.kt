package com.aigallery.app.ai.classification

import com.aigallery.app.data.database.ScreenshotEntityItem
import com.aigallery.app.data.database.ScreenshotPlatformEntity
import java.util.Locale

object PlatformDetector {

    data class PlatformRule(
        val platform: String,
        val urlKeywords: List<String>,
        val textKeywords: List<String>,
        val exactPhrases: List<String>
    )

    private val RULES = listOf(
        PlatformRule(
            platform = "LinkedIn",
            urlKeywords = listOf("linkedin.com", "lnkd.in"),
            textKeywords = listOf("linkedin", "connections", "easy apply", "repost", "followers", "job details"),
            exactPhrases = listOf("easy apply", "mutual connections", "people also viewed")
        ),
        PlatformRule(
            platform = "WhatsApp",
            urlKeywords = listOf("whatsapp.com", "wa.me"),
            textKeywords = listOf("whatsapp", "type a message", "last seen", "read receipt", "forwarded"),
            exactPhrases = listOf("type a message", "last seen today", "end-to-end encrypted")
        ),
        PlatformRule(
            platform = "Instagram",
            urlKeywords = listOf("instagram.com", "ig.me"),
            textKeywords = listOf("instagram", "reels", "liked by", "follow back", "sponsored"),
            exactPhrases = listOf("liked by", "view all comments", "send message")
        ),
        PlatformRule(
            platform = "YouTube",
            urlKeywords = listOf("youtube.com", "youtu.be"),
            textKeywords = listOf("youtube", "subscribers", "subscribe", "views", "live chat", "remix"),
            exactPhrases = listOf("subscribe", "subscribers", "saved to watch later")
        ),
        PlatformRule(
            platform = "Amazon",
            urlKeywords = listOf("amazon.com", "amazon.in", "amzn.to"),
            textKeywords = listOf("amazon", "prime", "add to cart", "buy now", "free delivery", "fulfilled by"),
            exactPhrases = listOf("add to cart", "buy now", "free delivery")
        ),
        PlatformRule(
            platform = "Flipkart",
            urlKeywords = listOf("flipkart.com", "fkrt.it"),
            textKeywords = listOf("flipkart", "supercoins", "special price", "assured", "buy now"),
            exactPhrases = listOf("special price", "f-assured", "bank offer")
        ),
        PlatformRule(
            platform = "Google Maps",
            urlKeywords = listOf("maps.google.com", "goo.gl/maps"),
            textKeywords = listOf("google maps", "directions", "traffic", "mins away", "route"),
            exactPhrases = listOf("start route", "fastest route", "directions to")
        ),
        PlatformRule(
            platform = "Gmail",
            urlKeywords = listOf("mail.google.com"),
            textKeywords = listOf("gmail", "inbox", "unsubscribe", "compose", "primary", "promotions"),
            exactPhrases = listOf("reply to all", "click here to unsubscribe", "view in browser")
        ),
        PlatformRule(
            platform = "Twitter / X",
            urlKeywords = listOf("twitter.com", "x.com"),
            textKeywords = listOf("retweet", "repost", "post your reply", "trending", "what's happening"),
            exactPhrases = listOf("post your reply", "trending in", "repost this")
        ),
        PlatformRule(
            platform = "GitHub",
            urlKeywords = listOf("github.com"),
            textKeywords = listOf("github", "pull request", "repository", "commits", "stars", "fork"),
            exactPhrases = listOf("pull request", "commit history", "star this repository")
        ),
        PlatformRule(
            platform = "Settings",
            urlKeywords = emptyList(),
            textKeywords = listOf("settings", "battery", "wi-fi", "bluetooth", "display & brightness", "about phone"),
            exactPhrases = listOf("about phone", "system update", "apps & notifications")
        ),
        PlatformRule(
            platform = "Chrome",
            urlKeywords = emptyList(),
            textKeywords = listOf("chrome", "search or type url", "new tab", "incognito tab", "bookmarks"),
            exactPhrases = listOf("search or type url", "add to home screen", "open in new tab")
        ),
        PlatformRule(
            platform = "Swiggy",
            urlKeywords = listOf("swiggy.com"),
            textKeywords = listOf("swiggy", "instamart", "delivery partner", "canteen"),
            exactPhrases = listOf("insta mart", "instamart", "swiggy delivery")
        ),
        PlatformRule(
            platform = "Spotify",
            urlKeywords = listOf("spotify.com"),
            textKeywords = listOf("spotify", "premium student", "listen offline", "audio quality"),
            exactPhrases = listOf("premium student", "download to listen", "very high audio quality")
        ),
        PlatformRule(
            platform = "Discord",
            urlKeywords = listOf("discord.gg", "discord.com"),
            textKeywords = listOf("discord", "join the discord"),
            exactPhrases = listOf("join the discord", "discord server")
        ),
        PlatformRule(
            platform = "BikeWale",
            urlKeywords = listOf("bikewale.com"),
            textKeywords = listOf("bikewale", "select your variant"),
            exactPhrases = listOf("bikewale.com", "select your variant")
        ),
        PlatformRule(
            platform = "EliteHubs",
            urlKeywords = listOf("elitehubs.com"),
            textKeywords = listOf("elitehubs", "elite.hubs"),
            exactPhrases = listOf("elitehubs.com", "elite.hubs")
        ),
        PlatformRule(
            platform = "Truecaller",
            urlKeywords = listOf("truecaller.com"),
            textKeywords = listOf("truecaller", "call ended less than"),
            exactPhrases = listOf("call ended less than", "truecaller")
        ),
        PlatformRule(
            platform = "Google Pay",
            urlKeywords = listOf("pay.google.com"),
            textKeywords = listOf("google pay", "gpay", "say hello!", "search transactions", "unopened reward"),
            exactPhrases = listOf("banking name:", "say hello!", "search transactions", "unopened reward", "scratch to reveal")
        )
    )

    fun detect(screenshotId: Long, fullText: String, entities: List<ScreenshotEntityItem>): ScreenshotPlatformEntity? {
        val lowerText = fullText.lowercase(Locale.ROOT)
        var bestPlatform: String? = null
        var bestScore = 0.0f

        // Check extracted URL entities first (highest confidence signal)
        for (entity in entities) {
            if (entity.type == "URL") {
                val urlLower = entity.value.lowercase(Locale.ROOT)
                for (rule in RULES) {
                    if (rule.urlKeywords.any { urlLower.contains(it) }) {
                        return ScreenshotPlatformEntity(
                            screenshotId = screenshotId,
                            platform = rule.platform,
                            confidence = 0.98f
                        )
                    }
                }
            }
        }

        // Check text keywords and exact phrases
        for (rule in RULES) {
            var score = 0.0f

            for (phrase in rule.exactPhrases) {
                if (lowerText.contains(phrase)) {
                    score += 0.45f
                }
            }

            for (keyword in rule.textKeywords) {
                if (lowerText.contains(keyword)) {
                    score += 0.25f
                }
            }

            if (score > bestScore && score >= 0.40f) {
                bestScore = score
                bestPlatform = rule.platform
            }
        }

        return bestPlatform?.let {
            ScreenshotPlatformEntity(
                screenshotId = screenshotId,
                platform = it,
                confidence = bestScore.coerceIn(0.60f, 0.95f)
            )
        }
    }
}
