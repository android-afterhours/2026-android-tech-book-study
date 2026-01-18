import kotlinx.coroutines.*
import kotlin.math.min

// =========================
// 가짜 서버: 몇 번 확인하면 완료되는지로 폴링 시뮬레이션
// =========================
class FakeServer(private val doneAfter: Int) {
    private var count = 0
    fun isDone(): Boolean {
        count++
        return count >= doneAfter
    }
}

// =========================
// 폴링 + (옵션) 지수 백오프
// - useBackoff=false : 주기 고정 폴링
// - useBackoff=true  : 폴링 + 지수 백오프
// =========================
suspend fun polling(useBackoff: Boolean) {
    val title = if (useBackoff) "폴링 + 지수 백오프" else "폴링(주기 고정)"
    println("\n=== $title 시작 ===")

    val server = FakeServer(doneAfter = 5)

    var waitMs = 500L           // 백오프 시작값
    val fixedMs = 1000L         // 고정 주기
    val maxWaitMs = 4000L       // 백오프 최대치

    while (true) {
        val nextWait = if (useBackoff) waitMs else fixedMs
        println("📡 상태 확인 (다음 대기: ${nextWait}ms)")

        if (server.isDone()) {
            println("✅ 완료!")
            break
        }

        // 여기서 "다음까지 기다림"이 폴링의 핵심
        delay(nextWait)

        // 지수 백오프는 "대기시간 조절 로직"일 뿐, 폴링(반복 확인) 자체와는 별개
        if (useBackoff) {
            waitMs = min(waitMs * 2, maxWaitMs) // 500 -> 1000 -> 2000 -> 4000 ...
        }
    }

    println("=== 종료 ===")
}

fun main() = runBlocking {
    // 폴링(주기 고정)
    polling(useBackoff = false)

    // 폴링 + 지수 백오프
    polling(useBackoff = true)
}
