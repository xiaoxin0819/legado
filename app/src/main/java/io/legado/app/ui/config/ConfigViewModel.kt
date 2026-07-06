package io.legado.app.ui.config

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.help.AppWebDav

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

}
