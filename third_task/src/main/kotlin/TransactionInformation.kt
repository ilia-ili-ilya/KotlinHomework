package main

import kotlinx.serialization.Serializable

@Serializable
data class Result(val last_price: Double)

@Serializable
data class Root(val result: Result)