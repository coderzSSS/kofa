package io.kofa.platform.api.codec

import io.kofa.platform.api.codec.CodecUtils.generateSourceAbbr
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

object CodecUtilsTest : FunSpec({
    test("encodeSourceToInt") {
        //Arrange
        val input = "ex-ate 123_1 3 4 5"
        val abbr = generateSourceAbbr(input)

        //Action
        val output = CodecUtils.encodeSourceAbbrToInt(abbr)
        val decoded = CodecUtils.decodeIntToSource(output)

        //Assert
        decoded shouldBe abbr
    }

})