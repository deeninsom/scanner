import 'package:flutter/material.dart';
import 'fingerprintScanner.dart';
void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  final FingerprintScanner scanner = FingerprintScanner();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fingerprint Scanner',
      home: Scaffold(
        appBar: AppBar(
          title: Text('Fingerprint Scanner'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              ElevatedButton(
                onPressed: () {
                  scanner.listenToFingerprintData();
                  scanner.scanFingerprint(); // Memulai pemindaian sidik jari
                },
                child: Text('Start Fingerprint Scan'),
              ),
              SizedBox(height: 20),
              // Menampilkan gambar sidik jari setelah tertangkap
              FutureBuilder(
                future: Future.delayed(Duration(seconds: 1)), // Tunggu sedikit agar data tersedia
                builder: (context, snapshot) {
                  return scanner.getFingerprintImage();
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}