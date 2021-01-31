package com.jalmassi.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (NationWide)"

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        //Fetch national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive valid response body")
                    return
                }
                setupEventListener()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

        })
        //Fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Did not receive valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state } //map key: state, value: rest of covidData
                Log.i(TAG, "Update spinner with state names")
                //update spinner with state names
                updateSpinnerWithStateData(perStateDailyData.keys)
            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbrList = stateNames.toMutableList()
        stateAbbrList.sort() //sort states alphabetically
        stateAbbrList.add(0, ALL_STATES)
    }

    private fun setupEventListener() {
        //add listener to user scrubbing on the chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
//        TODO: Respond to radio button selected events
//        respond to radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK //only shows a week of data on graph
                R.id.radioButtonMonth -> TimeScale.MONTH //only shows past month
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
//        set chart line colour
        val colourRes = when(metric){
            Metric.POSITIVE -> R.color.colorPositive
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colourRes)
        sparkView.lineColor = colorInt
        tvMetricLabel.setTextColor(colorInt)

        //update graph metric
        adapter.metric = metric
        adapter.notifyDataSetChanged()

//        reset number and date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        //create SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        //update radio buttons to select positive cases and max by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        //Display metric for most recent date
//        updateInfoForDate(dailyData.last())
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}