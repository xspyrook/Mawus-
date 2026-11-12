package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap?
)

object AppLauncherHelper {

    fun getFavoriteApps(context: Context, limit: Int = 5): List<LaunchableApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val selfPackage = context.packageName

        val apps = mutableListOf<LaunchableApp>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage) continue // Skip ourselves

            val label = info.loadLabel(pm).toString()
            val drawable = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                null
            }
            val bitmap = drawable?.let { drawableToImageBitmap(it) }
            apps.add(LaunchableApp(packageName = pkg, label = label, iconBitmap = bitmap))
            if (apps.size >= limit) break
        }

        return apps
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width.coerceAtMost(192), height.coerceAtMost(192), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
