package com.example.data.remote

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    @param:Json(name = "amount") val amount: Double = 1.0,
    @param:Json(name = "base") val base: String,
    @param:Json(name = "date") val date: String,
    @param:Json(name = "rates") val rates: Map<String, Double> = emptyMap()
)

interface FrankfurterApiService {

    @GET("{date}")
    suspend fun getHistoricalRates(
        @Path("date") date: String, // format YYYY-MM-DD
        @Query("base") base: String,
        @Query("symbols") symbols: String? = null
    ): Response<FrankfurterResponse>

    @GET("latest")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String? = null
    ): Response<FrankfurterResponse>

    companion object {
        private const val BASE_URL = "https://api.frankfurter.dev/v1/"

        fun create(): FrankfurterApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(FrankfurterApiService::class.java)
        }
    }
}
