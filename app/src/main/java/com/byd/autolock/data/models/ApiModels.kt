package com.byd.autolock.data.models

// 车辆控制请求
data class CarControlRequest(
    val vin: String,
    val command: String, // "lock" 或 "unlock"
    val timestamp: Long,
    val source: String = "android_app"
)

// 车辆控制响应
data class CarControlResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long,
    val commandId: String? = null
)

// 车辆状态响应
data class CarStatusResponse(
    val vin: String,
    val locked: Boolean,
    val engineOn: Boolean,
    val lastUpdate: Long,
    val location: LocationData? = null
)

// 车辆信息响应
data class CarInfoResponse(
    val vin: String,
    val model: String,
    val year: Int,
    val color: String,
    val features: List<String>
)

// 位置数据
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

// 令牌刷新请求
data class TokenRefreshRequest(
    val refreshToken: String,
    val grantType: String = "refresh_token"
)

// 令牌响应
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)

// 用户车辆列表响应
data class UserCarsResponse(
    val cars: List<CarSummary>
)

// 车辆摘要
data class CarSummary(
    val vin: String,
    val model: String,
    val nickname: String?,
    val primary: Boolean
)

// 远程命令请求
data class RemoteCommandRequest(
    val command: String,
    val parameters: Map<String, Any>? = null,
    val timeout: Int = 30 // 秒
)

// 远程命令响应
data class RemoteCommandResponse(
    val commandId: String,
    val status: String, // "pending", "executing", "completed", "failed"
    val result: String?,
    val timestamp: Long
)