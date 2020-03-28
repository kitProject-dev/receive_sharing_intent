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

    private var initialTwicca: JSONObject? = null
    private var latestTwicca: JSONObject? = null

    private var initialTxiicha: JSONObject? = null
    private var latestTxiicha: JSONObject? = null

    private var eventSinkMedia: EventChannel.EventSink? = null
    private var eventSinkText: EventChannel.EventSink? = null
    private var eventSinkTwicca: EventChannel.EventSink? = null
    private var eventSinkTxiicha: EventChannel.EventSink? = null

    init {
        handleIntent(registrar.context(), registrar.activity().intent, true)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        when (arguments) {
            "media" -> eventSinkMedia = events
            "text" -> eventSinkText = events
            "twicca" -> eventSinkTwicca = events
            "txiicha" -> eventSinkTxiicha = events
        }
    }

    override fun onCancel(arguments: Any?) {
        when (arguments) {
            "media" -> eventSinkMedia = null
            "text" -> eventSinkText = null
            "twicca" -> eventSinkTwicca = null
            "txiicha" -> eventSinkTxiicha = null
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
        private const val EVENTS_CHANNEL_TWICCA = "receive_sharing_intent/events-twicca"
        private const val EVENTS_CHANNEL_TXIICHA = "receive_sharing_intent/events-txiicha"

        private const val ID = "id"
        private const val TEXT = "text"
        private const val LATITUDE = "latitude"
        private const val LONGITUDE = "longitude"
        private const val CREATED_AT = "created_at"
        private const val SOURCE = "source"
        private const val IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        private const val USER_SCREEN_NAME = "user_screen_name"
        private const val USER_NAME = "user_name"
        private const val USER_ID = "user_id"
        private const val USER_PROFILE_IMAGE_URL = "user_profile_image_url"
        private const val USER_PROFILE_IMAGE_URL_MINI = "user_profile_image_url_mini"
        private const val USER_PROFILE_IMAGE_URL_NORMAL = "user_profile_image_url_normal"
        private const val USER_PROFILE_IMAGE_URL_BIGGER = "user_profile_image_url_bigger"

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

            val eChannelTwicca = EventChannel(registrar.messenger(), EVENTS_CHANNEL_TWICCA)
            eChannelTwicca.setStreamHandler(instance)

            val eChannelTxiicha = EventChannel(registrar.messenger(), EVENTS_CHANNEL_TXIICHA)
            eChannelTxiicha.setStreamHandler(instance)

            registrar.addNewIntentListener(instance)
        }
    }


    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method == "getInitialMedia" -> result.success(initialMedia?.toString())
            call.method == "getInitialText" -> result.success(initialText)
            call.method == "getInitialTwicca" -> result.success(initialTwicca?.toString())
            call.method == "getInitialTxiicha" -> result.success(initialTxiicha?.toString())
            call.method == "reset" -> {
                initialMedia = null
                latestMedia = null
                initialText = null
                latestText = null
                initialTwicca = null
                latestTwicca = null
                initialTxiicha = null
                latestTxiicha = null
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
                val value = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (initial) initialText = value
                latestText = value
                eventSinkText?.success(latestText)
            }
            intent.action == Intent.ACTION_VIEW -> { // Opening URL
                val value = intent.dataString
                if (initial) initialText = value
                latestText = value
                eventSinkText?.success(latestText)
            }
            intent.action == "jp.r246.twicca.ACTION_SHOW_TWEET" -> { // Opening URL
                val value = getTwicca(intent)
                if (initial) initialTwicca = value
                latestTwicca = value
                eventSinkTwicca?.success(latestTwicca?.toString())
            }
            intent.action == "net.sinproject.android.txiicha.ACTION_SHOW_TWEET" -> { // Opening URL
                val value = getTxiicha(intent)
                if (initial) initialTxiicha = value
                latestTxiicha = value
                eventSinkTxiicha?.success(latestTxiicha?.toString())
            }
        }
    }

    private fun getTwicca(intent: Intent?): JSONObject? {
        if (intent == null) return null

        val id = intent.getStringExtra(ID)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val latitude = intent.getStringExtra(LATITUDE)
        val longitude = intent.getStringExtra(LONGITUDE)
        val createdAt = intent.getStringExtra(CREATED_AT)
        val source = intent.getStringExtra(SOURCE)
        val inReplyToStatusId = intent.getStringExtra(IN_REPLY_TO_STATUS_ID)
        val userScreenName = intent.getStringExtra(USER_SCREEN_NAME)
        val userName = intent.getStringExtra(USER_NAME)
        val userId = intent.getStringExtra(USER_ID)
        val userProfileImageUrl = intent.getStringExtra(USER_PROFILE_IMAGE_URL)
        val userProfileImageUrlMini = intent.getStringExtra(USER_PROFILE_IMAGE_URL_MINI)
        val userProfileImageUrlNormal = intent.getStringExtra(USER_PROFILE_IMAGE_URL_NORMAL)
        val userProfileImageUrlBigger = intent.getStringExtra(USER_PROFILE_IMAGE_URL_BIGGER)
        return JSONObject()
                .put(ID, id)
                .put(TEXT, text)
                .put(LATITUDE, latitude)
                .put(LONGITUDE, longitude)
                .put(CREATED_AT, createdAt)
                .put(SOURCE, source)
                .put(IN_REPLY_TO_STATUS_ID, inReplyToStatusId)
                .put(USER_SCREEN_NAME, userScreenName)
                .put(USER_NAME, userName)
                .put(USER_ID, userId)
                .put(USER_PROFILE_IMAGE_URL, userProfileImageUrl)
                .put(USER_PROFILE_IMAGE_URL_MINI, userProfileImageUrlMini)
                .put(USER_PROFILE_IMAGE_URL_NORMAL, userProfileImageUrlNormal)
                .put(USER_PROFILE_IMAGE_URL_BIGGER, userProfileImageUrlBigger)
    }

    private fun getTxiicha(intent: Intent?): JSONObject? {
        if (intent == null) return null

        val id = intent.getStringExtra(ID)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val createdAt = intent.getStringExtra(CREATED_AT)
        val source = intent.getStringExtra(SOURCE)
        val inReplyToStatusId = intent.getStringExtra(IN_REPLY_TO_STATUS_ID)
        val userScreenName = intent.getStringExtra(USER_SCREEN_NAME)
        val userName = intent.getStringExtra(USER_NAME)
        val userId = intent.getStringExtra(USER_ID)
        val userProfileImageUrl = intent.getStringExtra(USER_PROFILE_IMAGE_URL)
        val userProfileImageUrlMini = intent.getStringExtra(USER_PROFILE_IMAGE_URL_MINI)
        val userProfileImageUrlNormal = intent.getStringExtra(USER_PROFILE_IMAGE_URL_NORMAL)
        val userProfileImageUrlBigger = intent.getStringExtra(USER_PROFILE_IMAGE_URL_BIGGER)
        return JSONObject()
                .put(ID, id)
                .put(TEXT, text)
                .put(CREATED_AT, createdAt)
                .put(SOURCE, source)
                .put(IN_REPLY_TO_STATUS_ID, inReplyToStatusId)
                .put(USER_SCREEN_NAME, userScreenName)
                .put(USER_NAME, userName)
                .put(USER_ID, userId)
                .put(USER_PROFILE_IMAGE_URL, userProfileImageUrl)
                .put(USER_PROFILE_IMAGE_URL_MINI, userProfileImageUrlMini)
                .put(USER_PROFILE_IMAGE_URL_NORMAL, userProfileImageUrlNormal)
                .put(USER_PROFILE_IMAGE_URL_BIGGER, userProfileImageUrlBigger)
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
