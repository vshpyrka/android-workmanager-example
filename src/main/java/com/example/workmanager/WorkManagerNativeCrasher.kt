package com.example.workmanager

class WorkManagerNativeCrasher {

    external fun executeNativeCrash()

    companion object {
        init {
            System.loadLibrary("work_manager_crasher")
        }
    }
}
