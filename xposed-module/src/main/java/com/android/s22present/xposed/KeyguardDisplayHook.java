package com.android.s22present.xposed;

import android.view.Display;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class KeyguardDisplayHook extends XposedModule {

    private static final String TAG = "S22Present.XposedHook";

    public KeyguardDisplayHook(XposedInterface base, XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
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
            hook(showPresentation, ShowPresentationHooker.class);
            log("Hooked KeyguardDisplayManager.showPresentation");
        } catch (Throwable t) {
            log("Failed to hook KeyguardDisplayManager.showPresentation", t);
        }
    }

    public static class ShowPresentationHooker implements XposedInterface.Hooker {
        public static void before(XposedInterface.BeforeHookCallback callback) {
            Object[] args = callback.getArgs();
            if (args != null && args.length > 0 && args[0] instanceof Display) {
                Display display = (Display) args[0];
                if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                    callback.returnAndSkip(false);
                }
            }
        }
    }
}
