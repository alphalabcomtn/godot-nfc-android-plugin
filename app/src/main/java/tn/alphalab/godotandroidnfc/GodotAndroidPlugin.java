package tn.alphalab.godotandroidnfc;

import static android.content.Context.ACTIVITY_SERVICE;
import static tn.alphalab.godotandroidnfc.Constants.ALIAS_DEFAULT_FF;
import static tn.alphalab.godotandroidnfc.Constants.ALIAS_KEY_2KTDES;
import static tn.alphalab.godotandroidnfc.Constants.ALIAS_KEY_2KTDES_ULC;
import static tn.alphalab.godotandroidnfc.Constants.ALIAS_KEY_AES128;
import static tn.alphalab.godotandroidnfc.Constants.ALIAS_KEY_AES128_ZEROES;
import static tn.alphalab.godotandroidnfc.Constants.EXTRA_KEYS_STORED_FLAG;
import static tn.alphalab.godotandroidnfc.Constants.KEY_APP_MASTER;
import static tn.alphalab.godotandroidnfc.Constants.bytesKey;
import static tn.alphalab.godotandroidnfc.Constants.cipher;
import static tn.alphalab.godotandroidnfc.Constants.default_ff_key;
import static tn.alphalab.godotandroidnfc.Constants.default_zeroes_key;
import static tn.alphalab.godotandroidnfc.Constants.iv;
import static tn.alphalab.godotandroidnfc.Constants.objKEY_2KTDES;
import static tn.alphalab.godotandroidnfc.Constants.objKEY_2KTDES_ULC;
import static tn.alphalab.godotandroidnfc.Constants.objKEY_AES128;
import static tn.alphalab.godotandroidnfc.Constants.packageKey;
import com.nxp.nfclib.desfire.DESFireFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nxp.nfclib.CardType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.exceptions.NxpNfcLibException;
import com.nxp.nfclib.ultralight.UltralightFactory;
import com.nxp.nfclib.utils.NxpLogUtils;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

public class GodotAndroidPlugin extends GodotPlugin {
    private NxpNfcLib libInstance = null;
    public final Activity activity; // The main activity of the game
    private Intent previousIntent = null;
    private byte[] queuedWriteData = null;
    CardLogic mCardLogic;

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();

        signals.add(new SignalInfo("send_data_to_godot", String.class));

