package top.e404.mywol.platform

import top.e404.mywol.util.debug
import top.e404.mywol.util.logger

actual class WebsocketService {
    actual companion object {
        private val log = logger(WebsocketService::class)

        @Volatile
        actual var instance: WebsocketService? = null

        actual fun start(address: String, id: String, name: String, secret: String?): Boolean {
            log.debug { "启动服务: id: $id, name: $name, address: $address, secret: $secret" }
            if (instance != null) return false
            instance = WebsocketService()
            instance!!.start(address, id, name, secret)
            return true
        }

        actual fun stop() {
            instance?.stop()
            instance = null
        }
    }

    init {
        instance = this
    }

    @Volatile
    actual var handler: WebsocketHandler? = null
        private set

    fun start(address: String, id: String, name: String, secret: String?) {
        handler = WebsocketHandler(id, name, address, secret)
    }

    fun stop() {
        handler?.closeWebsocket()
        handler = null
        instance = null
    }
}