package com.jalmassi.covidtracker

import com.robinhood.spark.SparkAdapter

class  CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]
//        return chosenDayData.positiveIncrease.toFloat()
        return when(metric){
            Metric.POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            Metric.NEGATIVE -> chosenDayData.negativeIncrease.toFloat()
            Metric.DEATH -> chosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

}