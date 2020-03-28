package com.kasem.receive_sharing_intent

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection
import android.os.Build
import android.util.Log

class ReceiveSharingIntentPlugin(val registrar: Registrar) :
        MethodCallHandler,
        EventChannel.StreamHandler,
        PluginRegistry.NewIntentListener {

    private var initialMedia: JSONArray? = null
    private var latestMedia: JSONArray? = null

    private var initialText: String? = null
    private var latestText: String? = null

    private var initialTwitterUrl: JSONObject? = null
    private var latestTwitterUrl: JSONObject? = null

    private var eventSinkMedia: EventChannel.EventSink? = null
    private var eventSinkText: EventChannel.EventSink? = null
    private var eventSinkTwitterUrl: EventChannel.EventSink? = null

    init {
        handleIntent(registrar.context(), registrar.activity().intent, true)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        when (arguments) {
            "media" -> eventSinkMedia = events
            "text" -> eventSinkText = events
            "twitter_url" -> eventSinkTwitterUrl = events
        }
    }

    override fun onCancel(arguments: Any?) {
        when (arguments) {
            "media" -> eventSinkMedia = null
            "text" -> eventSinkText = null
            "twitter_url" -> eventSinkTwitterUrl = null
        }
    }

    override fun onNewIntent(intent: Intent): Boolean {
        handleIntent(registrar.context(), intent, false)
        return false
    }

    companion object {
        private const val MESSAGES_CHANNEL = "receive_sharing_intent/messages"
        private const val EVENTS_CHANNEL_MEDIA = "receive_sharing_intent/events-media"
        private const val EVENTS_CHANNEL_TEXT = "receive_sharing_intent/events-text"
        private const val EVENTS_CHANNEL_TWITTER_URL = "receive_sharing_intent/events-twitter-url"

        private const val TEXT = "text"
        private const val URL = "url"
        private const val HASHTAGS = "hashtags"
        private const val VIA = "via"
        private const val RELATED = "related"
        private const val IN_REPLY_TO = "in-reply-to"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            // Detect if we've been launched in background
            if (registrar.activity() == null) {
                return
            }

            val instance = ReceiveSharingIntentPlugin(registrar)

            val mChannel = MethodChannel(registrar.messenger(), MESSAGES_CHANNEL)
            mChannel.setMethodCallHandler(instance)

            val eChannelMedia = EventChannel(registrar.messenger(), EVENTS_CHANNEL_MEDIA)
            eChannelMedia.setStreamHandler(instance)

            val eChannelText = EventChannel(registrar.messenger(), EVENTS_CHANNEL_TEXT)
            eChannelText.setStreamHandler(instance)

            val eChannelTwitterUrl = EventChannel(registrar.messenger(), EVENTS_CHANNEL_TWITTER_URL)
            eChannelTwitterUrl.setStreamHandler(instance)

            registrar.addNewIntentListener(instance)
        }
    }


    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method == "getInitialMedia" -> result.success(initialMedia?.toString())
            call.method == "getInitialText" -> result.success(initialText)
            call.method == "getInitialTwitterUrl" -> result.success(initialTwitterUrl?.toString())
            call.method == "reset" -> {
                initialMedia = null
                latestMedia = null
                initialText = null
                latestText = null
                initialTwitterUrl = null
                latestTwitterUrl = null
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun handleIntent(context: Context, intent: Intent, initial: Boolean) {
        when {
            (intent.type?.startsWith("image") == true || intent.type?.startsWith("video") == true)
                    && (intent.action == Intent.ACTION_SEND
                    || intent.action == Intent.ACTION_SEND_MULTIPLE) -> { // Sharing images or videos

                val value = getMediaUris(context, intent)
                if (initial) initialMedia = value
                latestMedia = value
                eventSinkMedia?.success(latestMedia?.toString())
            }
            (intent.type == null || intent.type?.startsWith("text") == true)
                    && intent.action == Intent.ACTION_SEND -> { // Sharing text
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                var value = ""
                if (subject != null) {
                    value += "$subject "
                }
                value += text
                if (initial) initialText = value
                latestText = value
                eventSinkText?.success(latestText)
            }
            intent.action == Intent.ACTION_VIEW -> { // Opening URL
                val value = getTwitterUrl(intent)
                if (initial) initialTwitterUrl = value
                latestTwitterUrl = value
                eventSinkTwitterUrl?.success(latestTwitterUrl)
            }
        }
    }

    private fun getTwitterUrl(intent: Intent?): JSONObject? {
        if (intent == null) return null
        if (intent.dataString == null) return null
        val uri = Uri.parse(intent.dataString)
        val text = uri.getQueryParameter("text") ?: ""
        val url = uri.getQueryParameter("url") ?: ""
        val hashtags = uri.getQueryParameter("hashtags") ?: ""
        val via = uri.getQueryParameter("via") ?: ""
        val related = uri.getQueryParameter("related") ?: ""
        val inReplyTo = uri.getQueryParameter("in-reply-to") ?: ""

        return JSONObject()
                .put(TEXT, text)
                .put(URL, url)
                .put(HASHTAGS, hashtags)
                .put(VIA, via)
                .put(RELATED, related)
                .put(IN_REPLY_TO, inReplyTo)
    }

    private fun getMediaUris(context: Context, intent: Intent?): JSONArray? {
        if (intent == null) return null

        return when {
            intent.action == Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val path = FileDirectory.getAbsolutePath(context, uri)
                if (path != null) {
                    val type = getMediaType(path)
                    val thumbnail = getThumbnail(context, path, type)
                    val duration = getDuration(path, type)
                    JSONArray().put(
                            JSONObject()
                                    .put("path", path)
                                    .put("type", type)
                                    .put("thumbnail", thumbnail)
                                    .put("duration", duration)
                    )
                } else null
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                val value = uris?.mapNotNull { uri ->
                    val path = FileDirectory.getAbsolutePath(context, uri) ?: return@mapNotNull null
                    val type = getMediaType(path)
                    val thumbnail = getThumbnail(context, path, type)
                    val duration = getDuration(path, type)
                    return@mapNotNull JSONObject()
                            .put("path", path)
                            .put("type", type)
                            .put("thumbnail", thumbnail)
                            .put("duration", duration)
                }?.toList()
                if (value != null) JSONArray(value) else null
            }
            else -> null
        }
    }

    private fun getMediaType(path: String?): Int {
        val mimeType = URLConnection.guessContentTypeFromName(path)
        val isImage = mimeType?.startsWith("image") == true
        return if (isImage) 0 else 1
    }

    private fun getThumbnail(context: Context, path: String, type: Int): String? {
        if (type != 1) return null // get video thumbnail only

        val videoFile = File(path)
        val targetFile = File(context.cacheDir, "${videoFile.name}.png")
        val bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)
                ?: return null
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return targetFile.path
    }

    private fun getDuration(path: String, type: Int): Long? {
        if (type != 1) return null // get duration for video only
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull()
        retriever.release()
        return duration
    }
}
