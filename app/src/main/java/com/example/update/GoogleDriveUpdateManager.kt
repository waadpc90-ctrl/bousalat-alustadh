package com.example.update

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo900
import com.example.ui.theme.Indigo950
import com.example.ui.theme.Purple600
import com.example.ui.theme.Purple900
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class DriveUpdateConfig(
    val driveUrl: String = "",
    val targetVersionName: String = "4.4",
    val targetVersionCode: Int = 17,
    val releaseNotes: String = "تحديث جديد لتطبيق بوصلة الأستاذ يتضمن أحدث الميزات والتحسينات في سجل الدرجات والحضور والغياب.",
    val showPromptOnStartup: Boolean = false,
    val isMandatory: Boolean = false,
    val isUpdateDialogOpen: Boolean = false,
    val isCheckingRemote: Boolean = false,
    val isAutoDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadedSizeText: String = "",
    val downloadStatusMessage: String = "",
    val downloadedApkReadyPath: String = "",
    val lastBroadcastId: String = "",
    val acknowledgedBroadcastId: String = "",
    val acknowledgedDriveFileId: String = "",
    val acknowledgedVersionName: String = "",
    val acknowledgedVersionCode: Int = 0
)

data class RemoteBroadcastPayload(
    val active: Boolean,
    val broadcastId: String,
    val driveUrl: String,
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val autoInstall: Boolean,
    val isMandatory: Boolean
)

data class ApkDownloadOutcome(
    val success: Boolean,
    val apkFile: File? = null,
    val message: String = "",
    val installerLaunched: Boolean = false
)

object GoogleDriveUpdateManager {
    const val CURRENT_APP_VERSION_NAME = "4.4"
    const val CURRENT_APP_VERSION_CODE = 17
    private const val APK_FILE_NAME = "bousalat_alustadh_update.apk"

    // Shared remote broadcast channels between the standalone admin tool and all installed user apps
    const val BROADCAST_CHANNEL_PRIMARY = "https://ntfy.sh/bousalat_alustadh_waad_ota_v1"
    const val BROADCAST_CHANNEL_BACKUP = "https://ntfy.sh/bousalat_alustadh_waad_ota_v2"

    fun normalizeVersionName(rawVersion: String): String {
        return rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
    }

