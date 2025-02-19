package com.example.uareu4500

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import com.example.uareu4500.FingerprintsHandler

class MainActivity: FlutterActivity() {
    private val CHANNEL = "fingerprint_scanner"
    private lateinit var fingerprintsHandler: FingerprintsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "scanFingerprint") {
                fingerprintsHandler = FingerprintsHandler(this, MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL))
                fingerprintsHandler.scanFingerprint()
                result.success("Scanning started")
            } else {
                result.notImplemented()
            }
        }
    }
}
