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

fun getPrice(time: Int = -1): Double {
    val jsonParser = Json {
        ignoreUnknownKeys = true
    }
    val url = URL("https://www.deribit.com/api/v2/public/ticker?instrument_name=BTC-PERPETUAL")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val responseCode = connection.responseCode
    val inputStream = connection.inputStream
    val response = inputStream.bufferedReader().use { it.readText() }
    connection.disconnect()
    val result = jsonParser.decodeFromString<Root>(response)
    return result.result.last_price
}

fun approximation(data: List<Double>): List<Double> {
    val aData: MutableList<Double> = mutableListOf()
    val n = data.size
    val y = (0 until n).map { it.toDouble() }
    val (a, b, c) = quadraticApproximation(y, data)

    for (i in 0..5999) {
        val x = i.toDouble() / 100
        aData.add(c + b * x + a * x * x)
    }
    return aData.toList()
}



fun quadraticApproximation(x: List<Double>, y: List<Double>): Triple<Double, Double, Double> {
    val n = x.size
    var sumX = 0.0
    var sumX2 = 0.0
    var sumX3 = 0.0
    var sumX4 = 0.0
    var sumY = 0.0
    var sumXY = 0.0
    var sumX2Y = 0.0

    for (i in 0 until n) {
        val xi = x[i]
        val yi = y[i]
        val xi2 = xi * xi
        val xi3 = xi2 * xi
        val xi4 = xi3 * xi
        sumX += xi
        sumX2 += xi2
        sumX3 += xi3
        sumX4 += xi4
        sumY += yi
        sumXY += xi * yi
        sumX2Y += xi2 * yi
    }

    val A = arrayOf(
        doubleArrayOf(sumX4, sumX3, sumX2),
        doubleArrayOf(sumX3, sumX2, sumX),
        doubleArrayOf(sumX2, sumX, n.toDouble())
    )

    val B = doubleArrayOf(sumX2Y, sumXY, sumY)

    val coeffs = solveLinearSystem(A, B)
    return Triple(coeffs[0], coeffs[1], coeffs[2])
}

fun solveLinearSystem(A: Array<DoubleArray>, B: DoubleArray): DoubleArray {
    val n = B.size
    val augmented = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) A[i][j] else B[i] } }
    for (i in 0 until n) {
        var maxRow = i
        for (k in i + 1 until n) {
            if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                maxRow = k
            }
        }

        val temp = augmented[i]
        augmented[i] = augmented[maxRow]
        augmented[maxRow] = temp

        for (k in i + 1 until n) {
            val factor = augmented[k][i] / augmented[i][i]
            for (j in i until n + 1) {
                augmented[k][j] -= factor * augmented[i][j]
            }
        }
    }

    val result = DoubleArray(n)
    for (i in n - 1 downTo 0) {
        var sum = augmented[i][n]
        for (j in i + 1 until n) {
            sum -= augmented[i][j] * result[j]
        }
        result[i] = sum / augmented[i][i]
    }

    return result
}



fun drawPlot(data: List<Double>, chart: XYChart, chartPanel: JFrame) {
    val aData = approximation(data)
    val xData: MutableList<Double> = mutableListOf()
    val yData: MutableList<Double?> = mutableListOf()
    for (i in 0..5999) {
        xData.add(i.toDouble() / 100)
        if ((i % 100 == 0) and (i / 100 < data.size)) {
            yData.add(data[i/100])
        } else {
            yData.add(null)
        }
    }
    SwingUtilities.invokeLater {
        chart.updateXYSeries("Price", xData, yData, null)
        chart.updateXYSeries("Approximation", xData, aData, null)
        chartPanel.repaint()
    }
}

fun main() {
    val chart: XYChart = XYChartBuilder().width(800).height(600).title("Real-time Data").xAxisTitle("Time").yAxisTitle("Value").build()
    val yData = mutableListOf<Double>()
    yData.add(getPrice())

    val series1 = chart.addSeries("Price", listOf(0.0), yData)
    series1.lineStyle = SeriesLines.NONE
    series1.marker = SeriesMarkers.CIRCLE

    val series2 = chart.addSeries("Approximation", listOf(0.0), yData)
    series2.lineStyle = SeriesLines.SOLID
    series2.marker = SeriesMarkers.NONE

    val wrapper = SwingWrapper(chart)
    val chartPanel = wrapper.displayChart()
    fixedRateTimer("chartUpdater", initialDelay = 0, period = 1000) {

        yData.add(getPrice())

        if (yData.size > 50) {
            yData.removeFirst()
        }

        SwingUtilities.invokeLater {
            drawPlot(yData, chart, chartPanel)
        }
    }
}
