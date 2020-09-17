
import kotlinx.coroutines.*
import rx.Observable

inline fun <T : Any> rxSuspend(
    job: CompletableJob = Job(),
    supervisorJob: CompletableJob = SupervisorJob(job),
    scope: CoroutineScope = CoroutineScope(job),
    crossinline function: suspend CoroutineScope.() -> T
): Observable<T> {

    val deferred: Deferred<T> = scope.async { function() }
    return Observable.defer {
        Observable.just(runBlocking { deferred.await() })
    }.doOnRequest {
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
