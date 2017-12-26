package com.hackz.facebrio.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.hackz.facebrio.R;

public class MessagesShortcutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.putExtra("start_url", MainActivity.NOTIFICATION_OLD_MESSAGES_URL);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.messages));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_chat);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        setResult(RESULT_OK, intent);

        finish();
    }

}
