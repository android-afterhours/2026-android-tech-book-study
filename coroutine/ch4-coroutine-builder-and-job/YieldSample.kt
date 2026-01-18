import kotlinx.coroutines.*

suspend fun periodicUpdates() {
    for (i in 1..25) {
        println("Doing work $i")
        if (i % 5 == 0) {
            println("Performing periodic update")
            yield() // Allow other coroutines to run during updates
        }
    }
}

fun main() {
    sample1()
    sample2()
}

fun sample1() = runBlocking {
    val job1 = launch {
        repeat(5) {
            println("Coroutine 1 working")
            yield() // Simulate thread yield
        }
    }

    val job2 = launch {
        repeat(5) {
            println("Coroutine 2 working")
            yield() // Simulate thread yield
        }
    }

    job1.join()
    job2.join()
}

fun sample2() = runBlocking {
    val job1 = launch { periodicUpdates() }
    val job2 = launch {
        repeat(5) {
            println("Another coroutine working")
            delay(10) // Simulate work
        }
    }

    job1.join()
    job2.join()
}

