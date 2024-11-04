package com.example.workmanager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ArrayCreatingInputMerger
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.OverwritingInputMerger
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.setInputMerger
import androidx.work.workDataOf
import com.example.workmanager.databinding.ActivityWorkManagerBinding
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.TimeUnit

class WorkManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createNotificationChannel()

        staticLogger = ::printStatus

        binding.text.movementMethod = ScrollingMovementMethod()

        binding.simpleWork.exec(::runSimpleWork)
        binding.retryWork.exec(::runRetryWork)
        binding.errorWork.exec(::runErrorWork)
        binding.expeditedWork.exec(::runExpeditedWork)
        binding.periodicWork.exec(::runPeriodicTask)
        binding.periodicIntervalWork.exec(::runPeriodicIntervalTask)
        binding.constrainedWork.exec(::runConstrainedTask)
        binding.delayedWork.exec(::runDelayedTask)
        binding.backoffWork.exec(::runBackoffTask)
        binding.taggedWork.exec(::runTaggedTask)
        binding.inputDataWork.exec(::runInputTask)
        binding.uniqueWork.exec(::runUniqueTask)
        binding.uniquePeriodicWork.exec(::runUniquePeriodicTask)
        binding.observeWork.exec(::observerTask)
        binding.complexWorkQuery.exec(::complexWorkQuery)
        binding.enqueueWork.exec(::enqueueWork)
        binding.overwritingInputMergerWork.exec(::overwritingInputMergerWork)
        binding.arrayCreatingInputMergerWork.exec(::arrayCreatingInputMergerWork)
        binding.updateWork.exec(::updateWork)
        binding.workGeneration.exec(::workGeneration)
        binding.separateProcessWork.exec(::separateWorkRequest)
        binding.cancelAll.exec(::cancelAll)
    }

    override fun onDestroy() {
        super.onDestroy()
        staticLogger = null
    }

    private fun runSimpleWork() {
        // Simple way to create work request
        OneTimeWorkRequest.from(OneTimeWorkTask::class.java)
        // More complex way to build work request
        val workRequest = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .build()
        // Schedule work request
        WorkManager.getInstance(this)
            .enqueue(workRequest)

        printRequestStatus(workRequest.id)
    }

    private fun runRetryWork() {
        val workRequest = OneTimeWorkRequestBuilder<RetryWorkTask>().build()
        WorkManager.getInstance(this)
            .enqueue(workRequest)

        printRequestStatus(workRequest.id)
    }

    private fun runErrorWork() {
        val workRequest = OneTimeWorkRequestBuilder<ErrorWorkTask>().build()
        WorkManager.getInstance(this)
            .enqueue(workRequest)

        printRequestStatus(workRequest.id)
    }

    private fun runExpeditedWork() {
        val workRequest = OneTimeWorkRequestBuilder<ExpeditedWorkTask>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(this)
            .enqueue(workRequest)

        printRequestStatus(workRequest.id)
    }

    private fun runPeriodicTask() {
        val periodicRequest = PeriodicWorkRequestBuilder<PeriodicWorkTask>(
            1,
            TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this)
            .enqueue(periodicRequest)

        printRequestStatus(periodicRequest.id)
    }

    private fun runPeriodicIntervalTask() {
        val minRepeatInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
        val minFlexInterval = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS

        printStatus("$minRepeatInterval $minFlexInterval")
        val periodicRequest = PeriodicWorkRequestBuilder<PeriodicWorkTask>(
            repeatInterval = 20,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(this)
            .enqueue(periodicRequest)

        printRequestStatus(periodicRequest.id)
    }

    private fun runConstrainedTask() {
        val constraints = with(Constraints.Builder()) {
            setRequiredNetworkType(NetworkType.UNMETERED) // WIFI
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setRequiresDeviceIdle(false)
            }
            setRequiresStorageNotLow(true)

            build()
        }

        val constrainedWorkRequest = PeriodicWorkRequestBuilder<PeriodicWorkTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueue(constrainedWorkRequest)

        printRequestStatus(constrainedWorkRequest.id)
    }

    private fun runDelayedTask() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this)
            .enqueue(request)

        printRequestStatus(request.id)
    }

    private fun runBackoffTask() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            // 10, 20, 30, 40
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.LINEAR,
                backoffDelay = 10,
                timeUnit = TimeUnit.MINUTES
            )
            // 10, 20, 40, 80
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = 10,
                timeUnit = TimeUnit.MINUTES
            )
            .build()
        WorkManager.getInstance(this)
            .enqueue(request)

        printRequestStatus(request.id)
    }

    private fun runTaggedTask() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .addTag("cancellable")
            .build()
        val workManager = WorkManager.getInstance(this)

        workManager.enqueue(request)

        printRequestStatus(request.id, tag = "cancellable")

        workManager.cancelAllWorkByTag("cancellable")
    }

    private fun runInputTask() {
        val request = OneTimeWorkRequestBuilder<InputDataWorkTask>()
            .setInputData(
                workDataOf(
                    "key" to "Hello World!"
                ),
//                Data.Builder()
//                    .putString("key", UUID.randomUUID().toString())
//                    .build()
            )
            .build()
        WorkManager.getInstance(this)
            .enqueue(request)

        printRequestStatus(request.id)
    }

    private fun runUniqueTask() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "runUniqueTask",
                ExistingWorkPolicy.KEEP,
                request
            )

        printRequestStatus(request.id)
    }

    private fun runUniquePeriodicTask() {
        val request = PeriodicWorkRequestBuilder<PeriodicWorkTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "runUniquePeriodicTask",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        printRequestStatus(request.id)
    }

    private fun observerTask() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .addTag("observedTask")
            .build()

        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(request)
        val requestId = request.id

        val liveData = workManager.getWorkInfoByIdLiveData(requestId)
        liveData.observe(this) {
            printStatus(it.toString())
        }
    }

    private fun complexWorkQuery() {
        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .addTag("complexTask")
            .build()

        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(
            "uniqueComplexWork",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )

        val query = WorkQuery.Builder
            .fromTags(
                listOf("observedTask")
            )
            .addStates(
                listOf(WorkInfo.State.SUCCEEDED)
            )
            .addUniqueWorkNames(
                listOf(
                    "uniqueComplexWork"
                )
            ).build()

        val queryLiveData = workManager.getWorkInfosLiveData(query)
        queryLiveData.observe(this) { workInfoList ->
            workInfoList.forEach {
                printStatus(it.toString())
            }
        }
    }

    private fun enqueueWork() {
        val firstRequest = OneTimeWorkRequest.from(SimpleOneTimeWorkTask::class.java)
        val secondRequest = OneTimeWorkRequest.from(OneTimeWorkTask::class.java)
        WorkManager.getInstance(this)
            .beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()
    }

    private fun overwritingInputMergerWork() {
        val tasks = listOf(
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key1" to "value1")
                )
                .build(),
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key1" to "value1")
                )
                .build(),
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key2" to "value2")
                )
                .build()
        )
        val mergeInputTask = OneTimeWorkRequestBuilder<MergeInputTask>()
            .setInputMerger(OverwritingInputMerger::class)
            .build()

        // Will pass one key when it's occurred multiple times
        WorkManager.getInstance(this)
            .beginWith(tasks)
            .then(mergeInputTask)
            .enqueue()

        printRequestStatus(mergeInputTask.id)
    }

    private fun arrayCreatingInputMergerWork() {
        val tasks = listOf(
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key1" to "value1")
                )
                .build(),
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key1" to "value1")
                )
                .build(),
            OneTimeWorkRequestBuilder<MergingTask>()
                .setInputData(
                    workDataOf("key2" to "value2")
                )
                .build()
        )
        val mergeInputTask = OneTimeWorkRequestBuilder<MergeInputTask>()
            .setInputMerger(ArrayCreatingInputMerger::class)
            .build()
        // Will pass combined values with the same key
        WorkManager.getInstance(this)
            .beginWith(tasks)
            .then(mergeInputTask)
            .enqueue()

        printRequestStatus(mergeInputTask.id)
    }

    private fun updateWork() {
        val oldConstraints = with(Constraints.Builder()) {
            setRequiredNetworkType(NetworkType.METERED) // MOBILE
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)
            build()
        }

        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .setConstraints(oldConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()


        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(
            "uniqueUpdatedWork",
            ExistingWorkPolicy.REPLACE,
            request
        )

        val newConstraints = with(Constraints.Builder()) {
            setRequiredNetworkType(NetworkType.UNMETERED) // WIFI
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)
            build()
        }

        val newRequest = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .setId(request.id)
            .setConstraints(newConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        when (workManager.updateWork(newRequest).get()) {
            WorkManager.UpdateResult.NOT_APPLIED -> {
                println("Not applied")
            }

            WorkManager.UpdateResult.APPLIED_IMMEDIATELY -> {
                println("Applied immediately")
            }

            WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN -> {
                println("Applied for next run")
            }

            else -> {
                println("Something weird happened")
            }
        }
    }

    private fun workGeneration() {
        val oldConstraints = with(Constraints.Builder()) {
            setRequiredNetworkType(NetworkType.METERED) // MOBILE
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)
            build()
        }

        val request = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .setConstraints(oldConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()


        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(
            "uniqueGenerationTask",
            ExistingWorkPolicy.REPLACE,
            request
        )

        val newConstraints = with(Constraints.Builder()) {
            setRequiredNetworkType(NetworkType.UNMETERED) // WIFI
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)
            build()
        }

        val newRequest = OneTimeWorkRequestBuilder<OneTimeWorkTask>()
            .setId(request.id)
            .setConstraints(newConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        when (workManager.updateWork(newRequest).get()) {
            WorkManager.UpdateResult.APPLIED_IMMEDIATELY,
            WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN -> {

                val workInfo =
                    workManager.getWorkInfosForUniqueWork("uniqueGenerationTask")
                        .get()
                        .first()
                val generation = workInfo.generation
                println("Updated work generation = $generation")
            }

            else -> {

            }
        }
    }

    private fun separateWorkRequest() {
        val request = OneTimeWorkRequestBuilder<CrashingTask>()
            .setInputData(
                workDataOf(
                    ARGUMENT_PACKAGE_NAME to packageName,
                    ARGUMENT_CLASS_NAME to CustomRemoteWorkService::class.java.name
                )
            )
            .build()
        WorkManager.getInstance(this).enqueue(request)

        printRequestStatus(request.id)
    }

    private fun cancelAll() {
        WorkManager.getInstance(this)
            .cancelAllWork()
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat.Builder(
            "android.playground.workmanager.notification.channel.id",
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(
            "android.playground.workmanager.notification.channel.name"
        ).build()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(notificationChannel)
    }

    private inline fun View.exec(crossinline func: () -> Unit) {
        setOnClickListener {
            this as TextView
            clearStatus()
            printStatus("Status: $text")
            func()
        }
    }

    private fun printRequestStatus(id: UUID, tag: String? = null) {
        val workManager = WorkManager.getInstance(this)
        if (tag != null) {
            workManager.getWorkInfosByTagLiveData(tag)
                .observe(this) { workInfoList ->
                    workInfoList.forEach {
                        val stopReason = getStopReason(it.stopReason)
                        printStatus("${it.state} ${it.progress} $stopReason")
                    }
                }
        } else {
            workManager.getWorkInfoByIdLiveData(id)
                .observe(this) { workInfo ->
                    if (workInfo != null) {
                        val stopReason = getStopReason(workInfo.stopReason)
                        printStatus("${workInfo.state} ${workInfo.progress} $stopReason")
                    } else {
                        println("AAA workInfo is null")
                    }
                }
        }
    }

    private fun getStopReason(stopReason: Int) =
        when (stopReason) {
            WorkInfo.STOP_REASON_NOT_STOPPED -> "STOP_REASON_NOT_STOPPED"
            WorkInfo.STOP_REASON_UNKNOWN -> "STOP_REASON_UNKNOWN"
            WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "STOP_REASON_CANCELLED_BY_APP"
            WorkInfo.STOP_REASON_PREEMPT -> "STOP_REASON_PREEMPT"
            WorkInfo.STOP_REASON_TIMEOUT -> "STOP_REASON_TIMEOUT"
            WorkInfo.STOP_REASON_DEVICE_STATE -> "STOP_REASON_DEVICE_STATE"
            WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> "STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW"
            WorkInfo.STOP_REASON_CONSTRAINT_CHARGING -> "STOP_REASON_CONSTRAINT_CHARGING"
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "STOP_REASON_CONSTRAINT_CONNECTIVITY"
            WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> "STOP_REASON_CONSTRAINT_DEVICE_IDLE"
            WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> "STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW"
            WorkInfo.STOP_REASON_QUOTA -> "STOP_REASON_QUOTA"
            WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION -> "STOP_REASON_BACKGROUND_RESTRICTION"
            WorkInfo.STOP_REASON_APP_STANDBY -> "STOP_REASON_APP_STANDBY"
            WorkInfo.STOP_REASON_USER -> "STOP_REASON_USER"
            WorkInfo.STOP_REASON_SYSTEM_PROCESSING -> "STOP_REASON_SYSTEM_PROCESSING"
            WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED -> "STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED"
            else -> "Unknown"
        }

    private fun printStatus(msg: String) {
        val func = {
            val newText = binding.text.text.toString() + "\n" + msg
            binding.text.text = newText
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            binding.text.post { func() }
        } else {
            func()
        }
    }

    private fun clearStatus() {
        binding.text.text = null
    }
}

