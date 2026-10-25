package com.example.data.remote

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFactsProduct(
    @param:Json(name = "product_name") val productName: String? = null,
    @param:Json(name = "brands") val brands: String? = null,
    @param:Json(name = "categories") val categories: String? = null,
    @param:Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @param:Json(name = "image_url") val imageUrl: String? = null,
    @param:Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenFactsResponse(
    @param:Json(name = "status") val status: Int = 0,
    @param:Json(name = "product") val product: OpenFactsProduct? = null
)

interface OpenBarcodeApiService {

    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductFromProductsFacts(@Path("barcode") barcode: String): Response<OpenFactsResponse>

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductFromFoodFacts(@Path("barcode") barcode: String): Response<OpenFactsResponse>

    companion object {
        private const val PRODUCTS_FACTS_BASE = "https://world.openproductsfacts.org/"
        private const val FOOD_FACTS_BASE = "https://world.openfoodfacts.org/"

        fun createProductsFactsService(): OpenBarcodeApiService {
            return Retrofit.Builder()
                .baseUrl(PRODUCTS_FACTS_BASE)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenBarcodeApiService::class.java)
        }

        fun createFoodFactsService(): OpenBarcodeApiService {
            return Retrofit.Builder()
                .baseUrl(FOOD_FACTS_BASE)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenBarcodeApiService::class.java)
        }
    }
}
