# DisableAlarmPerIntentXposed 1.0.0
Adds functionality to disable all `com.android.deskclock` alarms using an Xposed hook.

## How to use

1. Install Xposed hook.
2. Enable/whitelist hook for `com.android.deskclock`.
3. Send the `android.intent.action.DISMISS_ALARM` intent to the `com.android.deskclock` with the extra `DISABLE_ALL_ACTIVE_ALARMS` set to `true``

Everything else will be ignored!