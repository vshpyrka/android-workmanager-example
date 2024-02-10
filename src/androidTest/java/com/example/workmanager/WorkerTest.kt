package com.example.workmanager

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WorkerTest {

    private var context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var worker: OneTimeWorkTask

    @Before
    fun setUp() {
        worker = TestListenableWorkerBuilder<OneTimeWorkTask>(context)
            .build()
    }

    @Test
    fun testWorker() = runTest {
        val result = worker.doWork()
        Assert.assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testConstrainedPeriodicWork() = runTest {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<PeriodicWorkTask>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // sync call to make sure work is enqueued
        workManager.enqueue(request).result.get()

        with(testDriver) {
            setPeriodDelayMet(request.id)
            setAllConstraintsMet(request.id)
        }

        val workInfo = workManager.getWorkInfoByIdFlow(request.id).first()
        Assert.assertEquals(workInfo.state, WorkInfo.State.RUNNING)
    }

    @Test
    fun testConfiguredWork() = runTest {
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)

        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .build()
        workManager.enqueue(request).result.get()
    }
}