        return signals;
    }

    public GodotAndroidPlugin(Godot godot) {
        super(godot);
        this.activity = getActivity();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "GodotAndroidPlugin";
    }

    @UsedByGodot
    public int sum(int a, int b) {
        Log.i("alpha", "Summing numbers");
        return a + b;
    }


    @UsedByGodot
    public void init() {
        mCardLogic = CardLogic.getInstance();
        Log.i("alpha", "init()");
        initializeLibrary();
        initializeKeys();
        initializeCipherinitVector();

        if (libInstance != null) {
            libInstance.startForeGroundDispatch();
        }
    }


    private void initializeLibrary() {
        libInstance = NxpNfcLib.getInstance();
        try {
            libInstance.registerActivity(activity, packageKey);
        } catch (NxpNfcLibException ex) {
            Log.i("godot", ex.getMessage());
        } catch (Exception e) {
            // handle the exception
        }
    }

    private void initializeKeys() {
        KeyInfoProvider infoProvider = KeyInfoProvider.getInstance(activity.getApplicationContext());

        SharedPreferences sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE);
        boolean keysStoredFlag = sharedPrefs.getBoolean(EXTRA_KEYS_STORED_FLAG, false);
        if (!keysStoredFlag) {
            byte[] ulc24Keys = new byte[24];
            System.arraycopy(SampleAppKeys.KEY_2KTDES_ULC, 0, ulc24Keys, 0, SampleAppKeys.KEY_2KTDES_ULC.length);
            System.arraycopy(SampleAppKeys.KEY_2KTDES_ULC, 0, ulc24Keys, SampleAppKeys.KEY_2KTDES_ULC.length, 8);
            infoProvider.setKey(ALIAS_KEY_2KTDES_ULC, SampleAppKeys.EnumKeyType.EnumDESKey, ulc24Keys);
            infoProvider.setKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey, SampleAppKeys.KEY_2KTDES);
            infoProvider.setKey(ALIAS_KEY_AES128, SampleAppKeys.EnumKeyType.EnumAESKey, SampleAppKeys.KEY_AES128);
            infoProvider.setKey(ALIAS_KEY_AES128_ZEROES, SampleAppKeys.EnumKeyType.EnumAESKey, SampleAppKeys.KEY_AES128_ZEROS);
            infoProvider.setKey(ALIAS_DEFAULT_FF, SampleAppKeys.EnumKeyType.EnumMifareKey, SampleAppKeys.KEY_DEFAULT_FF);

            sharedPrefs.edit().putBoolean(EXTRA_KEYS_STORED_FLAG, true).apply();
        }
        try {
            objKEY_2KTDES_ULC = infoProvider.getKey(ALIAS_KEY_2KTDES_ULC, SampleAppKeys.EnumKeyType.EnumDESKey);
            objKEY_2KTDES = infoProvider.getKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey);
            objKEY_AES128 = infoProvider.getKey(ALIAS_KEY_AES128, SampleAppKeys.EnumKeyType.EnumAESKey);
            default_zeroes_key = infoProvider.getKey(ALIAS_KEY_AES128_ZEROES, SampleAppKeys.EnumKeyType.EnumAESKey);
            default_ff_key = infoProvider.getMifareKey(ALIAS_DEFAULT_FF);
        } catch (Exception e) {
            ((ActivityManager) Objects.requireNonNull(GodotAndroidPlugin.this.activity.getSystemService(ACTIVITY_SERVICE)))
                    .clearApplicationUserData();
        }
    }

    private void initializeCipherinitVector() {
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        bytesKey = KEY_APP_MASTER.getBytes();

        byte[] ivSpec = new byte[16];
        Arrays.fill(ivSpec, (byte) 0xCD);
        iv = new IvParameterSpec(ivSpec);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @UsedByGodot
    public void pollTags() {
        Intent intent = Godot.getCurrentIntent();
        if (intent == previousIntent)
            return;

        Log.i("alpha", "new intent detected");
        previousIntent = intent;

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            processNfcIntent(intent);
        }
    }

    //This API identifies the card type and calls the specific operations
    private void processNfcIntent(final Intent intent) {
        String cardData = "";
        final Bundle extras = intent.getExtras();
        Object tagName = Objects.requireNonNull(extras).get("android.nfc.extra.TAG");
        Log.i("alpha", tagName.toString());
        CardType type = libInstance.getCardType(intent); //Get the type of the card
        Log.e("ALPHA: cardLogic", "cardLogic: " + type.getTagName());
        if (type == CardType.UnknownCard) {
            Log.e("ALPHA: cardLogic", "UnknownCard");
        }
        Log.e("ALPHA: cardLogic", "Known Card");
        switch (type) {
            case UltralightEV1_11:
            case UltralightEV1_21:
                Log.e("ALPHA: cardLogic", "UltralightEV1_11/UltralightEV1_12");
                cardData = mCardLogic.ultralightEV1CardLogic(this.activity, UltralightFactory.getInstance().getUltralightEV1(libInstance.getCustomModules()));
                break;
            case UltralightC:
                Log.e("ALPHA: cardLogic", "UltralightC");
                cardData = mCardLogic.ultralightcCardLogic(this.activity, UltralightFactory.getInstance().getUltralightC(libInstance.getCustomModules()));
                Log.e("ALPHA: cardLogic", cardData);
                break;
            case DESFireEV2:
                Log.e("ALPHA: cardLogic", "DESFireEV2");
                cardData = mCardLogic.desfireEV2CardLogic(this.activity, DESFireFactory.getInstance().getDESFireEV2(libInstance.getCustomModules()));
                break;
            case DESFireEV3:
                Log.e("ALPHA: cardLogic", "DESFireEV2");
                cardData = mCardLogic.desfireEV2CardLogic(this.activity, DESFireFactory.getInstance().getDESFireEV3(libInstance.getCustomModules()));
                break;
        }
//        To save the logs to file \sdcard\NxpLogDump\logdump.xml
        NxpLogUtils.save();
        emitSignal("send_data_to_godot", cardData);
    }

    @UsedByGodot
    public boolean isNfcSupported() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        return (nfcAdapter != null);
    }

}
