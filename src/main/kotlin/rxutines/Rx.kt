package rxutines

import kotlinx.coroutines.*
import rx.Observable
import rx.Single
import rx.Subscription
import rxutines.rx1.rxSuspend
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Rx<T> internal constructor(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val recycleResult: Boolean = false,
    private val function: suspend CoroutineScope.() -> T,
) {

    class Builder<T> {

        lateinit var callable: suspend CoroutineScope.() -> T

        fun callable(function: suspend CoroutineScope.() -> T): Builder<T> = apply { callable = function }

        fun build(): Rx<T> = Rx(function = callable)
    }

    private val observable: Observable<T> by lazy {
        rxSuspend(
            coroutineScope = coroutineScope,
            recycleResult = recycleResult,
            function = function
        )
    }

    fun toObservable(): Observable<T> = observable

    fun toSingle(): Single<T> {
        // TODO more efficient Single strategy
        return toObservable().toSingle()
    }
}

suspend inline fun <reified T> Rx<T>.await(): T = toObservable().await()

suspend inline fun <reified T> Observable<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        var subscription: Subscription? = null
        val observable: Observable<T> = doOnSubscribe {
            cont.invokeOnCancellation {
                subscription?.unsubscribe()
            }
        }
        subscription = observable.subscribe({ t: T ->
            cont.resume(t)
        }, { error: Throwable ->
            cont.resumeWithException(error)
        }, {
            // on complete
        })
    }
}

inline fun <reified T> rx(crossinline func: Rx.Builder<T>.() -> Unit): Rx.Builder<T> = Rx.Builder<T>().apply(func)

inline fun <reified T, reified R> Observable<T>.flatMapSuspend(noinline function: suspend CoroutineScope.(T) -> R): Observable<R> {
    return map { t: T ->
        rx<R> {
            callable {
//                withContext(dispatcher()) {
                function(t)
//                }
            }
        }.build()
    }.flatMap(Rx<R>::toObservable)
}

inline fun <reified T> fromCallable(noinline function: suspend CoroutineScope.() -> T): Rx.Builder<T> {
    return rx { callable(function) }
}

fun dispatcher(): CoroutineDispatcher {
    val currentThread = Thread.currentThread()
    check(Thread.getAllStackTraces().keys.size < 30)
    return when {
        "RxIoScheduler" in currentThread.name -> Dispatchers.IO
        else -> Dispatchers.Default
    }.apply {
        println(currentThread.name)
    }
}
