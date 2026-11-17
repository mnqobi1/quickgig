package com.quickgig.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.quickgig.app.data.local.AppDatabase
import com.quickgig.app.data.remote.NetworkMonitor
import com.quickgig.app.data.remote.RetrofitClient
import com.quickgig.app.data.remote.SessionManager
import com.quickgig.app.data.repository.AuthRepository
import com.quickgig.app.data.repository.GigRepository
import com.quickgig.app.data.repository.MessageRepository
import com.quickgig.app.data.repository.UserRepository
import com.quickgig.app.sync.SyncWorker

class QuickGigApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var gigRepository: GigRepository
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var messageRepository: MessageRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        sessionManager = SessionManager(this)
        val networkMonitor = NetworkMonitor(this)
        val api = RetrofitClient.create(sessionManager)

        authRepository = AuthRepository(api, sessionManager)
        gigRepository = GigRepository(api, database.gigDao(), database.draftGigDao(), networkMonitor)
        userRepository = UserRepository(api, sessionManager)
        messageRepository = MessageRepository(api, database.messageDao(), networkMonitor)

        watchForReconnectionAndSync()
    }

    private fun watchForReconnectionAndSync() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    SyncWorker.enqueue(this@QuickGigApplication)
                }
            },
        )
    }
}
