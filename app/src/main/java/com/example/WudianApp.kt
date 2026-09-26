package com.example

import android.app.Application
import com.example.data.api.SessionStore

class WudianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionStore.init(this)
    }
}
