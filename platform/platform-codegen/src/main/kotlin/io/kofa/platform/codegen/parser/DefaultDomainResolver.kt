package io.kofa.platform.codegen.parser

import arrow.atomic.AtomicInt
import io.kofa.platform.codegen.domain.*
import io.kofa.platform.codegen.domain.registry.DomainTypeRegistry
import io.kofa.platform.codegen.domain.type.ArrayFieldTypeWrapper
import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.GeneratedEnumFieldType
import io.kofa.platform.codegen.domain.type.GeneratedFieldType
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.findEnumByName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.findMessageByName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.findTypeByName

class DefaultDomainResolver(
    private val domainProvider: () -> PlainDomain,
    private val existingDomainProvider: () -> PlainDomain?
) {
    companion object {
        const val ARRAY_SUFFIX = "[]"
    }

    val typeRegistry = DomainTypeRegistry()
    val lazyTypes = mutableMapOf<String, Lazy<GeneratedFieldType>>()

    fun resolve(): ResolvedDomain {
        val plainDomain = parseLatest()
        val existingDomain = parseExisting()

        val finalDomain = if (existingDomain != null) {
            checkAndMerge(plainDomain, existingDomain)
        } else {
            plainDomain
        }

        finalDomain.imports.forEach { domain ->
            registerDomainType(domain)
        }

        registerDomainType(finalDomain)

        val resolved = resolveDomain(finalDomain)

        return populateDomain(resolved)
    }

    private fun populateDomain(domain: ResolvedDomain): ResolvedDomain {

        val importedEnums = getImportedEnumNames(domain).map { name -> checkNotNull(domain.findEnumByName(name)) { "missing enum definition for $name" } }
        val importedTypes = getImportedTypeNames(domain).map { name -> checkNotNull(domain.findTypeByName(name)) { "missing type definition for $name" } }
        val importedMessages = getImportedMessageNames(domain).map { name -> checkNotNull(domain.findMessageByName(name)) { "missing message definition for $name" } }

        return domain.copy(
            enums = domain.enums + importedEnums,
            types = domain.types + importedTypes,
            messages = domain.messages + importedMessages
        )
    }

    private fun getImportedMessageNames(domain: ResolvedDomain): List<String> {
        return domain.messages.flatMap { message ->
            message.fields.filter { field -> field.type is GeneratedFieldType && field.type.isMessage  }
                .map { field -> field.type.typeName }.distinct()
        }.distinct() - domain.messages.map { type -> type.name }
    }

    private fun getImportedTypeNames(domain: ResolvedDomain): List<String> {
        return domain.messages.flatMap { message ->
            message.fields.filter { field -> field.type is GeneratedFieldType && !field.type.isMessage }
                .map { field -> field.type.typeName }.distinct()
        }.distinct() + domain.types.flatMap { message ->
            message.fields.filter { field -> field.type is GeneratedFieldType && !field.type.isMessage }
                .map { field -> field.type.typeName }.distinct()
        }.distinct() - domain.types.map { type -> type.name }
    }

    private fun getImportedEnumNames(domain: ResolvedDomain): List<String> {
        return domain.messages.flatMap { message ->
            message.fields.filter { field -> field.type.isEnum && field.type.isGenerated }
                .map { field -> field.type.typeName }.distinct()
        }.distinct() + domain.types.flatMap { message ->
            message.fields.filter { field -> field.type.isEnum && field.type.isGenerated }
                .map { field -> field.type.typeName }.distinct()
        }.distinct() - domain.enums.map { type -> type.name }
    }

    private fun checkAndMerge(masterDomain: PlainDomain, generatedDomain: PlainDomain): PlainDomain {
        // ignore imported domain because it is already flattened
        // for each domain type in masterDomain, check if it exists in generatedDomain by name, if not, add it, if yes, check if field id is the same or absent in masterDomain,
        // and make sure the other property is identical
        val finalTypes = checkAndMergeTypes(masterDomain.types, generatedDomain.types)
        val finalEnums = checkAndMergeEnums(masterDomain.enums, generatedDomain.enums)
        val finalInterfaces = checkAndMergeInterfaces(masterDomain.interfaces, generatedDomain.interfaces)
        val finalMessages = checkAndMergeMessages(masterDomain.messages, generatedDomain.messages)

        return PlainDomain(
            domainName = masterDomain.domainName,
            pkgName = masterDomain.pkgName,
            imports = masterDomain.imports,
            types = finalTypes,
            enums = finalEnums,
            interfaces = finalInterfaces,
            messages = finalMessages
        )
    }

    private fun checkAndMergeInterfaces(
        current: List<DomainInterface<PlainDomainField>>,
        existing: List<DomainInterface<PlainDomainField>>
    ) = checkAndMerge(current, existing, { it.name }) { t1, t2 ->
        checkAndMerge(t1.fields, t2.fields)
    }

    private fun checkAndMergeMessages(
        current: List<DomainMessage<PlainDomainField>>,
        existing: List<DomainMessage<PlainDomainField>>
    ) = checkAndMerge(
        current,
        existing,
        { it.name },
        { t, b -> t },
        { t -> t.id },
        { t, i -> t.copy(id = i) }) { t1, t2 ->
        checkAndMerge(t1.fields, t2.fields)
    }

    private fun checkAndMergeTypes(
        current: List<DomainType<PlainDomainField>>,
        existing: List<DomainType<PlainDomainField>>
    ) = checkAndMerge(
        current, existing, { it.name }) { t1, t2 ->
        checkAndMerge(t1.fields, t2.fields)
    }

    private fun checkAndMergeEnums(
        current: List<DomainType<DomainEnumField>>,
        existing: List<DomainType<DomainEnumField>>
    ) = checkAndMerge(current, existing, { it.name }) { t1, t2 ->
        checkAndMergeEnumFields(t1.fields, t2.fields)
    }

    private fun checkAndMergeEnumFields(current: List<DomainEnumField>, existing: List<DomainEnumField>) =
        checkAndMerge(
            current,
            existing,
            { it.name },
            { type, deprecated -> type.copy(deprecated = deprecated) }) { t1, t2 ->
            check(t1.value == t2.value) { "conflict enum value found for ${t1.name}: ${t1.value} vs ${t2.value}" }
        }

    private fun checkAndMerge(current: List<PlainDomainField>, existing: List<PlainDomainField>) =
        checkAndMerge(
            current,
            existing,
            { it.name },
            { type, deprecated -> type.copy(deprecated = deprecated) },
            { t -> t.id },
            { t, i -> t.copy(id = i) }) { t1, t2 ->
            check(t1.typeName == t2.typeName) { "field type is not allowed to change for ${t1.name}, ${t1.typeName} vs ${t2.typeName}, create a new field instead" }
            check(
                (t1.length ?: 0) >= (t2.length ?: 0)
            ) { "length is not allowed to be more strict for ${t1.name}, ${t1.length} vs ${t2.length}, change to a bigger number" }
        }

    private fun <T> checkAndMerge(
        current: List<T>, existing: List<T>,
        nameProvider: (T) -> String,
        deprecatedConsumer: Function2<T, Boolean, T> = { t: T, b: Boolean -> t },
        idProvider: Function1<T, Int?>? = null,
        idConsumer: Function2<T, Int, T>? = null,
        postAction: Function2<T, T, Unit>? = null,
    ): List<T> {
        val latest = current.map { currentType ->
            val name = nameProvider(currentType)
            val existingType = existing.singleOrNull { nameProvider(it) == name }
            if (existingType != null) {
                // check and set id
                val enriched = if (idProvider != null && idConsumer != null) {
                    val currentId = idProvider.invoke(currentType)
                    val existingId = idProvider.invoke(existingType)

                    val candidateId = currentId ?: existingId
                    check(candidateId == existingId) { "conflict id $currentId vs $existingId found for $name" }

                    candidateId?.let {
                        idConsumer.invoke(currentType, it)
                    } ?: currentType
                } else {
                    currentType
                }

                postAction?.invoke(enriched, existingType)

                enriched
            } else {
                currentType
            }
        }

        val deprecated = existing.filter { existingType ->
            current.none { nameProvider(it) == nameProvider(existingType) }
        }.map { deprecatedConsumer.invoke(it, true) }

        return latest + deprecated
    }

    private fun registerDomainType(domain: PlainDomain) {
        domain.enums.forEach { type ->
            val toRegister = GeneratedEnumFieldType(
                typeName = type.name,
                packageName = domain.pkgName,
                values = type.fields
            )
            typeRegistry.register(toRegister)
        }

        domain.types.forEach { type ->
            registerDomainType(false, domain.pkgName, type.name, type.fields)
        }

        domain.messages.forEach { type ->
            registerDomainType(true, domain.pkgName, type.name, type.fields)
        }

        lazyTypes.forEach { entry -> typeRegistry.register(entry.value.value) }
    }

    private fun registerDomainType(isMessage: Boolean, pkgName: String, name: String, fields: List<PlainDomainField>) {
        val isReadyToRegister = fields.all(this::checkDomainFieldType)

        if (isReadyToRegister) {
            typeRegistry.register(resolveDomainType(isMessage, pkgName, name, fields))
        } else {
            lazyTypes.put(name, lazy { resolveDomainType(isMessage, pkgName, name, fields) })
        }
    }

    private fun resolveDomainType(
        isMessage: Boolean,
        pkgName: String,
        name: String,
        fields: List<PlainDomainField>
    ): GeneratedFieldType {
        val resolvedFields = fields.associateBy({ it.name }) { f -> resolveDomainFieldType(f) }

        return GeneratedFieldType(
            typeName = name,
            packageName = pkgName,
            isEnum = false,
            isComposite = resolvedFields.values.all { it.isComposite },
            isMessage = isMessage,
            fields = resolvedFields
        )
    }

    private fun checkDomainFieldType(plainDomainField: PlainDomainField): Boolean {
        val typeName = plainDomainField.typeName.removeSuffix(ARRAY_SUFFIX)

        return typeRegistry.tryGet(typeName) != null
    }

    private fun resolveDomain(plainDomain: PlainDomain, createIdIfAbsent: Boolean = true): ResolvedDomain {
        val fieldIdCounter = buildMap<String, AtomicInt> {
            putAll(plainDomain.types.associateBy({ type -> type.name }) { type ->
                AtomicInt(type.fields.maxOf { it.id ?: 0 })
            })

            putAll(plainDomain.messages.associateBy({ type -> type.name }) { type ->
                AtomicInt(type.fields.maxOf { it.id ?: 0 })
            })
        }

        val enumValueCounter = plainDomain.enums.associateBy({ type -> type.name }) { type ->
            AtomicInt(type.fields.maxOf { it.value ?: 0 })
        }

        val messageIdCounter = AtomicInt(plainDomain.messages.maxOf { m -> m.id ?: 0 })

        return ResolvedDomain(
            domainName = plainDomain.domainName,
            pkgName = plainDomain.pkgName,
            imports = plainDomain.imports.map { d -> resolveDomain(d) },
            types = plainDomain.types.map { type ->
                DomainType<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map { f ->
                        resolveDomainField(
                            f,
                            fieldIdCounter[type.name]!!,
                            createIdIfAbsent
                        )
                    }
                )
            },
            enums = plainDomain.enums.map { type ->
                DomainType<DomainEnumField>(
                    name = type.name,
                    fields = type.fields.map { f ->
                        DomainEnumField(
                            name = f.name,
                            value = f.value ?: enumValueCounter[type.name]!!.incrementAndGet()
                        )
                    }
                )
            },
            interfaces = plainDomain.interfaces.map { type ->
                DomainInterface<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map { f ->
                        resolveDomainField(
                            f,
                            null,
                            createIdIfAbsent
                        )
                    }
                )
            },
            messages = plainDomain.messages.map { type ->
                DomainMessage<ResolvedDomainField>(
                    id = if (createIdIfAbsent && type.id == null) {
                        messageIdCounter.incrementAndGet()
                    } else {
                        type.id
                    },
                    name = type.name,
                    fields = type.fields.map { f ->
                        resolveDomainField(
                            f,
                            fieldIdCounter[type.name]!!,
                            createIdIfAbsent
                        )
                    }
                )
            },
        )
    }

    private fun resolveDomainField(
        plainDomainField: PlainDomainField,
        counter: AtomicInt?,
        createId: Boolean
    ): ResolvedDomainField {
        return ResolvedDomainField(
            id = if (createId && plainDomainField.id == null && counter != null) {
                counter.incrementAndGet()
            } else {
                plainDomainField.id
            },
            name = plainDomainField.name,
            type = resolveDomainFieldType(plainDomainField),
            deprecated = plainDomainField.deprecated
        )
    }

    private fun resolveDomainFieldType(plainDomainField: PlainDomainField): DomainFieldType {
        val typeName = plainDomainField.typeName.removeSuffix(ARRAY_SUFFIX)

        val type = checkNotNull(
            typeRegistry.tryGet(typeName)
        ) { "no type found from registry by name: $typeName" }

        if (plainDomainField.typeName.endsWith(ARRAY_SUFFIX)) {
            return ArrayFieldTypeWrapper(
                delegateType = type,
                fixedLength = plainDomainField.length
            )
        }

        return type
    }

    private fun parseLatest() = domainProvider.invoke()

    private fun parseExisting() = existingDomainProvider.invoke()
}