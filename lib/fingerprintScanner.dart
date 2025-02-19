import 'package:flutter/services.dart';
import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/material.dart';

class FingerprintScanner {
  static const MethodChannel _channel = MethodChannel('fingerprint_scanner');

  // Variabel untuk menyimpan data gambar
  Uint8List? _fingerprintImageBytes;

  // Fungsi untuk memulai pemindaian sidik jari
  Future<void> scanFingerprint() async {
    try {
      final String result = await _channel.invokeMethod('scanFingerprint');
      print(result); // Ini hanya menampilkan "Scanning started"
    } on PlatformException catch (e) {
      print("Error: ${e.message}");
    }
  }

  // Fungsi untuk menangani data sidik jari yang tertangkap
  Future<void> listenToFingerprintData() async {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onFingerprintData') {
        String fingerprintHexString = call.arguments;
        print('Fingerprint Data: $fingerprintHexString');

        // Mengonversi hex string ke byte array
        List<int> fingerprintBytes = _hexStringToBytes(fingerprintHexString);
        setFingerprintImage(fingerprintBytes);
      } else if (call.method == 'onScanError') {
        String errorMessage = call.arguments;
        print('Scan Error: $errorMessage');
      }
    });
  }

  // Fungsi untuk mengonversi hex string ke byte array
  List<int> _hexStringToBytes(String hexString) {
    List<int> bytes = [];
    for (int i = 0; i < hexString.length; i += 2) {
      String byteString = hexString.substring(i, i + 2);
      bytes.add(int.parse(byteString, radix: 16));
    }
    return bytes;
  }

  // Fungsi untuk mengonversi byte array menjadi gambar
  void setFingerprintImage(List<int> fingerprintBytes) {
    _fingerprintImageBytes = Uint8List.fromList(fingerprintBytes);
  }

  // Widget untuk menampilkan gambar sidik jari
  Widget getFingerprintImage() {
    if (_fingerprintImageBytes != null) {
      return Image.memory(_fingerprintImageBytes!);  // Menampilkan gambar dari byte array
    } else {
      return Text('No fingerprint data available');
    }
  }
}
