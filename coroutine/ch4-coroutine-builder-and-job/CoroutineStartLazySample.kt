import kotlinx.coroutines.*

// 결제 SDK 콜백을 받아야 하는 상황에서 무한 로딩 발생하여 타임아웃 Job을 선언하는 경우에 대한 예제
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

// CoroutineStart.LAZY 사용 예시
class PaymentManagerLazy(private val scope: CoroutineScope) {
    private var isPaymentCompleted = false

    // ✅ 타임아웃 Job을 미리 정의만 해둠
    private val timeoutJob: Job = scope.launch(start = CoroutineStart.LAZY) {
        println("[Timer-LAZY] ⏱️ 타이머 시작 (5초)")
        delay(5_000)

        if (!isPaymentCompleted) {
            println("[Timer-LAZY] 🚨 타임아웃 발생")
            showErrorAndDismiss("결제 응답 시간이 초과되었습니다.")
        }
    }

    fun startProcess() {
        initPayment()
        FakePaymentSDK.startPayment()
    }

    fun onUserReturnedToApp() {
        println("[Manager-LAZY] 🔄 앱 복귀")
        if (!isPaymentCompleted) {
            timeoutJob.start() // ⚠️ 반드시 한 번만 호출되어야 함
        }
    }

    private fun initPayment() {
        FakePaymentSDK.setCallback(object : FakePaymentSDK.PaymentCallback {
            override fun onPaymentSuccess(id: String) {
                isPaymentCompleted = true
                timeoutJob.cancel()
                println("[Manager-LAZY] ✅ 결제 성공: $id")
            }

            override fun onPaymentFailed(code: Int) {
                isPaymentCompleted = true
                timeoutJob.cancel()
                println("[Manager-LAZY] ❌ 결제 실패: $code")
            }
        })
    }

    private fun showErrorAndDismiss(msg: String) {
        println("[UI-LAZY] $msg")
    }
}

// Job? 방식
class PaymentManagerJob(private val scope: CoroutineScope) {
    private var isPaymentCompleted = false
    private var timeoutJob: Job? = null

    fun startProcess() {
        isPaymentCompleted = false
        initPayment()
        FakePaymentSDK.startPayment()
    }

    fun onUserReturnedToApp() {
        println("[Manager-JOB] 🔄 앱 복귀")
        if (isPaymentCompleted) return

        // ✅ 이전 타이머 무효화 후 새 기준점에서 시작
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            println("[Timer-JOB] ⏱️ 타이머 시작 (5초)")
            delay(5_000)

            if (!isPaymentCompleted) {
                println("[Timer-JOB] 🚨 타임아웃 발생")
                showErrorAndDismiss("결제 응답 시간이 초과되었습니다.")
            }
        }
    }

    private fun initPayment() {
        FakePaymentSDK.setCallback(object : FakePaymentSDK.PaymentCallback {
            override fun onPaymentSuccess(id: String) {
                isPaymentCompleted = true
                timeoutJob?.cancel()
                println("[Manager-JOB] ✅ 결제 성공: $id")
            }

            override fun onPaymentFailed(code: Int) {
                isPaymentCompleted = true
                timeoutJob?.cancel()
                println("[Manager-JOB] ❌ 결제 실패: $code")
            }
        })
    }

    private fun showErrorAndDismiss(msg: String) {
        println("[UI-JOB] $msg")
    }
}

fun main() = runBlocking {
    // val manager = PaymentManagerLazy(this)
    val manager = PaymentManagerJob(this)

    manager.startProcess()
    delay(2_000)

    manager.onUserReturnedToApp()

    delay(7_000)
    println("--- 메인 종료 ---")
}
