package koeln.paffrath.notruftrainer

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object KioskManager {
    private val LOCK_TASK_ALLOWED_PACKAGES = arrayOf(
        "koeln.paffrath.notruftrainer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
    )

    fun applyKioskPolicies(activity: Activity) {
        val context = activity.applicationContext
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        val admin = ComponentName(context, AdminReceiver::class.java)

        dpm.setLockTaskPackages(admin, LOCK_TASK_ALLOWED_PACKAGES)
        dpm.setLockTaskFeatures(
            admin,
            DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO or
                DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
        )

        try {
            dpm.setKeyguardDisabled(admin, false)
        } catch (_: SecurityException) {
        }

        val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("home_set", false)) {
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            val home = ComponentName(context, MainActivity::class.java)
            dpm.addPersistentPreferredActivity(admin, homeFilter, home)
            prefs.edit().putBoolean("home_set", true).apply()
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            activity.startLockTask()
        }
    }

    fun exitKiosk(activity: Activity) {
        val context = activity.applicationContext
        context.stopService(Intent(context, PowerButtonDetectorService::class.java))

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            activity.stopLockTask()
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            val admin = ComponentName(context, AdminReceiver::class.java)
            dpm.clearPackagePersistentPreferredActivities(admin, context.packageName)
            context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)
                .edit().putBoolean("home_set", false).apply()
        }

        launchSystemLauncher(context)
    }

    private fun launchSystemLauncher(context: Context) {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcher = context.packageManager
            .queryIntentActivities(homeIntent, 0)
            .firstOrNull { it.activityInfo.packageName != context.packageName }
            ?: return
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            setClassName(launcher.activityInfo.packageName, launcher.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
