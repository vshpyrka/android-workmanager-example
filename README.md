# android-workmanager-example

Set of exaples of `WorkManager` APIs

https://github.com/vshpyrka/android-workmanager-example/assets/2741602/2a4bf2ef-d2d2-4f0f-9aab-ec6206b146da

| Example  | Description |
| ------------- | ------------- |
| runSimpleWork | Runs Simple `OneTimeWorkReuest` using `OneTimeWorkRequestBuilder` class |
| runRetryWork | Runs work task which fail first with `Result.retry()` and is sent back to the queue for retry execution |
| runErrorWork | Runs work task which returns `Result.failure()` |
| runExpeditedWork | Runs important expedited work task using `OneTimeWorkRequestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` |
| runPeriodicTask | Runs periodic work task with specified repeat interval |
| runPeriodicIntervalTask | Runs periodic work task with specified repeat interval and flex period |
| runConstrainedTask | Runs periodic work task with specified system constraints, like required network type, charging required, that should be met to run the job |
| runDelayedTask | Runs work task with initial delay set with duration using `OneTimeWorkRequestBuilder.setInitialDelay` |
| runBackoffTask | Runs backoff work task with linear(i.e. 10m, 20m, 30m, 40m) or exponential(i.e. 10m, 20m, 40m, 80m) backoff criteria using `OneTimeWorkRequestBuilder.setBackoffCriteria` |
| runTaggedTask | Runs work task with specified tag using `OneTimeWorkRequestBuilder.addTag` method, that can be used to group certain tasks or cancel them by tag |
| runInputTask | Runs work task with work input data set using `OneTimeWorkRequestBuilder.setInputData` method |
| runUniqueTask | Runs one time work task with specified work name that is used to enqueue unique work request using `WorkManager.enqueueUniqueWork` method |
| runUniquePeriodicTask | Runs periodic work task with specified work name that is used to enqueue unique work request using `WorkManager.enqueueUniqueWork` method |
| observerTask | Provides example of how to observe enqueued task status information using `WorkManager.getWorkInfoByIdLiveData` method |
| complexWorkQuery | Provides example of how write complext query to retrieve information about work tasks that conform requested query settings using `WorkQuery.Builder` class |
| enqueueWork | Runs sequence of work requests using `WorkManager.beginWith` and `WorkManager.then` methods |
| overwritingInputMergerWork | Runs multiple work requests which return set of data that is used as an input for the next enqueued task. Duplicated input data is merged to a set of unique elements using `OneTimeWorkRequestBuilder..setInputMerger(OverwritingInputMerger::class)` |
| arrayCreatingInputMergerWork | Runs multiple work requests which return set of data that is used as an input for the next enqueued task. Duplicated input data is merged to a list of values with the same key using `OneTimeWorkRequestBuilder..setInputMerger(ArrayCreatingInputMerger::class)` |
| updateWork | Provides example of how to update existing work request with different parameters and constraints using `WorkManager.updateWork` method |
| workGeneration | Provides example of how to update existing work request and see the generation version of updated work request. Generation is retrieved from `WorkInfo` instance |
| separateWorkRequest | Provides example of how to use separate `:process` to run work tasks and shows example how to make a native c++ crash inside of the work task and consequently the main app process doesn't crash |
