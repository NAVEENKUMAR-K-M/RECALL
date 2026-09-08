package com.aigallery.app.domain.organization.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenshotCategory(
    val id: String,
    val displayName: String,
    val description: String
) {
    WORK(
        id = "WORK",
        displayName = "Work",
        description = "Career, jobs, professional projects, and emails"
    ),
    EDUCATION(
        id = "EDUCATION",
        displayName = "Education",
        description = "Courses, tutorials, lectures, and academic notes"
    ),
    SHOPPING(
        id = "SHOPPING",
        displayName = "Shopping",
        description = "Products, carts, discounts, and e-commerce orders"
    ),
    TRAVEL(
        id = "TRAVEL",
        displayName = "Travel",
        description = "Flights, hotels, destinations, and navigation maps"
    ),
    FINANCE(
        id = "FINANCE",
        displayName = "Finance",
        description = "UPI payments, bank receipts, investments, and invoices"
    ),
    SOCIAL_MEDIA(
        id = "SOCIAL_MEDIA",
        displayName = "Social Media",
        description = "Instagram, LinkedIn, Twitter/X, and social posts"
    ),
    ENTERTAINMENT(
        id = "ENTERTAINMENT",
        displayName = "Entertainment",
        description = "Movies, music, streaming, gaming, and memes"
    ),
    TECHNOLOGY(
        id = "TECHNOLOGY",
        displayName = "Technology",
        description = "Code, terminal, developer tools, and hardware specs"
    ),
    MESSAGING(
        id = "MESSAGING",
        displayName = "Messaging",
        description = "WhatsApp chats, SMS conversations, and messaging apps"
    ),
    DOCUMENTS(
        id = "DOCUMENTS",
        displayName = "Documents",
        description = "PDFs, government IDs, certificates, and paperwork"
    ),
    FOOD(
        id = "FOOD",
        displayName = "Food",
        description = "Recipes, Swiggy/Zomato orders, restaurants, and menus"
    ),
    TICKETS(
        id = "TICKETS",
        displayName = "Tickets",
        description = "Concert passes, movie bookings, bus and train tickets"
    ),
    IMPORTANT(
        id = "IMPORTANT",
        displayName = "Important",
        description = "Urgent reminders, passwords, notices, and priority items"
    ),
    OTHER(
        id = "OTHER",
        displayName = "Other",
        description = "General unsorted screenshots awaiting categorization"
    );

    val icon: ImageVector get() = getIcon(this)

    companion object {
        fun fromId(id: String): ScreenshotCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: entries.firstOrNull { it.displayName.equals(id, ignoreCase = true) }
                ?: OTHER
        }

        fun getIcon(category: ScreenshotCategory): ImageVector {
            return when (category) {
                WORK -> Icons.Default.Work
                EDUCATION -> Icons.Default.School
                SHOPPING -> Icons.Default.ShoppingBag
                TRAVEL -> Icons.Default.Flight
                FINANCE -> Icons.Default.AttachMoney
                SOCIAL_MEDIA -> Icons.Default.Share
                ENTERTAINMENT -> Icons.Default.Movie
                TECHNOLOGY -> Icons.Default.Memory
                MESSAGING -> Icons.Default.Chat
                DOCUMENTS -> Icons.Default.Description
                FOOD -> Icons.Default.Restaurant
                TICKETS -> Icons.Default.ConfirmationNumber
                IMPORTANT -> Icons.Default.Bookmark
                OTHER -> Icons.Default.Folder
            }
        }
    }
}
