import kotlinx.coroutines.*

class VoiceVerificationController(
    private val scope: CoroutineScope
) {
    private val silenceJob = scope.launch(start = CoroutineStart.LAZY) {
        delay(5_000)
        println("[UI] 음성이 감지되지 않습니다.")
    }

    private val timeoutJob = scope.launch(start = CoroutineStart.LAZY) {
        delay(30_000)
        println("[Result] 인증 실패")
        stopVerification()
    }

    fun onRecorderReady() {
        // “녹음 시작”이라는 단일 기준점
        println("[Controller] 🎙️ 녹음 시작")
        silenceJob.start()
        timeoutJob.start()
    }

    fun onVoiceDetected() {
        println("[Controller] 🗣️ 음성 감지됨")
        silenceJob.cancel()
    }

    fun stopVerification() {
        println("[Controller] ⛔ 인증 종료")
        silenceJob.cancel()
        timeoutJob.cancel()
    }
}

fun main() = runBlocking {
    println("=== CASE 1: 정상 음성 입력 ===")

    val controller = VoiceVerificationController(this)

    controller.onRecorderReady()

    delay(2_000)      // 2초 후 사용자가 말함
    controller.onVoiceDetected()

    delay(10_000)     // 남은 시간 관찰
    println("=== END CASE 1 ===")
}
