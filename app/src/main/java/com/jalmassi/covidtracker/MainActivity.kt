package com.jalmassi.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

class MainActivity : AppCompatActivity() {
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
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null){
                    Log.w(TAG,"Did not receive valid response body")
                    return
                }
                setupEventListener()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

        })
        //Fetch the state data
        covidService.getStatesData().enqueue(object: Callback<List<CovidData>>{
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null){
                    Log.w(TAG,"Did not receive valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner with state names")
                //TODO: update spinner with state names
            }

        })
    }

    private fun setupEventListener() {
        TODO("Not yet implemented")
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        //create SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        //update radio buttons to select positive cases and max by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        //Display metric for most recent date
        updateInfoForDate(dailyData.last())
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adapter.metric) {
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}