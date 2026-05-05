apk_debug := "app/build/outputs/apk/debug/app-debug.apk"

build:
    ./gradlew assembleDebug

build-release:
    ./gradlew assembleRelease

install: build
    adb install {{apk_debug}}
