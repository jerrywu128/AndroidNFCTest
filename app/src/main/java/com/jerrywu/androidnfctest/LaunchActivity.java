package com.jerrywu.androidnfctest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jerrywu.androidnfctest.databinding.LaunchActivityBinding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class LaunchActivity extends AppCompatActivity {
    private LaunchActivityBinding binding;
    private NfcAdapter nfcAdapter;
    private NdefMessage ndefMessage;
    private boolean isReading = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = LaunchActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        binding.readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
                    // NFC 不可用
                    binding.textResult.setText("Please open NFC");
                } else {
                    // NFC 可用
                    binding.textResult.setText("Around NFC Tag");
                    isReading = true;
                    enableNfcForegroundDispatch();
                }


            }
        });

        binding.clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.textResult.setText("Hi");
            }
        });

        binding.writeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
                    // NFC 不可用
                    binding.textResult.setText("Please open NFC");
                } else {
                    // NFC 可用
                    binding.readBtn.setEnabled(false);
                    showAlertDialog();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter!=null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private void enableNfcForegroundDispatch() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter[] intentFiltersArray = new IntentFilter[] {};
        String[][] techListsArray = new String[][] {};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            if (!isReading) {
                binding.readBtn.setEnabled(true);
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    try {
                        Parcelable[] rawMessages =
                                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                        if (rawMessages != null) {
                            NdefMessage[] messages = new NdefMessage[rawMessages.length];
                            for (int i = 0; i < rawMessages.length; i++) {
                                messages[i] = (NdefMessage) rawMessages[i];
                                String messageString = NdefMessageToString(messages[i]);
                                NdefRecord record = NdefRecord.createTextRecord(null, "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz");
                                ndefMessage = new NdefMessage(new NdefRecord[]{record});
                                Intent intent2 = new Intent(this, getClass());
                                intent2.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
                                intent2.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[]{ndefMessage});

                                ndef.connect();
                                ndef.writeNdefMessage(ndefMessage);
                                binding.textResult.setText("NFC write success");
                                Toast.makeText(this, "NFC write success", Toast.LENGTH_SHORT).show();

                            }
                        }

                    } catch (IOException | FormatException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "NFC write fail ", Toast.LENGTH_SHORT).show();
                    } finally {
                        try {
                            ndef.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(this, "can't connect NFC Tag", Toast.LENGTH_SHORT).show();

                }
            }else {


                Parcelable[] rawMessages =
                        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMessages != null) {
                    NdefMessage[] messages = new NdefMessage[rawMessages.length];
                    for (int i = 0; i < rawMessages.length; i++) {
                        messages[i] = (NdefMessage) rawMessages[i];
                        String messageString = NdefMessageToString(messages[i]);
                        binding.textResult.setText(messageString);
                        Log.i("test", messageString);

                    }
                }
                isReading = false;
            }

        }


    }
    private  void showAlertDialog(){
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint("Enter your message");

        new AlertDialog.Builder(this)
                .setTitle("Input text to write")
                .setView(editText)
                .setPositiveButton("sure", (dialog2, which2) -> {


                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                    IntentFilter[] intentFiltersArray = new IntentFilter[] {};
                    String[][] techListsArray = new String[][] {};
                    nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
                    binding.textResult.setText("Around NFC Tag");



                })
                .setNegativeButton("cancel", (dialog, which) -> {

                })
                .show();

    }

    private String NdefMessageToString(NdefMessage ndefMessage) {
        if (ndefMessage == null) {
            return "";
        }

        NdefRecord[] records = ndefMessage.getRecords();
        if (records == null || records.length == 0) {
            return "";
        }

        NdefRecord firstRecord = records[0];
        byte[] payload = firstRecord.getPayload();
        if (payload == null || payload.length <= 3) {
            return "";
        }

        // 判断有效载荷的编码类型
        String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";

        // 获取有效载荷长度信息
        int languageCodeLength = payload[0] & 0x3F;

        try {
            // 解析有效载荷并转换为字符串
            String payloadString = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            return payloadString;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
