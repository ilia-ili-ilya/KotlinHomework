import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface RequestSender {
    fun send(message: String): String
}

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

@OptIn(ExperimentalUuidApi::class)
fun RequestSender.sendAll(templates: Map<Uuid, Map<String, String>>, basicRequest: String): Map<Uuid, String> {
    val answer = mutableMapOf<Uuid, String>()
    for ((key, template) in templates) {
        val finalRequest: String
        try {
            finalRequest = template.transformString(basicRequest)
        } catch (e: Exception) {
            throw Exception("Test $key is incorrect", e)
        }
        answer[key] = this.send(finalRequest)
    }
    return answer
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


