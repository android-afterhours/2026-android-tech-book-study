# 네트워크 요청 방식 비교

Android에서 네트워크 요청을 하는 4가지 방식을 비교하는 예제입니다.

## 📁 파일 구조

```
app/src/main/java/com/thread/thread/
├── MainActivity.kt                          # 메인 화면 (버튼으로 각 방식 테스트)
└── network/
    ├── SingleThreadExample.kt              # 1. 단일 스레드 (금지된 방식)
    ├── MultiThreadExample.kt               # 2. 멀티 스레드 - Thread
    ├── ExecutorExample.kt                  # 3. 멀티 스레드 - Executor
    └── CoroutineExample.kt                 # 4. 코루틴 (권장)
```

## 🔧 필요한 재료

### 1. AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 2. build.gradle.kts (app 레벨)
```kotlin
dependencies {
    // 네트워크 라이브러리
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 코루틴 (4번 방식에만 필요)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

## 📚 방식별 비교

### 1️⃣ 단일 스레드 (SingleThreadExample.kt)

**특징:**
- 메인 스레드에서 직접 네트워크 요청
- ⚠️ Android에서 **금지**됨 (NetworkOnMainThreadException 발생)

**코드:**
```kotlin
fun fetchDataOnMainThread(url: String) {
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute() // ⚠️ 예외 발생!
}
```

**장점:**
- 없음

**단점:**
- 실행 불가능 (예외 발생)
- UI가 멈춤 (ANR 발생 가능)

**사용 시기:**
- 절대 사용하지 말 것

---

### 2️⃣ 멀티 스레드 - Thread 클래스 (MultiThreadExample.kt)

**특징:**
- Thread 클래스를 직접 생성하여 백그라운드에서 실행
- Handler로 메인 스레드로 결과 전달

**코드:**
```kotlin
fun fetchDataWithThread(url: String, onResult: (String?) -> Unit) {
    Thread {
        // 백그라운드에서 네트워크 요청
        val response = client.newCall(request).execute()

        // 메인 스레드로 결과 전달
        mainHandler.post {
            onResult(response.body?.string())
        }
    }.start()
}
```

**장점:**
- 단순하고 직관적
- 별도의 라이브러리 불필요

**단점:**
- 스레드 관리가 어려움
- 메모리 누수 위험
- 콜백 지옥 (Callback Hell)
- 예외 처리 복잡

**사용 시기:**
- 간단한 일회성 작업
- 레거시 코드 유지보수

---

### 3️⃣ 멀티 스레드 - Executor (ExecutorExample.kt)

**특징:**
- Executor를 사용하여 스레드 풀 관리
- Thread 방식보다 효율적

**코드:**
```kotlin
private val executor = Executors.newFixedThreadPool(3)

fun fetchDataWithExecutor(url: String, onResult: (String?) -> Unit) {
    executor.execute {
        // 백그라운드에서 네트워크 요청
        val response = client.newCall(request).execute()

        // 메인 스레드로 결과 전달
        mainHandler.post {
            onResult(response.body?.string())
        }
    }
}
```

**장점:**
- 스레드 재사용 (성능 향상)
- 스레드 수 제어 가능
- Thread 방식보다 안전

**단점:**
- 여전히 콜백 기반
- 코드가 복잡
- 취소/예외 처리 어려움

**사용 시기:**
- 여러 개의 백그라운드 작업
- Thread 방식보다 나은 관리가 필요할 때

---

### 4️⃣ 코루틴 (CoroutineExample.kt) ⭐ **권장**

**특징:**
- Kotlin의 비동기 프로그래밍 솔루션
- 순차적 코드처럼 작성 가능
- 가볍고 효율적

**코드:**
```kotlin
suspend fun fetchDataWithCoroutine(url: String): String? {
    return withContext(Dispatchers.IO) {
        // IO 스레드에서 네트워크 요청
        val response = client.newCall(request).execute()
        response.body?.string()
    }
}

// 사용
lifecycleScope.launch {
    val result = fetchDataWithCoroutine(url)
    // 자동으로 메인 스레드에서 실행
    updateUI(result)
}
```

**장점:**
- 순차적 코드 (가독성 좋음)
- 자동 스레드 전환
- 쉬운 취소 및 예외 처리
- 메모리 효율적
- Android 공식 권장 방식

**단점:**
- 학습 곡선 (처음엔 낯설 수 있음)
- 코루틴 스코프 이해 필요

**사용 시기:**
- 모든 비동기 작업 (권장)
- 특히 Android Jetpack과 함께 사용

---

## 🎯 비교 표

| 방식 | 난이도 | 성능 | 가독성 | 추천도 | 비고 |
|------|--------|------|--------|--------|------|
| 단일 스레드 | ⭐ | ❌ | ⭐⭐⭐ | ❌ | 사용 불가 |
| Thread | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐ | 레거시 |
| Executor | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | Thread보다 나음 |
| Coroutine | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **권장** |

## 🚀 실행 방법

1. 앱을 실행합니다
2. 화면에 4개의 버튼이 표시됩니다
3. 각 버튼을 클릭하여 방식별 네트워크 요청을 테스트합니다
4. Logcat에서 상세한 로그를 확인할 수 있습니다

```bash
# Logcat 필터링
adb logcat | grep -E "SingleThread|MultiThread|Executor|Coroutine"
```

## 📊 실행 결과 예시

### 성공적인 요청 (방식 2, 3, 4)
```
MultiThread: 요청 시작 (Thread: Thread-2)
MultiThread: 응답 성공 (Thread: Thread-2)
MultiThread: UI 업데이트 (Thread: main)
```

### 실패하는 요청 (방식 1)
```
SingleThread: 요청 시작
E/AndroidRuntime: FATAL EXCEPTION: main
    android.os.NetworkOnMainThreadException
