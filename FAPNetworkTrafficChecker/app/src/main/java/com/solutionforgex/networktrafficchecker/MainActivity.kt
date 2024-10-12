package com.solutionforgex.networktrafficchecker

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var txReceived: TextView
    private lateinit var txTransmitted: TextView
    private lateinit var txReceivedPerSecond: TextView
    private lateinit var txTransmittedPerSecond: TextView
    private lateinit var txConnectionStatus: TextView
    private lateinit var txNetworkLog: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnAutoScroll: Button
    private lateinit var btnToggleRotation: Button
    private lateinit var btnInformation: Button
    private lateinit var btnEnglish: RadioButton
    private lateinit var btnKorean: RadioButton
    private lateinit var chart: LineChart
    private val networkLog = StringBuilder()
    private var isAutoScrollEnabled = true
    private var isEnglish = false
    private var isRotationLocked = false

    // 이전 트래픽 값 저장용
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0

    // 그래프 데이터
    private var timeCounter = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupLanguageSwitch()
        setupChart()
    }

    private fun initializeViews() {
        txReceived = findViewById(R.id.txReceived)
        txTransmitted = findViewById(R.id.txTransmitted)
        txReceivedPerSecond = findViewById(R.id.txReceivedPerSecond)
        txTransmittedPerSecond = findViewById(R.id.txTransmittedPerSecond)
        txConnectionStatus = findViewById(R.id.txConnectionStatus)
        txNetworkLog = findViewById(R.id.txNetworkLog)
        scrollView = findViewById(R.id.scrollView)
        btnAutoScroll = findViewById(R.id.btnAutoScroll)
        btnToggleRotation = findViewById(R.id.btnToggleRotation)
        btnInformation = findViewById(R.id.btnInformation)
        btnEnglish = findViewById(R.id.btnEnglish)
        btnKorean = findViewById(R.id.btnKorean)
        chart = findViewById(R.id.chart)

        btnAutoScroll.setOnClickListener {
            isAutoScrollEnabled = !isAutoScrollEnabled
            updateAutoScrollButtonText()
        }

        val toggleButton: Button = findViewById(R.id.btnToggleRotation)
        toggleButton.setOnClickListener {
            isRotationLocked = !isRotationLocked
            updateRotationLock()
        }

        btnInformation.setOnClickListener {
            val intent = Intent(this, InformationActivity::class.java)
            intent.putExtra("isEnglish", isEnglish)  // Pass the language preference
            startActivity(intent)
        }

    }

    private fun updateRotationLock() {
        if (isRotationLocked) {
            // 화면 회전 잠금
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            // 또는 특정 방향으로 고정하려면 다음 중 하나를 사용:
            // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            btnToggleRotation.text = if(isEnglish) "Rotate Unlock" else "화면회전 Unlock"
        } else {
            // 화면 회전 잠금 해제
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            btnToggleRotation.text = if(isEnglish) "Rotate Lock" else "화면회전 Lock"
        }
    }

    private fun setupLanguageSwitch() {
        btnEnglish.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isEnglish = true
                updateLanguage()
            }
        }

        btnKorean.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isEnglish = false
                updateLanguage()
            }
        }

        // 초기 언어 설정
        btnKorean.isChecked = true
        updateLanguage()
    }

    private fun updateLanguage() {
        supportActionBar?.title = if (isEnglish) getString(R.string.activity_title_english) else getString(R.string.activity_title_korean)
        btnAutoScroll.text = if (isEnglish) if (isAutoScrollEnabled) "Auto Scroll OFF" else "Auto Scroll ON" else if (isAutoScrollEnabled) "자동 스크롤 OFF" else "자동 스크롤 ON"
        btnToggleRotation.text = if(isEnglish) if(isRotationLocked) "Screen Rotate ON" else "Screen Rotate OFF" else if(isRotationLocked) "화면 회전 ON" else "화면 회전 OFF"
        btnInformation.text = if (isEnglish) "Info" else "프로그램 정보"
        updateTexts()
    }

    private fun updateTexts() {
        if (isEnglish) {
            txReceived.text = "Total Received Bytes: ${formatBytes(TrafficStats.getTotalRxBytes())}"
            txTransmitted.text = "Total Transmitted Bytes: ${formatBytes(TrafficStats.getTotalTxBytes())}"
            txReceivedPerSecond.text = "Receive Rate: 0 B/s"
            txTransmittedPerSecond.text = "Transmit Rate: 0 B/s"
            txConnectionStatus.text = "Status: Checking..."
        } else {
            txReceived.text = "총 수신 바이트: ${formatBytes(TrafficStats.getTotalRxBytes())}"
            txTransmitted.text = "총 송신 바이트: ${formatBytes(TrafficStats.getTotalTxBytes())}"
            txReceivedPerSecond.text = "수신 속도: 0 B/s"
            txTransmittedPerSecond.text = "송신 속도: 0 B/s"
            txConnectionStatus.text = "상태: 확인 중..."
        }
    }

    private fun setupChart() {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(true)
            setBackgroundColor(Color.WHITE)
            animateX(1500)
        }

        // X축 설정
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.DKGRAY
            setDrawGridLines(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
//                    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date((value * 1000).toLong()))
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")  // 타임존을 UTC로 설정
                    return dateFormat.format(Date((value * 1000).toLong()))
                }
            }
        }

        // Y축 설정
        chart.axisLeft.apply {
            textColor = Color.DKGRAY
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
            axisLineColor = Color.DKGRAY
        }
        chart.axisRight.isEnabled = false

        // 범례 설정
        chart.legend.apply {
            form = Legend.LegendForm.LINE
            textColor = Color.BLACK
        }

        // 데이터셋 생성 및 설정
        val receivedDataSet = createLineDataSet("Receive", Color.rgb(65, 105, 225))
        val transmittedDataSet = createLineDataSet("Transmit", Color.rgb(220, 20, 60))

        val data = LineData(receivedDataSet, transmittedDataSet)
        chart.data = data
    }

    private fun createLineDataSet(label: String, color: Int): LineDataSet {
        return LineDataSet(null, label).apply {
            setDrawIcons(false)
            setColor(color)
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            formLineWidth = 1f
            formSize = 15f
            fillAlpha = 30
            fillColor = color
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            try {
                // 현재 총 수신 및 송신 바이트
                val received = TrafficStats.getTotalRxBytes()
                val transmitted = TrafficStats.getTotalTxBytes()

                // 초당 수신 및 송신 바이트 계산
                val rxBytesPerSecond = received - previousRxBytes
                val txBytesPerSecond = transmitted - previousTxBytes

                // UI 업데이트
                if (isEnglish) {
                    txReceived.text = "Total Received Bytes: $received (${formatBytes(received)})"
                    txTransmitted.text = "Total Transmitted Bytes: $transmitted (${formatBytes(transmitted)})"
                    txReceivedPerSecond.text = "Receive Rate: ${formatBytesPerSecond(rxBytesPerSecond)}"
                    txTransmittedPerSecond.text = "Transmit Rate: ${formatBytesPerSecond(txBytesPerSecond)}"
                } else {
                    txReceived.text = "총 수신 바이트: $received (${formatBytes(received)})"
                    txTransmitted.text = "총 송신 바이트: $transmitted (${formatBytes(transmitted)})"
                    txReceivedPerSecond.text = "수신 속도: ${formatBytesPerSecond(rxBytesPerSecond)}"
                    txTransmittedPerSecond.text = "송신 속도: ${formatBytesPerSecond(txBytesPerSecond)}"
                }

                // 그래프 데이터 추가
                addEntry(rxBytesPerSecond.toFloat(), txBytesPerSecond.toFloat())

                // 이전 값을 현재 값으로 업데이트
                previousRxBytes = received
                previousTxBytes = transmitted

                // 인터넷 연결 상태 확인
                val (isConnected, networkType) = checkInternetConnection()

                // 연결 상태를 UI에 표시
                if (isConnected) {
                    txConnectionStatus.text = if (isEnglish) "Status: Connected ($networkType)" else "상태: 연결됨 ($networkType)"
                } else {
                    txConnectionStatus.text = if (isEnglish) "Status: Not Connected" else "상태: 연결되지 않음"
                }

                // 로그에 연결 상태 추가
                val logMessage = if (isConnected) {
                    if (isEnglish) {
                        "$networkType Connected. Receive: ${formatBytesPerSecond(rxBytesPerSecond)}, Transmit: ${formatBytesPerSecond(txBytesPerSecond)}"
                    } else {
                        "$networkType 연결됨. 수신: ${formatBytesPerSecond(rxBytesPerSecond)}, 송신: ${formatBytesPerSecond(txBytesPerSecond)}"
                    }
                } else {
                    if (isEnglish) "Not Connected" else "연결되지 않음"
                }
                addNetworkLog(logMessage)

                // 자동 스크롤 기능 실행
                if (isAutoScrollEnabled) {
                    scrollView.post {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                handler.postDelayed(this, 1000) // 1초마다 업데이트
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 이전 트래픽 값 초기화
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()

        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    // 초당 바이트 값을 적절한 단위로 변환하는 함수
    private fun formatBytesPerSecond(bytesPerSecond: Long): String {
        return formatBytes(bytesPerSecond) + "/s"
    }

    // 바이트 값을 적절한 단위로 변환하는 함수
    private fun formatBytes(bytes: Long): String {
        val kilo = 1024.0
        val mega = kilo.pow(2)
        val giga = kilo.pow(3)

        return when {
            bytes >= giga -> String.format("%.2f GB", bytes / giga)
            bytes >= mega -> String.format("%.2f MB", bytes / mega)
            bytes >= kilo -> String.format("%.2f KB", bytes / kilo)
            else -> "$bytes B"
        }
    }

    // 인터넷 연결 상태 확인 함수
    private fun checkInternetConnection(): Pair<Boolean, String> {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = networkCapabilities != null &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val networkType = when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> if (isEnglish) "Wi-Fi" else "와이파이"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> if (isEnglish) "Mobile Data" else "모바일 데이터"
                else -> if (isEnglish) "Unknown" else "알 수 없음"
            }

            Pair(isConnected, networkType)
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected

            val networkType = when (activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> if (isEnglish) "Wi-Fi" else "와이파이"
                ConnectivityManager.TYPE_MOBILE -> if (isEnglish) "Mobile Data" else "모바일 데이터"
                else -> if (isEnglish) "Unknown" else "알 수 없음"
            }

            Pair(isConnected, networkType)
        }
    }

    // 네트워크 로그 추가 함수
    private fun addNetworkLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        networkLog.append("[$timestamp] \n$message\n")
        txNetworkLog.text = networkLog.toString()
    }

    // 자동 스크롤 버튼 텍스트 업데이트 함수
    private fun updateAutoScrollButtonText() {
        btnAutoScroll.text = if (isEnglish) {
            if (isAutoScrollEnabled) "Auto Scroll OFF" else "Auto Scroll ON"
        } else {
            if (isAutoScrollEnabled) "자동 스크롤 OFF" else "자동 스크롤 ON"
        }
    }

    // 그래프에 데이터 추가
    private fun addEntry(receivedBytes: Float, transmittedBytes: Float) {
        chart.data?.let { data ->
            data.dataSets.forEachIndexed { index, _ ->
                val entry = Entry(timeCounter, if (index == 0) receivedBytes else transmittedBytes)
                data.addEntry(entry, index)
            }

            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.moveViewToX(timeCounter)
            chart.setVisibleXRangeMaximum(60f)  // 최근 60초의 데이터만 표시
            timeCounter++
        }
    }
}
