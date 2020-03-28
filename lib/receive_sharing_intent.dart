import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';

class ReceiveSharingIntent {
  static const MethodChannel _mChannel =
      const MethodChannel('receive_sharing_intent/messages');

  static const EventChannel _eChannelMedia =
      const EventChannel("receive_sharing_intent/events-media");
  static const EventChannel _eChannelLink =
      const EventChannel("receive_sharing_intent/events-text");
  static const EventChannel _eChannelTwicca =
      const EventChannel("receive_sharing_intent/events-twicca");
  static const EventChannel _eChannelTxiicha =
      const EventChannel("receive_sharing_intent/events-txiicha");

  static Stream<List<SharedMediaFile>> _streamMedia;
  static Stream<String> _streamLink;
  static Stream<SharedTwicca> _streamTwicca;
  static Stream<SharedTxiicha> _streamTxiicha;

  /// Returns a [Future], which completes to one of the following:
  ///
  ///   * the initially stored media uri (possibly null), on successful invocation;
  ///   * a [PlatformException], if the invocation failed in the platform plugin.
  ///
  /// NOTE. The returned media on iOS (iOS ONLY) is already copied to a temp folder.
  /// So, you need to delete the file after you finish using it
  static Future<List<SharedMediaFile>> getInitialMedia() async {
    final String json = await _mChannel.invokeMethod('getInitialMedia');
    if (json == null) return null;
    final encoded = jsonDecode(json);
    return encoded
        .map<SharedMediaFile>((file) => SharedMediaFile.fromJson(file))
        .toList();
  }

  /// Returns a [Future], which completes to one of the following:
  ///
  ///   * the initially stored link (possibly null), on successful invocation;
  ///   * a [PlatformException], if the invocation failed in the platform plugin.
  static Future<String> getInitialText() async {
    return await _mChannel.invokeMethod('getInitialText');
  }

  /// A convenience method that returns the initially stored link
  /// as a new [Uri] object.
  ///
  /// If the link is not valid as a URI or URI reference,
  /// a [FormatException] is thrown.
  static Future<Uri> getInitialTextAsUri() async {
    final String data = await getInitialText();
    if (data == null) return null;
    return Uri.parse(data);
  }

  static Future<SharedTwicca> getInitialTwicca() async {
    final String json = await _mChannel.invokeMethod('getInitialTwicca');
    if (json == null) return null;
    return SharedTwicca.fromJson(jsonDecode(json));
  }

  static Future<SharedTxiicha> getInitialTxiicha() async {
    final String json = await _mChannel.invokeMethod('getInitialTxiicha');
    if (json == null) return null;
    return SharedTxiicha.fromJson(jsonDecode(json));
  }

  /// Sets up a broadcast stream for receiving incoming media share change events.
  ///
  /// Returns a broadcast [Stream] which emits events to listeners as follows:
  ///
  ///   * a decoded data ([List]) event (possibly null) for each successful
  ///   event received from the platform plugin;
  ///   * an error event containing a [PlatformException] for each error event
  ///   received from the platform plugin.
  ///
  /// Errors occurring during stream activation or deactivation are reported
  /// through the `FlutterError` facility. Stream activation happens only when
  /// stream listener count changes from 0 to 1. Stream deactivation happens
  /// only when stream listener count changes from 1 to 0.
  ///
  /// If the app was started by a link intent or user activity the stream will
  /// not emit that initial one - query either the `getInitialMedia` instead.
  static Stream<List<SharedMediaFile>> getMediaStream() {
    if (_streamMedia == null) {
      final stream =
          _eChannelMedia.receiveBroadcastStream("media").cast<String>();
      _streamMedia = stream.transform<List<SharedMediaFile>>(
        new StreamTransformer<String, List<SharedMediaFile>>.fromHandlers(
          handleData: (String data, EventSink<List<SharedMediaFile>> sink) {
            if (data == null) {
              sink.add(null);
            } else {
              final encoded = jsonDecode(data);
              sink.add(encoded
                  .map<SharedMediaFile>(
                      (file) => SharedMediaFile.fromJson(file))
                  .toList());
            }
          },
        ),
      );
    }
    return _streamMedia;
  }

