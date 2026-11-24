#!/usr/bin/env bash
# Builds the APK without Gradle: aapt2 -> javac -> d8 -> zip -> zipalign -> apksigner
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-/opt/android-sdk}"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
OUT=build; APK=salah-bahrain.apk
rm -rf $OUT && mkdir -p $OUT/{compiled,classes,dex}

"$BT/aapt2" compile --dir res -o $OUT/compiled
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml -A assets \
  --min-sdk-version 26 --target-sdk-version 34 \
  $OUT/compiled/*.flat -o $OUT/unsigned.apk --java $OUT/gen
javac -nowarn --release 8 -classpath "$JAR" -d $OUT/classes $(find src $OUT/gen -name '*.java') 2>&1 | { grep -v "^Picked up\|warning\|^Note:" || true; }
[ -f $OUT/classes/com/salah/bahrain/MainActivity.class ] || { echo 'COMPILE FAILED'; exit 1; }
"$BT/d8" --release --min-api 26 --lib "$JAR" --output $OUT/dex $(find $OUT/classes -name '*.class')
(cd $OUT/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 $OUT/unsigned.apk $OUT/aligned.apk

KS=salah.keystore
[ -f $KS ] || keytool -genkeypair -keystore $KS -storepass salah123 -keypass salah123 -alias salah \
  -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=Salah Bahrain" 2>/dev/null
"$BT/apksigner" sign --ks $KS --ks-pass pass:salah123 --key-pass pass:salah123 --out $APK $OUT/aligned.apk
"$BT/apksigner" verify --verbose $APK | head -5
ls -l $APK
