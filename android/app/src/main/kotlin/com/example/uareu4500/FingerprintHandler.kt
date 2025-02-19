package com.example.uareu4500

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import io.flutter.plugin.common.MethodChannel

class FingerprintsHandler(private val context: Context, private val channel: MethodChannel) {
  private val TAG = "FingerprintsHandler"
  private val ACTION_USB_PERMISSION = "com.example.uareu4500.USB_PERMISSION"
  private val U_ARE_U_4500B_PRODUCT_ID = 10
  private val U_ARE_U_4500B_VENDOR_ID = 1466
  private var usbManager: UsbManager? = null
  private var uruConnection: UruConnection? = null

  init {
      usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
      uruConnection = UruConnection()
      Log.d(TAG, "USB Manager initialized")
  }

  private val usbReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
          if (ACTION_USB_PERMISSION == intent?.action) {
              synchronized(this) {
                  val usbDevice: UsbDevice? = intent.getParcelableExtra("device")
                  if (intent.getBooleanExtra("permission", false)) {
                      Log.d(TAG, "USB permission granted for device: ${usbDevice?.deviceName}")
                      usbDevice?.let { initiateCommunication(it) }
                  } else {
                      Log.e(TAG, "Permission denied to access USB device")
                      channel.invokeMethod("onScanError", "Permission denied")
                  }
              }
          }
      }
  }

  // Fungsi untuk memulai pemindaian sidik jari
  fun scanFingerprint() {
      Log.d(TAG, "Scanning for fingerprint scanner...")

      val deviceList = usbManager?.deviceList
      if (deviceList.isNullOrEmpty()) {
          Log.e(TAG, "No USB devices found")
          channel.invokeMethod("onScanError", "No USB devices found")
          return
      }

      Log.d(TAG, "USB devices found: ${deviceList.values.joinToString { it.deviceName }}")

      val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
          context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT
      )

      val filter = IntentFilter(ACTION_USB_PERMISSION)
      context.registerReceiver(usbReceiver, filter)
      Log.d(TAG, "USB permission receiver registered")

      for (device in deviceList.values) {
          Log.d(TAG, "Checking device: ${device.deviceName} (Vendor: ${device.vendorId}, Product: ${device.productId})")
          if (device.productId == U_ARE_U_4500B_PRODUCT_ID && device.vendorId == U_ARE_U_4500B_VENDOR_ID) {
              Log.d(TAG, "U.are.U 4500 scanner detected, requesting permission...")
              usbManager?.requestPermission(device, pendingIntent)
              return
          }
      }

      Log.e(TAG, "U.are.U 4500 scanner not connected")
      channel.invokeMethod("onScanError", "Scanner not connected")
  }

  // Fungsi untuk memulai komunikasi dengan perangkat USB
  private fun initiateCommunication(usbDevice: UsbDevice) {
    Log.d(TAG, "Initiating communication with device: ${usbDevice.deviceName}")

    val usbInterface: UsbInterface = usbDevice.getInterface(0)
    val connection: UsbDeviceConnection? = usbManager?.openDevice(usbDevice)

    if (connection == null) {
        Log.e(TAG, "Failed to open USB device connection")
        channel.invokeMethod("onScanError", "Failed to connect to device")
        return
    }

    if (!connection.claimInterface(usbInterface, true)) {
        Log.e(TAG, "Failed to claim USB interface")
        channel.invokeMethod("onScanError", "Failed to claim USB interface")
        return
    }

    Log.d(TAG, "USB interface claimed successfully")

    // Inisialisasi perangkat menggunakan UruConnection
    uruConnection?.initReader(connection)

    // Pastikan perangkat siap sebelum melanjutkan ke capture fingerprint
    if (uruConnection?.connection == null) {  // Mengakses mConnection melalui getter
        Log.e(TAG, "USB connection is still null, unable to capture fingerprint.")
        channel.invokeMethod("onScanError", "USB connection error")
        return
    }

    // Menunggu dan menangkap sidik jari
    uruConnection?.awaitFinger()

    // Tangkap data fingerprint
    val fingerprintData = uruConnection?.captureFingerprint()

    if (fingerprintData != null) {
        val fingerprintHexString = fingerprintData.joinToString("") { String.format("%02X", it) }
        channel.invokeMethod("onFingerprintData", fingerprintHexString)
    } else {
        channel.invokeMethod("onScanError", "Fingerprint capture failed")
    }
}


}
