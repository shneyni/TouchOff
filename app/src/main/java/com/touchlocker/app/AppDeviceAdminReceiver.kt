package com.touchlocker.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class AppDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "ביטול הרשאת מנהל המכשיר יאפשר הסרת Touch Locker. להמשיך?"
    }
}
