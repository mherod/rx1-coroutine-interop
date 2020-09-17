package rxutines

import org.junit.Test
import rx.Observable

class RxTest {
    @Test
    fun testRxBuilderHello() {
        fromCallable { "hello" }
            .build()
            .toObservable()
            .test()
            .assertValue("hello")
    }

    @Test
    fun numbers() {
        Observable.from(1..50_000)
//            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
            .flatMapSuspend { it * 2 }
            .test()
            .assertNoErrors()
            .assertValueCount(50_000)
    }
}
