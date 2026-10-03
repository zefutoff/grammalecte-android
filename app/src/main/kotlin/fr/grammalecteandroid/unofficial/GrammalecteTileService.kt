package fr.grammalecteandroid.unofficial

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class GrammalecteTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()

        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.qs_tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        unlockAndRun {
            openImePickerAndCollapse()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openImePickerAndCollapse() {
        val intent =
            Intent(
                this,
                ImePickerActivity::class.java,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
                )

            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
