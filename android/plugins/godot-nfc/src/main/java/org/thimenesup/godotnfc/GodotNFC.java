package org.thimenesup.godotnfc;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import android.content.Intent;
import android.app.PendingIntent;

import android.nfc.NfcAdapter;

import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.os.Parcelable;

public class GodotNFC extends GodotPlugin {

    private NfcAdapter nfcAdapter = null;
    private int status = 0;

    public GodotNFC(Godot godot) {
        super(godot);
    }

    @Override
    public String getPluginName() {
        return "GodotNFC";
    }

    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                "enableNFC",
                "getStatus",
                "pollTags"
        );
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();

        signals.add(new SignalInfo("nfc_enabled", Integer.class));
        signals.add(new SignalInfo("tag_readed", byte[].class));

        return signals;
    }

    public void enableNFC() {
        getActivity().runOnUiThread(new Runnable() {
            public void run()
            {
                nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter == null) {
                    status = -1;
                }

                if (nfcAdapter.isEnabled()) {
                    status = 1;

                    Intent intent = new Intent(getActivity(), getActivity().getClass());
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);
                    //IntentFilter[] intentFilter = new IntentFilter[]{};

                    nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, null, null);
                }
                else {
                    status = 0;
                }

                emitSignal("nfc_enabled", status);
            }
        });
    }

    public int getStatus() {
        return status;
    }

    public void pollTags() {
        Intent intent = Godot.getCurrentIntent();
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (parcelables == null)
                return;

            for (int i = 0; i < parcelables.length; ++i) {
                NdefMessage ndefMessage = (NdefMessage)parcelables[i];
                NdefRecord[] ndefRecords = ndefMessage.getRecords();
                if (ndefRecords == null)
                    continue;

                for (int j = 0; j < ndefRecords.length; ++j) {
                    NdefRecord ndefRecord = ndefRecords[j];
                    byte[] payload = ndefRecord.getPayload();

                    emitSignal("tag_readed", payload);
                }
            }
        }
    }

}
