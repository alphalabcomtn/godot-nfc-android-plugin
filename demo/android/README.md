# Adapt manifest.xml

<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />

    <!-- Enable access to OpenXR on Oculus mobile devices, no-op on other Android
    platforms. -->
    <category android:name="com.oculus.intent.category.VR" />
    <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
    <action android:name="android.nfc.action.TAG_DISCOVERED"/>
    <action android:name="android.nfc.action.TECH_DISCOVERED"/>
    <category android:name="android.intent.category.DEFAULT"/>
</intent-filter>