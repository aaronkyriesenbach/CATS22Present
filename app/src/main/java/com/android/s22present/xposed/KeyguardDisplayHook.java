package com.android.s22present.xposed;

import android.util.Log;
import android.view.Display;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class KeyguardDisplayHook extends XposedModule {

    private static final String TAG = "S22Present.XposedHook";

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!"com.android.systemui".equals(param.getPackageName())) {
            return;
        }

        try {
            Class<?> kdmClass = Class.forName(
                "com.android.keyguard.KeyguardDisplayManager",
                false,
                param.getDefaultClassLoader()
            );
            Method showPresentation = kdmClass.getDeclaredMethod("showPresentation", Display.class);
            hook(showPresentation).intercept(new ShowPresentationHooker());
            log(Log.INFO, TAG, "Hooked KeyguardDisplayManager.showPresentation");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook KeyguardDisplayManager.showPresentation", t);
        }
    }

    private static class ShowPresentationHooker implements XposedInterface.Hooker {
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            Object arg = chain.getArg(0);
            if (arg instanceof Display) {
                Display display = (Display) arg;
                if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                    return false;
                }
            }
            return chain.proceed();
        }
    }
}
