package com.champaca.inventorydata.common

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Service
class ItemLockService {
    private val lockMap = ConcurrentHashMap<String, ReentrantLock>()

    fun lock(itemRefCode: String) {
        lockMap.computeIfAbsent(itemRefCode) { ReentrantLock() }.lock()
    }

    fun unlock(itemRefCode: String) {
        val lock = lockMap[itemRefCode]
        lock?.unlock()
    }
}