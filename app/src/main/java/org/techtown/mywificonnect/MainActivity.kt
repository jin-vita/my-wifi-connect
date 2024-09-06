package org.techtown.mywificonnect

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.techtown.mywificonnect.databinding.ActivityMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val callBackList = hashMapOf<String, ConnectivityManager.NetworkCallback>()
    private val ssidList = hashMapOf(
        "UNS WiFi 5G" to "uns1234567!",
        "VITA_S20" to "qqqqwwww",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            button.setOnClickListener { connectWiFi() }
            button1.setOnClickListener { connectWiFi("UNS WiFi 5G") }
            button2.setOnClickListener { connectWiFi("VITA_S20") }
        }
    }

    private fun ActivityMainBinding.connectWiFi(id: String = "") {
        printLog("connectWiFi called. id: $id")
        val ssid: String
        val pw: String
        if (id.isBlank()) {
            ssid = idInput.text.toString()
            pw = pwInput.text.toString()
            if (ssid.isBlank() || pw.isBlank()) {
                AppData.showToast(this@MainActivity, "입력값을 확인해주세요. ssid: $ssid, pw: $pw")
                return
            }
        } else {
            ssid = id
            pw = ssidList[ssid] ?: run {
                AppData.showToast(this@MainActivity, "리스트에 없는 ssid 입니다. ssid: $ssid")
                return
            }
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(pw)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (callBackList[ssid] != null) {
            AppData.showToast(this@MainActivity, "이미 callback 등록된 상태.")
            return
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // 연결되었을 때
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                printLog("[$ssid] onAvailable")
            }

            // 연결 시도 했는데 wifi 모듈이 꺼져있을 때
            override fun onUnavailable() {
                super.onUnavailable()
                printLog("onUnavailable")
            }

            // 연결을 잃었을 때
            override fun onLost(network: Network) {
                super.onLost(network)
                printLog("[$ssid] onLost")

                // 콜백 제거
                connectivityManager.unregisterNetworkCallback(callBackList[ssid]!!)
                callBackList.remove(ssid)
            }
        }

        // 콜백 등록
        connectivityManager.requestNetwork(request, networkCallback)
        callBackList[ssid] = networkCallback
    }

    private fun printLog(message: String) = runOnUiThread {
        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        val now = LocalDateTime.now().format(formatter)
        val log = "[$now] $message"
        if (AppData.logList.size > 1000) AppData.logList.removeAt(1)
        AppData.logList.add(log)
        val sb = StringBuilder()
        AppData.logList.forEach { sb.appendLine(it) }
        binding.resultText.text = sb
        moveToBottom(binding.resultText)
    }

    private fun moveToBottom(textView: TextView) = textView.post {
        val scrollAmount = try {
            textView.layout.getLineTop(textView.lineCount) - textView.height
        } catch (_: NullPointerException) {
            0
        }
        if (scrollAmount > 0) textView.scrollTo(0, scrollAmount)
        else textView.scrollTo(0, 0)
    }
}