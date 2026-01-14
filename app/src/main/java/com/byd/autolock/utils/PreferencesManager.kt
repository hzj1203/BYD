package com.byd.autolock.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 车辆信息
    fun saveVin(vin: String) {
        sharedPreferences.edit().putString(KEY_VIN, vin).apply()
    }

    fun getVin(): String {
        return sharedPreferences.getString(KEY_VIN, "") ?: ""
    }

    // 访问令牌
    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "") ?: ""
    }

    // 刷新令牌
    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, "") ?: ""
    }

    // 自动解锁设置
    fun setAutoUnlockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_UNLOCK, enabled).apply()
    }

    fun isAutoUnlockEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_UNLOCK, true)
    }

    // 自动上锁设置
    fun setAutoLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_LOCK, true).apply()
    }

    fun isAutoLockEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_LOCK, true)
    }

    // 解锁信号强度阈值（dBm）
    fun setUnlockThreshold(threshold: Int) {
        sharedPreferences.edit().putInt(KEY_UNLOCK_THRESHOLD, threshold).apply()
    }

    fun getUnlockThreshold(): Int {
        return sharedPreferences.getInt(KEY_UNLOCK_THRESHOLD, -60) // 默认-60dBm
    }

    // 上锁信号强度阈值（dBm）
    fun setLockThreshold(threshold: Int) {
        sharedPreferences.edit().putInt(KEY_LOCK_THRESHOLD, threshold).apply()
    }

    fun getLockThreshold(): Int {
        return sharedPreferences.getInt(KEY_LOCK_THRESHOLD, -80) // 默认-80dBm
    }

    // 目标蓝牙设备列表
    fun saveTargetDevices(devices: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_TARGET_DEVICES, devices).apply()
    }

    fun getTargetDevices(): Set<String> {
        return sharedPreferences.getStringSet(KEY_TARGET_DEVICES, emptySet()) ?: emptySet()
    }

    fun addTargetDevice(deviceAddress: String) {
        val currentDevices = getTargetDevices().toMutableSet()
        currentDevices.add(deviceAddress)
        saveTargetDevices(currentDevices)
    }

    fun removeTargetDevice(deviceAddress: String) {
        val currentDevices = getTargetDevices().toMutableSet()
        currentDevices.remove(deviceAddress)
        saveTargetDevices(currentDevices)
    }

    // 延迟设置（毫秒）
    fun setUnlockDelay(delayMs: Long) {
        sharedPreferences.edit().putLong(KEY_UNLOCK_DELAY, delayMs).apply()
    }

    fun getUnlockDelay(): Long {
        return sharedPreferences.getLong(KEY_UNLOCK_DELAY, 2000) // 默认2秒
    }

    fun setLockDelay(delayMs: Long) {
        sharedPreferences.edit().putLong(KEY_LOCK_DELAY, delayMs).apply()
    }

    fun getLockDelay(): Long {
        return sharedPreferences.getLong(KEY_LOCK_DELAY, 5000) // 默认5秒
    }

    // 通知设置
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
    }

    // 获取所有设置
    fun getSettings(): Map<String, Any> {
        return mapOf(
            "vin" to getVin(),
            "autoUnlock" to isAutoUnlockEnabled(),
            "autoLock" to isAutoLockEnabled(),
            "unlockThreshold" to getUnlockThreshold(),
            "lockThreshold" to getLockThreshold(),
            "targetDevices" to getTargetDevices(),
            "unlockDelay" to getUnlockDelay(),
            "lockDelay" to getLockDelay(),
            "notifications" to isNotificationsEnabled()
        )
    }

    // 清除所有数据
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "byd_autolock_prefs"

        private const val KEY_VIN = "vin"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AUTO_UNLOCK = "auto_unlock"
        private const val KEY_AUTO_LOCK = "auto_lock"
        private const val KEY_UNLOCK_THRESHOLD = "unlock_threshold"
        private const val KEY_LOCK_THRESHOLD = "lock_threshold"
        private const val KEY_TARGET_DEVICES = "target_devices"
        private const val KEY_UNLOCK_DELAY = "unlock_delay"
        private const val KEY_LOCK_DELAY = "lock_delay"
        private const val KEY_NOTIFICATIONS = "notifications"
    }
}