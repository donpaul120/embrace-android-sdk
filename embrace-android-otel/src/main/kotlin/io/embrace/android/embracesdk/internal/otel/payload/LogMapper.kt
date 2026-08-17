package io.embrace.android.embracesdk.internal.otel.payload

import io.embrace.android.embracesdk.internal.payload.Attribute
import io.embrace.android.embracesdk.internal.payload.Log
import io.opentelemetry.kotlin.logging.model.ReadableLogRecord

fun ReadableLogRecord.toEmbracePayload(): Log {
    val isSpanContextValid = spanContext.isValid
    // If the caller tagged this log with span_id/trace_id attributes (e.g. EmbraceRemoteLogger
    // passes the active Flutter screen span), promote them to the OTel top-level fields so
    // Signoz auto-links the log to its specific span rather than the session span.
    val attrSpanId = attributes["span_id"]?.toString()
    val attrTraceId = attributes["trace_id"]?.toString()
    return Log(
        traceId = attrTraceId ?: if (isSpanContextValid) spanContext.traceId else null,
        spanId = attrSpanId ?: if (isSpanContextValid) spanContext.spanId else null,
        timeUnixNano = timestamp,
        severityNumber = severityNumber?.ordinal,
        severityText = severityText,
        body = body?.toString(),
        attributes = attributes.map { (key, value) -> Attribute(key, value.toString()) }
    )
}
