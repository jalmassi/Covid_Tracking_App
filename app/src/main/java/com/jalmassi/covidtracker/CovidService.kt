package com.jalmassi.covidtracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {
    @GET("us/daily.json")
    fun getNationalData(): Call<List<CovidData>> //Call is a list of covid data from json GET request
    @GET("states/daily.json")
    fun getStatesData(): Call<List<CovidData>>
}