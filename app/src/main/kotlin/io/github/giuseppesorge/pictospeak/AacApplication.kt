package io.github.giuseppesorge.pictospeak

import android.app.Application

class AacApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // M2+: drop image caches on TRIM_MEMORY_UI_HIDDEN; M6: release the LLM engine.
        // (docs/perf-budgets.md — memory tactics.)
    }
}
