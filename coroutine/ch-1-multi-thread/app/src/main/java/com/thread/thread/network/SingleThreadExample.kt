package com.thread.thread.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 1. 단일 스레드 방식 (메인 스레드)
 *
 * 특징
 * ⚠️ 경고: Android는 메인 스레드에서 네트워크 요청을 금지합니다!
 * 이 코드를 실행하면 NetworkOnMainThreadException이 발생합니다.
 * 학습 목적으로만 작성된 코드입니다.
 */
class SingleThreadExample {

    private val client = OkHttpClient()

    /**
     * 메인 스레드에서 직접 네트워크 요청 (실행 시 예외 발생)
     */
    fun fetchDataOnMainThread(url: String) {
        try {
            Log.d("SingleThread", "요청 시작: $url")

            val request = Request.Builder()
                .url(url)
                .build()

            // ⚠️ 여기서 NetworkOnMainThreadException 발생!
            val response = client.newCall(request).execute()

            response.use {
                if (it.isSuccessful) {
                    val body = it.body?.string()
                    Log.d("SingleThread", "응답 성공: ${body?.take(100)}")
                } else {
                    Log.e("SingleThread", "응답 실패: ${it.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("SingleThread", "오류 발생: ${e.message}", e)
        }
    }
}
