package com.android.s22present.xposed;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class KeyDebounceHook extends XposedModule {

    private static final String TAG = "S22Present.DebounceHook";
    private static final ConcurrentHashMap<Integer, Long> lastDownTime = new ConcurrentHashMap<>();
    private static volatile long debounceWindowMs = 100;

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        try {
            SharedPreferences prefs = getRemotePreferences("s22present_module");
            debounceWindowMs = prefs.getInt("debounce_ms", 100);
            prefs.registerOnSharedPreferenceChangeListener((p, key) -> {
                if ("debounce_ms".equals(key)) {
                    debounceWindowMs = p.getInt(key, 100);
                    Log.i(TAG, "Debounce window updated: " + debounceWindowMs + "ms");
                }
            });
            Log.i(TAG, "Loaded debounce setting: " + debounceWindowMs + "ms");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load remote preferences, using default " + debounceWindowMs + "ms", t);
        }

        try {
            ClassLoader cl = param.getClassLoader();
            Class<?> pwmClass = Class.forName(
                "com.android.server.policy.PhoneWindowManager", false, cl);
            Method method = pwmClass.getDeclaredMethod(
                "interceptKeyBeforeQueueing", KeyEvent.class, int.class);
            hook(method).intercept(new InterceptKeyHooker());
            Log.i(TAG, "Hooked PhoneWindowManager.interceptKeyBeforeQueueing");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to hook interceptKeyBeforeQueueing", t);
        }
    }

    private static class InterceptKeyHooker implements XposedInterface.Hooker {
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            if (debounceWindowMs <= 0) {
                return chain.proceed();
            }

            KeyEvent event = (KeyEvent) chain.getArg(0);

            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return chain.proceed();
            }

            int keyCode = event.getKeyCode();
            long eventTime = event.getEventTime();

            Long lastTime = lastDownTime.get(keyCode);
            if (lastTime != null && (eventTime - lastTime) < debounceWindowMs) {
                return 0;
            }

            lastDownTime.put(keyCode, eventTime);
            return chain.proceed();
        }
    }
}
