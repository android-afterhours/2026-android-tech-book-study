package com.thread.thread.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 4. 코루틴 방식 (Kotlin Coroutines)
 *
 * 특징
 * - 스레드 전환이 간단 (withContext)
 * - 순차적 코드처럼 작성 가능 (콜백 지옥 방지)
 * - 자동 취소 및 예외 처리
 * - 메모리 효율적
 */
class CoroutineExample {

    private val client = OkHttpClient()

    /**
     * 코루틴을 사용한 네트워크 요청
     */
    suspend fun fetchDataWithCoroutine(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("Coroutine", "요청 시작 (Thread: ${Thread.currentThread().name})")

                val request = Request.Builder()
                    .url(url)
                    .build()

                // IO 스레드에서 네트워크 요청 실행
                val response = client.newCall(request).execute()

                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string()
                        Log.d("Coroutine", "응답 성공 (Thread: ${Thread.currentThread().name})")
                        body
                    } else {
                        Log.e("Coroutine", "응답 실패: ${it.code}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("Coroutine", "오류 발생: ${e.message}", e)
                null
            }
        }
    }
}
