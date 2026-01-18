data class Result(val value: String)

fun main() = runBlocking {
    println("--- [시나리오 1: 상태 덮어쓰기 위험] ---")
    var currentResult: Result? = null

    val job1 = launch(Dispatchers.Default) {
        try {
            delay(100) // 작업 중 취소 발생 지점
            // 만약 cancel() 후 join을 안 하면, 아래 로직이 새 작업 이후에 실행될 위험이 있음
            currentResult = Result("Old Data (취소된 작업)")
            println("[Job 1] 데이터 쓰기 완료")
        } finally {
            delay(50) // 정리 작업에 시간이 걸린다고 가정
            println("[Job 1] 자원 정리 완료 (finally)")
        }
    }

    delay(20)
    println("[Main] Job 1 취소 요청")
    job1.cancel() // cancelAndJoin() 대신 cancel()만 사용

    // Job 1이 아직 완전히 죽지 않았는데 바로 다음 작업 시작
    launch {
        delay(100) // 새 작업이 시작 전 아주 잠깐 멈춘 사이...
        currentResult = Result("New Data (새 작업)")
        println("[Job 2] 새 데이터 쓰기 완료")
    }

    delay(300)
    println("[결과] 최종 데이터 상태: ${currentResult?.value}")

    println("\n--- [시나리오 2: 리소스 충돌 위험] ---")
    testResourceConflict()
}

suspend fun testResourceConflict() = coroutineScope {
    var isResourceOpen = false

    val fileJob = launch {
        try {
            isResourceOpen = true
            println("[파일Job] 파일 열고 작업 중...")
            delay(500)
        } finally {
            delay(100) // 파일 닫는 데 걸리는 시간
            isResourceOpen = false
            println("[파일Job] 파일 안전하게 닫음")
        }
    }

    delay(100)
    fileJob.cancel() // 취소만 하고 즉시 다음 진행

    // 순차성이 보장되지 않으면 발생하는 문제
    if (isResourceOpen) {
        println("[에러] 충돌 발생! 이전 파일이 아직 닫히지 않았는데 접근 시도함")
    } else {
        println("[성공] 파일이 닫힌 것을 확인하고 안전하게 접근")
    }
}
