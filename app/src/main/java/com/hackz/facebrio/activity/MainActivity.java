package com.hackz.facebrio.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.hackz.facebrio.MyApplication;
import com.hackz.facebrio.R;
import com.hackz.facebrio.service.NotificationsService;
import com.hackz.facebrio.util.AndroidBug5497Workaround;
import com.hackz.facebrio.util.CheckUpdatesTask;
import com.hackz.facebrio.util.Connectivity;
import com.hackz.facebrio.util.Dimension;
import com.hackz.facebrio.util.DownloadManagerResolver;
import com.hackz.facebrio.util.Miscellany;
import com.hackz.facebrio.webview.MFBWebView;
import com.hackz.facebrio.webview.MyWebViewClient;
import com.rom4ek.arcnavigationview.ArcNavigationView;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
public class MainActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener {

    // reference to this object
    @SuppressLint("StaticFieldLeak")
    private static Activity mainActivity;



    // main layout, pull to refresh, webview
    private DrawerLayout contentMain;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MFBWebView webView;
    private ProgressBar progressBar;

    // fullscreen videos
    private MyWebChromeClient mWebChromeClient;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout customViewContainer;
    private View mCustomView;
    private int previousUiVisibility;

    // variables for camera and choosing files methods
    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI;

    // the same for Android 5.0 methods only
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // log tag, preferences, runtime permissions
    private static final String TAG = MainActivity.class.getSimpleName();
    private SharedPreferences preferences;
    private static final int REQUEST_STORAGE = 1;
    private static final int REQUEST_LOCATION = 2;

    // create link handler (long clicked links)
    private final MyHandler linkHandler = new MyHandler(this);

    // save images
    private static final int ID_CONTEXT_MENU_SAVE_IMAGE = 2562617;
    private static final int ID_CONTEXT_MENU_SHARE_IMAGE = 2562618;
    private String mPendingImageUrlToSave;
    private static String appDirectoryName;

    // user agents
    private static String userAgentDefault;
    private static final String USER_AGENT_BASIC = "Mozilla/5.0 (Linux; U; Android 2.3.3; en-gb; " +
            "Nexus S Build/GRI20) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
    public static final String USER_AGENT_MESSENGER = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

    public static final String MESSENGER_URL = "https://www.messenger.com/login";
    public static final String NOTIFICATION_OLD_MESSAGES_URL = "https://m.facebook.com/messages#";
    private static final long UPDATE_CHECK_INTERVAL = 43200000;  // 12 hours


    ArcNavigationView arcNavigationView;
    String webViewUrl;
    public static FloatingActionMenu mMenuFAB;


