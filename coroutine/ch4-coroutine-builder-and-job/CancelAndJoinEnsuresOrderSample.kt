import kotlinx.coroutines.*

data class Result(val value: String)

fun main() = runBlocking {
    var currentResult: Result? = null

    val job = launch {
        try {
            delay(100) 
            // 취소 가능 지점(suspension point)
            // cancel()이 호출돼도, 여기서 깨어나기 전까지는 실행이 계속될 수 있음

            currentResult = Result("old")
            // ❗ 취소된 줄 알았던 이전 작업이
            // 이후 결과를 덮어쓸 수 있는 지점

            println("old written")
        } finally {
            println("job finished")
            // 취소되더라도 finally는 반드시 실행됨
        }
    }

    delay(10)
    // job이 delay 안에 들어간 상태를 만들기 위한 타이밍

    job.cancel()
    // ❌ 취소 "요청"만 보냄
    // 실제 종료 시점은 보장되지 않음

    launch {
        currentResult = Result("new")
        // 개발자 의도상 최종 결과

        println("new written")
    }

    delay(200)
    // 두 코루틴 실행이 충분히 끝날 때까지 대기

    println("final result = $currentResult")
    // ⚠️ 대부분은 new지만, 이론적으로는 old가 나올 수도 있음
}
