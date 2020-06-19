/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.camera2basic.UserDefinedTargets.UserDefinedTargets;
import com.example.android.camera2basic.data.BarcodeFormat;
import com.example.android.camera2basic.data.DataStore;
import com.example.android.camera2basic.data.ItemData;
import com.example.android.camera2basic.data.ZbarData;
import com.example.android.camera2basic.ui.AutoFitTextureView;
import com.example.android.camera2basic.ui.BarcodeRectDrawView;
import com.example.android.camera2basic.ui.ToastHelper;
import com.example.android.camera2basic.util.GlobalConstants;
import com.example.android.camera2basic.util.GraphicDecoder;
import com.example.android.camera2basic.util.ZBarDecoder;
import com.example.android.camera2basic.util.Zoom;
import com.yanzhenjie.zbar.Config;
import com.yanzhenjie.zbar.ImageScanner;
import com.yanzhenjie.zbar.Symbol;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback, GraphicDecoder.DecodeListener {
    private Dialog mBarcodeDialog;
    private ImageScanner mScanner;
    private List<BarcodeFormat> mFormats;
    private ArrayList<ZbarData> mZbarDataList;
    DataStore mDataStore;
    ArrayList<ItemData> mItemDataArrayList;
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    public float finger_spacing = 0;
    private float mZoom_level = 0;
    private TextView zoom1, zoom2, zoom3, bCodeTv, itemTv, locTv;
    CheckBox isFoundCv;
    private Button prevBtn, nextBtn, showBtn;

    private Bitmap mBitmap;
    private int[] mPixels;
    private byte[] mYUVFrameData;

    private int mWidth;
    private int mHeight;
    private RectF mClipRectRatio;
    String mResult = null;
    int mCount = 0;
    private static int imgCount = 0;

    private String mItemSize;
    private String mItemColor;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * An {@link BarcodeRectDrawView} for barcode preview.
     */
    private BarcodeRectDrawView mBarcodeRectDrawView;

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
            setupScanner();
            if (null != mCameraId) {
                initZoom();
                setZoomLevel();
                mZbarDataList = new ArrayList<ZbarData>();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    private ZBarDecoder mZBarDecoder;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    private int mCapCounter = 0;
    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //displayBarcodeOld(reader.acquireNextImage());
            //printBarcodeToConsole(reader.acquireNextImage());
            Image pImage = reader.acquireNextImage();
            if (mCapCounter == 7) {
                //mZbarDataList = new ArrayList<>();
                printBarcodeToConsole(pImage);
                mCapCounter = 0;
            } else {
                mCapCounter++;
            }
            pImage.close();
        }

    };
    private Zoom mZoom;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;


    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();//capture happens here at scanning
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };
    private TextView titleTV;
    private TextView valueTV;
    private int index;
    private TextView indexTV;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Surface mSurface;
    private int position = 0;
    private Toast mToast;
    private String mItemName;
    private String mItemLoc;
    private int [] mRatio;

    /* */

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        /*scan_btn = view.findViewById(R.id.scan_btn);
        scan_btn.setOnClickListener(this);*/
        zoom1 = view.findViewById(R.id.zoom_l1);
        zoom1.setOnClickListener(this);
        zoom2 = view.findViewById(R.id.zoom_l2);
        zoom2.setOnClickListener(this);
        zoom3 = view.findViewById(R.id.zoom_l3);
        zoom3.setOnClickListener(this);
        bCodeTv = view.findViewById(R.id.i_content_tv);
        itemTv = view.findViewById(R.id.i_title_tv);
        locTv = view.findViewById(R.id.i_loc_tv);
        isFoundCv = view.findViewById(R.id.item_found);
        prevBtn = view.findViewById(R.id.prev_btn);
        prevBtn.setOnClickListener(this);
        nextBtn = view.findViewById(R.id.next_btn);
        nextBtn.setOnClickListener(this);
        showBtn = view.findViewById(R.id.show_btn);
        showBtn.setOnClickListener(this);
        mTextureView = view.findViewById(R.id.texture);
        mBarcodeRectDrawView = view.findViewById(R.id.draw_rect_view);
        mZbarDataList = new ArrayList<ZbarData>();
        mDataStore = DataStore.getInstance();
        position = mDataStore.getCurPosition();
        setDataFromLocal();
    }

    private void setDataFromLocal() {
        mItemDataArrayList = mDataStore.getItemDataArrayList();
        /*if (mDataStore.getItemDataArrayList().size() < 1) {
            mItemDataArrayList = mDataStore.getItemDataArrayList();
        } else {
            mItemDataArrayList = mDataStore.getPendingItems();
        }*/

        if (mItemDataArrayList.size() > 1) {
            nextBtn.setText(getResources().getString(R.string.next));
        } else {
            nextBtn.setText(getResources().getString(R.string.skip));
        }

        setItemData(mDataStore.getCurPosition());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //mFile = new File(getExternalStorageDirectory() + "/pic31.jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        position = mDataStore.getCurPosition();
        setDataFromLocal();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable() && mCameraDevice != null) {
            setupScanner();
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        setZoomLevel();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setItemData(int pos) {
        mItemSize = mItemDataArrayList.get(pos).getItemSize();
        mItemColor = mItemDataArrayList.get(pos).getItemColor();
        mItemName = mItemDataArrayList.get(pos).getItemName();
        mItemLoc = mItemDataArrayList.get(pos).getItemLoc();
        mRatio = mItemDataArrayList.get(pos).getRatio();
        itemTv.setText(mItemName);
        locTv.setText(mItemLoc);
        bCodeTv.setText(mItemDataArrayList.get(pos).getItemContent());
        isFoundCv.setChecked(mItemDataArrayList.get(pos).isItemFound());
        if (mItemDataArrayList.get(pos).getItemContent() != null) {
            nextBtn.setText(getResources().getString(R.string.next));
        } else {
            nextBtn.setText(getResources().getString(R.string.skip));
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (null != mZoom) {
                mZoom = null;
            }
            mZoom_level = 0;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            mSurface = new Surface(texture);


            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mSurface);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                /*mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);*//**Focus set to center by handleFocus*/
                                // Flash is automatically enabled when necessary.
                                handleFocus();
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();

                                mCaptureRequest = mCaptureRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mCaptureRequest, null, null);
                                //mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                //       mCaptureCallback, mBackgroundHandler);
                                /**Added lockFocus here to implement continuous capture*/
                                lockFocus();
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
            /**TODO added for capturing aeach frame*/

            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

            /**end added for capture each frame*/
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
        //lockFocusForScan();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            /*mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);*//**Focus set to center by handleFocus*/
            handleFocus();
            if (mZoom == null) initZoom();
            mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mZoom.getCropRegion());
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mCaptureRequestBuilder.build(), null,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            /*captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);*//**Focus set to center by handleFocus*/
            handleFocus();
            if (mZoom == null) initZoom();
            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mZoom.getCropRegion());
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.e("Zoom", "mZoom_level: " + mZoom_level + " " + "mZoom: " + mZoom.getCropRegion());
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mZoom.setZoom(mCaptureRequestBuilder, mZoom_level);
            mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mZoom.getCropRegion());
            setAutoFlash(mCaptureRequestBuilder);
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            // keeping the camera preview.
            mPreviewRequest = mCaptureRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(mPreviewRequest, null,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /*case R.id.scan_btn:
                takePicture();
                break;*/
            case R.id.zoom_l1:
                zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
                zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                try {
                    zoom(0);
                } catch (CameraAccessException pE) {
                    pE.printStackTrace();
                }
                break;
            case R.id.zoom_l2:
                zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
                zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                try {
                    zoom(2);
                } catch (CameraAccessException pE) {
                    pE.printStackTrace();
                }
                break;
            case R.id.zoom_l3:
                zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
                zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
                try {
                    zoom(4);
                } catch (CameraAccessException pE) {
                    pE.printStackTrace();
                }
                break;
            case R.id.prev_btn:
                if (position > 0) {
                    position--;
                    mDataStore.setCurPosition(position);
                    setItemData(mDataStore.getCurPosition());
                }
                break;
            case R.id.next_btn:
                if (mZbarDataList.size() > 0 || mItemDataArrayList.get(mDataStore.getCurPosition()).isItemFound()) {
                    nextButtonClick();
                } else {
                    showDialog_SkipItem(mItemDataArrayList.get(mDataStore.getCurPosition()).getItemName());
                }
                break;
            case R.id.show_btn:
                showButtonClick();
                break;
        }
    }

    private void nextButtonClick() {
        setDataToList();
        if (position < mItemDataArrayList.size() - 1) {
            if (position < mItemDataArrayList.size() - 1) {
                position++;
                mDataStore.setCurPosition(position);
                setItemData(mDataStore.getCurPosition());
            }
        } else {
            Intent intent = new Intent(getContext(), HomeActivity.class);
            intent.putExtra("data", "data");
            startActivity(intent);
            getActivity().finish();
        }
        mZbarDataList.clear();
    }
    private void showButtonClick(){
        Intent intent = new Intent(getContext(), UserDefinedTargets.class);
        intent.putExtra("color", mItemColor);
        intent.putExtra("size", mItemSize);
        intent.putExtra("name", mItemName);
        intent.putExtra("loc", mItemLoc);
        intent.putExtra("ratio", mRatio);
        startActivity(intent);
       // getActivity().finish();
    }

    private void setDataToList() {
        ItemData itemData = mItemDataArrayList.get(mDataStore.getCurPosition());
        if (!itemData.isItemFound()) {
            if (mZbarDataList.size() > 0) {
                itemData.setItemContent(mZbarDataList.get(index).getmData());
                itemData.setItemFound(true);
            }
            setDataToLocalList(itemData);
            mDataStore.setItemDataArrayList(itemData);
            /*if (mDataStore.hasItemDataPickedList(itemData)) {
                mDataStore.setItemDataPickedList(itemData);
            } else {
                mDataStore.addItemDataPickedList(itemData);
            }*/
        }

    }

    private void setDataToLocalList(ItemData itemData) {
        for (int j = 0; j < mItemDataArrayList.size(); j++) {
            if (mItemDataArrayList.get(j).getId() == itemData.getId()) {
                mItemDataArrayList.set(j, itemData);
            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Process the image to crop the barcode and send for decoding
     */
    public void displayBarcodeOld(Image pImage) {
        ByteBuffer buffer = pImage.getPlanes()[0].getBuffer();

        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        Matrix matrix = new Matrix();

        Bitmap srcBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        /**crop the image in the centre*/
        mBitmap = Bitmap.createBitmap(
                srcBmp,
                (srcBmp.getWidth() / 2) - 500,
                (srcBmp.getHeight() / 2) - 250,
                1000,
                500, matrix,
                true
        );
        /**Calling deblur method to clean cropped image*/
        Bitmap dstBitmap = deBlur(mBitmap);
        if (dstBitmap != null) {
            mWidth = dstBitmap.getWidth();
            mHeight = dstBitmap.getHeight();

            mPixels = getBitmapPixels(dstBitmap);
            mYUVFrameData = getYUVFrameData(mPixels, mWidth, mHeight);
            if (mClipRectRatio == null) {
                mClipRectRatio = new RectF();
            }
            mClipRectRatio.set(0, 0, 1, 1);
            mZBarDecoder.decodeForResult(dstBitmap, mClipRectRatio, 999);
        }
        pImage.close();
    }

    /**
     * Process the image to decode
     */
    private void printBarcodeToConsole(Image pImage) {
        ByteBuffer buffer = pImage.getPlanes()[0].getBuffer();

        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        Matrix matrix = new Matrix();

        Bitmap srcBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Log.e("Density", "Density: " + srcBmp.getDensity() + " Width: " + srcBmp.getWidth() + " Height: " + srcBmp.getHeight());
        GlobalConstants.SCR_HEIGHT = (srcBmp.getHeight() / 2) - 250;
        GlobalConstants.SCR_WIDTH = (srcBmp.getWidth() / 2) - 500;
        GlobalConstants.SCR_HEIGHT_RATIO = GlobalConstants.REAL_SCR_HEIGHT / (float) srcBmp.getHeight();
        GlobalConstants.SCR_WIDTH_RATIO = GlobalConstants.REAL_SCR_WIDTH / (float) srcBmp.getWidth();
        /**crop the image in the centre*/
        mBitmap = Bitmap.createBitmap(
                srcBmp,
                (srcBmp.getWidth() / 2) - 500,
                (srcBmp.getHeight() / 2) - 250,
                1000,
                500, matrix,
                true
        );
        Mat srcMat = new Mat();
        Bitmap bmp32 = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, srcMat);
        Log.e("Density", "Density: " + mBitmap.getDensity() + " Width: " + mBitmap.getWidth() + " Height: " + mBitmap.getHeight());
        //Log.e("processZbar", "BEFORE");
        /**Calling Native method to process image and print barcode value to console*/
        String[] result = CvUtil.processZbar(srcMat);
        //Log.e("processZbar", "AFTER");
        mBarcodeRectDrawView.clearScreen();
        if (result != null && result.length > 0) {
            for (int i = 0; i < result.length; i += 3) {
                if (!result[i + 1].equals(mResult)) {
                    RectF rect = stringToRect(result[i + 2]);
                    mZbarDataList.add(new ZbarData(result[i + 1], result[i], rect));
                    if (rect.contains(GlobalConstants.REAL_SCR_WIDTH / 2, GlobalConstants.REAL_SCR_HEIGHT / 2)) {
                        setBarcodeData(result[i + 1]);
                    }

                    mBarcodeRectDrawView.setBarcodeRect(rect);
                }
                Log.e("processZbar", "Type: " + result[i] + " Data: " + result[i + 1] + " Loc: " + result[i + 2]);
            }
            //showDialog_BarcodeFound();
            //mBarcodeRectDrawView.setBarcodeRect(new RectF(400, 200, 800, 600));
        }
        pImage.close();
    }

    private void setBarcodeData(final String data) {
        getActivity().runOnUiThread(new Thread(new Runnable() {
            public void run() {
                bCodeTv.setText(data);
                nextBtn.setText(getResources().getString(R.string.next));
                showToastResult(mItemDataArrayList.get(mDataStore.getCurPosition()).getItemName() + " picked: " + data);
                //ToastHelper.showToast(getActivity(), mItemDataArrayList.get(position).getItemName() + " picked: " + data, ToastHelper.LENGTH_SHORT);

            }
        }));
    }

    private RectF stringToRect(String locData) {
        List<String> numbers = Arrays.asList(locData.split(","));
        List<Integer> numbersInt = new ArrayList<>();
        for (String number : numbers) {
            numbersInt.add(Integer.valueOf(number));
        }
        float l = ((numbersInt.get(0) + GlobalConstants.SCR_WIDTH) * GlobalConstants.SCR_WIDTH_RATIO) - 10;
        float t = ((numbersInt.get(1) + GlobalConstants.SCR_HEIGHT) * GlobalConstants.SCR_HEIGHT_RATIO) - 10;
        float r = ((numbersInt.get(2) + GlobalConstants.SCR_WIDTH) * GlobalConstants.SCR_WIDTH_RATIO) + 10;
        float b = ((numbersInt.get(3) + GlobalConstants.SCR_HEIGHT) * GlobalConstants.SCR_HEIGHT_RATIO) + 10;
        Log.e("Density", "Rect: Width: " + (r - l) + " Height: " + (b - t));
        Log.e("Density", " W: " + GlobalConstants.REAL_SCR_WIDTH + " H: " + GlobalConstants.REAL_SCR_HEIGHT);
        Log.e(TAG, "l: " + l + " t: " + t + " r:" + r + " b: " + b);
        return new RectF(l, t, r, b);
    }

    /**
     * Deblur and crop image to isolate barcode image
     */
    private Bitmap deBlur(Bitmap sourceBmp) {

        Mat srcMat = new Mat();
        Bitmap bmp32 = sourceBmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, srcMat);

        /**Calling Native method to process image. Mat returned to crop image*/
        Mat destMat = CvUtil.processMat(srcMat);

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(destMat.cols(), destMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(destMat, bmp);
            Log.e(TAG, "deBlur after matToBitmap");
            return bmp;
        } catch (CvException e) {
            Log.d("Exception deBlur", e.getMessage());
        }
        return null;
    }

    @Override
    /**Callback after decoding barcode*/
    public void decodeComplete(String result, int type, int quality, int requestCode) {
        if (result == null) return;
        if (result.equals(mResult)) {
            if (++mCount > 3) {//Show the result four times in a row (mainly filter dirty data, you can also customize rules according to the barcode type）
                if (quality < 10) {
                    ToastHelper.showToast(getActivity(), "[Types of" + type + "/accuracy00" + quality + "]" + result, ToastHelper.LENGTH_SHORT);
                } else if (quality < 100) {
                    ToastHelper.showToast(getActivity(), "[Types of" + type + "/accuracy0" + quality + "]" + result, ToastHelper.LENGTH_SHORT);
                } else {
                    ToastHelper.showToast(getActivity(), "[Types of" + type + "/accuracy" + quality + "]" + result, ToastHelper.LENGTH_SHORT);
                }
            }
        } else {
            mCount = 1;
            mResult = result;
            showDialog_OnBarcodeFound(result, type);
        }
        Log.d(TAG, getClass().getName() + "deBlur decodeComplete() -> " + mResult);
    }

    private int[] getBitmapPixels(Bitmap bitmap) {
        if (bitmap == null) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        //bitmap.recycle();
        return pixels;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    private void initZoom() {
        try {
            if (null == mCameraId) return;
            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            mZoom = new Zoom(characteristics);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e("Zoom", "mZoom_level: " + mZoom_level);
    }

    /**
     * updating zoom level
     */
    private void setZoomLevel() {
        if (mZoom_level == 0) {
            zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
            zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
            zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
        } else if (mZoom_level == 2) {
            zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
            zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
            zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
        } else {
            zoom3.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_press_bg));
            zoom2.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
            zoom1.setBackground(getActivity().getResources().getDrawable(R.drawable.circle_bg));
        }
    }

    /**
     * Setting zoom level
     */
    private void zoom(float zoomLevel) throws CameraAccessException {
        if (mZoom == null) initZoom();
        mZoom_level = zoomLevel;
        mZoom.setZoom(mCaptureRequestBuilder, mZoom_level);
        if (mCaptureSession != null) {
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
        }
    }

    /**
     * Setup scanner config
     */
    public void setupScanner() {
        mZBarDecoder = new ZBarDecoder(this, null);
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for (BarcodeFormat format : getFormats()) {
            mScanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }

    /**
     * Get Barcode formats
     */
    public Collection<BarcodeFormat> getFormats() {
        if (mFormats == null) {
            return BarcodeFormat.ALL_FORMATS;
        }
        return mFormats;
    }

    /**
     * Get YUV data from Frame
     */
    private byte[] getYUVFrameData(int[] pixels, int width, int height) {
        if (pixels == null) return null;

        int index = 0;
        int yIndex = 0;
        int R, G, B, Y, U, V;
        byte[] frameData = new byte[width * height];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                R = (pixels[index] & 0xff0000) >> 16;
                G = (pixels[index] & 0xff00) >> 8;
                B = (pixels[index] & 0xff);

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
//                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
//                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                frameData[yIndex++] = (byte) (Math.max(0, Math.min(Y, 255)));
//                if (j % 2 == 0 && index % 2 == 0) {
//                    yuv420sp[uvIndex++] = (byte)(Math.max(0, Math.min(U, 255)));
//                    yuv420sp[uvIndex++] = (byte)(Math.max(0, Math.min(V, 255)));
//                }
                index++;
            }
        }
        return frameData;
    }

    /**
     * Dialog to show barcode and type
     */
    private void showDialog_OnBarcodeFound(final String barcodeData, final int type) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //onPause();
                mBarcodeDialog = new Dialog(getActivity());
                mBarcodeDialog.setContentView(R.layout.barcode_dialog);
                TextView titleTV = mBarcodeDialog.findViewById(R.id.title_tv);
                titleTV.setText(mZBarDecoder.getBarcodeType(type));
                TextView valueTV = mBarcodeDialog.findViewById(R.id.result_tv);
                valueTV.setText(barcodeData);
                Button nextBtn = mBarcodeDialog.findViewById(R.id.ok_btn);
                Button rescanBtn = mBarcodeDialog.findViewById(R.id.rescan_btn);
                rescanBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBarcodeDialog.dismiss();
                        Toast.makeText(getActivity(), "Rescaning!", Toast.LENGTH_SHORT).show();
                        mResult = null;
                        //onResume();
                    }
                });
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBarcodeDialog.dismiss();
                        Toast.makeText(getActivity(), "This is where you call your API with data from Barcode!", Toast.LENGTH_SHORT).show();
                        mResult = null;
                        //onResume();
                    }
                });
                mBarcodeDialog.show();
            }
        });
    }

    /**
     * Dialog to show barcode and type
     */
    private void showDialog_BarcodeFound() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //onPause();
                if (mZbarDataList.get(index).getmData().equals(mResult)) return;
                if (mBarcodeDialog != null && mBarcodeDialog.isShowing()) {
                    mBarcodeDialog.dismiss();
                }
                index = 0;
                mResult = mZbarDataList.get(index).getmData();
                mBarcodeDialog = new Dialog(getActivity());
                mBarcodeDialog.setContentView(R.layout.barcode_dialog);
                titleTV = mBarcodeDialog.findViewById(R.id.title_tv);
                titleTV.setText(mZbarDataList.get(index).getmType());
                valueTV = mBarcodeDialog.findViewById(R.id.result_tv);
                valueTV.setText(mResult);
                indexTV = mBarcodeDialog.findViewById(R.id.count_tv);
                indexTV.setText((index + 1) + "/" + mZbarDataList.size());
                Button nextBtn = mBarcodeDialog.findViewById(R.id.nxt_btn);
                Button rescanBtn = mBarcodeDialog.findViewById(R.id.rescan_btn);
                Button okBtn = mBarcodeDialog.findViewById(R.id.ok_btn);
                rescanBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBarcodeDialog.dismiss();
                        Toast.makeText(getActivity(), "Rescaning!", Toast.LENGTH_SHORT).show();
                        mResult = null;
                        mZbarDataList = new ArrayList<>();
                    }
                });
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mZbarDataList.size() > 1 && index < mZbarDataList.size() - 1) {
                            index++;
                        } else {
                            index = 0;
                        }
                        titleTV.setText(mZbarDataList.get(index).getmType());
                        valueTV.setText(mZbarDataList.get(index).getmData());
                        indexTV.setText((index + 1) + "/" + mZbarDataList.size());
                        //mBarcodeDialog.dismiss();
                    }
                });
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mZbarDataList = new ArrayList<>();
                        mResult = null;
                        mBarcodeDialog.dismiss();
                        mBarcodeRectDrawView.clearScreen();
                        //TODO: Add what you are going to do with this data
                    }
                });
                mBarcodeDialog.show();
            }
        });
    }

    /**
     * Dialog to show skip item
     */
    private void showDialog_SkipItem(String item) {
        mBarcodeDialog = new Dialog(getActivity());
        mBarcodeDialog.setContentView(R.layout.skip_dialog);
        TextView titleTV = mBarcodeDialog.findViewById(R.id.title_tv);
        titleTV.setText(getResources().getString(R.string._skip_item) + " " + item + " ?");
        TextView yesTv = mBarcodeDialog.findViewById(R.id.yes_tv);
        TextView noTv = mBarcodeDialog.findViewById(R.id.no_tv);
        yesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nextButtonClick();
                        mBarcodeDialog.dismiss();
                    }
                });
            }
        });
        noTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBarcodeDialog.dismiss();
            }
        });
        mBarcodeDialog.show();
    }

    /**
     * Get width of screen
     */
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    /**
     * Get height of screen
     */
    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /**
     * Focus camera on the centre of the screen
     */
    public void handleFocus() {

        float x = getScreenWidth() / 2;
        float y = getScreenHeight() / 2;

        Rect touchRect = new Rect(
                (int) (x - 300),
                (int) (y - 200),
                (int) (x + 300),
                (int) (y + 200));

        if (mCameraId == null) return;
        Activity activity = getActivity();
        CameraManager cm = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics cc = null;
        try {
            cc = cm.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        MeteringRectangle focusArea = new MeteringRectangle(touchRect, MeteringRectangle.METERING_WEIGHT_DONT_CARE);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,
                new MeteringRectangle[]{focusArea});
        mCaptureRequestBuilder
                .set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusArea});
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void showToastResult(String msg) {

        if (mToast != null) {
            mToast.cancel();
        }
        // Get your custom_toast.xml ayout
        LayoutInflater inflater = getLayoutInflater();

        View layout = inflater.inflate(R.layout.toast_result,
                (ViewGroup) getActivity().findViewById(R.id.custom_toast_layout_id));

        // set a message
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);

        // Toast...
        mToast = new Toast(getActivity());
        mToast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 120);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.setView(layout);
        mToast.show();
    }

}