    @Override
    @SuppressLint("setJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set reference to this object
        mainActivity = this;

        // get shared preferences and TrayPreferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        appDirectoryName = getString(R.string.app_name).replace(" ", "");

        // set the main content view (for drawer position)
        setContentView(R.layout.activity_main);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        arcNavigationView = (ArcNavigationView) findViewById(R.id.drawer_slider);
        arcNavigationView.setNavigationItemSelectedListener(this);

        // the main layout, everything is inside
        contentMain = (DrawerLayout) findViewById(R.id.drawer_layout);

        //for admob ad

        if (preferences.getBoolean("keyboard_fix", false))
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // if the app is being launched for the first time
        if (preferences.getBoolean("first_run", true)) {
            // show quick start guide
            onCoachMark();
            // save the fact that the app has been started at least once
            preferences.edit().putBoolean("first_run", false).apply();
        }



        // start the service when it's activated but somehow it's not running
        // when it's already running nothing happens so it's ok
        if (preferences.getBoolean("notifications_activated", false) || preferences.getBoolean("message_notifications", false)) {
            final Intent intent = new Intent(MyApplication.getContextOfApplication(), NotificationsService.class);
            MyApplication.getContextOfApplication().startService(intent);
        }

        // KitKat layout fix
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // apply top padding to avoid layout being hidden by the status bar
            contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
            // bug fix for resizing the view while opening soft keyboard
            AndroidBug5497Workaround.assistActivity(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // transparent navBar (above KitKat) when it's enabled
            if (preferences.getBoolean("transparent_nav", false)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                // apply top padding to avoid layout being hidden by the status bar
                contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                // bug fix for resizing the view while opening soft keyboard
                AndroidBug5497Workaround.assistActivity(this);

                // bug fix (1.4.1) for launching the app in landscape mode
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), Dimension.getNavigationBarHeight(getApplicationContext(), 0), 0);
                else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, 0, 0, Dimension.getStatusBarHeight(getApplicationContext()));
                }
            }
        }


        // Inflate the FAB menu
        mMenuFAB = (FloatingActionMenu) findViewById(R.id.menuFAB);


        // Nasty hack to get the FAB menu button
        mMenuFAB.getChildAt(4).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMenuFAB.hideMenu(true);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Show your View after 3 seconds
                        mMenuFAB.showMenu(true);
                    }
                }, 3000);
                return false;
            }
        });
        findViewById(R.id.news_feed).setOnClickListener(mFABClickListener);
        findViewById(R.id.refresh).setOnClickListener(mFABClickListener);
        findViewById(R.id.settings).setOnClickListener(mFABClickListener);
        findViewById(R.id.top).setOnClickListener(mFABClickListener);
        findViewById(R.id.chat).setOnClickListener(mFABClickListener);





        // define url that will open in webView
        webViewUrl = "https://mobile.facebook.com/";

        if (preferences.getBoolean("touch_mode", false))
            webViewUrl = "https://touch.facebook.com/";
        else if (preferences.getBoolean("basic_mode", false))
            webViewUrl = "https://mbasic.facebook.com/";


        // most recent posts
        webViewUrl = appendMostRecentInfix(webViewUrl);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE);

        // fullscreen videos display here
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        // bind progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // webView code without handling external links
        webView = (MFBWebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        // show full images
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setOnScrollChangedCallback(new MFBWebView.OnScrollChangedCallback() {
            @Override
            public void onScrollChange(WebView view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // Make sure the hiding is enabled and the scroll was significant
                if (Math.abs(oldScrollY - scrollY) > getApplication().getResources().getDimensionPixelOffset(R.dimen.fab_button_dimen)) {
                    if (scrollY > oldScrollY && scrollY > 0) {
                        // User scrolled down, hide the button
                        mMenuFAB.hideMenuButton(true);
                    } else if (scrollY < oldScrollY) {
                        // User scrolled up, show the button
                        mMenuFAB.showMenuButton(true);
                    }
                }
            }
        });



        // text size (percent)
        try {
            int textScale = Integer.valueOf(preferences.getString("font_size", "100"));
            if (textScale > 0 && textScale < 1000)
                webView.getSettings().setTextZoom(textScale);
            else
                preferences.edit().remove("font_size").apply();
        } catch (NumberFormatException e) {
            preferences.edit().remove("font_size").apply();
        }

        // location
        if (preferences.getBoolean("location", false)) {
            webView.getSettings().setGeolocationEnabled(true);
            if (Build.VERSION.SDK_INT < 24) {
                //noinspection deprecation
                webView.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());
            }
        }

        // since API 18 cache quota is managed automatically
        if (Build.VERSION.SDK_INT < 18) {
            //noinspection deprecation
            webView.getSettings().setAppCacheMaxSize(5 * 1024 * 1024);  // 5 MB
        }

        // enable caching
        webView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);  // load online by default

        // get user agent
        userAgentDefault = webView.getSettings().getUserAgentString();
        preferences.edit().putString("webview_user_agent", userAgentDefault).apply();

        if (!preferences.getString("custom_user_agent", getString(R.string.predefined_user_agent)).isEmpty())
            webView.getSettings().setUserAgentString(preferences.getString("custom_user_agent", getString(R.string.predefined_user_agent)));
        else if (preferences.getBoolean("basic_mode", false))
            webView.getSettings().setUserAgentString(USER_AGENT_BASIC);

        // disable images to reduce data usage
        if (preferences.getBoolean("no_images", false))
            webView.getSettings().setLoadsImagesAutomatically(false);


        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://mobile.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);

                if (preferences.getBoolean("touch_mode", false))
                    webViewUrl = String.format("https://touch.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                else if (preferences.getBoolean("basic_mode", false))
                    webViewUrl = String.format("https://mbasic.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }



        // notify when there is no internet connection (offline mode have its own messages)
        if (!Connectivity.isConnected(this) && !preferences.getBoolean("offline_mode", false))
            Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

        // set webview clients
        mWebChromeClient = new MyWebChromeClient();
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(mWebChromeClient);

        // speed it up for some devices
        if (Build.VERSION.SDK_INT < 18) {
            //noinspection deprecation
            webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        }

        // set webView reference
        MyWebViewClient.setWebviewReference(webView);

        // load url in a webView
        MyWebViewClient.currentlyLoadedPage = webViewUrl;
        webView.loadUrl(webViewUrl);

        // OnLongClickListener for detecting long clicks on links and images
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // activate long clicks on links and image links according to settings
                if (preferences.getBoolean("long_clicks", true)) {
                    WebView.HitTestResult result = webView.getHitTestResult();
                    if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        Message msg = linkHandler.obtainMessage();
                        webView.requestFocusNodeHref(msg);
                        return true;
                    }
                }
                return false;
            }
        });

        // check for app updates
        if (preferences.getBoolean("app_updates", true)) {
            final long now = System.currentTimeMillis();
            final long lastUpdateCheck = preferences.getLong("latest_update_check", 0);
            final long sinceLastCheck = now - lastUpdateCheck;
            if (sinceLastCheck > UPDATE_CHECK_INTERVAL && Connectivity.isConnected(this) && !preferences.getBoolean("first_run", true)) {
                new CheckUpdatesTask(this).execute();
            }
        }
    }


    private class MyWebChromeClient extends WebChromeClient {

        // page loading progress, gone when fully loaded
        public void onProgressChanged(WebView view, int progress) {
            // display it only when it's enabled (default true)
            if (preferences.getBoolean("progress_bar", true)) {
                if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE)
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                // set progress, it changes
                progressBar.setProgress(progress);
                if (progress == 100)
                    progressBar.setVisibility(ProgressBar.GONE);
            } else {
                // if progress bar is disabled hide it immediately
                progressBar.setVisibility(ProgressBar.GONE);
            }
        }

        // for >= Lollipop, all in one
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {

            /** Request permission for external storage access.
             *  If granted it's awesome and go on,
             *  otherwise just stop here and leave the method.
             */
            requestStoragePermission();
            if (!hasStoragePermission())
                return false;

            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // create the file where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // continue only if the file was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

            return true;
        }

        // creating image files (Lollipop only)
        private File createImageFile() throws IOException {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

            if (!imageStorageDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageStorageDir.mkdirs();
            }

            // create an image file name
            imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            return imageStorageDir;
        }

        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;

            try {
                File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

                if (!imageStorageDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    imageStorageDir.mkdirs();
                }

                File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                mCapturedImageURI = Uri.fromFile(file); // save to the private variable

                final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                //captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent chooserIntent = Intent.createChooser(i, getString(R.string.image_chooser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_exception), Toast.LENGTH_LONG).show();
            }

        }

        // not needed but let's make it overloaded just in case
        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        // openFileChooser for other Android versions
        /** may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
         *  https://code.google.com/p/android/issues/detail?id=62220
         *  however newer versions of KitKat fixed it on some devices */
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            openFileChooser(uploadMsg, acceptType);
        }

        /** This method was deprecated in API level 18.
         *  This method supports the obsolete plugin mechanism,
         *  and will not be invoked in future
         */
        @SuppressWarnings("deprecation")
        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;

            // hide webView and swipeRefreshLayout
            webView.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.GONE);

            // show customViewContainer
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;

            // activate immersive mode
            if (Build.VERSION.SDK_INT >= 19)
                hideSystemUI();
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mCustomView == null)
                return;

            // hide and remove customViewContainer
            mCustomView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.GONE);
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            // show swipeRefreshLayout and webView
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);

            mCustomView = null;

            // deactivate immersive mode
            if (Build.VERSION.SDK_INT >= 19)
                showSystemUI();
        }

        // location
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            /** Request location permission.
             *  If granted it's awesome and go on,
             *  otherwise just stop here and leave the method.
             */
            requestLocationPermission();
            if (!hasLocationPermission())
                return;

            callback.invoke(origin, true, false);
        }

    }

    // handle long clicks on links, an awesome way to avoid memory leaks
    private static class MyHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {

                // get url to share
                String url = (String) msg.getData().get("url");

                if (url != null) {
                    /* "clean" an url to remove Facebook tracking redirection while sharing
                    and recreate all the special characters */
                    url = Miscellany.cleanAndDecodeUrl(url);

                    Log.v("Link long clicked", url);
                    // create share intent for long clicked url
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_link)));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideSystemUI() {
        previousUiVisibility = contentMain.getSystemUiVisibility();
        contentMain.setPadding(0, 0, 0, 0);

        contentMain.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showSystemUI() {
        contentMain.setSystemUiVisibility(previousUiVisibility);
        // fake a configuration change to set the right padding
        onConfigurationChanged(getResources().getConfiguration());
    }

    // request storage permission
    private void requestStoragePermission() {
        String[] permissions = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (!hasStoragePermission()) {
            Log.e(TAG, "No storage permission at the moment. Requesting...");
            ActivityCompat.requestPermissions(this, permissions, REQUEST_STORAGE);
        } else {
            Log.e(TAG, "We already have storage permission. Yay!");
            // new image is about to be saved
            if (mPendingImageUrlToSave != null)
                saveImageToDisk(mPendingImageUrlToSave);
        }
    }

    // check is storage permission granted
    private boolean hasStoragePermission() {
        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int hasPermission = ContextCompat.checkSelfPermission(this, storagePermission);
        return (hasPermission == PackageManager.PERMISSION_GRANTED);
    }

    // request location permission
    private void requestLocationPermission() {
        String[] permissions = new String[] { Manifest.permission.ACCESS_FINE_LOCATION };
        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission at the moment. Requesting...");
            ActivityCompat.requestPermissions(this, permissions, REQUEST_LOCATION);
        } else {
            Log.e(TAG, "We already have location permission. Yay!");
        }
    }

    // check is location permission granted
    private boolean hasLocationPermission() {
        String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        int hasPermission = ContextCompat.checkSelfPermission(this, locationPermission);
        return (hasPermission == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Storage permission granted");
                    // new image is about to be saved
                    if (mPendingImageUrlToSave != null)
                        saveImageToDisk(mPendingImageUrlToSave);
                } else {
                    Log.e(TAG, "Storage permission denied");
                    Toast.makeText(getApplicationContext(), getString(R.string.no_storage_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Location permission granted");
                    webView.reload();
                } else {
                    Log.e(TAG, "Location permission denied");
                    Toast.makeText(getApplicationContext(), getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // return here when file selected from camera or from SD Card
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage)
                    return;

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK)
                        result = null;
                    else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                }
                catch(Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :"+e, Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }

        } // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // if there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[] {Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[] {Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } // end of code for Lollipop only
    }

    private final SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

        // refreshing pages
        @Override
        public void onRefresh() {
            // notify when there is no internet connection (offline mode have its own messages)
            if (!Connectivity.isConnected(getApplicationContext()) && !preferences.getBoolean("offline_mode", false))
                Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

            webView.stopLoading();

            // reloading page (if offline try to load a live version first)
            if (preferences.getBoolean("offline_mode", false) && MyWebViewClient.wasOffline)
                webView.loadUrl(MyWebViewClient.currentlyLoadedPage);
            else
                webView.reload();

            // if no internet connection and offline mode enabled show a different loading indicator
            if (preferences.getBoolean("offline_mode", false) && !Connectivity.isConnected(getApplicationContext()))
                swipeRefreshLayout.setRefreshing(false);
            else {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        // done!
                    }

                }, 2000);
            }
        }};



    // survive screen orientation change
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // don't change padding during fullscreen video playback
        if (mCustomView == null) {
            // bug fix (1.4.1) for landscape mode
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && preferences.getBoolean("transparent_nav", false)) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), Dimension.getNavigationBarHeight(getApplicationContext(), 0), 0);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, 0, 0, Dimension.getStatusBarHeight(getApplicationContext()));
                }
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && preferences.getBoolean("transparent_nav", false)) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                }
            }
        }
    }

    // app is already running and gets a new intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // recreate activity when something important was just changed
        if (getIntent().getBooleanExtra("core_settings_changed", false)) {
            finish(); // finish and create a new Instance
            Intent restart = new Intent(MainActivity.this, MainActivity.class);
            startActivity(restart);
        }

        // grab an url if opened by clicking a link
        String webViewUrl = getIntent().getDataString();

        // handle  fb://profile/<facebook_id>  links
        if (!TextUtils.isEmpty(webViewUrl))
            webViewUrl = webViewUrl.replace("fb://profile/", "https://facebook.com/");

        // set the right user agent
        setUserAgent();

        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://mobile.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);

                if (preferences.getBoolean("touch_mode", false))
                    webViewUrl = String.format("https://touch.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                else if (preferences.getBoolean("basic_mode", false))
                    webViewUrl = String.format("https://mbasic.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }

        /** if opened by a notification or a shortcut */
        try {
            if (getIntent().getExtras().getString("start_url") != null) {
                webViewUrl = getIntent().getExtras().getString("start_url");
            }
        } catch (Exception ignored) {}


        // notify when there is no internet connection
        if (!Connectivity.isConnected(getApplicationContext()) && !preferences.getBoolean("offline_mode", false))
            Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

        // location
        if (preferences.getBoolean("location", false)) {
            webView.getSettings().setGeolocationEnabled(true);
            if (Build.VERSION.SDK_INT < 24) {
                //noinspection deprecation
                webView.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());
            }
        } else {
            webView.getSettings().setGeolocationEnabled(false);
        }

        // text size (percent)
        try {
            int textScale = Integer.valueOf(preferences.getString("font_size", "100"));
            if (textScale > 0 && textScale < 1000)
                webView.getSettings().setTextZoom(textScale);
            else {
                preferences.edit().remove("font_size").apply();
                webView.getSettings().setTextZoom(100);
            }
        } catch (NumberFormatException e) {
            preferences.edit().remove("font_size").apply();
            webView.getSettings().setTextZoom(100);
        }
    }

    // handling back button
    @Override
    public void onBackPressed() {
        if (inCustomView())
            hideCustomView();
        else if (mCustomView == null && webView.canGoBack()) {
            webView.stopLoading();
            setUserAgent();
            webView.goBack();
        } else {
            if (preferences.getBoolean("confirm_exit", false))
                showExitDialog();
            else
                super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        registerForContextMenu(webView);
        preferences.edit().putBoolean("activity_visible", true).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            unregisterForContextMenu(webView);
            webView.onPause();
            webView.pauseTimers();
        }
        preferences.edit().putBoolean("activity_visible", false).apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Destroying...");
        super.onDestroy();
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        // just in case, it should be GCed anyway
        if (mWebChromeClient != null)
            mWebChromeClient = null;
    }

    // is a video played in fullscreen mode
    private boolean inCustomView() {
        return (mCustomView != null);
    }

    // deactivate fullscreen for video playback
    private void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        WebView.HitTestResult result = webView.getHitTestResult();
        if (result != null) {
            int type = result.getType();

            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                showLongPressedImageMenu(menu, result.getExtra());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ID_CONTEXT_MENU_SAVE_IMAGE:
                /** In order to save anything we need storage permission.
                 *  onRequestPermissionsResult will save an image.
                 */
                requestStoragePermission();
                break;
            case ID_CONTEXT_MENU_SHARE_IMAGE:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, mPendingImageUrlToSave);
                startActivity(Intent.createChooser(share, getString(R.string.share_link)));
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showLongPressedImageMenu(ContextMenu menu, String imageUrl) {
        mPendingImageUrlToSave = imageUrl;
        menu.add(0, ID_CONTEXT_MENU_SAVE_IMAGE, 0, getString(R.string.save_img));
        menu.add(0, ID_CONTEXT_MENU_SHARE_IMAGE, 1, getString(R.string.share_link));
    }

    private void saveImageToDisk(String imageUrl) {
        if (!DownloadManagerResolver.resolve(this)) {
            mPendingImageUrlToSave = null;
            return;
        }

        try {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

            if (!imageStorageDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageStorageDir.mkdirs();
            }

            // default image extension
            String imgExtension = ".jpg";

            if (imageUrl.contains(".gif"))
                imgExtension = ".gif";
            else if (imageUrl.contains(".png"))
                imgExtension = ".png";
            else if (imageUrl.contains(".3gp"))
                imgExtension = ".3gp";

            String date = DateFormat.getDateTimeInstance().format(new Date());
            String file = "Messenger-image-" + date.replace(" ", "").replace(":", "").replace(".", "") + imgExtension;

            DownloadManager dm = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(imageUrl);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES + File.separator + appDirectoryName, file)
                    .setTitle(file).setDescription(getString(R.string.save_img))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            dm.enqueue(request);

            Toast.makeText(this, getString(R.string.downloading_img), Toast.LENGTH_LONG).show();
        } catch (IllegalStateException ex) {
            Toast.makeText(this, getString(R.string.cannot_access_storage), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            // just in case, it should never be called anyway
            Toast.makeText(this, getString(R.string.file_cannot_be_saved), Toast.LENGTH_LONG).show();
        } finally {
            mPendingImageUrlToSave = null;
        }
    }

    private String appendMostRecentInfix(String url) {
        if (preferences.getBoolean("most_recent", false))
            url += "?sk=h_chr";
        return url;
    }

    // first run dialog with introduction
    private void onCoachMark() {


        Handler handler = new Handler();
        Runnable run= new Runnable() {

            @Override
            public void run() {

                Intent in = new Intent(MainActivity.this, WelcomeActivity.class);
                in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(in);

            }
        };
        handler.post(run);

    }

    private AlertDialog createExitDialog() {
        AppCompatTextView messageTextView = new AppCompatTextView(this);
        messageTextView.setTextSize(16f);
        messageTextView.setText(getString(R.string.really_quit_question));
        messageTextView.setPadding(50, 50, 50, 0);
        messageTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
        return new AlertDialog.Builder(this)
                .setView(messageTextView)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing to do here
                    }
                })
                .setCancelable(true)
                .create();
    }

    private void showExitDialog() {
        AlertDialog alertDialog = createExitDialog();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
    }

    private void setUserAgent() {
        // set the right user agent
        if (!preferences.getString("custom_user_agent", getString(R.string.predefined_user_agent)).isEmpty()) {
            webView.getSettings().setUserAgentString(preferences.getString("custom_user_agent", getString(R.string.predefined_user_agent)));
            return;
        }

        if (preferences.getBoolean("basic_mode", false))
            webView.getSettings().setUserAgentString(USER_AGENT_BASIC);
        else
            webView.getSettings().setUserAgentString(userAgentDefault);
    }

    private void addLauncherShortcut() {
        final Intent shortcut = new Intent(this, CustomShortcutActivity.class);
        shortcut.putExtra(CustomShortcutActivity.URL_FIELD, webView.getUrl());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.label));
        final AppCompatEditText input = new AppCompatEditText(this);
        input.setHint(webView.getTitle());
        input.setSingleLine();

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String label = input.getText().toString();
                if (TextUtils.isEmpty(label))
                    label = webView.getTitle();
                shortcut.putExtra(CustomShortcutActivity.NAME_FIELD, label);
                startActivity(shortcut);
                Toast.makeText(getApplicationContext(), "\uD83D\uDC4C", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
    }

    public static Activity getMainActivity() {
        return mainActivity;
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_newsFeed) {
            webView.loadUrl(webViewUrl);
        } else if (id == R.id.nav_profile) {
            webView.loadUrl(webViewUrl + "profile.php");

        } else if (id == R.id.nav_onlineFriends) {
            webView.loadUrl(webViewUrl + "buddylist.php");

        } else if (id == R.id.nav_message) {
            webView.loadUrl( webViewUrl + "messages/?more");

        } else if (id == R.id.nav_fbSettings) {
            webView.loadUrl( webViewUrl + "settings/?");

        } else if (id == R.id.nav_pages) {
            webView.loadUrl( webViewUrl + "pages/?");

        } else if (id == R.id.nav_findFriends){
            webView.loadUrl( webViewUrl + "friends/?");

        } else if (id == R.id.nav_pokes) {
            webView.loadUrl( webViewUrl + "pokes/?");

        } else if (id == R.id.nav_groups) {
            webView.loadUrl( webViewUrl + "groups/?");

        } else if (id == R.id.nav_day) {
            webView.loadUrl( webViewUrl + "onthisday/?");

        } else if (id == R.id.nav_events) {
            webView.loadUrl( webViewUrl + "events/?");

        } else if (id == R.id.nav_saved) {
            webView.loadUrl( webViewUrl + "saved/?");

        } else if (id == R.id.nav_translate) {
            webView.loadUrl("http://www.facebook.com/shubhamhackz");

        } else if (id == R.id.nav_share) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.downloadThisApp));
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share)));

            Toast.makeText(getApplicationContext(), getResources().getString(R.string.thanks),
                    Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_preferences) {
            Intent intent= new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_about) {

            //for a lovely about dialog box
            new LovelyInfoDialog(this)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.mipmap.ic_launcher)
                    //This will add Don't show again checkbox to the dialog. You can pass any ID as argument
                    .setTitle(R.string.info_title)
                    .setMessage(R.string.info_message)
                    .show();


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private final View.OnClickListener mFABClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.news_feed:
                    webView.loadUrl(webViewUrl);
                    break;
                case R.id.chat:
                    webView.loadUrl(webViewUrl + "buddylist.php");
                    break;
                case R.id.settings:
                    Intent intent= new Intent(MainActivity.this,SettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.refresh:
                    webView.reload();
                    break;
                case R.id.top:
                    webView.scrollTo(0,0);
                    break;
                default:
                    break;
            }
            mMenuFAB.close(true);
        }
    };



}
