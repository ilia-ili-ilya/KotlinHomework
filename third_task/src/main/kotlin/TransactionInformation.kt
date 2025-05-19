package main

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Result(
    @SerialName("last_price") val lastPrice: Double
)

@Serializable
data class Root(val result: Result)