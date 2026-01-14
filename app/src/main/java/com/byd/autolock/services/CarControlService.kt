package com.byd.autolock.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.byd.autolock.data.api.BydApiService
import com.byd.autolock.data.models.CarControlRequest
import com.byd.autolock.data.repositories.CarControlRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CarControlService : Service() {

    private val TAG = "CarControlService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var carControlRepository: CarControlRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "车辆控制服务创建")
        carControlRepository = CarControlRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "车辆控制服务启动")

        val action = intent?.action
        val vin = intent?.getStringExtra(EXTRA_VIN)
        val token = intent?.getStringExtra(EXTRA_TOKEN)

        if (action != null && vin != null && token != null) {
            when (action) {
                ACTION_UNLOCK -> performUnlock(vin, token)
                ACTION_LOCK -> performLock(vin, token)
            }
        }

        // 服务执行完任务后停止
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun performUnlock(vin: String, token: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "执行解锁操作: $vin")
                val success = carControlRepository.unlockCar(vin, token)
                Log.d(TAG, "解锁结果: $success")

                // 发送结果广播
                sendResultBroadcast(ACTION_UNLOCK, success)
            } catch (e: Exception) {
                Log.e(TAG, "解锁失败", e)
                sendResultBroadcast(ACTION_UNLOCK, false)
            }
        }
    }

    private fun performLock(vin: String, token: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "执行上锁操作: $vin")
                val success = carControlRepository.lockCar(vin, token)
                Log.d(TAG, "上锁结果: $success")

                // 发送结果广播
                sendResultBroadcast(ACTION_LOCK, success)
            } catch (e: Exception) {
                Log.e(TAG, "上锁失败", e)
                sendResultBroadcast(ACTION_LOCK, false)
            }
        }
    }

    private fun sendResultBroadcast(action: String, success: Boolean) {
        val intent = Intent(ACTION_RESULT).apply {
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_SUCCESS, success)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_UNLOCK = "com.byd.autolock.action.UNLOCK"
        const val ACTION_LOCK = "com.byd.autolock.action.LOCK"
        const val ACTION_RESULT = "com.byd.autolock.action.RESULT"

        const val EXTRA_VIN = "vin"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_ACTION = "action"
        const val EXTRA_SUCCESS = "success"

        fun createUnlockIntent(context: android.content.Context, vin: String, token: String): Intent {
            return Intent(context, CarControlService::class.java).apply {
                action = ACTION_UNLOCK
                putExtra(EXTRA_VIN, vin)
                putExtra(EXTRA_TOKEN, token)
            }
        }

        fun createLockIntent(context: android.content.Context, vin: String, token: String): Intent {
            return Intent(context, CarControlService::class.java).apply {
                action = ACTION_LOCK
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_VIN, vin)
            }
        }
    }
}