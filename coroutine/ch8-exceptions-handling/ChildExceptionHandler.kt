import kotlinx.coroutines.*

fun main() = runBlocking {
    val parentHandler = CoroutineExceptionHandler { _, e -> println("부모 핸들러 동작: ${e.message}") }
    val childHandler = CoroutineExceptionHandler { _, e -> println("자식 핸들러 동작: ${e.message}") }

    // 케이스 1. 일반 Job (자식 핸들러 무시됨)
    val scope = CoroutineScope(Job() + parentHandler)
    scope.launch {
        launch(childHandler) { // ❌ 무시됨 (예외가 부모로 즉시 전파)
            throw Exception("일반 Job 예외")
        }
    }.join()

    // 케이스 2. SupervisorJob (자식 핸들러가 동작함)
    val supervisorScope = CoroutineScope(SupervisorJob() + parentHandler)
    supervisorScope.launch(childHandler) { // ✅ 동작함 (예외가 위로 전파되지 않음)
        throw Exception("SupervisorJob 예외")
    }.join()
}

/****
 * [출력 결과]
 * 부모 핸들러 동작: 일반 Job 예외
 * 자식 핸들러 동작: SupervisorJob 예외
 */
