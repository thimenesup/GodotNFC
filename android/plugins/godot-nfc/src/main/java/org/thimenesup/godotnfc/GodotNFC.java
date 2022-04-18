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
import android.nfc.Tag;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.TagLostException;
import android.nfc.FormatException;

import android.os.Parcelable;

import java.io.IOException;

public class GodotNFC extends GodotPlugin {

    private Intent previousIntent = null;

    private NfcAdapter nfcAdapter = null;
    private int status = 0;

    private byte[] queuedWriteData = null;

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
            "pollTags",
            "queueWrite"
        );
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();

        signals.add(new SignalInfo("nfc_enabled", Integer.class));
        signals.add(new SignalInfo("tag_readed", byte[].class));
        signals.add(new SignalInfo("tag_written"));

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
        if (intent == previousIntent)
            return;

        previousIntent = intent;

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            if (queuedWriteData == null) { //Read
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (parcelables != null) {
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
            else { //Write
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                writeTag(tag, queuedWriteData);
                queuedWriteData = null;
            }
        }
    }

    private void writeTag(Tag tag, byte[] data) {
        NdefRecord relayRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, null, data);
        NdefMessage message = new NdefMessage(new NdefRecord[] { relayRecord });
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) { //Tag not formatted
            NdefFormatable format = NdefFormatable.get(tag);
            if (format != null) {
                try {
                    format.connect();
                    format.format(message);
                    emitSignal("tag_written");
                } catch (TagLostException tle) {
                } catch (IOException ioe) {
                } catch (FormatException fe) {
                }
            }
        }
        else { //Tag already formatted
            try {
                ndef.connect();
                if (ndef.isWritable()) {
                    if (data.length <= ndef.getMaxSize()) {
                        try {
                            ndef.writeNdefMessage(message);
                            emitSignal("tag_written");
                        } catch (TagLostException tle) {
                        } catch (IOException ioe) {
                        } catch (FormatException fe) {
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public void queueWrite(byte[] data) {
        queuedWriteData = data;
    }

}
