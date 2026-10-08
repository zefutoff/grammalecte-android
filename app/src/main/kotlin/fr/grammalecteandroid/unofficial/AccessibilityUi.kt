package fr.grammalecteandroid.unofficial

import android.os.Build
import android.view.View
import android.widget.TextView

internal fun TextView.markAsAccessibilityHeading() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isAccessibilityHeading = true
    }
}

internal fun TextView.enablePoliteAccessibilityUpdates() {
    accessibilityLiveRegion =
        View.ACCESSIBILITY_LIVE_REGION_POLITE
}
