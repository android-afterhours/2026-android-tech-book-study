## 코루틴 간의 경쟁 상태 

[관련 이슈](https://github.com/android-afterhours/2026-android-tech-book-study/issues/15)

단일 스레드 환경에서도 코루틴 간의 경쟁 상태(Race Condition)는 발생할 수 있습니다.

멀티 스레드의 경쟁 상태는 여러 스레드가 동시에 메모리에 접근해서 발생한다면, 단일 스레드 환경에서 코루틴의 경쟁 상태는 작업이 중단(Suspend) 되었다가 재개(Resume) 되는 사이의 간극에서 발생합니다.

1. 코루틴 A가 공유 변수 값을 읽습니다.
2. 코루틴 A가 suspend 함수(예: delay, 네트워크 호출 등)를 만나 잠시 멈춥니다.
3. 이때 코루틴 B가 실행되어 공유 변수 값을 수정합니다.
4. 코루틴 A가 다시 깨어나서 아까 읽어두었던 (이미 과거의 것이 된) 값을 바탕으로 수정을 마칩니다.
5. 결과적으로 코루틴 B의 작업이 덮어씌워져 사라집니다.

예시 코드는 다음과 같습니다. 

```kotlin 
import kotlinx.coroutines.*

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() = runBlocking {
    printWithThread("실행")

    var sum = 0
    val singleThreadContext = newSingleThreadContext("SingleThread")

    withContext(singleThreadContext) {
        printWithThread("실행")

        val jobs = List(10) {
            launch {
                repeat(10) {
                    val cnt = sum
                    delay(1) // 중단 지점 발생! 이때 다른 코루틴에게 제어권이 넘어감
                    sum = cnt + 1
                }
                printWithThread("sum: $sum")
            }
        }
        jobs.joinAll()
    }

    println("Total Sum: $sum") // 기대값: 100, 실제값: 10
}
```

>[main @coroutine#1] 실행<br>
[SingleThread @coroutine#1] 실행<br>
[SingleThread @coroutine#2] sum: 10<br>
[SingleThread @coroutine#3] sum: 10<br>
[SingleThread @coroutine#4] sum: 10<br>
[SingleThread @coroutine#5] sum: 10<br>
[SingleThread @coroutine#6] sum: 10<br>
[SingleThread @coroutine#7] sum: 10<br>
[SingleThread @coroutine#8] sum: 10<br>
[SingleThread @coroutine#9] sum: 10<br>
[SingleThread @coroutine#10] sum: 10<br>
[SingleThread @coroutine#11] sum: 10<br>
Total Sum: 10<br>

| 순서 | 실행 대상  | 작업 내용                       | sum (공유 변수) |
| -- | ------ | --------------------------- | ----------- |
| 1  | 코루틴 1  | cnt = sum (0 읽음) → delay 시작 | 0           |
| 2  | 코루틴 2  | cnt = sum (0 읽음) → delay 시작 | 0           |
|...|...|(코루틴 3~9 동일 반복)|0|
| 10 | 코루틴 10 | cnt = sum (0 읽음) → delay 시작 | 0           |
| 11 | 코루틴 1  | sum = 0 + 1 (쓰기)            | 1           |
| 12 | 코루틴 2  | sum = 0 + 1 (쓰기, 덮어씀)       | 1           |
| 13 | 코루틴 10 | sum = 0 + 1 (쓰기, 덮어씀)       | 1           |

10개의 코루틴이 한 번씩 루프를 돌았는데, sum은 10이 아니라 1이 됩니다. 이 과정이 10번 반복되므로 최종 결과가 10이 나오게 됩니다. 

## 동시성 문제 해결 방법 

### Mutex 

```kotlin 
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() = runBlocking {
    var sum = 0
    val singleThreadContext = newSingleThreadContext("SingleThread")
    val mutex = Mutex()

    withContext(singleThreadContext) {
        val jobs = List(10) {
            launch {
                repeat(10) {
                    mutex.withLock {
                        val cnt = sum
                        delay(1)
                        sum = cnt + 1
                    }
                }

            }
        }
        jobs.joinAll()
    }

    println("Total Sum: $sum") // 100 
}
```

### Atomic 변수 

```kotlin
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val counter = AtomicInteger(0)
    val singleThreadContext = newSingleThreadContext("SingleThread")

    withContext(singleThreadContext) {
        val jobs = List(10) {
            launch {
                repeat(10) {
                    delay(1)
                    counter.incrementAndGet()
                }
            }
        }
        jobs.joinAll()
    }

    println("Total Sum: $counter") // 100 
}
```


