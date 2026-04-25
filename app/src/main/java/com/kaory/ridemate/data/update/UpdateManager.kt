package com.kaory.ridemate.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL


object UpdateManager {

    private const val GITHUB_API = "https://api.github.com/repos/igir228/RideMateApp/releases/latest"

    suspend fun checkForUpdate(ctx: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = URL(GITHUB_API).readText()
            val release = JSONObject(json)
            val tagName = release.getString("tag_name")
            val remoteVersion = tagName.removePrefix("v")

            val currentVersion = try {
                val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                info.versionName ?: "0"
            } catch (e: Exception) {
                "0"
            }

            if (compareVersions(remoteVersion, currentVersion) > 0) {
                val assets = release.getJSONArray("assets")
                if (assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    return@withContext UpdateResult.Available(remoteVersion, downloadUrl)
                }
            }
        } catch (_: Exception) {
            // ошибка сети или отсутствие релиза
        }
        UpdateResult.None
    }

    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(aParts.size, bParts.size)) {
            val aVal = aParts.getOrElse(i) { 0 }
            val bVal = bParts.getOrElse(i) { 0 }
            if (aVal != bVal) return aVal - bVal
        }
        return 0
    }

    fun downloadAndInstall(context: Context, url: String) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("RideMate Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ridemate_update.apk")

        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "ridemate_update.apk"
                    )
                    installApk(context!!, file)
                    context.unregisterReceiver(this)
                }
            }
        }

        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    sealed class UpdateResult {
        object None : UpdateResult()
        data class Available(val version: String, val downloadUrl: String) : UpdateResult()
    }
}