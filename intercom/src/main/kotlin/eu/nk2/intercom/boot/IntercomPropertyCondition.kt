package eu.nk2.intercom.boot

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

open class IntercomPropertyCondition<T>(
    private val acceptedPropertyNames: Iterable<String>,
    private val propertyToValueMapper: (String) -> T,
    private val acceptedPropertyValues: Iterable<T>
): Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        acceptedPropertyNames.asSequence()
            .map { context.environment.getProperty(it) }
            .filterNotNull()
            .firstOrNull()
            ?.let { propertyToValueMapper(it) } in acceptedPropertyValues
}
