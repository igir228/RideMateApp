package com.kaory.ridemate.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.kaory.ridemate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL


object UpdateManager {

    private const val GITHUB_API = "https://api.github.com/repos/YOUR_USER/YOUR_REPO/releases/latest"
    private var context: Context? = null

    suspend fun checkForUpdate(ctx: Context): UpdateResult = withContext(Dispatchers.IO) {
        context = ctx
        try {
            val json = URL(GITHUB_API).readText()
            val release = JSONObject(json)
            val tagName = release.getString("tag_name")
            val version = tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            if (version > currentVersion) {
                val assets = release.getJSONArray("assets")
                if (assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    return@withContext UpdateResult.Available(version, downloadUrl)
                }
            }
        } catch (e: Exception) {
            // ignored
        }
        UpdateResult.None
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