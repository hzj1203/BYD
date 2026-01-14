package com.byd.autolock.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.byd.autolock.data.repositories.BluetoothRepository
import com.byd.autolock.data.repositories.CarControlRepository
import com.byd.autolock.utils.PreferencesManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepository = BluetoothRepository(application)
    private val carControlRepository = CarControlRepository()
    private val preferencesManager = PreferencesManager(application)

    // 服务运行状态
    private val _isServiceRunning = MutableLiveData<Boolean>(false)
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning

    // 蓝牙状态
    private val _bluetoothState = MutableLiveData<Int>()
    val bluetoothState: LiveData<Int> = _bluetoothState

    // 已连接设备
    private val _connectedDevice = MutableLiveData<BluetoothDevice?>()
    val connectedDevice: LiveData<BluetoothDevice?> = _connectedDevice

    // 最后操作
    private val _lastAction = MutableLiveData<String>()
    val lastAction: LiveData<String> = _lastAction

    // 信号强度
    private val _connectionStrength = MutableLiveData<Int?>()
    val connectionStrength: LiveData<Int?> = _connectionStrength

    // 车辆状态
    private val _carLocked = MutableLiveData<Boolean>(true)
    val carLocked: LiveData<Boolean> = _carLocked

    init {
        // 初始化蓝牙监听
        bluetoothRepository.setCallbacks(
            onDeviceConnected = { device, rssi ->
                _connectedDevice.value = device
                _connectionStrength.value = rssi
                _lastAction.value = "设备已连接: ${device.name}"

                // 检查是否应该自动解锁
                if (shouldAutoUnlock(rssi)) {
                    viewModelScope.launch {
                        unlockCar()
                    }
                }
            },
            onDeviceDisconnected = { device ->
                _connectedDevice.value = null
                _connectionStrength.value = null
                _lastAction.value = "设备已断开: ${device.name}"

                // 检查是否应该自动上锁
                if (shouldAutoLock()) {
                    viewModelScope.launch {
                        lockCar()
                    }
                }
            },
            onSignalStrengthChanged = { rssi ->
                _connectionStrength.value = rssi

                // 根据信号强度动态控制
                when {
                    shouldAutoUnlock(rssi) && _carLocked.value == true -> {
                        viewModelScope.launch {
                            unlockCar()
                        }
                    }
                    shouldAutoLock() && _carLocked.value == false -> {
                        viewModelScope.launch {
                            lockCar()
                        }
                    }
                }
            }
        )
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateBluetoothState(state: Int) {
        _bluetoothState.value = state
    }

    suspend fun unlockCar() {
        try {
            val success = carControlRepository.unlockCar(
                vin = preferencesManager.getVin(),
                token = preferencesManager.getAccessToken()
            )
            if (success) {
                _carLocked.value = false
                _lastAction.value = "车辆已解锁"
            } else {
                _lastAction.value = "解锁失败"
            }
        } catch (e: Exception) {
            _lastAction.value = "解锁错误: ${e.message}"
        }
    }

    suspend fun lockCar() {
        try {
            val success = carControlRepository.lockCar(
                vin = preferencesManager.getVin(),
                token = preferencesManager.getAccessToken()
            )
            if (success) {
                _carLocked.value = true
                _lastAction.value = "车辆已上锁"
            } else {
                _lastAction.value = "上锁失败"
            }
        } catch (e: Exception) {
            _lastAction.value = "上锁错误: ${e.message}"
        }
    }

    private fun shouldAutoUnlock(rssi: Int?): Boolean {
        if (rssi == null) return false
        val threshold = preferencesManager.getUnlockThreshold()
        return rssi >= threshold && preferencesManager.isAutoUnlockEnabled()
    }

    private fun shouldAutoLock(): Boolean {
        return preferencesManager.isAutoLockEnabled() &&
               _connectedDevice.value == null
    }

    fun getSettings() = preferencesManager.getSettings()
}