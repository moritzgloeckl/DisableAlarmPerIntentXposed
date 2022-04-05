package me.gloeckl.disablealarmperintentxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DisableAlarmPerIntent implements IXposedHookLoadPackage {

    private static final String TAG = "DisableAlarmPerIntent";

    private static final String DISABLE_ALL_ACTIVE_ALARMS_INTENT_EXTRA = "DISABLE_ALL_ACTIVE_ALARMS";

    private static final String ALARM_CLASS = "com.android.deskclock.provider.Alarm";
    private static final String ALARM_CLASS_GET_ALARMS = "getAlarms";
    private static final String ALARM_CLASS_UPDATE_ALARM = "updateAlarm";

    private static final String ALARM_STATE_MANAGER_CLASS = "com.android.deskclock.alarms.AlarmStateManager";
    private static final String ALARM_STATE_MANAGER_CLASS_DELETE_ALL_INSTANCES = "deleteAllInstances";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod("com.android.deskclock.HandleApiCalls", loadPackageParam.classLoader, "handleDismissAlarm", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Intent intent = (Intent) param.args[0];
                    if (!intent.hasExtra(DISABLE_ALL_ACTIVE_ALARMS_INTENT_EXTRA)) {
                        Log.v(TAG, "Ignoring call, it is probably meant for the original app.");
                        return;
                    }

                    boolean disableAllAlarms = intent.getBooleanExtra(DISABLE_ALL_ACTIVE_ALARMS_INTENT_EXTRA, false);
                    Log.v(TAG, "DISABLE_ALL_ACTIVE_ALARMS_INTENT_EXTRA: " + String.valueOf(disableAllAlarms));

                    if (!disableAllAlarms) {
                        Log.v(TAG, "Nothing to do here...");
                        return;
                    }

                    param.setResult(true);

                    Context context = (Context) AndroidAppHelper.currentApplication();

                    List<Object> allEnabledAlarms = (List<Object>)
                            XposedHelpers.callStaticMethod(XposedHelpers.findClass(ALARM_CLASS, loadPackageParam.classLoader),
                                    ALARM_CLASS_GET_ALARMS,
                                    context.getContentResolver(),
                                    "enabled=?",
                                    new String[]{"1"});

                    Log.v(TAG, "Found " + String.valueOf(allEnabledAlarms.size()) + " enabled alarm(s).");

                    if (allEnabledAlarms.size() == 0) {
                        Log.v(TAG, "No enabled alarms found.");
                        return;
                    }

                    for (Object enabledAlarm : allEnabledAlarms) {
                        XposedHelpers.setBooleanField(enabledAlarm, "enabled", false);
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass(ALARM_CLASS, loadPackageParam.classLoader),
                                ALARM_CLASS_UPDATE_ALARM,
                                context.getContentResolver(),
                                enabledAlarm);

                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass(ALARM_STATE_MANAGER_CLASS, loadPackageParam.classLoader),
                                ALARM_STATE_MANAGER_CLASS_DELETE_ALL_INSTANCES,
                                context,
                                XposedHelpers.getLongField(enabledAlarm, "id")
                        );
                    }


                    Toast.makeText(context, "Disabled " + String.valueOf(allEnabledAlarms.size()) + " alarm(s).", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Throwable t) {
            Log.v(TAG, t.getMessage());
            XposedBridge.log(t);
        }
    }
}
