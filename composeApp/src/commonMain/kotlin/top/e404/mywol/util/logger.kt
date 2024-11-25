@file:Suppress("UNUSED")

package top.e404.mywol.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

fun logger(name: String): Logger {
    return LoggerFactory.getILoggerFactory().getLogger(name)
}

inline fun <reified T : Any> T.logger() = logger(T::class)

fun logger(clazz: KClass<out Any>): Logger {
    return LoggerFactory.getILoggerFactory().getLogger(clazz.qualifiedName!!)
}

inline fun Logger.trace(message: () -> String) {
    if (isTraceEnabled) {
        trace(message())
    }
}

inline fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) {
        debug(message())
    }
}

inline fun Logger.info(message: () -> String) {
    if (isInfoEnabled) {
        info(message())
    }
}

@OverloadResolutionByLambdaReturnType
inline fun Logger.warn(message: () -> String) {
    if (isWarnEnabled) {
        warn(message())
    }
}

@JvmName("warnThrowable")
@OverloadResolutionByLambdaReturnType
inline fun Logger.warn(e: () -> Throwable) {
    contract { callsInPlace(e, InvocationKind.AT_MOST_ONCE) }
    if (isErrorEnabled) {
        val exception = e()
        warn(exception.message, exception)
    }
}

@OverloadResolutionByLambdaReturnType
inline fun Logger.error(message: () -> String) {
    error(message())
}

@JvmName("errorThrowable")
@OverloadResolutionByLambdaReturnType
inline fun Logger.error(e: () -> Throwable) {
    contract { callsInPlace(e, InvocationKind.AT_MOST_ONCE) }
    if (isErrorEnabled) {
        val exception = e()
        error(exception.message, exception)
    }
}

inline fun Logger.trace(exception: Throwable? = null, message: () -> String) {
    if (isTraceEnabled) {
        trace(message(), exception)
    }
}

inline fun Logger.debug(exception: Throwable? = null, message: () -> String) {
    if (isDebugEnabled) {
        debug(message(), exception)
    }
}

inline fun Logger.info(exception: Throwable? = null, message: () -> String) {
    if (isInfoEnabled) {
        info(message(), exception)
    }
}

inline fun Logger.warn(exception: Throwable? = null, message: () -> String) {
    if (isWarnEnabled) {
        warn(message(), exception)
    }
}

inline fun Logger.error(exception: Throwable? = null, message: () -> String) {
    if (isErrorEnabled) {
        error(message(), exception)
    }
}