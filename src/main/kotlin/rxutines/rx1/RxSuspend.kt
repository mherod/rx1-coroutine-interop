package rxutines.rx1

import kotlinx.coroutines.*
import rx.Observable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
inline fun <T> rxSuspend(
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    recycleResult: Boolean = false,
    crossinline function: suspend CoroutineScope.() -> T,
): Observable<T> {

    val supervisorJob = SupervisorJob()
    val makeDeferred: () -> Deferred<T> = {
        (coroutineScope + supervisorJob + Job()).async { function() }
    }
    var deferred: Deferred<T> = makeDeferred()

    return Observable.fromCallable {
        val (value: T, d: Duration) = runBlocking {
            measureTimedValue {
                deferred.await()
            }
        }
        value
    }.doOnRequest {
        deferred.start()
    }.doOnSubscribe {
        if (deferred.isCompleted && !recycleResult) {
            deferred.cancel()
            deferred = makeDeferred()
        }
        deferred.start()
    }.doOnUnsubscribe {
        deferred.cancel()
    }.doOnError { throwable: Throwable ->
        supervisorJob.completeExceptionally(throwable)
    }.doOnTerminate {
        supervisorJob.complete()
    }
}
