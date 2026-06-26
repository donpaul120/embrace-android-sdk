package io.embrace.android.embracesdk.internal.otel.logs

import io.embrace.android.embracesdk.internal.Initializable
import io.embrace.android.embracesdk.internal.utils.Provider
import io.opentelemetry.kotlin.attributes.AttributesMutator
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.logging.Logger
import io.opentelemetry.kotlin.logging.SeverityNumber

/**
 * An OTel-agnostic API to create telemetry modeled as OTel LogRecords aka Events
 */
interface EventService : Initializable {
    /**
     * Records an event using the given OTel Logger instance. Defaults to the SDK instance if not provided
     */
    fun log(
        impl: Logger? = null,
        eventName: String?,
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        addCurrentMetadata: Boolean,
        eventAttributes: (AttributesMutator.() -> Unit)?,
    )

    /**
     * Sets a provider that supplies a snapshot of the current metadata that describes the state of the SDK
     */
    fun setMetadataProvider(provider: Provider<Map<String, String>>)

    /**
     * Sets a provider that supplies the active OTel Context to stamp onto log records.
     * Used to populate top-level trace_id/span_id fields so Signoz can link logs to traces.
     */
    fun setContextProvider(provider: Provider<Context?>)

    /**
     * Sets a factory that resolves the OTel Context for a log record based on its attributes.
     * Takes precedence over [setContextProvider] when it returns a non-null value.
     * Use this to look up a specific span by ID (e.g. the active Flutter screen span).
     */
    fun setContextFactory(factory: (Map<String, Any>) -> Context?)
}
