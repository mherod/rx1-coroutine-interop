import org.junit.Ignore
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

    @Ignore("this test checks that work for the same observable is re-invoked, still thinking about this")
    @Test
    fun rxSuspend5() {
        val atomic = AtomicInteger(0)
        val observable = rxSuspend { atomic.getAndIncrement() }
        repeat(1_000) {
            observable.test().assertValue(atomic.get())
        }
    }

    @Test
    fun rxSuspend6() {
        val atomic = AtomicInteger(0)
        val get = atomic.get()
        val observable = rxSuspend { atomic.getAndIncrement() }
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
