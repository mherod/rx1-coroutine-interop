
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MainKtTest {

    @Test
    fun rxSuspend2() {
        rxSuspend { "hello" }.test().assertValue("hello")
    }

    @Test
    fun rxSuspend3() {
        rxSuspend { 1 }.test().assertValue(1)
    }

    @Test
    fun rxSuspend4() {
        val atomic = AtomicInteger(0)
        repeat(100) {
            val expected = atomic.getAndIncrement()
            rxSuspend { expected }
                .test()
                .assertValue(expected)
        }
    }

    @Test
    fun rxSuspend5() {
        val atomic = AtomicInteger(0)
        val observable = rxSuspend { atomic.get() }
        repeat(1_000) {
            observable.test().assertValue(atomic.getAndIncrement())
        }
    }

    @Test
    fun rxSuspend6() {
        val atomic = AtomicInteger(0)
        val get = atomic.get()
        val observable = rxSuspend(recycleResult = true) { atomic.getAndIncrement() }
        repeat(1_000) {
            observable.test().assertValue(get)
        }
    }

    @Test
    fun rxSuspend7() {
        repeat(1_000) {
            val exception = IllegalArgumentException()
            rxSuspend { throw exception }
                .test()
                .assertError(exception::class.java)
        }
    }
}