```

## 💡 권장 사항

1. **신규 프로젝트**: 코루틴 사용 (방식 4)
2. **레거시 유지보수**:
   - Thread → Executor로 마이그레이션 (방식 2 → 3)
   - 최종적으로 코루틴으로 전환 (방식 3 → 4)
3. **학습 순서**: Thread → Executor → Coroutine

---

## 🔥 Executor의 치명적 문제점 (3번 테스트)

### 문제 1: 콜백 지옥 (Callback Hell)

**시나리오**: 사용자 정보를 순차적으로 가져오기

```kotlin
// ❌ Executor 방식 - 콜백 4단계 중첩!
executor.execute {
    val userId = login()  // 1단계

    executor.execute {
        val userInfo = getUserInfo(userId)  // 2단계

        executor.execute {
            val image = downloadImage()  // 3단계

            executor.execute {
                val friends = getFriends()  // 4단계

                friends.forEach { friend ->
                    executor.execute {
                        getStatus(friend)  // 5단계!!!
                    }
                }
            }
        }
    }
}
```

**문제점:**
- 들여쓰기가 점점 깊어짐 (가독성 최악)
- 각 단계마다 에러 처리 필요
- 코드 수정이 매우 어려움

### 문제 2: 스레드 블록킹

**시나리오**: 풀 크기 2개, 작업 A가 B, C를 실행하고 결과를 기다림

```kotlin
val executor = Executors.newFixedThreadPool(2)  // 스레드 2개만!

// 작업 A 실행
executor.execute {
    // B, C 작업 제출
    val futureB = executor.submit { /* 작업 B */ }
    val futureC = executor.submit { /* 작업 C */ }

    // ❌ 여기서 블록킹 발생!
    val resultB = futureB.get()  // 스레드가 대기하며 낭비됨
    val resultC = futureC.get()  // 스레드가 대기하며 낭비됨
}
```

**무슨 일이 일어나나?**
1. 작업 A가 스레드 1개 점유
2. 작업 A는 `futureB.get()`에서 **블록킹** (스레드 낭비!)
3. 남은 스레드 1개로 B, C를 **순차 실행** (병렬 아님!)
4. 성능 저하

**책에서 말한 문제:**
> "스레드 블록킹은 스레드 기반 작업을 하는 멀티 스레드 프로그래밍에서 피할 수 없는 문제"

→ `future.get()`을 호출하면 스레드가 대기하며 **블록**됨
→ 만들어진 스레드가 성능을 제대로 발휘하지 못함

### 문제 3: 복잡한 작업 종속성

**시나리오**: API 호출 종속성

```
API 1 호출
    ↓
API 2, 3 병렬 호출 (API 1 결과 사용)
    ↓
API 4 호출 (API 2, 3 결과 사용)
```

**Executor로 구현:**
```kotlin
executor.execute {
    val result1 = callAPI1()  // 1번 호출

    val future2 = executor.submit { callAPI2(result1) }  // 2번 병렬
    val future3 = executor.submit { callAPI3(result1) }  // 3번 병렬

    val result2 = future2.get()  // ❌ 블록킹
    val result3 = future3.get()  // ❌ 블록킹

    val result4 = callAPI4(result2, result3)  // 4번 호출
}
```

**문제점:**
- 작업 간 종속성 관리가 복잡
- `future.get()`으로 스레드 블록킹 불가피
- 에러 처리 복잡 (각 단계마다 try-catch)
- 취소 처리 어려움

### ✅ 해결책: 코루틴!

**같은 작업을 코루틴으로:**
```kotlin
suspend fun loadUserData() {
    val userId = login()                    // 순차
    val userInfo = getUserInfo(userId)      // 순차

    // 병렬 실행 (async)
    val image = async { downloadImage() }
    val friends = async { getFriends() }

    // 결과 대기 (블록킹 없음!)
    val imageResult = image.await()
    val friendsList = friends.await()

    // 친구 상태 조회
    friendsList.forEach { friend ->
        val status = getStatus(friend)
    }
}
```

**장점:**
1. **순차적 코드**: 위에서 아래로 읽힘 (가독성 좋음)
2. **콜백 없음**: 중첩 없음!
3. **스레드 블록킹 없음**: `await()`는 스레드를 블록하지 않음
4. **쉬운 병렬 처리**: `async`/`await`
5. **간단한 에러 처리**: `try-catch`로 끝
6. **자동 취소**: 코루틴 스코프가 취소되면 자동 취소

---

## 📝 추가 학습 자료

- [Kotlin Coroutines 공식 문서](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android 코루틴 가이드](https://developer.android.com/kotlin/coroutines)
- [OkHttp 공식 문서](https://square.github.io/okhttp/)
