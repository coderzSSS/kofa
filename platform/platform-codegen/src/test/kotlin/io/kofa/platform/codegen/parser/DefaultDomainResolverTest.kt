package io.kofa.platform.codegen.parser

import io.kofa.platform.codegen.domain.DomainEnumField
import io.kofa.platform.codegen.domain.DomainInterface
import io.kofa.platform.codegen.domain.DomainMessage
import io.kofa.platform.codegen.domain.DomainType
import io.kofa.platform.codegen.domain.PlainDomain
import io.kofa.platform.codegen.domain.PlainDomainField
import io.kofa.platform.codegen.parser.xml.XmlDomainParser
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.spyk
import kotlin.test.assertNotNull

class DefaultDomainResolverTest : DescribeSpec({
    val domainResolver = spyk(DefaultDomainResolver({ masterDomain }, { generatedDomain }))

    beforeTest {

    }

    describe("test_resolve") {
        it("should_resolve_success") {
            val result = domainResolver.resolve()

            assertNotNull(result)
        }

        it("integration_test_should_success") {
            val masterDomain = XmlDomainParser().parse("test-master.xml")

            val result = DefaultDomainResolver({ masterDomain }, { null }).resolve()

            assertNotNull(result)
        }
    }

}) {
    companion object {
        val masterDomain = PlainDomain(
            domainName = "TestDomain",
            pkgName = "io.kofa.platform.codegen.parser",
            imports = listOf(),
            types = listOf(
                DomainType<PlainDomainField>(
                    name = "TestType",
                    fields = listOf(
                        PlainDomainField(
                            name = "testField",
                            typeName = "string"
                        )
                    )
                )
            ),
            enums = listOf(
                DomainType<DomainEnumField>(
                    name = "TestEnum",
                    fields = listOf(
                        DomainEnumField(
                            name = "TEST_ENUM_VALUE",
                            value = 1
                        )
                    )
                )
            ),
            interfaces = listOf(
                DomainInterface<PlainDomainField>(
                    name = "TestInterface",
                    fields = listOf(
                        PlainDomainField(
                            name = "testField",
                            typeName = "string"
                        )
                    )
                )
            ),
            messages = listOf(
                DomainMessage<PlainDomainField>(
                    id = 1,
                    name = "TestMessage",
                    fields = listOf(
                        PlainDomainField(
                            name = "testField",
                            typeName = "string"
                        ),
                        PlainDomainField(
                            name = "testField2",
                            typeName = "TestMessage2"
                        )
                    )
                ),
                DomainMessage<PlainDomainField>(
                    name = "TestMessage2",
                    fields = listOf(
                        PlainDomainField(
                            name = "field1",
                            typeName = "TestType"
                        ),
                        PlainDomainField(
                            name = "field2",
                            typeName = "TestEnum"
                        )
                    )
                )
            )
        )

        val generatedDomain = PlainDomain(
            domainName = "TestDomain",
            pkgName = "io.kofa.platform.codegen.parser",
            imports = listOf(),
            types = listOf(
                DomainType<PlainDomainField>(
                    name = "TestType",
                    fields = listOf(
                        PlainDomainField(
                            id = 1,
                            name = "testField",
                            typeName = "string"
                        )
                    )
                )
            ),
            enums = listOf(
                DomainType<DomainEnumField>(
                    name = "TestEnum",
                    fields = listOf(
                        DomainEnumField(
                            name = "TEST_ENUM_VALUE",
                            value = 1
                        )
                    )
                )
            ),
            interfaces = listOf(
                DomainInterface<PlainDomainField>(
                    name = "TestInterface",
                    fields = listOf(
                        PlainDomainField(
                            id = 1,
                            name = "testField",
                            typeName = "string"
                        )
                    )
                )
            ),
            messages = listOf(
                DomainMessage<PlainDomainField>(
                    id = 1,
                    name = "TestMessage",
                    fields = listOf(
                        PlainDomainField(
                            id = 1,
                            name = "testField",
                            typeName = "string"
                        )
                    )
                )
            )
        )
    }
}