package eu.nk2.intercom.application

internal class AsyncTester(runnable: () -> Unit) {
    private val thread: Thread
    private var exception: Exception? = null

    fun start() {
        thread.start()
    }

    fun test() {
        thread.join()

        if (exception != null)
            throw exception as Exception
    }

    init {
        thread = Thread {
            try {
                runnable()
            } catch (e: Exception) {
                exception = e
            }
        }
    }
}