package com.jinxtris.ram.livongofit

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field.FIELD_STEPS
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import com.jinxtris.ram.livongofit.adapter.StepItemAdapter
import com.jinxtris.ram.livongofit.broadcast.ConnectivityReceiver
import com.jinxtris.ram.livongofit.model.StepItem
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


enum class FitActionRequestCode {
    INSERT_AND_READ_DATA
}

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), View.OnClickListener, ConnectivityReceiver.ConnectivityReceiverListener {
    private var totalCount: Int = 0
    private var lastDay = ""
    private var currentDay = ""
    private var stepList: ArrayList<StepItem> = arrayListOf()

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeResources()
    }

    private fun initializeResources() {
        txtMainSorting.setOnClickListener(this)
        checkInternetConnection()
    }

    private fun checkInternetConnection() {
        if (!isConnectedToInternet()) {
            toast(getString(R.string.internet_error))
        } else {
            fitnessSignIn(FitActionRequestCode.INSERT_AND_READ_DATA)
        }
    }

    /**
     * Checks that the user is signed in, and if so, executes the specified function. If the user is
     * not signed in, initiates the sign in flow, specifying the post-sign in function to execute.
     *
     * @param requestCode The request code corresponding to the action to perform after sign in.
     */
    private fun fitnessSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            insertAndReadData()
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                    this,
                    requestCode.ordinal,
                    getGoogleAccount(), fitnessOptions
                )
            }
        }
    }

    /**
     * Handles the callback from the OAuth sign in flow, executing the post sign in function
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                insertAndReadData()
            }
            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        toast(message)
    }

    private fun oAuthPermissionsApproved() =
        GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    /**
     * Gets a Google account for use in creating the Fitness client. This is achieved by either
     * using the last signed-in account, or if necessary, prompting the user to sign in.
     * `getAccountForExtension` is recommended over `getLastSignedInAccount` as the latter can
     * return `null` if there has been no sign in before.
     */
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    /**
     * Inserts and reads data by chaining {@link Task} from {@link #insertData()} and {@link
     * #readHistoryData()}.
     */
    private fun insertAndReadData() = insertData().continueWith { readHistoryData() }

    /** Creates a {@link DataSet} and inserts it into user's Google Fit history. */
    private fun insertData(): Task<Void> {
        // Create a new dataset and insertion request.
        val dataSet = insertFitnessData()

        return Fitness.getHistoryClient(this, getGoogleAccount())
            .insertData(dataSet)
            .addOnSuccessListener { }
            .addOnFailureListener {
            }
    }

    /**
     * Asynchronous task to read the history data. When the task succeeds, it will print out the
     * data.
     */
    private fun readHistoryData(): Task<DataReadResponse> {
        // Begin by creating the query.
        val readRequest = queryFitnessData()

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this, getGoogleAccount())
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                fetchData(dataReadResponse)
            }
            .addOnFailureListener { e ->
                toast("There was a problem reading the data.")
            }
    }

    /**
     * Creates and returns a {@link DataSet} of step count data for insertion using the History API.
     */
    private fun insertFitnessData(): DataSet {
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = calendar.timeInMillis

        // Create a data source
        val dataSource = DataSource.Builder()
            .setAppPackageName(this)
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setStreamName("$TAG - step count")
            .setType(DataSource.TYPE_RAW)
            .build()

        // Create a data set
        val stepCountDelta = 950
        return DataSet.builder(dataSource)
            .add(
                DataPoint.builder(dataSource)
                    .setField(FIELD_STEPS, stepCountDelta)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
            ).build()
    }

    /** Returns a [DataReadRequest] for all step count changes in the past week.  */
    private fun queryFitnessData(): DataReadRequest {
        // Setting a start and end date using a range of 2 week before this moment.
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -2)
        val startTime = calendar.timeInMillis

        return DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.HOURS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Logs a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would
     * dump all the data. In this sample, logging also prints to the device screen, so we can see
     * what the query returns, but your app should not log fitness information as a privacy
     * consideration. A better option would be to dump the data you receive to a local data
     * directory to avoid exposing it to other applications.
     */
    private fun fetchData(dataReadResult: DataReadResponse) {
        stepList.clear()
        totalCount = 0

        if (dataReadResult.buckets.isNotEmpty()) {
            for (bucket in dataReadResult.buckets) {
                bucket.dataSets.forEach { parseDataSet(it) }
            }
        } else if (dataReadResult.dataSets.isNotEmpty()) {
            dataReadResult.dataSets.forEach { parseDataSet(it) }
        }

        addDataInList(lastDay, totalCount)

        if (stepList.isNotEmpty()) {
            setAdapter()
        }
    }

    private fun parseDataSet(dataSet: DataSet) {
        for (dp in dataSet.dataPoints) {
            currentDay = dp.getStartDayString()

            dp.dataType.fields.forEach { _ ->
                if (!lastDay.isBlank() && currentDay != lastDay) {
                    addDataInList(lastDay, totalCount)
                    totalCount = 0
                }

                lastDay = currentDay
                totalCount += if (dataSet.isEmpty) 0 else dataSet.dataPoints[0].getValue(FIELD_STEPS).asInt()
            }
        }

    }

    private fun addDataInList(strDay: String, intCount: Int) {
        val stepObject = StepItem()
        stepObject.strDate = strDay
        stepObject.totalCount = intCount
        stepList.add(stepObject)
    }

    private fun setAdapter() {
        stepList.reverse()
        var adapter: StepItemAdapter = StepItemAdapter(stepList)

        if (!adapter.hasObservers()) {
            adapter.setHasStableIds(true)
        }

        rvMain.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.txtMainSorting -> {
                if (stepList.isNotEmpty()) {
                    setAdapter()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppContext.getInstance().setConnectivityListener(this)
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            toast(getString(R.string.internet_error))
        }
    }
}