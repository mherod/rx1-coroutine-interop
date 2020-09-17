import kotlinx.coroutines.*
import rx.Observable

inline fun <T : Any> rxSuspend(
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    crossinline function: suspend CoroutineScope.() -> T,
): Observable<T> {

    val supervisorJob = SupervisorJob()
    val makeDeferred: () -> Deferred<T> = {
        (coroutineScope + supervisorJob + Job()).async { function() }
    }
    var deferred: Deferred<T> = makeDeferred()

    return Observable.fromCallable {
        runBlocking { deferred.await() }
    }.doOnRequest {
        if (deferred.isCompleted) {
            deferred = makeDeferred()
        }
        deferred.start()
    }.doOnSubscribe {
        deferred.start()
    }.doOnUnsubscribe {
        deferred.cancel()
    }.doOnError { throwable: Throwable ->
        supervisorJob.completeExceptionally(throwable)
    }.doOnTerminate {
        supervisorJob.complete()
    }
}
