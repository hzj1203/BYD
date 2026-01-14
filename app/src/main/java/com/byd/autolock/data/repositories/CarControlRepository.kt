package com.byd.autolock.data.repositories

import android.util.Log
import com.byd.autolock.data.api.BydApiService
import com.byd.autolock.data.api.RetrofitClient
import com.byd.autolock.data.models.CarControlRequest
import com.byd.autolock.data.models.CarControlResponse
import retrofit2.Response

class CarControlRepository {

    private val TAG = "CarControlRepository"
    private val apiService: BydApiService = RetrofitClient.create()

    suspend fun unlockCar(vin: String, accessToken: String): Boolean {
        return try {
            Log.d(TAG, "尝试解锁车辆: $vin")

            val request = CarControlRequest(
                vin = vin,
                command = "unlock",
                timestamp = System.currentTimeMillis()
            )

            val response = apiService.controlCar("Bearer $accessToken", request)

            handleApiResponse(response, "解锁")

        } catch (e: Exception) {
            Log.e(TAG, "解锁车辆失败", e)
            false
        }
    }

    suspend fun lockCar(vin: String, accessToken: String): Boolean {
        return try {
            Log.d(TAG, "尝试上锁车辆: $vin")

            val request = CarControlRequest(
                vin = vin,
                command = "lock",
                timestamp = System.currentTimeMillis()
            )

            val response = apiService.controlCar("Bearer $accessToken", request)

            handleApiResponse(response, "上锁")

        } catch (e: Exception) {
            Log.e(TAG, "上锁车辆失败", e)
            false
        }
    }

    suspend fun getCarStatus(vin: String, accessToken: String): Boolean? {
        return try {
            Log.d(TAG, "获取车辆状态: $vin")

            val response = apiService.getCarStatus("Bearer $accessToken", vin)

            if (response.isSuccessful) {
                response.body()?.let { statusResponse ->
                    Log.d(TAG, "车辆状态获取成功: 锁定状态=${statusResponse.locked}")
                    return statusResponse.locked
                }
            } else {
                Log.e(TAG, "获取车辆状态失败: ${response.code()} ${response.message()}")
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "获取车辆状态异常", e)
            null
        }
    }

    private fun handleApiResponse(response: Response<CarControlResponse>, operation: String): Boolean {
        return if (response.isSuccessful) {
            response.body()?.let { controlResponse ->
                val success = controlResponse.success
                Log.d(TAG, "$operation响应: 成功=$success, 消息=${controlResponse.message}")

                if (success) {
                    Log.i(TAG, "车辆$operation成功")
                } else {
                    Log.w(TAG, "车辆$operation失败: ${controlResponse.message}")
                }

                success
            } ?: run {
                Log.e(TAG, "$operation响应为空")
                false
            }
        } else {
            Log.e(TAG, "$operation请求失败: ${response.code()} ${response.message()}")
            false
        }
    }
}