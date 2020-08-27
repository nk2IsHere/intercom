package eu.nk2.intercom.utils

import java.lang.Exception
import kotlin.reflect.KClass

//
// Shoutouts: https://github.com/MaaxGr/KotlinUtils/blob/master/src/main/kotlin/de/maaxgr/kotlinutils/Unsafe.kt
//
class Unsafe {

    private var dangerCallback: () -> Unit = {  }
    private val exceptionHandlers = mutableMapOf<KClass<out Any>, () -> Unit>()

    companion object {
        fun Any.unsafe(callback: Unsafe.() -> Unit) {
            Unsafe().apply {
                callback()
                execute()
            }
        }
    }

    private fun execute() {
        try {
            dangerCallback()
        } catch (ex: Exception) {
            exceptionHandlers
                .filter { (type, _) -> ex::class.simpleName == type.simpleName }
                .forEach { it.value() }
        }
    }

    fun danger(callback: () -> Unit) {
        this.dangerCallback = callback
    }

    fun except(vararg exceptionTypes: KClass<out Any>, callback: () -> Unit) {
        exceptionTypes.forEach {
            exceptionHandlers[it] = callback
        }
    }
}