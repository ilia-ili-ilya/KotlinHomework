package main

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.*
import org.knowm.xchart.XYChart
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.lines.SeriesLines
import org.knowm.xchart.style.markers.SeriesMarkers
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer
import org.apache.commons.math3.linear.*
import space.kscience.kmath.operations.DoubleField.pow

const val GRAPH_POINTS_COUNT = 6000
const val GRAPH_POINTS_IN_ONE_SECOND = 100
const val EPSILON = 1e-6

data class QuadraticCoefficients(val a: Double, val b: Double, val c: Double)

fun getPrice(jsonParser: Json): Double {
    val url = URL("https://www.deribit.com/api/v2/public/ticker?instrument_name=BTC-PERPETUAL")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val responseCode = connection.responseCode
    val inputStream = connection.inputStream
    val response = inputStream.bufferedReader().use { it.readText() }
    connection.disconnect()
    val result = jsonParser.decodeFromString<Root>(response)
    return result.result.lastPrice
}

fun approximation(data: List<Double>): List<Double> {
    val n = data.size
    val y = (0 until n).map { it.toDouble() }
    val (a, b, c) = quadraticApproximation(y, data)

    val aData: List<Double> = List(GRAPH_POINTS_COUNT) { i ->
        val x = i.toDouble() / GRAPH_POINTS_IN_ONE_SECOND
        c + b * x + a * x * x
    }
    return aData.toList()
}

fun quadraticApproximation(x: List<Double>, y: List<Double>): QuadraticCoefficients {
    val n = x.size
    val sumX = x.indices.sumOf { i -> x[i] }
    val sumX2 = x.indices.sumOf { i -> x[i].pow(2) }
    val sumX3 = x.indices.sumOf { i -> x[i].pow(3) }
    val sumX4 = x.indices.sumOf { i -> x[i].pow(4) }
    val sumY = y.indices.sumOf { i -> y[i] }
    val sumXY = x.indices.sumOf { i -> x[i] * y[i] }
    val sumX2Y = x.indices.sumOf { i -> x[i].pow(2) * y[i] }

    val coefficients = arrayOf(
        doubleArrayOf(sumX4 + EPSILON, sumX3, sumX2),
        doubleArrayOf(sumX3, sumX2, sumX),
        doubleArrayOf(sumX2, sumX, n.toDouble())
    )
    val constants = doubleArrayOf(sumX2Y, sumXY, sumY)
    val matrix = Array2DRowRealMatrix(coefficients)
    val vector = ArrayRealVector(constants)
    val solution = LUDecomposition(matrix).solver.solve(vector).toArray()

    return QuadraticCoefficients(solution[0], solution[1], solution[2])
}

fun drawPlot(aData: List<Double>, xData: List<Double>, yData: List<Double?>, chart: XYChart, chartPanel: JFrame) {
    SwingUtilities.invokeLater {
        chart.updateXYSeries("Price", xData, yData, null)
        chart.updateXYSeries("Approximation", xData, aData, null)
        chartPanel.repaint()
    }
}

fun main() {
    val jsonParser = Json {
        ignoreUnknownKeys = true
    }
    val chart: XYChart = XYChartBuilder().width(800).height(600).title("Real-time Data").xAxisTitle("Time").yAxisTitle("Value").build()
    val data = mutableListOf<Double>()
    data.add(getPrice(jsonParser))

    val series1 = chart.addSeries("Price", listOf(0.0), data)
    series1.lineStyle = SeriesLines.NONE
    series1.marker = SeriesMarkers.CIRCLE

    val series2 = chart.addSeries("Approximation", listOf(0.0), data)
    series2.lineStyle = SeriesLines.SOLID
    series2.marker = SeriesMarkers.NONE

    val wrapper = SwingWrapper(chart)
    val chartPanel = wrapper.displayChart()
    fixedRateTimer("chartUpdater", initialDelay = 0, period = 1000) {

        data.add(getPrice(jsonParser))

        if (data.size > 50) {
            data.removeFirst()
        }

        val aData = approximation(data)
        val xData: MutableList<Double> = mutableListOf()
        val yData: MutableList<Double?> = mutableListOf()
        for (i in 0 until GRAPH_POINTS_COUNT) {
            xData.add(i.toDouble() / GRAPH_POINTS_IN_ONE_SECOND)
            if ((i % GRAPH_POINTS_IN_ONE_SECOND == 0) and (i / GRAPH_POINTS_IN_ONE_SECOND < data.size)) {
                yData.add(data[i/GRAPH_POINTS_IN_ONE_SECOND])
            } else {
                yData.add(null)
            }
        }
        drawPlot(aData, xData, yData, chart, chartPanel)
    }
}
