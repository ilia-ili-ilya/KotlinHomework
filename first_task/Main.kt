fun Map<String, String>.transformString(basicRequest: String): String {
    var finalRequest = basicRequest
    while (finalRequest.contains("\${")) {
        var somethingChanged = false
        for ((key, value) in this) {
            if (finalRequest.contains("\${$key}") ) {
                finalRequest = finalRequest.replace("\${$key}", value)
                somethingChanged = true
            }
        }
        if (!somethingChanged) {
            throw Exception("Something went wrong")
        }
    }
    return finalRequest
}


fun testOneTransform(
    template: Map<String, String>,
    basicRequest: String,
    expectedValue: String,
    expectedError: Boolean
): Boolean {
    val transformedString: String
    try {
        transformedString = template.transformString(basicRequest)
    } catch (e: Exception) {
        return expectedError
    }
    if (expectedError) return false
    return transformedString == expectedValue
}

fun testTransformString(): Boolean {
    val basicRequest = "\${aa}\${bb}aabb"
    if (!testOneTransform(mapOf("aa" to "aaa", "bb" to "bbb"), basicRequest, "aaabbbaabb", false)) {
        return false
    }
    if (!testOneTransform(mapOf("a" to "aaa", "bb" to "bbb"), basicRequest, "", true)) {
        return false
    }
    if (!testOneTransform(mapOf("aa" to "\${bb}", "" to "xaxa", "bb" to "c"), basicRequest, "ccaabb", false)) {
        return false
    }

    return true
}



fun main() {
    if (testTransformString()) {
        println("all tests passed")
    } else {
        println("something went wrong")
    }
}


