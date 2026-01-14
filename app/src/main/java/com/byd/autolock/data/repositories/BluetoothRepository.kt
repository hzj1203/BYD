package com.byd.autolock.data.repositories

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log

class BluetoothRepository(private val context: Context) {

    private val TAG = "BluetoothRepository"

    private var onDeviceConnected: ((BluetoothDevice, Int?) -> Unit)? = null
    private var onDeviceDisconnected: ((BluetoothDevice) -> Unit)? = null
    private var onSignalStrengthChanged: ((Int) -> Unit)? = null

    fun setCallbacks(
        onDeviceConnected: (BluetoothDevice, Int?) -> Unit,
        onDeviceDisconnected: (BluetoothDevice) -> Unit,
        onSignalStrengthChanged: (Int) -> Unit
    ) {
        this.onDeviceConnected = onDeviceConnected
        this.onDeviceDisconnected = onDeviceDisconnected
        this.onSignalStrengthChanged = onSignalStrengthChanged
    }

    fun notifyDeviceConnected(device: BluetoothDevice, rssi: Int? = null) {
        Log.d(TAG, "通知设备连接: ${device.name} (${device.address}), RSSI: $rssi")
        try {
            onDeviceConnected?.invoke(device, rssi)
        } catch (e: Exception) {
            Log.e(TAG, "处理设备连接回调失败", e)
        }
    }

    fun notifyDeviceDisconnected(device: BluetoothDevice) {
        Log.d(TAG, "通知设备断开: ${device.name} (${device.address})")
        try {
            onDeviceDisconnected?.invoke(device)
        } catch (e: Exception) {
            Log.e(TAG, "处理设备断开回调失败", e)
        }
    }

    fun notifySignalStrengthChanged(rssi: Int) {
        Log.d(TAG, "通知信号强度变化: ${rssi}dBm")
        try {
            onSignalStrengthChanged?.invoke(rssi)
        } catch (e: Exception) {
            Log.e(TAG, "处理信号强度回调失败", e)
        }
    }

    fun getConnectedDevices(): List<BluetoothDevice> {
        // 这里可以返回当前已连接的设备列表
        // 实际实现需要根据系统API获取
        return emptyList()
    }

    fun isDeviceInRange(device: BluetoothDevice, thresholdDbm: Int): Boolean {
        // 检查设备是否在信号范围内
        // 这需要实际的RSSI值来判断
        return true // 简化实现
    }

    fun calculateDistance(rssi: Int): Double {
        // 根据RSSI估算距离（简化算法）
        // 实际应该使用更复杂的路径损耗模型
        val txPower = -59 // 1米处的参考RSSI值
        if (rssi == 0) return -1.0

        val ratio = (txPower - rssi).toDouble() / 20.0
        return Math.pow(10.0, ratio)
    }
}