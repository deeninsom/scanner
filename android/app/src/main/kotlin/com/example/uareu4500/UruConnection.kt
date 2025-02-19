package com.example.uareu4500

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

class UruConnection {
  companion object {
      private const val CTRL_IN = 192
      private const val CTRL_OUT = 64
      private const val CTRL_TIMEOUT = 5000
      private const val REG_MODE = 78
      private const val MODE_OFF: Byte = 112
      private const val MODE_AWAIT_FINGER_ON: Byte = 16
      private const val MODE_CAPTURE: Byte = 32
      private const val LOG_TAG = "UareU-Device"
      var isOn = false
  }

  private var currentMode: Byte = 0
  private var mConnection: UsbDeviceConnection? = null

  val connection: UsbDeviceConnection?
        get() = mConnection
  // Fungsi untuk menginisialisasi pembaca sidik jari
  fun initReader(connection: UsbDeviceConnection) {
    Log.d(LOG_TAG, "Initializing device")
    mConnection = connection // Inisialisasi mConnection

    val registerData = readRegister(connection, 7, 1)
    var status = registerData[0]

    if ((status.toInt() and 0x80) == 0) {
        registerData[0] = (status.toInt() or 0x80).toByte()
        setRegister(connection, 7, registerData, 1)
    }

    repeat(200) {
        if ((status.toInt() and 0x80) == 0) return@repeat
        registerData[0] = (status.toInt() and 0x07).toByte()
        setRegister(connection, 7, registerData, 1)
        status = readRegister(connection, 7, 1)[0]
    }

    if ((status.toInt() and 0x80) == 0) {
        Log.d(LOG_TAG, "<---- SCANNER TURNED ON ----->")
        isOn = true
    }
}

  private fun readRegister(connection: UsbDeviceConnection, reg: Int, length: Int): ByteArray {
      val buffer = ByteArray(length)
      val result = connection.controlTransfer(CTRL_IN, 4, reg, 0, buffer, length, CTRL_TIMEOUT)
      if (result < 0) {
          Log.e(LOG_TAG, "Read device error: $result")
      }
      return buffer
  }

  private fun setRegister(connection: UsbDeviceConnection, reg: Int, data: ByteArray, length: Int) {
      val result = connection.controlTransfer(CTRL_OUT, 4, reg, 0, data, length, CTRL_TIMEOUT)
      if (result < length) {
          Log.e(LOG_TAG, "Error sending data: $result")
      }
  }

  // Fungsi untuk mengatur mode pemindaian sidik jari
  fun setMode(mode: Byte) {
      mConnection?.let {
          setRegister(it, REG_MODE, byteArrayOf(mode), 1)
          currentMode = mode
      }
  }

  // Fungsi untuk mengecek apakah sidik jari terdeteksi
private fun isFingerDetected(): Boolean {
  // Assuming your device has a specific register or command to check if the finger is detected
  val statusData = readRegister(mConnection!!, 8, 1) // 8 is a hypothetical register that could hold the finger detection status
  
  // For example, let's assume that the device returns 0x01 if the finger is detected
  return statusData[0] == 0x01.toByte()
}

// Fungsi untuk menunggu sidik jari dan memulai pemindaian jika sidik jari terdeteksi
fun awaitFinger() {
  val startTime = System.currentTimeMillis()
  setMode(MODE_AWAIT_FINGER_ON) // Set the mode to wait for a finger

  while (System.currentTimeMillis() - startTime < 5000) {  // Wait for 5 seconds
      if (isFingerDetected()) {
          Log.d(LOG_TAG, "Finger detected, starting capture.")
          setMode(MODE_CAPTURE)  // Start the capture mode
          val fingerprint = captureFingerprint() // Capture the fingerprint data
          if (fingerprint != null) {
              Log.d(LOG_TAG, "Fingerprint captured successfully.")
          } else {
              Log.e(LOG_TAG, "Failed to capture fingerprint.")
          }
          return
      }
  }

  // Timeout reached, turn off scanner if no finger detected
  Log.d(LOG_TAG, "No finger detected within timeout, turning off scanner.")
  setMode(MODE_OFF)
  isOn = false
}


  // Fungsi untuk menangkap data sidik jari
  fun captureFingerprint(): ByteArray? {
    if (mConnection == null) {
        Log.e(LOG_TAG, "USB connection is null. Cannot capture fingerprint.")
        return null
    }

    // Melakukan proses capture fingerprint di sini
    val buffer = ByteArray(512)  // Misalnya, ukuran buffer untuk fingerprint
    val result = mConnection?.controlTransfer(CTRL_IN, 4, 128, 0, buffer, buffer.size, CTRL_TIMEOUT)

    if (result != null && result > 0) {
        Log.d(LOG_TAG, "Fingerprint captured successfully")
        return buffer
    } else {
        Log.e(LOG_TAG, "Error capturing fingerprint")
        return null
    }
}

  // Fungsi untuk mematikan perangkat
  fun turnScannerOff() {
      if (currentMode == MODE_OFF || !isOn) return
      setMode(MODE_OFF)
      isOn = false
  }
}


