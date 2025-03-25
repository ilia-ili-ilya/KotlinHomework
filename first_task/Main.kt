
interface Sender {
    fun send(message: String) : String
}

class Generator (private val allMaps : Array<MutableMap<String, String>>, private val sender: Sender) {

    fun transformString (s : String, numOfMap : Int) : String {
        val ans = StringBuilder()

        val replacementPart = StringBuilder()
        var isReplaceOpen = false
        var isDollar = false
        for (c in s) {
            if (isReplaceOpen) {
                if (c == '}') {
                    ans.append(allMaps[numOfMap].getValue(replacementPart.toString()))
                    isReplaceOpen = false
                    replacementPart.clear()
                } else {
                    replacementPart.append(c)
                }
            } else {
                when (c) {
                    '$' -> isDollar = true
                    '{' -> {
                        isReplaceOpen = isDollar
                        isDollar = false
                    }
                    else -> {
                        ans.append(c)
                        isDollar = false
                    }
                }
            }
        }
        if (isReplaceOpen) {throw Exception("автор $s дурень, она не закрыта")}
        return ans.toString()
    }

    fun sendRequest (s: String) : String {
        return sender.send(s)
    }

    fun sendAllRequest(s: String) : MutableList<String> {
        val ans = mutableListOf<String>()
        for (index in allMaps.indices) {
            val tString = transformString(s, index)
            ans.add(sendRequest(tString))
        }
        return ans
    }
}


class kekSender : Sender {
    override fun send(message: String) : String {
        return "ye, nice string"
    }
}

fun testSendRequest() : Boolean {
    val superMap : Array<MutableMap<String, String>> = arrayOf(mutableMapOf(), mutableMapOf(), mutableMapOf())
    superMap[0]["aa"] = "aaa"
    superMap[0]["bb"] = "bbb"
    superMap[1]["a"] = "aaa"
    superMap[1]["bb"] = "bbb"
    superMap[2]["aa"] = "\${bb}"
    superMap[2][""] = "xaxa"
    superMap[2]["bb"] = "c"
    val t = kekSender()
    val g = Generator(superMap, t)
    var s : String
    try {
        s = g.transformString("\${aa}\${bb}aabb", 0)
    } catch (e: Exception) {
        return false
    }
    if (s != "aaabbbaabb") {
        return false
    }
    var findError = false
    try {
        println(g.transformString("\${aa}\${bb}aabb", 1))
    } catch (e: Exception) {
        findError = true
    }
    if (!findError) {
        return false
    }
    try {
        s = g.transformString("\${aa}\${bb}aabb", 2)
    } catch (e: Exception) {
        return false
    }
    return (s == "\${bb}caabb")
}



fun main() {
    if (testSendRequest()) {
        println("all tests passed")
    } else {
        println("something went wrong")
    }
}


