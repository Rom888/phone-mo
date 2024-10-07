package org.fossify.phone.services

import android.app.KeyguardManager
import android.content.Context
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import org.fossify.phone.activities.CallActivity
import org.fossify.phone.extensions.config
import org.fossify.phone.extensions.isOutgoing
import org.fossify.phone.extensions.powerManager
import org.fossify.phone.helpers.CallManager
import org.fossify.phone.helpers.CallNotificationManager
import org.fossify.phone.helpers.NoCall
import org.fossify.phone.models.Events
import org.greenrobot.eventbus.EventBus

class CallService : InCallService() {
    private val callNotificationManager by lazy { CallNotificationManager(this) }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                callNotificationManager.cancelNotification()
            } else {
                callNotificationManager.setupNotification()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callListener)

        startActivity(CallActivity.getStartIntent(this))
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        val wasPrimaryCall = call == CallManager.getPrimaryCall()
        CallManager.onCallRemoved(call)
        if (CallManager.getPhoneState() == NoCall) {
            CallManager.inCallService = null
            callNotificationManager.cancelNotification()
        } else {
            callNotificationManager.setupNotification()
            if (wasPrimaryCall) {
                startActivity(CallActivity.getStartIntent(this))
            }
        }

        EventBus.getDefault().post(Events.RefreshCallLog)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        if (audioState != null) {
            CallManager.onAudioStateChanged(audioState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callNotificationManager.cancelNotification()
    }
}
