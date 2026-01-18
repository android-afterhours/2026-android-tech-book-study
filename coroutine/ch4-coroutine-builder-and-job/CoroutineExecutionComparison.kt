import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/**
 * 코루틴의 병렬 실행(joinAll)과 순차 실행(individual join)의 차이를 비교하는 예제
 */
fun main() = runBlocking {
    println("--- 테스트 시작 ---")

    // 방식 1 실행: 병렬 (Parallel)
    val time1 = measureTimeMillis {
        testParallelJoinAll()
    }
    println("방식 1 (joinAll) 총 소요 시간: ${time1}ms\n")

    println("---------------------------------------")

    // 방식 2 실행: 순차 (Sequential)
    val time2 = measureTimeMillis {
        testSequentialJoin()
    }
    println("방식 2 (순차 join) 총 소요 시간: ${time2}ms")

    println("--- 테스트 종료 ---")
}

/**
 * 방식 1: joinAll 사용 (병렬 실행 후 대기)
 * 코루틴들이 동시에 시작되며, 가장 오래 걸리는 작업 시간만큼만 대기합니다.
 */
suspend fun testParallelJoinAll() = coroutineScope {
    println("[방식 1] 모든 코루틴을 동시에 실행하고 joinAll로 기다립니다.")

    val job1 = launch {
        println("  -> Job 1: 시작 (3초)")
        delay(3000)
        println("  -> Job 1: 완료")
    }

    val job2 = launch {
        println("  -> Job 2: 시작 (1초)")
        delay(1000)
        println("  -> Job 2: 완료")
    }

    // job1과 job2는 이미 동시에 돌아가고 있습니다.
    // joinAll은 내부적으로 job1.join() -> job2.join() 순서로 호출하지만
    // 이미 실행 중인 job2가 먼저 끝나 있어도 상관없이 모든 종료를 보장합니다.
    joinAll(job1, job2)
}

/**
 * 방식 2: 개별 join 사용 (완전한 순차 실행)
 * 하나가 완전히 끝난 뒤에 다음 코루틴을 시작(launch)합니다.
 */
suspend fun testSequentialJoin() = coroutineScope {
    println("[방식 2] 코루틴을 하나씩 순차적으로 실행하고 기다립니다.")

    // 1번 실행 및 종료 대기
    val job1 = launch {
        println("  -> Job 1: 시작 (3초)")
        delay(3000)
        println("  -> Job 1: 완료")
    }
    job1.join() 

    // 1번이 끝난 뒤에야 2번을 launch함
    val job2 = launch {
        println("  -> Job 2: 시작 (1초)")
        delay(1000)
        println("  -> Job 2: 완료")
    }
    job2.join()
}

/*
실행 결과:
--- 테스트 시작 ---
[방식 1] 모든 코루틴을 동시에 실행하고 joinAll로 기다립니다.
  -> Job 1: 시작 (3초)
  -> Job 2: 시작 (1초)
  -> Job 2: 완료
  -> Job 1: 완료
방식 1 (joinAll) 총 소요 시간: 3019ms

---------------------------------------
[방식 2] 코루틴을 하나씩 순차적으로 실행하고 기다립니다.
  -> Job 1: 시작 (3초)
  -> Job 1: 완료
  -> Job 2: 시작 (1초)
  -> Job 2: 완료
방식 2 (순차 join) 총 소요 시간: 4020ms
--- 테스트 종료 ---
*/
