package de.rki.coronawarnapp.util.encoding

import io.kotest.matchers.shouldBe
import okio.internal.commonAsUtf8ToByteArray
import org.junit.jupiter.api.Test
import testhelpers.BaseTest

class Base45ExtensionsTest : BaseTest() {

    @Test
    fun `encode - extension`() {
        "AB".toByteArray().base45() shouldBe "BB8"
        "Hello!!".commonAsUtf8ToByteArray().base45() shouldBe "%69 VD92EX0"
        "base-45".commonAsUtf8ToByteArray().base45() shouldBe "UJCLQE7W581"
    }

    @Test
    fun `decode - extension`() {
        "BB8".decodeBase45() shouldBe "AB".toByteArray()
        "%69 VD92EX0".decodeBase45() shouldBe "Hello!!".toByteArray()
        "UJCLQE7W581".decodeBase45() shouldBe "base-45".toByteArray()
    }
}