var staticLogger: ((String) -> Unit)? = null

fun println(msg: String) {
    staticLogger?.invoke(msg)
}

// ------------- Tasks ----------------

class SimpleOneTimeWorkTask(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        println("Start work")
        if (isStopped) {
            println("Stop processing")
        }
        Thread.sleep(5000)
        println("Start complete")
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
    }
}

class OneTimeWorkTask(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val progress = "Progress"

    override suspend fun doWork(): Result {
        setProgress(workDataOf(progress to 0))
        println("Do heavy work")
        setProgress(workDataOf(progress to 30))
        delay(2000)
        setProgress(workDataOf(progress to 70))
        delay(2000)
        println("Do heavy work Complete")
        setProgress(workDataOf(progress to 100))
        return Result.success()
    }
}

class RetryWorkTask(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        var FAILURE_TIMES = 0
    }

    override fun doWork(): Result {
        return if (FAILURE_TIMES == 0) {
            FAILURE_TIMES++
            Result.retry()
        } else {
            Result.success()
        }
    }
}

class ErrorWorkTask(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        println("Do error work")
        return Result.failure()
    }
}

class ExpeditedWorkTask(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Required for Android < 12
    override suspend fun getForegroundInfo(): ForegroundInfo {

        val builder = NotificationCompat.Builder(
            context,
            "android.playground.workmanager.notification.channel.id"
        )
            .setSmallIcon(R.drawable.ic_work)
            .setContentTitle("Work Manager")
            .setContentText("Doing expedited work")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    1000,
                    Intent(context, WorkManagerActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        val notification = builder.build()
        return ForegroundInfo(0, notification)
    }

    override suspend fun doWork(): Result {
        println("Do heavy work")
        delay(5000)
        println("Do heavy work Complete")
        return Result.success()
    }
}

class PeriodicWorkTask(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        println("Do heavy work")
        delay(5000)
        println("Do heavy work Complete")
        return Result.success()
    }
}

class InputDataWorkTask(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val input = inputData.getString("key")
        println("Do heavy work with $input")
        delay(5000)
        println("Do heavy work Complete with $input")
        return Result.success()
    }
}

class MergingTask(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        delay(5000)
        return Result.success(
            inputData
        )
    }
}

class MergeInputTask(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val inputData = inputData
        println(inputData.toString())
        delay(5000)
        return Result.success()
    }
}

class CrashingTask(
    context: Context,
    params: WorkerParameters,
) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result {
        WorkManagerNativeCrasher().executeNativeCrash()
        return Result.success()
    }
}
