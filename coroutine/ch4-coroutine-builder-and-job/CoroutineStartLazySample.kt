import kotlinx.coroutines.*

// 1. 외부 가상 결제 SDK (콜백이 안 오는 버그 시뮬레이션용)
object FakePaymentSDK {
    interface PaymentCallback {
        fun onPaymentSuccess(id: String)
        fun onPaymentFailed(code: Int)
    }

    private var callback: PaymentCallback? = null

    fun setCallback(callback: PaymentCallback) {
        this.callback = callback
    }

    fun startPayment() {
        println("[SDK] 외부 결제 화면으로 이동합니다... (사용자가 결제 중)")
        // 여기서 버그 발생: 결제가 끝났는데도 아무 콜백을 호출하지 않음!
    }
}

// 2. 결제 다이얼로그 관리 클래스
class PaymentManager(private val scope: CoroutineScope) {
    private var isPaymentCompleted = false

    // ✅ 타임아웃 Job을 LAZY로 미리 준비
    private val timeoutJob: Job = scope.launch(start = CoroutineStart.LAZY) {
        println("[Timer] ⏱️ 결제 콜백 누락 감지 타이머 시작 (5초 대기...)")
        delay(5000L)

        if (!isPaymentCompleted) {
            println("[Timer] 🚨 5초간 콜백이 오지 않았습니다. 무한 로딩 방지 처리를 합니다.")
            showErrorAndDismiss("결제 응답 시간이 초과되었습니다.")
        }
    }

    fun initPayment() {
        FakePaymentSDK.setCallback(object : FakePaymentSDK.PaymentCallback {
            override fun onPaymentSuccess(id: String) {
                // 정상 콜백 시 타이머 취소
                timeoutJob.cancel()
                isPaymentCompleted = true
                println("[Manager] ✅ 결제 성공 완료: $id")
            }

            override fun onPaymentFailed(code: Int) {
                timeoutJob.cancel()
                println("[Manager] ❌ 결제 실패: $code")
            }
        })
    }

    fun startProcess() {
        initPayment()
        FakePaymentSDK.startPayment()
    }

    /**
     * 사용자가 외부 앱(결제)을 마치고 우리 앱으로 돌아왔을 때 호출되는 시뮬레이션
     */
    fun onUserReturnedToApp() {
        println("[Manager] 🔄 사용자가 앱으로 돌아옴 (Focus 받음)")
        if (!isPaymentCompleted) {
            // ✅ 콜백이 아직 안 왔다면, 미리 준비한 LAZY 타이머를 여기서 발사!
            timeoutJob.start()
        }
    }

    private fun showErrorAndDismiss(msg: String) {
        println("[UI] 토스트 알림: $msg")
        println("[UI] 로딩 다이얼로그 닫기")
    }
}

// 3. 메인 실행 루프
fun main() = runBlocking {
    val manager = PaymentManager(this)

    // 1단계: 결제 프로세스 시작 (화면 이동)
    manager.startProcess()

    delay(2000L) // 사용자가 결제하는 시간 2초 가정

    // 2단계: 사용자가 우리 앱으로 돌아왔는데 콜백이 안 온 상황 발생!
    manager.onUserReturnedToApp()

    // 3단계: 결과 확인을 위해 대기
    delay(7000L)
    println("--- 메인 프로세스 종료 ---")
}

/**

[SDK] 외부 결제 화면으로 이동합니다... (사용자가 결제 중)
[Manager] 🔄 사용자가 앱으로 돌아옴 (Focus 받음)
[Timer] ⏱️ 결제 콜백 누락 감지 타이머 시작 (5초 대기...)
[Timer] 🚨 5초간 콜백이 오지 않았습니다. 무한 로딩 방지 처리를 합니다.
[UI] 토스트 알림: 결제 응답 시간이 초과되었습니다.
[UI] 로딩 다이얼로그 닫기
--- 메인 프로세스 종료 ---

**/
