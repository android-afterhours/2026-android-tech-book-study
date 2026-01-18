package com.thread.thread

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.thread.thread.network.CoroutineExample
import com.thread.thread.network.ExecutorExample
import com.thread.thread.network.MultiThreadExample
import com.thread.thread.network.SingleThreadExample
import com.thread.thread.network.ThreadBlockingExample
import com.thread.thread.ui.theme.ThreadTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 예제 인스턴스들
    private val singleThreadExample = SingleThreadExample()
    private val multiThreadExample = MultiThreadExample()
    private val executorExample = ExecutorExample()
    private val coroutineExample = CoroutineExample()
    private val threadBlockingExample = ThreadBlockingExample()

    // 테스트용 URL
    private val testUrl = "https://jsonplaceholder.typicode.com/posts/1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThreadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NetworkExampleScreen(
                        modifier = Modifier.padding(innerPadding),

                        onSingleThreadClick = ::testSingleThread,
                        onMultiThreadClick = ::testMultiThread,
                        onExecutorClick = ::testExecutor,
                        onCoroutineClick = ::testCoroutine,

                        onMutexBlockingClick = ::testMutexBlocking,
                        onSemaphoreBlockingClick = ::testSemaphoreBlocking,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorExample.shutdown()
        threadBlockingExample.shutdown()
    }

    private fun testSingleThread() {
        Toast.makeText(this, "단일 스레드 테스트 시작 (예외 발생 예정)", Toast.LENGTH_SHORT).show()
        // 메인 스레드에서 실행하면 NetworkOnMainThreadException 발생
        singleThreadExample.fetchDataOnMainThread(testUrl)
    }

    private fun testMultiThread() {
        Toast.makeText(this, "멀티 스레드 테스트 시작", Toast.LENGTH_SHORT).show()
        multiThreadExample.fetchDataWithThread(testUrl) { result ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Thread 완료: ${result?.take(50)}...",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun testExecutor() {
        Toast.makeText(this, "Executor 테스트 시작", Toast.LENGTH_SHORT).show()
        executorExample.fetchDataWithExecutor(testUrl) { result ->
            Toast.makeText(
                this,
                "Executor 완료: ${result?.take(50)}...",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun testCoroutine() {
        Toast.makeText(this, "코루틴 테스트 시작", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = coroutineExample.fetchDataWithCoroutine(testUrl)
            Toast.makeText(
                this@MainActivity,
                "Coroutine 완료: ${result?.take(50)}...",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 스레드 블로킹 예제들
    private fun testMutexBlocking() {
        Toast.makeText(this, "Mutex 블로킹 테스트 (Logcat)", Toast.LENGTH_SHORT).show()
        threadBlockingExample.mutexBlockingExample()
    }

    private fun testSemaphoreBlocking() {
        Toast.makeText(this, "Semaphore 블로킹 테스트 (Logcat)", Toast.LENGTH_SHORT).show()
        threadBlockingExample.semaphoreBlockingExample()
    }
}

@Composable
fun NetworkExampleScreen(
    modifier: Modifier = Modifier,
    onSingleThreadClick: () -> Unit = {},
    onMultiThreadClick: () -> Unit = {},
    onExecutorClick: () -> Unit = {},
    onCoroutineClick: () -> Unit = {},
    onMutexBlockingClick: () -> Unit = {},
    onSemaphoreBlockingClick: () -> Unit = {}
) {
    var selectedMethod by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "네트워크 요청 방식 비교",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                selectedMethod = "Single Thread"
                onSingleThreadClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("1. 단일 스레드 (예외 발생)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedMethod = "Multi Thread"
                onMultiThreadClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("2. 멀티 스레드 - Thread")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedMethod = "Executor"
                onExecutorClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("3. 멀티 스레드 - Executor")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedMethod = "Coroutine"
                onCoroutineClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("4. 코루틴 (권장)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "스레드 블로킹 예제",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedMethod = "Mutex Blocking"
                onMutexBlockingClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("1. Mutex 블로킹")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                selectedMethod = "Semaphore Blocking"
                onSemaphoreBlockingClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("2. Semaphore 블로킹")
        }

        if (selectedMethod.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "선택된 방식: $selectedMethod",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
