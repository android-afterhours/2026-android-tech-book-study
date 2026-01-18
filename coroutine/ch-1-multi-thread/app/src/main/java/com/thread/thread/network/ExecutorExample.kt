package com.thread.thread.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors

/**
 * 3. 멀티 스레드 방식 - Executor 사용
 *
 * 특징
 * Executor를 사용하여 스레드 풀을 관리하고 효율적으로 네트워크 요청을 수행합니다.
 * 직접 Thread를 생성하는 것보다 스레드 재사용, 관리가 편리합니다.
 */
class ExecutorExample {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 고정 크기 스레드 풀 (3개의 스레드)
    private val executor = Executors.newFixedThreadPool(3)

    /**
     * Executor를 사용하여 네트워크 요청 수행
     */
    fun fetchDataWithExecutor(url: String, onResult: (String?) -> Unit) {
        // Executor에 작업 제출
        executor.execute {
            try {
                Log.d("Executor", "요청 시작 (Thread: ${Thread.currentThread().name})")

                val request = Request.Builder()
                    .url(url)
                    .build()

                // 백그라운드 스레드에서 네트워크 요청 실행
                val response = client.newCall(request).execute()

                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string()
                        Log.d("Executor", "응답 성공 (Thread: ${Thread.currentThread().name})")

                        // 메인 스레드로 결과 전달
                        mainHandler.post {
                            Log.d("Executor", "UI 업데이트 (Thread: ${Thread.currentThread().name})")
                            onResult(body)
                        }
                    } else {
                        Log.e("Executor", "응답 실패: ${it.code}")
                        mainHandler.post { onResult(null) }
                    }
                }
            } catch (e: Exception) {
                Log.e("Executor", "오류 발생: ${e.message}", e)
                mainHandler.post { onResult(null) }
            }
        }
    }

    /**
     * Executor 종료 (앱 종료 시 호출)
     */
    fun shutdown() {
        executor.shutdown()
    }
}
