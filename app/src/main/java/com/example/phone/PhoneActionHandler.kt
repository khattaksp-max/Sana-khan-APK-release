package com.example.phone

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import com.example.data.model.ActionCardData
import com.example.data.model.ActionType
import java.net.URLEncoder

class PhoneActionHandler(private val context: Context) {

    fun executeAction(card: ActionCardData): Boolean {
        return try {
            when (card.type) {
                ActionType.WHATSAPP_OPEN -> openWhatsApp()
                ActionType.WHATSAPP_SEND -> sendWhatsAppMessage(card.target, card.description)
                ActionType.CALL_PHONE -> callPhone(card.target)
                ActionType.SET_ALARM -> setAlarm(card.target, card.description)
                ActionType.SET_REMINDER -> createReminder(card.target, card.description)
                ActionType.OPEN_APP -> launchApp(card.target)
                ActionType.WEB_SEARCH -> searchWeb(card.target)
                ActionType.NAVIGATE -> openNavigation(card.target)
            }
        } catch (e: Exception) {
            Log.e("PhoneActionHandler", "Failed to execute action ${card.type}", e)
            Toast.makeText(context, "Could not open action: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openWhatsApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }

        // Fallback to browser or Play Store
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com/")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        return true
    }

    fun sendWhatsAppMessage(recipient: String, message: String): Boolean {
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        // WhatsApp direct send intent
        val uri = Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            // General share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send to $recipient via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }
    }

    fun callPhone(contactOrNumber: String): Boolean {
        val digitsOnly = contactOrNumber.filter { it.isDigit() || it == '+' }
        val uri = if (digitsOnly.isNotBlank()) {
            Uri.parse("tel:$digitsOnly")
        } else {
            Uri.parse("tel:")
        }

        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        return true
    }

    fun setAlarm(timeStr: String, label: String): Boolean {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, min)
            putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "SANA Alarm" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (alarmIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(alarmIntent)
            true
        } else {
            Toast.makeText(context, "Alarm app not available", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun createReminder(title: String, timeInfo: String): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, "Created by SANA voice assistant ($timeInfo)")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            Toast.makeText(context, "Calendar app not available", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun launchApp(appName: String): Boolean {
        val lower = appName.lowercase()
        val pkg = when {
            lower.contains("whatsapp") -> "com.whatsapp"
            lower.contains("youtube") -> "com.google.android.youtube"
            lower.contains("chrome") -> "com.android.chrome"
            lower.contains("map") -> "com.google.android.apps.maps"
            lower.contains("camera") -> {
                val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(camIntent)
                return true
            }
            lower.contains("setting") -> {
                val setIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(setIntent)
                return true
            }
            else -> null
        }

        if (pkg != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        // Try searching installed applications
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(0)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }

        // If not found, search Google Play or Web
        searchWeb(appName)
        return true
    }

    fun searchWeb(query: String): Boolean {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (searchIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(searchIntent)
            true
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        }
    }

    fun openNavigation(destination: String): Boolean {
        val uri = Uri.parse("geo:0,0?q=${URLEncoder.encode(destination, "UTF-8")}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            true
        } else {
            val browserMap = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(destination, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserMap)
            true
        }
    }
}
