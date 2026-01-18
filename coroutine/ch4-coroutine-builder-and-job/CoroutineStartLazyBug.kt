import kotlinx.coroutines.*

fun main() = runBlocking {
    println("before broken()")

    broken()

    println("after broken()") // ❌ 이 줄은 절대 안 찍힘
}

suspend fun broken() = coroutineScope {
    val result = async(start = CoroutineStart.LAZY) {
        println("async started")
        delay(1000)
        42
    }

    println("끝!") // ❌ 이것도 안 찍힘
}
