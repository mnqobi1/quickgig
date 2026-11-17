package com.quickgig.app

import com.quickgig.app.data.model.AuthResponse
import com.quickgig.app.data.model.ChatMessage
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.model.GigListResponse
import com.quickgig.app.data.model.PublicProfile
import com.quickgig.app.data.remote.CreateGigRequest
import com.quickgig.app.data.remote.GoogleSsoRequest
import com.quickgig.app.data.remote.LoginRequest
import com.quickgig.app.data.remote.QuickGigApi
import com.quickgig.app.data.remote.RegisterRequest
import com.quickgig.app.data.remote.SendMessageRequest
import com.quickgig.app.data.remote.UpdateSettingsRequest
import com.quickgig.app.data.remote.UpdateStatusRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * A QuickGigApi that never actually calls the network — every method
 * returns an HTTP 501 error response. Used by UI tests (LoginScreenTest)
 * that only need to exercise client-side validation, so they run
 * deterministically without the API server or a network connection.
 */
class FakeQuickGigApi : QuickGigApi {

    private fun <T> notImplemented(): Response<T> =
        Response.error(501, "not implemented in FakeQuickGigApi".toResponseBody("text/plain".toMediaType()))

    override suspend fun register(body: RegisterRequest): Response<AuthResponse> = notImplemented()
    override suspend fun login(body: LoginRequest): Response<AuthResponse> = notImplemented()
    override suspend fun loginWithGoogle(body: GoogleSsoRequest): Response<AuthResponse> = notImplemented()
    override suspend fun listGigs(lat: Double?, lng: Double?, radiusKm: Double?, category: String?): Response<GigListResponse> = notImplemented()
    override suspend fun createGig(body: CreateGigRequest): Response<Gig> = notImplemented()
    override suspend fun getGig(id: Long): Response<Gig> = notImplemented()
    override suspend fun applyToGig(id: Long): Response<Unit> = notImplemented()
    override suspend fun updateGigStatus(id: Long, body: UpdateStatusRequest): Response<Unit> = notImplemented()
    override suspend fun getProfile(id: Long): Response<PublicProfile> = notImplemented()
    override suspend fun updateSettings(id: Long, body: UpdateSettingsRequest): Response<Unit> = notImplemented()
    override suspend fun getMessages(gigId: Long): Response<Map<String, List<ChatMessage>>> = notImplemented()
    override suspend fun sendMessage(gigId: Long, body: SendMessageRequest): Response<ChatMessage> = notImplemented()
}
