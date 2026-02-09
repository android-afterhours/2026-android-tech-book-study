취소 중에도 반드시 실행되어야 하는 정리 작업(DB 닫기 등)이 있다면 `withContext(NonCancellable) { ... }` 블록을 활용할 수 있습니다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch {
        try {
            println("작업 시작: 5초 동안 진행됩니다...")
            repeat(5) { i ->
                println("작업 중... ($i)")
                delay(1000) // 1초마다 중단
            }
        } catch (e: CancellationException) {
            println("취소 신호 감지!")
        } finally {
            // [주의] 일반적인 finally 안에서 delay 같은 중단 함수를 호출하면
            // 이미 취소된 상태라 바로 exception이 발생해 실행되지 않습니다.
            
            withContext(NonCancellable) {
                println("정리 시작: 취소되었지만 이 작업은 끝까지 실행됩니다.")
                delay(1000) // NonCancellable 덕분에 1초간 대기(중단) 가능
                println("정리 완료: 자원을 안전하게 해제했습니다.")
            }
            
            println("코루틴이 완전히 종료되었습니다.")
        }
    }

    delay(2500) // 2.5초 대기 후
    println("부모: 이제 자식 코루틴을 취소합니다.")
    job.cancelAndJoin()
    println("부모: 모든 작업이 끝났습니다.")
}
```

```
작업 시작: 5초 동안 진행됩니다...
작업 중... (0)
작업 중... (1)
작업 중... (2)
부모: 이제 자식 코루틴을 취소합니다.
취소 신호 감지!
정리 시작: 취소되었지만 이 작업은 끝까지 실행됩니다.
정리 완료: 자원을 안전하게 해제했습니다.
코루틴이 완전히 종료되었습니다.
부모: 모든 작업이 끝났습니다.
```

NonCancellable 객체는 항상 활성화 상태(isActive = true)를 유지하도록 설계된 특수한 싱글톤 Job 객체입니다.

- isActive: 무조건 true
- isCompleted: 무조건 false
- isCancelled: 무조건 false
- parent: 없음 (연결되지 않음)

즉, 누군가 이 Job에게 "취소해!"라고 명령해도, 내부적으로 아무 일도 일어나지 않도록 설계되어 있습니다.

구체적인 흐름은 다음과 같습니다.

1. Context 교체: 현재 코루틴이 가지고 있던 원래의 Job(취소된 상태일 수 있음)을 잠시 옆으로 치워둡니다.
2. 임시 Job 장착: 그 자리에 NonCancellable이라는 "절대 취소되지 않는 가짜 Job"을 끼워 넣습니다.
3. 중단 함수 실행: 이제 delay() 같은 중단 함수를 호출해도, 현재 Context의 Job(NonCancellable)이 `isActive == true`이므로 예외를 던지지 않고 정상 작동합니다.
4. 복구: 블록이 끝나면 다시 원래의 (취소된) Job으로 되돌립니다.

추가로 NonCancellable은 withContext와 함께 사용할 때만 의미가 있습니다. 예를 들어 `launch(NonCancellable) { ... }` 처럼 새로운 코루틴을 만들 때 사용하는 건 매우 위험합니다. 

부모와의 연결고리가 완전히 끊어진 '고아 코루틴'이 되어, 부모가 취소되어도 앱이 꺼질 때까지 혼자 돌아가는 좀비가 될 수 있기 때문입니다. 