    fun getInstalledVersionName(context: Context?): String {
        if (context == null) return CURRENT_APP_VERSION_NAME
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            normalizeVersionName(pkgInfo.versionName ?: CURRENT_APP_VERSION_NAME)
                .ifBlank { CURRENT_APP_VERSION_NAME }
        } catch (_: Exception) {
            CURRENT_APP_VERSION_NAME
        }
    }

    @Suppress("DEPRECATION")
    fun getInstalledVersionCode(context: Context?): Int {
        if (context == null) return CURRENT_APP_VERSION_CODE
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                pkgInfo.versionCode
            }
            maxOf(code, CURRENT_APP_VERSION_CODE)
        } catch (_: Exception) {
            CURRENT_APP_VERSION_CODE
        }
    }

    /**
     * Compares two semantic version strings (e.g., "4.5" vs "4.4").
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    fun compareVersionNames(v1: String, v2: String): Int {
        val clean1 = normalizeVersionName(v1)
        val clean2 = normalizeVersionName(v2)
        if (clean1 == clean2) return 0
        if (clean1.isBlank()) return -1
        if (clean2.isBlank()) return 1

        val parts1 = clean1.split(Regex("""[^0-9]+""")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(Regex("""[^0-9]+""")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return clean1.compareTo(clean2)
    }

    /**
     * Returns true ONLY if the target update version is strictly newer than the currently installed app version.
     */
    fun isRemoteVersionNewerThanInstalled(
        context: Context?,
        targetVersionName: String,
        targetVersionCode: Int
    ): Boolean {
        val currentName = getInstalledVersionName(context)
        val currentCode = getInstalledVersionCode(context)
        val cleanTargetName = normalizeVersionName(targetVersionName)

        if (cleanTargetName.isNotBlank()) {
            val nameCmp = compareVersionNames(cleanTargetName, currentName)
            if (nameCmp > 0) return true
            if (nameCmp < 0) return false
            // Same versionName: only consider newer if targetVersionCode is strictly higher
            return targetVersionCode > currentCode
        }
        return targetVersionCode > currentCode
    }

    /**
     * Checks whether the given update has already been installed, confirmed ("موافق"), or matches the current version,
     * preventing duplicate update prompts.
     */
    fun isUpdateAlreadyHandled(
        context: Context?,
        config: DriveUpdateConfig,
        broadcastId: String = config.lastBroadcastId,
        driveUrl: String = config.driveUrl,
        targetVersionName: String = config.targetVersionName,
        targetVersionCode: Int = config.targetVersionCode
    ): Boolean {
        // 1. If the installed app version is already >= the target update version, no update is needed
        if (!isRemoteVersionNewerThanInstalled(context, targetVersionName, targetVersionCode)) {
            return true
        }

        // 2. If this specific broadcast ID was already acknowledged/completed by the user
        if (broadcastId.isNotBlank() && broadcastId == config.acknowledgedBroadcastId) {
            return true
        }

        // 3. If the user already acknowledged/installed this exact version and Drive file
        val cleanTargetVer = normalizeVersionName(targetVersionName)
        val cleanAckVer = normalizeVersionName(config.acknowledgedVersionName)
        val currentFileId = extractDriveFileId(driveUrl) ?: driveUrl.trim()
        if (cleanTargetVer.isNotBlank() && cleanTargetVer == cleanAckVer) {
            if (currentFileId.isBlank() || currentFileId == config.acknowledgedDriveFileId) {
                return true
            }
            if (targetVersionCode <= config.acknowledgedVersionCode) {
                return true
            }
        }

        return false
    }

    /**
     * Polls the shared remote broadcast channels to check if the developer has broadcast a new
     * APK update from the standalone admin tool (`ملف_تحديث_التطبيق_المستقل.html`).
     */
    suspend fun fetchLatestRemoteBroadcast(): RemoteBroadcastPayload? = withContext(Dispatchers.IO) {
        val channels = listOf(
            "$BROADCAST_CHANNEL_PRIMARY/json?poll=1&since=all",
            "$BROADCAST_CHANNEL_BACKUP/json?poll=1&since=all"
        )
        for (endpoint in channels) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode in 200..299) {
                    val lines = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                        reader.readLines()
                    }
                    conn.disconnect()
                    // Iterate backwards from the newest broadcast message
                    for (i in lines.indices.reversed()) {
                        val line = lines[i].trim()
                        if (!line.startsWith("{")) continue
                        val envelope = JSONObject(line)
                        if (envelope.optString("event") != "message") continue
                        val msgRaw = envelope.optString("message", "").trim()
                        val payloadObj = if (msgRaw.startsWith("{")) {
                            JSONObject(msgRaw)
                        } else {
                            envelope
                        }
                        if (!payloadObj.has("active") && !payloadObj.has("apkDriveUrl") && !payloadObj.has("driveUrl")) {
                            continue
                        }
                        val active = payloadObj.optBoolean("active", true)
                        val apkUrl = payloadObj.optString("apkDriveUrl")
                            .ifBlank { payloadObj.optString("directDownloadUrl") }
                            .ifBlank { payloadObj.optString("driveUrl") }
                            .trim()
                        val bId = payloadObj.optString("broadcastId")
                            .ifBlank { envelope.optString("id", "ota_${envelope.optLong("time", 0L)}") }
                        val verName = normalizeVersionName(
                            payloadObj.optString("versionName", CURRENT_APP_VERSION_NAME)
                        ).ifBlank { CURRENT_APP_VERSION_NAME }
                        val verCode = payloadObj.optInt("versionCode", CURRENT_APP_VERSION_CODE)
                        val notes = payloadObj.optString(
                            "releaseNotes",
                            "تحديث تلقائي جديد لتطبيق بوصلة الأستاذ يتضمن أحدث الميزات والتحسينات."
                        )
                        val autoInstall = payloadObj.optBoolean("autoInstall", true)
                        val mandatory = payloadObj.optBoolean("isMandatory", true)

                        return@withContext RemoteBroadcastPayload(
                            active = active,
                            broadcastId = bId,
                            driveUrl = apkUrl,
                            versionName = verName,
                            versionCode = verCode,
                            releaseNotes = notes,
                            autoInstall = autoInstall,
                            isMandatory = mandatory
                        )
                    }
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Try backup channel if primary fails
            }
        }
        null
    }

    /**
     * Extracts the Google Drive File ID from any standard Google Drive sharing URL:
     * - https://drive.google.com/file/d/{FILE_ID}/view?usp=sharing
     * - https://drive.google.com/open?id={FILE_ID}
     * - https://drive.google.com/uc?export=download&id={FILE_ID}
     * - https://drive.usercontent.google.com/download?id={FILE_ID}
     */
    fun extractDriveFileId(rawUrl: String): String? {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return null

        val fileDRegex = Regex("""/file/d/([a-zA-Z0-9_-]{10,})""")
        fileDRegex.find(clean)?.groupValues?.getOrNull(1)?.let { return it }

        val idParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]{10,})""")
        idParamRegex.find(clean)?.groupValues?.getOrNull(1)?.let { return it }

        val foldersRegex = Regex("""/folders/([a-zA-Z0-9_-]{10,})""")
        foldersRegex.find(clean)?.groupValues?.getOrNull(1)?.let { return it }

        // Also support raw file ID if user pasted only the 28-44 char ID
        if (clean.matches(Regex("""^[a-zA-Z0-9_-]{25,50}$"""))) {
            return clean
        }

        return null
    }

    /**
     * Ensures update download URLs strictly use HTTPS and belong to trusted Google Drive hosts.
     */
    fun isTrustedUpdateUrl(rawUrl: String): Boolean {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return false
        if (extractDriveFileId(clean) != null && !clean.contains("://")) return true
        return try {
            val uri = Uri.parse(clean)
            val scheme = uri.scheme?.lowercase() ?: ""
            val host = uri.host?.lowercase() ?: ""
            scheme == "https" && (
                host == "drive.google.com" ||
                host == "drive.usercontent.google.com" ||
                host.endsWith(".googleusercontent.com") ||
                host == "docs.google.com"
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Converts a Google Drive share link into a direct binary download link that bypasses
     * the virus scan warning page (`confirm=t`) on `drive.usercontent.google.com`.
     */
    fun toDirectDownloadUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return ""
        val fileId = extractDriveFileId(clean)
        return if (fileId != null) {
            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        } else if (isTrustedUpdateUrl(clean)) {
            clean
        } else {
            ""
        }
    }

    /**
     * Secondary fallback direct URL on `drive.google.com/uc`.
     */
    fun toFallbackUcDownloadUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        val fileId = extractDriveFileId(clean) ?: return toDirectDownloadUrl(clean)
        return "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
    }

    /**
     * Converts a Google Drive URL or ID into the standard Google Drive viewer URL.
     */
    fun toDriveViewUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return ""
        val fileId = extractDriveFileId(clean)
        return if (fileId != null) {
            "https://drive.google.com/file/d/$fileId/view?usp=sharing"
        } else if (isTrustedUpdateUrl(clean)) {
            clean
        } else {
            ""
        }
    }

    /**
     * Builds the ready-to-upload JSON file (`bousalat_alustadh_update.json`) that can be uploaded to Google Drive.
     */
    fun buildUpdateManifestJson(config: DriveUpdateConfig): String {
        val obj = JSONObject()
        val rawUrl = config.driveUrl.trim()
        val directUrl = toDirectDownloadUrl(rawUrl)
        val viewUrl = toDriveViewUrl(rawUrl)
        obj.put("appName", "بوصلة الأستاذ")
        obj.put("packageName", "com.aistudio.alwaadsmart.wbvqkz")
        obj.put("versionName", config.targetVersionName.ifBlank { CURRENT_APP_VERSION_NAME })
        obj.put("versionCode", config.targetVersionCode)
        obj.put("apkDriveUrl", viewUrl.ifBlank { rawUrl })
        obj.put("directDownloadUrl", directUrl.ifBlank { rawUrl })
        obj.put("releaseNotes", config.releaseNotes.ifBlank {
            "تحديث جديد لتطبيق بوصلة الأستاذ يتضمن أحدث الميزات والتحسينات."
        })
        obj.put("showPromptOnStartup", true)
        obj.put("isMandatory", config.isMandatory)
        return obj.toString(2)
    }

    /**
     * Parses a JSON string (either downloaded from Google Drive or pasted directly) into DriveUpdateConfig.
     */
    fun parseUpdateManifestJson(jsonStr: String, fallbackCurrent: DriveUpdateConfig): DriveUpdateConfig? {
        return try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("{")) return null
            val obj = JSONObject(trimmed)
            val apkUrl = obj.optString("apkDriveUrl")
                .ifBlank { obj.optString("directDownloadUrl") }
                .ifBlank { obj.optString("driveUrl") }
                .ifBlank { fallbackCurrent.driveUrl }
            val verName = obj.optString("versionName", fallbackCurrent.targetVersionName)
                .removePrefix("v")
                .removePrefix("V")
                .ifBlank { fallbackCurrent.targetVersionName }
            val verCode = obj.optInt("versionCode", fallbackCurrent.targetVersionCode)
            val notes = obj.optString("releaseNotes", fallbackCurrent.releaseNotes)
                .ifBlank { fallbackCurrent.releaseNotes }
            val mandatory = obj.optBoolean("isMandatory", fallbackCurrent.isMandatory)
            fallbackCurrent.copy(
                driveUrl = apkUrl,
                targetVersionName = verName,
                targetVersionCode = verCode,
                releaseNotes = notes,
                isMandatory = mandatory,
                showPromptOnStartup = obj.optBoolean("showPromptOnStartup", true)
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks a Google Drive link or remote JSON manifest URL.
     */
    suspend fun resolveOrCheckDriveUpdate(
        rawInput: String,
        currentConfig: DriveUpdateConfig
    ): DriveUpdateConfig = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return@withContext currentConfig

        // 1. Check if rawInput is directly a JSON manifest string
        parseUpdateManifestJson(trimmed, currentConfig)?.let { parsed ->
            return@withContext parsed.copy(isUpdateDialogOpen = true, isCheckingRemote = false)
        }

        // 2. Try fetching small text header from the direct download URL in case it's a JSON manifest on Google Drive
        val downloadUrl = toDirectDownloadUrl(trimmed)
        if (downloadUrl.startsWith("http")) {
            try {
                val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Range", "bytes=0-8191")
                }
                val contentType = conn.contentType?.lowercase() ?: ""
                if (conn.responseCode in 200..299 &&
                    contentType.contains("json")
                ) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                        val sb = StringBuilder()
                        var line: String?
                        var totalChars = 0
                        while (reader.readLine().also { line = it } != null && totalChars < 8192) {
                            sb.append(line).append('\n')
                            totalChars += (line?.length ?: 0)
                        }
                        sb.toString()
                    }
                    conn.disconnect()
                    parseUpdateManifestJson(text, currentConfig)?.let { remoteManifest ->
                        return@withContext remoteManifest.copy(
                            isUpdateDialogOpen = true,
                            isCheckingRemote = false
                        )
                    }
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Treat trimmed URL directly as the APK Drive link
            }
        }

        currentConfig.copy(
            driveUrl = trimmed,
            showPromptOnStartup = true,
            isUpdateDialogOpen = true,
            isCheckingRemote = false
        )
    }

    /**
     * Parses Google Drive's HTML virus-scan warning page if returned, and extracts the confirmed
     * direct download URL (including `uuid`, `confirm=t`, `at`, etc.).
     */
    private fun extractConfirmedUrlFromDriveHtml(html: String, fileId: String?): String? {
        // Look for <form id="download-form" action="https://drive.usercontent.google.com/download" ...>
        val uuidMatch = Regex("""name="uuid"\s+value="([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
        val confirmMatch = Regex("""name="confirm"\s+value="([^"]+)"""").find(html)?.groupValues?.getOrNull(1) ?: "t"
        val idMatch = Regex("""name="id"\s+value="([^"]+)"""").find(html)?.groupValues?.getOrNull(1) ?: fileId
        val atMatch = Regex("""name="at"\s+value="([^"]+)"""").find(html)?.groupValues?.getOrNull(1)

        if (!idMatch.isNullOrBlank()) {
            return buildString {
                append("https://drive.usercontent.google.com/download?id=").append(idMatch)
                append("&export=download&confirm=").append(confirmMatch)
                if (!uuidMatch.isNullOrBlank()) {
                    append("&uuid=").append(uuidMatch)
                }
                if (!atMatch.isNullOrBlank()) {
                    append("&at=").append(Uri.encode(atMatch))
                }
            }
        }

        // Or look for direct href with export=download
        val hrefRegex = Regex("""href="(https://drive\.usercontent\.google\.com/download\?[^"]+)"""")
        hrefRegex.find(html)?.groupValues?.getOrNull(1)?.let { rawHref ->
            return rawHref.replace("&amp;", "&")
        }

        return null
    }

    /**
     * Checks if a downloaded file starts with the standard ZIP/APK magic bytes ('P', 'K').
     */
    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 50_000L) return false
        return try {
            file.inputStream().use { input ->
                val b0 = input.read()
                val b1 = input.read()
                b0 == 0x50 && b1 == 0x4B // 'P' 'K'
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun formatMegabytes(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    /**
     * Downloads the APK directly from Google Drive into the app's cache directory while reporting
     * real-time progress, then automatically launches the Android Package Installer to update the app.
     */
    suspend fun downloadAndInstallApkAutomatically(
        context: Context,
        rawUrl: String,
        onProgress: (percent: Int, sizeText: String, status: String) -> Unit
    ): ApkDownloadOutcome = withContext(Dispatchers.IO) {
        val cleanInput = rawUrl.trim()
        if (cleanInput.isEmpty()) {
            return@withContext ApkDownloadOutcome(
                success = false,
                message = "لا يتوفر رابط تحديث حالياً ⚠️"
            )
        }

        try {
            onProgress(2, "", "جاري تجهيز التحديث...")

            // If the link is a JSON manifest, extract its apkDriveUrl first
            var effectiveRawUrl = cleanInput
            parseUpdateManifestJson(cleanInput, DriveUpdateConfig())?.let { parsed ->
                if (parsed.driveUrl.isNotBlank()) effectiveRawUrl = parsed.driveUrl
            }

            val fileId = extractDriveFileId(effectiveRawUrl)
            var targetDownloadUrl = toDirectDownloadUrl(effectiveRawUrl)

            val outputDir = context.externalCacheDir ?: context.cacheDir
            val outputFile = File(outputDir, APK_FILE_NAME)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            var connection = openDriveConnection(targetDownloadUrl)
            var responseCode = connection.responseCode
            var contentType = connection.contentType?.lowercase() ?: ""

            // If a JSON manifest file is returned instead of an APK, read the APK URL inside it
            if (responseCode in 200..299 && contentType.contains("json")) {
                val jsonText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val manifest = parseUpdateManifestJson(jsonText, DriveUpdateConfig())
                if (manifest != null && manifest.driveUrl.isNotBlank()) {
                    effectiveRawUrl = manifest.driveUrl
                    targetDownloadUrl = toDirectDownloadUrl(effectiveRawUrl)
                    connection = openDriveConnection(targetDownloadUrl)
                    responseCode = connection.responseCode
                    contentType = connection.contentType?.lowercase() ?: ""
                }
            }

            // If an HTML confirmation page is returned for large APK files,
            // parse the confirmation form and automatically follow the confirmed download URL
            if (responseCode in 200..299 && contentType.contains("text/html")) {
                val htmlText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val confirmedUrl = extractConfirmedUrlFromDriveHtml(htmlText, fileId)
                    ?: toFallbackUcDownloadUrl(effectiveRawUrl)
                onProgress(5, "", "جاري بدء تنزيل التحديث...")
                connection = openDriveConnection(confirmedUrl)
                responseCode = connection.responseCode
            }

            if (responseCode !in 200..299) {
                connection.disconnect()
                // Fallback to system DownloadManager
                val dmStarted = enqueueSystemDownloadManager(context, targetDownloadUrl)
                return@withContext if (dmStarted) {
                    ApkDownloadOutcome(
                        success = true,
                        apkFile = null,
                        message = "جاري تنزيل التحديث تلقائياً ⬇️",
                        installerLaunched = false
                    )
                } else {
                    ApkDownloadOutcome(
                        success = false,
                        message = "تعذر تنزيل التحديث حالياً ⚠️"
                    )
                }
            }

            val totalBytes = connection.contentLengthLong
            onProgress(
                8,
                if (totalBytes > 0) "0.0 MB / ${formatMegabytes(totalBytes)}" else "0.0 MB",
                "جاري تنزيل التحديث..."
            )

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var downloadedBytes = 0L
                    var lastReportedPercent = 8
                    var lastReportTimeMs = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val now = System.currentTimeMillis()
                        if (now - lastReportTimeMs >= 120) {
                            lastReportTimeMs = now
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 90L) / totalBytes).toInt().coerceIn(8, 98)
                            } else {
                                // Estimate based on ~32 MB typical APK size
                                ((downloadedBytes * 90L) / (32L * 1024L * 1024L)).toInt().coerceIn(8, 95)
                            }
                            lastReportedPercent = percent
                            val sizeLabel = if (totalBytes > 0) {
                                "${formatMegabytes(downloadedBytes)} / ${formatMegabytes(totalBytes)}"
                            } else {
                                formatMegabytes(downloadedBytes)
                            }
                            onProgress(
                                lastReportedPercent,
                                sizeLabel,
                                "جاري تنزيل التحديث... ($lastReportedPercent%)"
                            )
                        }
                    }
                    output.flush()
                }
            }
            connection.disconnect()

            if (!isValidApkFile(outputFile)) {
                outputFile.delete()
                val dmStarted = enqueueSystemDownloadManager(context, targetDownloadUrl)
                if (!dmStarted) {
                    openDriveUpdateUrl(context, effectiveRawUrl, useDirectDownload = true)
                }
                return@withContext ApkDownloadOutcome(
                    success = false,
                    message = "جاري إكمال تنزيل التحديث تلقائياً ⬇️"
                )
            }

            val finalSizeText = formatMegabytes(outputFile.length())
            onProgress(100, finalSizeText, "جاري تثبيت التحديث تلقائياً...")

            val launched = withContext(Dispatchers.Main) {
                installDownloadedApkFile(context, outputFile)
            }

            ApkDownloadOutcome(
                success = true,
                apkFile = outputFile,
                message = "تم تجهيز التحديث وبدء التثبيت التلقائي 📲",
                installerLaunched = launched
            )
        } catch (e: Exception) {
            val targetUrl = toDirectDownloadUrl(cleanInput)
            val dmStarted = enqueueSystemDownloadManager(context, targetUrl)
            if (!dmStarted) {
                withContext(Dispatchers.Main) {
                    openDriveUpdateUrl(context, cleanInput, useDirectDownload = true)
                }
            }
            ApkDownloadOutcome(
                success = false,
                message = "جاري تنزيل التحديث تلقائياً ⬇️"
            )
        }
    }

    private fun openDriveConnection(urlStr: String): HttpURLConnection {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "*/*")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = location
                    redirects++
                    continue
                }
            }
            return conn
        }
        return (URL(currentUrl).openConnection() as HttpURLConnection)
    }

    /**
     * Launches the Android system package installer for a downloaded APK file using FileProvider.
     */
    fun installDownloadedApkFile(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Secondary fallback using Android's system DownloadManager with notification visibility.
     */
    fun enqueueSystemDownloadManager(context: Context, directUrl: String): Boolean {
        if (!directUrl.startsWith("http")) return false
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
            val request = DownloadManager.Request(Uri.parse(directUrl)).apply {
                setTitle("تحديث تطبيق بوصلة الأستاذ")
                setDescription("جاري تنزيل النسخة المحدثة...")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            dm.enqueue(request)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun readFromClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        return item?.coerceToText(context)?.toString()?.trim() ?: ""
    }

    fun openDriveUpdateUrl(context: Context, rawUrl: String, useDirectDownload: Boolean): Boolean {
        val targetUrl = if (useDirectDownload) {
            toDirectDownloadUrl(rawUrl)
        } else {
            toDriveViewUrl(rawUrl)
        }
        if (targetUrl.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * مربع التحديث التلقائي للتطبيق
 */
@Composable
fun GoogleDriveUpdateDialog(
    config: DriveUpdateConfig,
    darkMode: Boolean,
    onDriveUrlChange: (String) -> Unit = {},
    onPasteDriveLink: () -> Unit = {},
    onStartAutoUpdateNow: () -> Unit,
    onInstallReadyApk: () -> Unit = {},
    onOpenInGoogleDrive: () -> Unit,
    onCopyDriveLink: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!config.isUpdateDialogOpen) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentInstalledVer = GoogleDriveUpdateManager.getInstalledVersionName(context)

    // Guard: if the installed version is already equal to or newer than the target update version,
    // do not show the update dialog so the update prompt never repeats.
    if (!config.isAutoDownloading &&
        !GoogleDriveUpdateManager.isRemoteVersionNewerThanInstalled(
            context = context,
            targetVersionName = config.targetVersionName,
            targetVersionCode = config.targetVersionCode
        )
    ) {
        androidx.compose.runtime.LaunchedEffect(config.targetVersionName, config.targetVersionCode) {
            onDismiss()
        }
        return
    }

    val cardBg = if (darkMode) Slate800 else Color.White
    val subBoxBg = if (darkMode) Slate900 else Slate50
    val borderCol = if (darkMode) Slate700 else Slate200
    val textPrimary = if (darkMode) Color.White else Slate900

    Dialog(
        onDismissRequest = {
            if (!config.isMandatory && !config.isAutoDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !config.isMandatory && !config.isAutoDownloading,
            dismissOnClickOutside = !config.isMandatory && !config.isAutoDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("google_drive_update_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, Indigo500.copy(alpha = 0.5f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Gradient Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Indigo900, Purple900, Indigo600)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!config.isMandatory && !config.isAutoDownloading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .clickable { onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(2.dp, Amber400, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (config.isAutoDownloading) {
                                "جاري تحديث التطبيق ⏳"
                            } else {
                                "تحديث جديد متوفر للتطبيق 🚀"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Version Comparison Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = subBoxBg,
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "النسخة الحالية",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate400
                                )
                                Text(
                                    text = "v$currentInstalledVer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = textPrimary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Emerald500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "النسخة الجديدة المتوفرة ✨",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Emerald500
                                )
                                Text(
                                    text = "v${GoogleDriveUpdateManager.normalizeVersionName(config.targetVersionName)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Emerald500
                                )
                            }
                        }
                    }

                    // Live Download Progress Section (only while actively downloading)
                    if (config.isAutoDownloading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Emerald500.copy(alpha = 0.10f))
                                .border(1.5.dp, Emerald500.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = config.downloadStatusMessage.ifBlank { "جاري تنزيل التحديث..." },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (darkMode) Color(0xFF6EE7B7) else Emerald600,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (config.downloadProgressPercent > 0) {
                                        Text(
                                            text = "${config.downloadProgressPercent}%",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                            color = Emerald500
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { (config.downloadProgressPercent.coerceIn(0, 100)) / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = Emerald500,
                                    trackColor = if (darkMode) Slate700 else Slate200
                                )

                                if (config.downloadedSizeText.isNotBlank()) {
                                    Text(
                                        text = "📦 ${config.downloadedSizeText}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Release Notes Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(subBoxBg)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📋 تفاصيل التحديث الجديد:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Indigo500
                            )
                            Text(
                                text = config.releaseNotes.ifBlank {
                                    "• تحديث شامل وتحسينات في الأداء وسجل الدرجات والحضور والغياب."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 19.sp
                                ),
                                color = textPrimary
                            )
                        }
                    }

                    // Primary Action: "موافق" -> downloads and automatically installs without a second button
                    Button(
                        onClick = onStartAutoUpdateNow,
                        enabled = !config.isAutoDownloading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("confirm_drive_update_now_btn")
                    ) {
                        if (config.isAutoDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جاري التحديث (${config.downloadProgressPercent}%)...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "موافق",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }

                    if (!config.isMandatory && !config.isAutoDownloading) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (darkMode) Slate700 else Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("dismiss_drive_update_btn")
                        ) {
                            Text(
                                text = "لاحقاً",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = if (darkMode) Color.White else Slate800
                            )
                        }
                    }
                }
            }
        }
    }
}

