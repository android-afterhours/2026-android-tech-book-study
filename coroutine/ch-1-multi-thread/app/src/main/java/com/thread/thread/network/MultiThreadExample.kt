package com.thread.thread.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 2. 멀티 스레드 방식 - Thread 클래스 직접 사용
 *
 * 특징
 * Thread 클래스를 직접 생성하여 백그라운드에서 네트워크 요청을 수행합니다.
 * Handler를 사용하여 메인 스레드로 결과를 전달합니다.
 */
class MultiThreadExample {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 새로운 Thread를 생성하여 네트워크 요청 수행
     */
    fun fetchDataWithThread(url: String, onResult: (String?) -> Unit) {
        // 새로운 스레드 생성 및 시작
        Thread {
            try {
                Log.d("MultiThread", "요청 시작 (Thread: ${Thread.currentThread().name})")

                val request = Request.Builder()
                    .url(url)
                    .build()

                // 백그라운드 스레드에서 네트워크 요청 실행
                val response = client.newCall(request).execute()

                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string()
                        Log.d("MultiThread", "응답 성공 (Thread: ${Thread.currentThread().name})")

                        // 메인 스레드로 결과 전달
                        mainHandler.post {
                            Log.d("MultiThread", "UI 업데이트 (Thread: ${Thread.currentThread().name})")
                            onResult(body)
                        }
                    } else {
                        Log.e("MultiThread", "응답 실패: ${it.code}")
                        mainHandler.post { onResult(null) }
                    }
                }
            } catch (e: Exception) {
                Log.e("MultiThread", "오류 발생: ${e.message}", e)
                mainHandler.post { onResult(null) }
            }
        }.start() // 스레드 시작
    }
}
