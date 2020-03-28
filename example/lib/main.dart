import 'package:flutter/material.dart';
import 'dart:async';

import 'package:receive_sharing_intent/receive_sharing_intent.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  StreamSubscription _intentDataStreamSubscription;
  List<SharedMediaFile> _sharedFiles;
  String _sharedText;
  SharedTwitterUrl _sharedTwitterUrl;

  @override
  void initState() {
    super.initState();

    // For sharing images coming from outside the app while the app is in the memory
    _intentDataStreamSubscription = ReceiveSharingIntent.getMediaStream()
        .listen((List<SharedMediaFile> value) {
      setState(() {
        _sharedFiles = value;
        print("Shared:" + (_sharedFiles?.map((f) => f.path)?.join(",") ?? ""));
      });
    }, onError: (err) {
      print("getIntentDataStream error: $err");
    });

    // For sharing images coming from outside the app while the app is closed
    ReceiveSharingIntent.getInitialMedia().then((List<SharedMediaFile> value) {
      setState(() {
        _sharedFiles = value;
        print("Shared:" + (_sharedFiles?.map((f) => f.path)?.join(",") ?? ""));
      });
    });

    // For sharing or opening urls/text coming from outside the app while the app is in the memory
    _intentDataStreamSubscription =
        ReceiveSharingIntent.getTextStream().listen((String value) {
      setState(() {
        _sharedText = value;
        print("Shared: $_sharedText");
      });
    }, onError: (err) {
      print("getLinkStream error: $err");
    });

    // For sharing or opening urls/text coming from outside the app while the app is closed
    ReceiveSharingIntent.getInitialText().then((String value) {
      setState(() {
        _sharedText = value;
        print("Shared: $_sharedText");
      });
    });

    _intentDataStreamSubscription = ReceiveSharingIntent.getTwitterUrlStream()
        .listen((SharedTwitterUrl value) {
      setState(() {
        _sharedTwitterUrl = value;
        print("Shared:" + (_sharedTwitterUrl.toString() ?? ""));
      });
    }, onError: (err) {
      print("getIntentDataStream error: $err");
    });

    ReceiveSharingIntent.getInitialTwitterUrl().then((SharedTwitterUrl value) {
      setState(() {
        _sharedTwitterUrl = value;
        print("Shared:" + (_sharedTwitterUrl.toString() ?? ""));
      });
    });
  }

  @override
  void dispose() {
    _intentDataStreamSubscription.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const textStyleBold = const TextStyle(fontWeight: FontWeight.bold);
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Text("Shared files:", style: textStyleBold),
              Text(_sharedFiles?.map((f) => f.path)?.join(",") ?? ""),
              SizedBox(height: 100),
              Text("Shared urls/text:", style: textStyleBold),
              Text(_sharedText ?? ""),
              Text("Shared twitter url:", style: textStyleBold),
              Text(_sharedTwitterUrl.toString() ?? "")
            ],
          ),
        ),
      ),
    );
  }
}
