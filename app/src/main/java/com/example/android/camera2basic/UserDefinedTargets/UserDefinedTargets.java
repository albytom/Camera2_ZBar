/*===============================================================================
Copyright (c) 2020 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.example.android.camera2basic.UserDefinedTargets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.camera2basic.R;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleActivityBase;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleAppMenu;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleAppMenuGroup;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleAppMessage;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleAppTimer;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleApplicationControl;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleApplicationException;
import com.example.android.camera2basic.UserDefinedTargets.sample.SampleApplicationSession;
import com.example.android.camera2basic.UserDefinedTargets.utils.Constants;
import com.example.android.camera2basic.UserDefinedTargets.utils.LoadingDialogHandler;
import com.example.android.camera2basic.UserDefinedTargets.utils.SampleAppMenuInterface;
import com.example.android.camera2basic.UserDefinedTargets.utils.SampleApplicationGLView;
import com.example.android.camera2basic.UserDefinedTargets.utils.Texture;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.ArrayList;
import java.util.Vector;


/**
 * The main activity for the UserDefinedTargets sample.
 * User Defined Targets allows users to create an Image Target using a captured image
 * <p>
 * This class does high-level handling of the Vuforia lifecycle and any UI updates
 * <p>
 * For UderDefinedTarget-specific rendering, check out UserDefinedTargetRenderer.java
 * For the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class UserDefinedTargets extends SampleActivityBase implements
        SampleApplicationControl, View.OnClickListener {
    private static final String LOGTAG = "UserDefinedTargets";

    private SampleApplicationSession vuforiaAppSession;

    private SampleApplicationGLView mGlView;

    private UserDefinedTargetRenderer mRenderer;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    // View overlays to be displayed in the Augmented View
    private RelativeLayout mUILayout;
    private View mBottomBar;
    private View mCameraButton;

    // Alert dialog for displaying SDK errors
    private AlertDialog mDialog;

    private int targetBuilderCounter = 1;

    private DataSet dataSetUserDef = null;

    private GestureDetector mGestureDetector;

    private SampleAppMenu mSampleAppMenu;
    ArrayList<View> mSettingsAdditionalViews = new ArrayList<>();

    private SampleAppMessage mSampleAppMessage;
    private SampleAppTimer mRelocalizationTimer;
    private SampleAppTimer mStatusDelayTimer;

    private int mCurrentStatusInfo;

    private boolean mDeviceTracker = false;

    private final LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
            this);

    RefFreeFrame refFreeFrame;

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    private boolean mIsDroidDevice = false;
    private Trackable mTrackable;
    private String mItemName;
    private String mItemColor;
    private float mItemSize;
    private String mItemLoc;
    private TextView itemTv, locTv, bCodeTv;
    CheckBox isFoundCv;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        Intent vIntent = getIntent();

        mItemName = vIntent.getStringExtra("name");
        mItemColor = vIntent.getStringExtra("color");
        mItemSize = Float.parseFloat(String.valueOf(vIntent.getStringExtra("size")));
        mItemLoc = vIntent.getStringExtra("loc" );
        Constants.CUBOID_RATIO = vIntent.getIntArrayExtra("ratio");

        if (ContextCompat.checkSelfPermission(UserDefinedTargets.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(UserDefinedTargets.this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
            onPermissionGranted();
        itemTv = findViewById(R.id.i_title_tv);
        locTv = findViewById(R.id.i_loc_tv);
        isFoundCv = findViewById(R.id.item_found);
        itemTv.setText(mItemName);
        locTv.setText(mItemLoc);
        bCodeTv = findViewById(R.id.i_content_tv);
        bCodeTv.setVisibility(View.INVISIBLE);
        isFoundCv.setVisibility(View.INVISIBLE);
        backButton = findViewById(R.id.back_btn);
        backButton.setOnClickListener(this);

        onCameraClick();
    }

    @Override
    public void onClick(View pView) {
        switch (pView.getId()) {
            /*case R.id.scan_btn:
                takePicture();
                break;*/
            case R.id.back_btn:
                onBackPressed();
                break;
        }
    }

    private void onPermissionGranted() {
        vuforiaAppSession = new SampleApplicationSession(this);

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Load any sample specific textures:
        mTextures = new Vector<>();
        loadTextures(mItemColor);

        mGestureDetector = new GestureDetector(this, new GestureListener());

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
                "droid");

        addOverlayView(true);

        // Relocalization timer and message
        mSampleAppMessage = new SampleAppMessage(this, mUILayout, mUILayout.findViewById(R.id.topbar_layout), false);
        mRelocalizationTimer = new SampleAppTimer(10000, 1000) {
            @Override
            public void onFinish() {
                if (vuforiaAppSession != null) {
                    vuforiaAppSession.resetDeviceTracker();
                }

                super.onFinish();
            }
        };

        mStatusDelayTimer = new SampleAppTimer(1000, 1000) {
            @Override
            public void onFinish() {
                if (mRenderer.isTargetCurrentlyTracked()) {
                    super.onFinish();
                    return;
                }

                if (!mRelocalizationTimer.isRunning()) {
                    mRelocalizationTimer.startTimer();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSampleAppMessage.show(getString(R.string.instruct_relocalize));
                    }
                });

                super.onFinish();
            }
        };
    }




    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }


        // Process Single Tap event to trigger autofocus
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (!autofocusResult)
                        Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                }
            }, 1000L);

            return true;
        }
    }


    // Load specific textures from the APK, which we will later use for rendering.
    // Load specific textures from the APK, which we will later use for rendering.
    private void loadTextures(String texture) {
        if(mTextures!=null){
            mTextures.clear();
        }
        mTextures.add(Texture.loadTextureFromApk(texture,
                getAssets()));
    }

    // Set size of object, which we will later use for rendering.
    private void setSize(float pSize) {
        mItemSize = pSize;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        vuforiaAppSession.onResume();
    }


    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        vuforiaAppSession.onPause();
    }


    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }


    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();

        // Removes the current layout and inflates a proper layout
        // for the new screen orientation

        if (mUILayout != null) {
            mUILayout.removeAllViews();
            ((ViewGroup) mUILayout.getParent()).removeView(mUILayout);

        }

        addOverlayView(false);
    }


    private void showErrorDialog() {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();

        mDialog = new AlertDialog.Builder(UserDefinedTargets.this).create();
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.button_OK), clickListener);

        mDialog.setTitle(getString(R.string.target_quality_error_title));

        String message = getString(R.string.target_quality_error_desc);

        mDialog.setMessage(message);
        mDialog.show();
    }


    private void showErrorDialogInUIThread() {
        runOnUiThread(new Runnable() {
            public void run() {
                showErrorDialog();
            }
        });
    }


    private void initApplicationAR() {
        // Do application initialization
        refFreeFrame = new RefFreeFrame(this);
        refFreeFrame.init();

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new UserDefinedTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mRenderer.setSize(mItemSize);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);

        setRendererReference(mRenderer);
    }


    // Adds the Overlay view to the GLView
    private void addOverlayView(boolean initLayout) {
        // Inflates the Overlay Layout to be displayed above the Camera View
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay_udt, null);

        mUILayout.setVisibility(View.VISIBLE);

        // If this is the first time that the application runs then the
        // uiLayout background is set to BLACK color, will be set to
        // transparent once the SDK is initialized and camera ready to draw
        if (initLayout) {
            mUILayout.setBackgroundColor(Color.BLACK);
        }

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // Gets a reference to the bottom navigation bar
        mBottomBar = mUILayout.findViewById(R.id.bottom_bar);

        // Gets a reference to the Camera button
        mCameraButton = mUILayout.findViewById(R.id.camera_button);

        // Gets a reference to the loading dialog container
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_layout);

        RelativeLayout topbarLayout = mUILayout.findViewById(R.id.topbar_layout);
        topbarLayout.setVisibility(View.INVISIBLE);

        /*TextView title = mUILayout.findViewById(R.id.topbar_title);
        title.setText(getText(R.string.feature_user_targets));*/

        mSettingsAdditionalViews.add(topbarLayout);

        initializeBuildTargetModeViews();

        mUILayout.bringToFront();
    }


    public void onCameraClick(View v) {
        if (isUserDefinedTargetsRunning()) {
            //dataSetUserDef.destroy(mTrackable); //(tried to destroy the user defined target)
           /* if (dataSetUserDef != null) {
                TrackableList mTrackableList = dataSetUserDef.getTrackables();
                Log.e("TrackableList", "size = " + mTrackableList.size());
                if ( mTrackableList.size() > 0) {
                    destroyTrackersData();
                    *//*for (int i = 0; i < mTrackableList.size(); i++) {
                        dataSetUserDef.destroy(mTrackableList.at(i));
                    }*//*
                }

            }*/
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

            // Builds the new target
            startBuild();
        }
    }
    public void onCameraClick() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                if (isUserDefinedTargetsRunning()) {
                    loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

                    // Builds the new target
                    startBuild();
                }else{
                    onCameraClick();
                }
            }
        }, 2000);


    }


    Texture createTexture(String nName) {
        return Texture.loadTextureFromApk(nName, getAssets());
    }


    // Callback function called when the target creation finished
    void targetCreated() {
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

        if (refFreeFrame != null) {
            refFreeFrame.reset();
        }
    }


    // Initialize UI
    private void initializeBuildTargetModeViews() {
        mBottomBar.setVisibility(View.VISIBLE);
        /*mCameraButton.setVisibility(View.VISIBLE);*/
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Process the Gestures
        return ((mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
                || mGestureDetector.onTouchEvent(event));
    }


    // Scan the environment for your User Defined Target
    private boolean startUserDefinedTargets() {
        Log.d(LOGTAG, "startUserDefinedTargets");

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
                .getTracker(ObjectTracker.getClassType()));

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();

            if (targetBuilder != null) {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();

                objectTracker.stop();

                targetBuilder.startScan();

            }
        } else
            return false;

        return true;
    }


    private boolean isUserDefinedTargetsRunning() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();
            if (targetBuilder != null) {
                Log.e(LOGTAG, "Quality> " + targetBuilder.getFrameQuality());
                return (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE);
            }
        }

        return false;
    }


    // Builds the User Defined Target
    private void startBuild() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();
            if (targetBuilder != null) {
                if (targetBuilder.getFrameQuality() == ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW) {
                    //showErrorDialogInUIThread();
                }

                String name;
                do {
                    name = "UserTarget-" + targetBuilderCounter;
                    Log.d(LOGTAG, "TRYING " + name);
                    targetBuilderCounter++;
                } while (!targetBuilder.build(name, .32f));

                refFreeFrame.setCreating();
            }
        }
    }


    void updateRendering() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        refFreeFrame.initGL(metrics.widthPixels, metrics.heightPixels);
    }


    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
                .getClassType());
        if (tracker == null) {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        } else {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.");
        }

        // Initialize the Positional Device Tracker
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                trackerManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null) {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        } else {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;
    }


    @Override
    public boolean doLoadTrackersData() {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(
                    LOGTAG,
                    "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        // Create the data set:
        dataSetUserDef = objectTracker.createDataSet();

        if (dataSetUserDef == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        if (!objectTracker.activateDataSet(dataSetUserDef)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }


    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start()) {
            Log.i(LOGTAG, "Successfully started Object Tracker");
        } else {
            Log.e(LOGTAG, "Failed to start Object Tracker");
            result = false;
        }

        if (isDeviceTrackingActive()) {
            PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager
                    .getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null && deviceTracker.start()) {
                Log.i(LOGTAG, "Successfully started Device Tracker");
            } else {
                Log.e(LOGTAG, "Failed to start Device Tracker");
            }
        }

        return result;
    }


    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.stop();
            Log.i(LOGTAG, "Successfully stopped object tracker");
        } else {
            Log.e(LOGTAG, "Failed to stop object tracker");
            result = false;
        }

        // Stop device tracker
        if (isDeviceTrackingActive()) {

            Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null) {
                deviceTracker.stop();
                Log.i(LOGTAG, "Successfully stopped device tracker");
            } else {
                Log.e(LOGTAG, "Could not stop device tracker");
            }
        }

        return result;
    }


    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because " +
                    "the ObjectTracker has not been initialized.");

            return false;
        }

        if (dataSetUserDef != null) {
            if (objectTracker.getActiveDataSets().at(0) != null
                    && !objectTracker.deactivateDataSet(dataSetUserDef)) {
                Log.d(LOGTAG,
                        "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            }

            if (!objectTracker.destroyDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }

            Log.d(LOGTAG, "Successfully destroyed the data set.");
            dataSetUserDef = null;
        }

        return result;
    }

    public void destroyTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because " +
                    "the ObjectTracker has not been initialized.");
            return;
        }

        if (dataSetUserDef != null) {
            if (objectTracker.getActiveDataSets().at(0) != null
                    && !objectTracker.deactivateDataSet(dataSetUserDef)) {
                Log.d(LOGTAG,
                        "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            }

            if (!objectTracker.destroyDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }

            Log.d(LOGTAG, "Successfully destroyed the data set.");
        }

    }


    @Override
    public boolean doDeinitTrackers() {
        if (refFreeFrame != null)
            refFreeFrame.deInit();

        TrackerManager tManager = TrackerManager.getInstance();
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;
    }


    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }


    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception) {
        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            vuforiaAppSession.startAR();

        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }


    @Override
    public void onVuforiaStarted() {
        mRenderer.updateRenderingPrimitives();

        if (!startUserDefinedTargets()) {
            Log.e(LOGTAG, "Failed to start User defined targets");
        }

        // Set camera focus mode
        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }

        showProgressIndicator(false);
    }


    private void showProgressIndicator(boolean show) {
        if (show) {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        } else {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    private void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        UserDefinedTargets.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    // Called every frame
    @Override
    public void onVuforiaUpdate(State state) {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (refFreeFrame.hasNewTrackableSource()) {
            Log.d(LOGTAG,
                    "Attempting to transfer the trackable source to the dataset");

            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSets().at(0));

            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            if (dataSetUserDef.hasReachedTrackableLimit()
                    || dataSetUserDef.getTrackables().size() >= 5)
                dataSetUserDef.destroy(dataSetUserDef.getTrackables().at(0));

            // Add new trackable source
            mTrackable = dataSetUserDef.createTrackable(refFreeFrame.getNewTrackableSource());

            // Reactivate current dataset
            objectTracker.activateDataSet(dataSetUserDef);
        }
    }


    private boolean isDeviceTrackingActive() {
        return mDeviceTracker;
    }


    // Menu options
    private final static int CMD_BACK = -1;
    private final static int CMD_DEVICE_TRACKER = 1;


    public void checkForRelocalization(final int statusInfo) {
        if (mCurrentStatusInfo == statusInfo) {
            return;
        }

        mCurrentStatusInfo = statusInfo;

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING) {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer.isRunning()) {
                mStatusDelayTimer.startTimer();
            }
        } else {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer.isRunning()) {
                mStatusDelayTimer.stopTimer();
            }

            if (mRelocalizationTimer.isRunning()) {
                mRelocalizationTimer.stopTimer();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSampleAppMessage.hide();
                }
            });
        }
    }


    private void clearSampleAppMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSampleAppMessage != null) {
                    mSampleAppMessage.hide();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Begin monitoring for Aruba Beacon-based Campaign events
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(LOGTAG, "requestCode: " + requestCode);
                //onPermissionGranted();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
