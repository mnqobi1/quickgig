package com.quickgig.app.data.remote

import com.quickgig.app.data.model.AuthResponse
import com.quickgig.app.data.model.ChatMessage
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.model.GigListResponse
import com.quickgig.app.data.model.PublicProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuickGigApi {
    @POST("api/devices")
suspend fun registerDevice(@Body body: Map<String, String>): Response<Unit>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleSsoRequest): Response<AuthResponse>

    @GET("api/gigs")
    suspend fun listGigs(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null,
        @Query("category") category: String? = null,
    ): Response<GigListResponse>

    @POST("api/gigs")
    suspend fun createGig(@Body body: CreateGigRequest): Response<Gig>

    @GET("api/gigs/{id}")
    suspend fun getGig(@Path("id") id: Long): Response<Gig>

    @POST("api/gigs/{id}/apply")
    suspend fun applyToGig(@Path("id") id: Long): Response<Unit>

    @POST("api/gigs/{id}/withdraw")
    suspend fun withdrawFromGig(@Path("id") id: Long): Response<Unit>

    @PATCH("api/gigs/{id}/status")
    suspend fun updateGigStatus(@Path("id") id: Long, @Body body: UpdateStatusRequest): Response<Unit>

    @GET("api/users/{id}")
    suspend fun getProfile(@Path("id") id: Long): Response<PublicProfile>

    @PATCH("api/users/{id}/settings")
    suspend fun updateSettings(@Path("id") id: Long, @Body body: UpdateSettingsRequest): Response<Unit>

    @GET("api/gigs/{gigId}/messages")
    suspend fun getMessages(@Path("gigId") gigId: Long): Response<Map<String, List<ChatMessage>>>

    @POST("api/gigs/{gigId}/messages")
    suspend fun sendMessage(@Path("gigId") gigId: Long, @Body body: SendMessageRequest): Response<ChatMessage>

    @GET("api/notifications")
    suspend fun listNotifications(@Query("after") after: Long? = null, @Query("limit") limit: Int? = null): Response<Map<String, Any>>

    @POST("api/notifications/read")
    suspend fun markNotificationsRead(@Body body: Map<String, Any>): Response<Unit>

}