  /// Sets up a broadcast stream for receiving incoming link change events.
  ///
  /// Returns a broadcast [Stream] which emits events to listeners as follows:
  ///
  ///   * a decoded data ([String]) event (possibly null) for each successful
  ///   event received from the platform plugin;
  ///   * an error event containing a [PlatformException] for each error event
  ///   received from the platform plugin.
  ///
  /// Errors occurring during stream activation or deactivation are reported
  /// through the `FlutterError` facility. Stream activation happens only when
  /// stream listener count changes from 0 to 1. Stream deactivation happens
  /// only when stream listener count changes from 1 to 0.
  ///
  /// If the app was started by a link intent or user activity the stream will
  /// not emit that initial one - query either the `getInitialText` instead.
  static Stream<String> getTextStream() {
    if (_streamLink == null) {
      _streamLink = _eChannelLink.receiveBroadcastStream("text").cast<String>();
    }
    return _streamLink;
  }

  /// A convenience transformation of the stream to a `Stream<Uri>`.
  ///
  /// If the value is not valid as a URI or URI reference,
  /// a [FormatException] is thrown.
  ///
  /// Refer to `getTextStream` about error/exception details.
  ///
  /// If the app was started by a share intent or user activity the stream will
  /// not emit that initial uri - query either the `getInitialTextAsUri` instead.
  static Stream<Uri> getTextStreamAsUri() {
    return getTextStream().transform<Uri>(
      new StreamTransformer<String, Uri>.fromHandlers(
        handleData: (String data, EventSink<Uri> sink) {
          if (data == null) {
            sink.add(null);
          } else {
            sink.add(Uri.parse(data));
          }
        },
      ),
    );
  }

  static Stream<SharedTwicca> getTwiccaStream() {
    if (_streamTwicca == null) {
      final stream =
          _eChannelTwicca.receiveBroadcastStream("twicca").cast<String>();
      _streamTwicca = stream.transform<SharedTwicca>(
        new StreamTransformer<String, SharedTwicca>.fromHandlers(
          handleData: (String data, EventSink<SharedTwicca> sink) {
            if (data == null) {
              sink.add(null);
            } else {
              sink.add(SharedTwicca.fromJson(jsonDecode(data)));
            }
          },
        ),
      );
    }
    return _streamTwicca;
  }

  static Stream<SharedTxiicha> getTxiichaStream() {
    if (_streamTxiicha == null) {
      final stream =
          _eChannelTxiicha.receiveBroadcastStream("txiicha").cast<String>();
      _streamTxiicha = stream.transform<SharedTxiicha>(
        new StreamTransformer<String, SharedTxiicha>.fromHandlers(
          handleData: (String data, EventSink<SharedTxiicha> sink) {
            if (data == null) {
              sink.add(null);
            } else {
              sink.add(SharedTxiicha.fromJson(jsonDecode(data)));
            }
          },
        ),
      );
    }
    return _streamTxiicha;
  }

  /// Call this method if you already consumed the callback
  /// and don't want the same callback again
  static void reset() {
    _mChannel.invokeMethod('reset').then((_) {});
  }
}

class SharedMediaFile {
  /// Image or Video path.
  /// NOTE. for iOS only the file is always copied
  final String path;

  /// Video thumbnail
  final String thumbnail;

  /// Video duration in milliseconds
  final int duration;

  /// Whether its a video or image
  final SharedMediaType type;

  SharedMediaFile(this.path, this.thumbnail, this.duration, this.type);

  SharedMediaFile.fromJson(Map<String, dynamic> json)
      : path = json['path'],
        thumbnail = json['thumbnail'],
        duration = json['duration'],
        type = SharedMediaType.values[json['type']];
}

enum SharedMediaType { IMAGE, VIDEO }

