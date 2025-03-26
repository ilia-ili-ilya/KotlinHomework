interface Sender {
    fun send(message: String): String
}

class Generator(private val templates: MutableMap<String, Map<String, String>>, private val sender: Sender) {
    fun transformString(basicRequest: String, nameOfTemplate: String): String {
        var finalRequest = basicRequest
        while (finalRequest.contains("\${")) {
            var somethingChanged = false
            for ((key, value) in templates[nameOfTemplate]!!) {
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

    fun sendRequest(s: String): String {
        return sender.send(s)
    }

    fun sendAllRequest(basicRequest: String): MutableMap<String, String> {
        val ansOfAllRequests = mutableMapOf<String, String>()
        for (templateName in templates.keys) {
            val textForRequest = transformString(basicRequest, templateName)
            ansOfAllRequests[templateName] = sendRequest(textForRequest)
        }
        return ansOfAllRequests
    }

    fun addNewTemplate(templateName: String, template: Map<String, String>) {
        templates[templateName] = template
    }

    fun deleteTemplate(templateName: String) {
        templates.remove(templateName)
    }
}


class TestSender : Sender {
    override fun send(message: String): String {
        return "ye, nice string"
    }
}

fun testOneTransform(
    generator: Generator,
    basicRequest: String,
    templateName: String,
    expectedValue: String,
    expectedError: Boolean
): Boolean {
    val transformedString: String
    try {
        transformedString = generator.transformString(basicRequest, templateName)
    } catch (e: Exception) {
        return expectedError
    }
    if (expectedError) return false
    return transformedString == expectedValue
}

fun testTransformString(): Boolean {
    val superMap = mutableMapOf(
        "first_test" to mapOf("aa" to "aaa", "bb" to "bbb"),
        "second_test" to mapOf("a" to "aaa", "bb" to "bbb"),
        "third_test" to mapOf("aa" to "\${bb}", "" to "xaxa", "bb" to "c")
    )
    val sender = TestSender()
    val generator = Generator(superMap, sender)

    val basicRequest = "\${aa}\${bb}aabb"
    if (!testOneTransform(generator, basicRequest, "first_test", "aaabbbaabb", false)) {
        return false
    }
    if (!testOneTransform(generator, basicRequest, "second_test", "", true)) {
        return false
    }
    if (!testOneTransform(generator, basicRequest, "third_test", "ccaabb", false)) {
        return false
    }

    return true
}


fun testAddDelTemplates(): Boolean {
    val sender = TestSender()
    val superMap: MutableMap<String, Map<String, String>> = mutableMapOf()
    val generator = Generator(superMap, sender)
    generator.addNewTemplate("name1", mutableMapOf("a" to "b"))
    if (!testOneTransform(generator, "\${a}", "name1", "b", false)) {
        return false
    }
    generator.deleteTemplate("name1")
    if (!testOneTransform(generator, "\${a}", "name1", "", true)) {
        return false
    }
    return true
}

fun runAllTests(): Boolean {
    if (!testTransformString()) return false
    if (!testAddDelTemplates()) return false
    return true
}

fun main() {
    if (runAllTests()) {
        println("all tests passed")
    } else {
        println("something went wrong")
    }
}


