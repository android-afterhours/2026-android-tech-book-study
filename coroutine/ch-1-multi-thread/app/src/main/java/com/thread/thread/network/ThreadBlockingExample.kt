package com.thread.thread.network

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock

/**
 * 스레드 블로킹 예제
 *
 * 스레드가 블로킹되는 상황을 보여줍니다:
 * 1. Mutex (뮤텍스)로 인한 블로킹 - synchronized와 ReentrantLock
 * 2. Semaphore (세마포어)로 인한 블로킹
 */
class ThreadBlockingExample {

    private val executor = Executors.newFixedThreadPool(5)

    /**
     * 예제 1: Mutex (뮤텍스)로 인한 블로킹
     *
     * Mutex는 상호 배제(Mutual Exclusion)를 의미하며,
     * 여러 스레드가 동시에 접근하려 할 때 하나의 스레드만 접근이 허용되고
     * 나머지는 블로킹됩니다.
     *
     * synchronized 키워드로 암묵적 뮤텍스 구현
     */
    fun mutexBlockingExample() {
        Log.d("ThreadBlocking", "=== Mutex (뮤텍스) 블로킹 예제 시작 ===")

        val sharedResource = SharedCounter()

        // 5개의 스레드가 동시에 공유 자원에 접근 시도
        // synchronized로 한 번에 1개만 접근 허용
        repeat(5) { index ->
            executor.execute {
                Log.d("ThreadBlocking", "Thread-$index: Mutex 진입 대기 중...")
                sharedResource.incrementWithSync(index)
            }
        }
    }

    /**
     * 예제 2: Semaphore (세마포어)로 인한 블로킹
     *
     * Semaphore는 동시에 접근할 수 있는 스레드 수를 제한합니다.
     * 허가(permit) 수를 초과하는 스레드는 블로킹됩니다.
     */
    fun semaphoreBlockingExample() {
        Log.d("ThreadBlocking", "=== Semaphore 블로킹 예제 시작 ===")

        // 최대 2개의 스레드만 동시 접근 허용
        val semaphore = Semaphore(2)

        // 5개의 스레드가 접근 시도 -> 3개는 대기
        repeat(5) { index ->
            executor.execute {
                Log.d("ThreadBlocking", "Thread-$index: Semaphore 허가 대기 중... (사용 가능: ${semaphore.availablePermits()}/2)")

                try {
                    semaphore.acquire() // 허가 획득 (없으면 블로킹)
                    Log.d("ThreadBlocking", "Thread-$index: Semaphore 허가 획득! 작업 시작")

                    // 긴 작업 시뮬레이션
                    Thread.sleep(2000)

                    Log.d("ThreadBlocking", "Thread-$index: 작업 완료, Semaphore 반납")
                } catch (e: InterruptedException) {
                    Log.e("ThreadBlocking", "Thread-$index 인터럽트 발생", e)
                } finally {
                    semaphore.release() // 허가 반납
                }
            }
        }
    }

    /**
     * Executor 종료
     */
    fun shutdown() {
        executor.shutdown()
    }

    /**
     * 공유 자원 클래스
     */
    private class SharedCounter {
        private var count = 0
        private val lock = ReentrantLock()

        /**
         * synchronized 키워드를 사용한 동기화
         */
        @Synchronized
        fun incrementWithSync(threadId: Int) {
            Log.d("ThreadBlocking", "Thread-$threadId: 동기화 블록 진입 성공! (현재 count: $count)")

            // 긴 작업 시뮬레이션 (2초)
            Thread.sleep(2000)

            count++
            Log.d("ThreadBlocking", "Thread-$threadId: 작업 완료 (count 증가: $count)")
        }
    }
}
