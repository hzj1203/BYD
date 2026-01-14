package com.byd.autolock.data.api

import com.byd.autolock.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface BydApiService {

    /**
     * 控制车辆（解锁/上锁）
     */
    @POST("v1/car/control")
    suspend fun controlCar(
        @Header("Authorization") token: String,
        @Body request: CarControlRequest
    ): Response<CarControlResponse>

    /**
     * 获取车辆状态
     */
    @GET("v1/car/{vin}/status")
    suspend fun getCarStatus(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<CarStatusResponse>

    /**
     * 获取车辆信息
     */
    @GET("v1/car/{vin}/info")
    suspend fun getCarInfo(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<CarInfoResponse>

    /**
     * 刷新访问令牌
     */
    @POST("v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest
    ): Response<TokenResponse>

    /**
     * 获取用户车辆列表
     */
    @GET("v1/user/cars")
    suspend fun getUserCars(
        @Header("Authorization") token: String
    ): Response<UserCarsResponse>

    /**
     * 发送远程命令
     */
    @POST("v1/car/{vin}/command")
    suspend fun sendCommand(
        @Header("Authorization") token: String,
        @Path("vin") vin: String,
        @Body request: RemoteCommandRequest
    ): Response<RemoteCommandResponse>
}