class SharedTwicca {
  final String id;
  final String text;
  final String latitude;
  final String longitude;
  final String createdAt;
  final String source;
  final String inReplyToStatusId;
  final String userScreenName;
  final String userName;
  final String userId;
  final String userProfileImageUrl;
  final String userProfileImageUrlMini;
  final String userProfileImageUrlNormal;
  final String userProfileImageUrlBigger;

  SharedTwicca(
      this.id,
      this.text,
      this.latitude,
      this.longitude,
      this.createdAt,
      this.source,
      this.inReplyToStatusId,
      this.userScreenName,
      this.userName,
      this.userId,
      this.userProfileImageUrl,
      this.userProfileImageUrlMini,
      this.userProfileImageUrlNormal,
      this.userProfileImageUrlBigger);

  SharedTwicca.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        text = json['text'],
        latitude = json['latitude'],
        longitude = json['longitude'],
        createdAt = json['created_at'],
        source = json['source'],
        inReplyToStatusId = json['in_reply_to_status_id'],
        userScreenName = json['user_screen_name'],
        userName = json['user_name'],
        userId = json['user_id'],
        userProfileImageUrl = json['user_profile_image_url'],
        userProfileImageUrlMini = json['user_profile_image_url_mini'],
        userProfileImageUrlNormal = json['user_profile_image_url_normal'],
        userProfileImageUrlBigger = json['user_profile_image_url_bigger'];

  @override
  String toString() => "id:$id, "
      "text:$text\n"
      "text:$text\n"
      "latitude:$latitude\n"
      "longitude:$longitude\n"
      "createdAt:$createdAt\n"
      "source:$source\n"
      "inReplyToStatusId:$inReplyToStatusId\n"
      "userScreenName:$userScreenName\n"
      "userName:$userName\n"
      "userId:$userId\n"
      "userProfileImageUrl:$userProfileImageUrl\n"
      "userProfileImageUrlMini:$userProfileImageUrlMini\n"
      "userProfileImageUrlNormal:$userProfileImageUrlNormal\n"
      "userProfileImageUrlBigger:$userProfileImageUrlBigger\n";
}

class SharedTxiicha {
  final String id;
  final String text;
  final String createdAt;
  final String source;
  final String inReplyToStatusId;
  final String userScreenName;
  final String userName;
  final String userId;
  final String userProfileImageUrl;
  final String userProfileImageUrlMini;
  final String userProfileImageUrlNormal;
  final String userProfileImageUrlBigger;

  SharedTxiicha(
      this.id,
      this.text,
      this.createdAt,
      this.source,
      this.inReplyToStatusId,
      this.userScreenName,
      this.userName,
      this.userId,
      this.userProfileImageUrl,
      this.userProfileImageUrlMini,
      this.userProfileImageUrlNormal,
      this.userProfileImageUrlBigger);

  SharedTxiicha.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        text = json['text'],
        createdAt = json['created_at'],
        source = json['source'],
        inReplyToStatusId = json['in_reply_to_status_id'],
        userScreenName = json['user_screen_name'],
        userName = json['user_name'],
        userId = json['user_id'],
        userProfileImageUrl = json['user_profile_image_url'],
        userProfileImageUrlMini = json['user_profile_image_url_mini'],
        userProfileImageUrlNormal = json['user_profile_image_url_normal'],
        userProfileImageUrlBigger = json['user_profile_image_url_bigger'];

  @override
  String toString() => "id:$id, "
      "text:$text\n"
      "text:$text\n"
      "createdAt:$createdAt\n"
      "source:$source\n"
      "inReplyToStatusId:$inReplyToStatusId\n"
      "userScreenName:$userScreenName\n"
      "userName:$userName\n"
      "userId:$userId\n"
      "userProfileImageUrl:$userProfileImageUrl\n"
      "userProfileImageUrlMini:$userProfileImageUrlMini\n"
      "userProfileImageUrlNormal:$userProfileImageUrlNormal\n"
      "userProfileImageUrlBigger:$userProfileImageUrlBigger\n";
}
