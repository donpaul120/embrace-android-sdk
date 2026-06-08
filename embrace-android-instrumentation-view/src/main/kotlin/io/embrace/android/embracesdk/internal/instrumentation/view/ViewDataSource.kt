package io.embrace.android.embracesdk.internal.instrumentation.view

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.embrace.android.embracesdk.internal.arch.InstrumentationArgs
import io.embrace.android.embracesdk.internal.arch.datasource.DataSourceImpl
import io.embrace.android.embracesdk.internal.arch.datasource.SpanToken
import io.embrace.android.embracesdk.internal.arch.limits.UpToLimitStrategy
import io.embrace.android.embracesdk.internal.arch.schema.SchemaType
import io.embrace.android.embracesdk.spans.EmbraceSpan

/**
 * Captures fragment views.
 */
class ViewDataSource(
    private val args: InstrumentationArgs,
) : DataSourceImpl(
    args,
    UpToLimitStrategy { args.configService.breadcrumbBehavior.getFragmentBreadcrumbLimit() },
    "view_data_source"
),
    Application.ActivityLifecycleCallbacks {

    private val application: Application = args.application

    private val viewSpans: LinkedHashMap<String, SpanToken> = LinkedHashMap()

    override fun onDataCaptureEnabled() {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onDataCaptureDisabled() {
        application.unregisterActivityLifecycleCallbacks(this)
    }

    /**
     * Returns the span for the currently active view, or null if no view is active.
     */
    fun getCurrentViewSpan(): EmbraceSpan? =
        synchronized(viewSpans) { viewSpans.values.lastOrNull()?.asEmbraceSpan() }

    /**
     * Called when a view is started. If a view with the same name is already running, it will be ended.
     */
    fun startView(name: String?): Boolean {
        captureTelemetry(inputValidation = { !name.isNullOrEmpty() }) {
            synchronized(viewSpans) {
                // Remove previous entry even if we don't replace it, like if we can't start a new view span because of limits
                viewSpans.remove(name)?.stop()
                startSpanCapture(SchemaType.View(checkNotNull(name)), clock.now(), name = checkNotNull(name)).apply {
                    viewSpans[name] = this
                }
            }
        }
        return true
    }

    /**
     * Called when a view is started, ending the last view.
     */
    fun changeView(name: String?) {
        synchronized(viewSpans) {
            val lastView = viewSpans.keys.lastOrNull()
            endView(lastView)
            startView(name)
        }
    }

    /**
     * Called when a view is ended.
     */
    fun endView(name: String?): Boolean {
        if (name.isNullOrEmpty()) {
            return false
        }
        synchronized(viewSpans) {
            viewSpans.remove(name)?.stop()
        }
        return true
    }

    /**
     * Called when the activity is closed (and therefore all views are assumed to close).
     */
    fun onViewClose() {
        synchronized(viewSpans) {
            viewSpans.forEach { (_, span) ->
                span.stop()
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        changeView(activity.javaClass.name)
    }

    /**
     * Close all open fragments when the activity closes
     */
    override fun onActivityStopped(activity: Activity) {
        onViewClose()
    }

    private val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            changeView(f.javaClass.name)
        }
        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            endView(f.javaClass.name)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        (activity as? FragmentActivity)?.supportFragmentManager
            ?.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }
}
