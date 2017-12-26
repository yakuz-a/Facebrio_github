package com.hackz.facebrio;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.wordpress.passcodelock.AppLockManager;


@ReportsCrashes(formUri = "",  // will not be used
        mailTo = "arclecorp@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = R.mipmap.ic_launcher,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogTheme = R.style.CrashDialog
        )

public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        super.onCreate();
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        /**
         * The following line triggers the initialization of ACRA.
         */
        ACRA.init(this);

    }

    /**
     * Get context of application for non-context classes
     * @return context of application
     */
    public static Context getContextOfApplication() {
        return mContext;
    }



}
