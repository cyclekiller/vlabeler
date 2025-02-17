package strings

import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import kotlin.test.Test

class StringFormatTest {

    private val maxParamCount = 5

    @Test
    fun test() {
        Language.values().forEach { language ->
            Strings.values().forEach { key ->
                stringCertain(key, language, *Array(maxParamCount) { it })
            }
        }
    }
}
