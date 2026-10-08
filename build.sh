#!/bin/sh
# Gradle olmadan doğrudan Android SDK araçlarıyla APK derler.
set -e
KS_PASS="${KS_PASS:-todo123}"
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-35/android.jar"
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"
KEYTOOL="$JAVA_HOME/bin/keytool"

rm -rf build && mkdir -p build/res build/gen build/classes build/dex

for f in res/*/*.xml; do "$BT/aapt2" compile "$(echo "$f" | sed 's#/#\\#g')" -o build/res/; done
"$BT/aapt2" link -o build/unsigned.apk -I "$JAR" --manifest AndroidManifest.xml \
    --java build/gen build/res/*.flat
"$JAVAC" -encoding UTF-8 --release 8 -classpath "$JAR" \
    -d build/classes $(find src build/gen -name '*.java')
"$JAVA" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 24 \
    --lib "$JAR" --output build/dex $(find build/classes -name '*.class')
cp build/unsigned.apk build/with-dex.apk
(cd build/dex && jar -uf ../with-dex.apk classes.dex)
"$BT/zipalign" -f -p 4 build/with-dex.apk build/aligned.apk

[ -f todo.keystore ] || "$KEYTOOL" -genkeypair -keystore todo.keystore -alias todo \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Akansel, O=Akansel, C=TR"
"$JAVA" -jar "$BT/lib/apksigner.jar" sign --ks todo.keystore --ks-pass "pass:$KS_PASS" \
    --key-pass "pass:$KS_PASS" --out Yapilacaklar.apk build/aligned.apk
"$JAVA" -jar "$BT/lib/apksigner.jar" verify Yapilacaklar.apk && echo "APK hazir: Yapilacaklar.apk"
