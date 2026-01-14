package com.byd.autolock.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.byd.autolock.MainActivity
import com.byd.autolock.R
import com.byd.autolock.data.repositories.BluetoothRepository
import com.byd.autolock.utils.PreferencesManager
import kotlinx.coroutines.*

class BluetoothMonitorService : Service() {

    private val TAG = "BluetoothMonitorService"

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var bluetoothRepository: BluetoothRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var signalStrengthMonitorJob: Job? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "设备已连接: ${it.name} (${it.address})")
                        handleDeviceConnected(it)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "设备已断开: ${it.name} (${it.address})")
                        handleDeviceDisconnected(it)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    Log.d(TAG, "蓝牙状态改变: $state")
                    handleBluetoothStateChanged(state)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        preferencesManager = PreferencesManager(this)
        bluetoothRepository = BluetoothRepository(this)

        createNotificationChannel()
        registerBluetoothReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动")

        val notification = createNotification("蓝牙监听服务运行中", "正在监控车辆连接状态")
        startForeground(NOTIFICATION_ID, notification)

        startSignalStrengthMonitoring()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务销毁")

        unregisterReceiver(bluetoothReceiver)
        signalStrengthMonitorJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "比亚迪自动控车服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监控蓝牙连接状态，自动控制车辆"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        // 检查是否是目标车辆设备
        val targetDevices = preferencesManager.getTargetDevices()
        if (targetDevices.contains(device.address)) {
            Log.d(TAG, "连接到目标车辆设备: ${device.name}")

            // 获取初始信号强度
            serviceScope.launch {
                val rssi = getDeviceRssi(device)
                bluetoothRepository.notifyDeviceConnected(device, rssi)

                // 更新通知
                updateNotification("已连接到车辆", "信号强度: ${rssi}dBm")
            }
        }
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        val targetDevices = preferencesManager.getTargetDevices()
        if (targetDevices.contains(device.address)) {
            Log.d(TAG, "车辆设备断开连接: ${device.name}")

            bluetoothRepository.notifyDeviceDisconnected(device)

            // 更新通知
            updateNotification("车辆连接已断开", "等待重新连接...")
        }
    }

    private fun handleBluetoothStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                Log.d(TAG, "蓝牙已开启")
                updateNotification("蓝牙监听服务运行中", "正在监控车辆连接状态")
            }
            BluetoothAdapter.STATE_OFF -> {
                Log.d(TAG, "蓝牙已关闭")
                updateNotification("蓝牙已关闭", "请开启蓝牙以使用自动控车功能")
            }
        }
    }

    private fun startSignalStrengthMonitoring() {
        signalStrengthMonitorJob?.cancel()
        signalStrengthMonitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    // 获取已连接的车辆设备并检查信号强度
                    val targetDevices = preferencesManager.getTargetDevices()
                    val connectedDevices = bluetoothAdapter.bondedDevices.filter {
                        targetDevices.contains(it.address)
                    }

                    for (device in connectedDevices) {
                        val rssi = getDeviceRssi(device)
                        if (rssi != null) {
                            bluetoothRepository.notifySignalStrengthChanged(rssi)
                        }
                    }

                    delay(5000) // 每5秒检查一次
                } catch (e: Exception) {
                    Log.e(TAG, "信号强度监控错误", e)
                    delay(10000) // 出错后等待10秒再试
                }
            }
        }
    }

    private fun getDeviceRssi(device: BluetoothDevice): Int? {
        return try {
            // 注意：这是一个简化的实现
            // 在实际应用中，你可能需要使用BluetoothGatt来获取准确的RSSI
            // 这里我们返回一个模拟值，实际实现需要根据设备情况调整
            val method = device.javaClass.getMethod("getRssi")
            method.invoke(device) as? Int
        } catch (e: Exception) {
            Log.w(TAG, "无法获取RSSI: ${e.message}")
            // 返回默认值或null
            null
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "byd_auto_lock_channel"
    }
}