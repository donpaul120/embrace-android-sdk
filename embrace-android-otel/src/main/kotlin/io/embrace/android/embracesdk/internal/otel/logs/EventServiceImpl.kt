package io.embrace.android.embracesdk.internal.otel.logs

import io.embrace.android.embracesdk.internal.otel.impl.EmbAttributesMutator
import io.embrace.android.embracesdk.internal.utils.Provider
import io.embrace.android.embracesdk.internal.utils.Uuid
import io.opentelemetry.kotlin.NoopOpenTelemetry
import io.opentelemetry.kotlin.attributes.AttributesMutator
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.logging.Logger
import io.opentelemetry.kotlin.logging.SeverityNumber
import io.opentelemetry.kotlin.semconv.LogAttributes
import java.util.concurrent.atomic.AtomicReference

class EventServiceImpl(
    private val sdkLoggerProvider: Provider<Logger>,
) : EventService {
    private val noopLogger = NoopOpenTelemetry.loggerProvider.getLogger("noop")
    private val sdkLoggerRef: AtomicReference<Logger> = AtomicReference(noopLogger)
    private val metadataSupplierProviderRef = AtomicReference<Provider<Map<String, String>>> { emptyMap() }
    private val contextProviderRef = AtomicReference<Provider<Context?>> { null }

    override fun initializeService(sdkInitStartTimeMs: Long) {
        sdkLoggerRef.set(sdkLoggerProvider())
    }

    override fun initialized(): Boolean = sdkLoggerRef.get() != noopLogger

    override fun log(
        impl: Logger?,
        eventName: String?,
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        addCurrentMetadata: Boolean,
        eventAttributes: (AttributesMutator.() -> Unit)?,
    ) {
        val logger = impl ?: sdkLoggerRef.get()
        val container = EmbAttributesMutator()
        eventAttributes?.invoke(container)
        if (!container.attributes.containsKey(LogAttributes.LOG_RECORD_UID)) {
            container.setStringAttribute(LogAttributes.LOG_RECORD_UID, Uuid.getEmbUuid())
        }
        if (addCurrentMetadata) {
            getCurrentMetadata().forEach { (k, v) -> container.setStringAttribute(k, v) }
        }

        // Caller-supplied context wins; fall back to the provider so log records carry the active span's trace_id/span_id when callers don't supply one.
        val resolvedContext = context ?: contextProviderRef.get().invoke()
        logger.emit(
            body = body,
            eventName = eventName,
            timestamp = timestamp,
            observedTimestamp = observedTimestamp,
            context = resolvedContext,
            severityNumber = severityNumber,
            severityText = severityText,
            attributes = {
                container.attributes.forEach { (k, v) -> setStringAttribute(k, v.toString()) }
            }
        )
    }

    override fun setMetadataProvider(provider: Provider<Map<String, String>>) {
        metadataSupplierProviderRef.set(provider)
    }

    override fun setContextProvider(provider: Provider<Context?>) {
        contextProviderRef.set(provider)
    }

    private fun getCurrentMetadata(): Map<String, String> = metadataSupplierProviderRef.get().invoke().toMap()
}
