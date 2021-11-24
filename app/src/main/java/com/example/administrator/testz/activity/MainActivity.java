package com.example.administrator.testz.activity;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.example.administrator.testz.DJISampleApplication;
import com.example.administrator.testz.utils.ModuleVerificationUtil;
import com.example.administrator.testz.R;
import com.example.administrator.testz.utils.ToastUtils;
import com.example.administrator.testz.utils.Wgs84ToGcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;

/**
 * 实现waypiont和图传现实功能
 */
public class MainActivity extends FragmentActivity implements View.OnClickListener, OnMapClickListener, TextureView.SurfaceTextureListener {

    protected static final String TAG = "MainActivity";

    private MapView mapView;
    private AMap aMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop;
    private Button rtmp;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataCallBack = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList;

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    /**
     * 云台控制
     */
    private Timer timer;
    private GimbalRotateTimerTask gimbalRotationTimerTask;
    private LiveStreamManager.OnLiveChangeListener listener;

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        removeListener();
        if (timer != null) {
            if (gimbalRotationTimerTask != null) {
                gimbalRotationTimerTask.cancel();
            }
            timer.cancel();
            timer.purge();
            gimbalRotationTimerTask = null;
            timer = null;
        }
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        rtmp = findViewById(R.id.rtmp_video);
        Button rise = findViewById(R.id.rise);
        Button descend = findViewById(R.id.descend);
        Button gimbal_stop = findViewById(R.id.gimbal_stop);

        // init mVideoSurface
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        listener = i -> {
            //wang
            ToastUtils.setResultToToast("status changed : " + i);
        };


        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        rise.setOnClickListener(this);
        descend.setOnClickListener(this);
        gimbal_stop.setOnClickListener(this);
        rtmp.setOnClickListener(this);
    }

    private void initMapView() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        //将84坐标转换成高德地图坐标
        double[] gcWgs = Wgs84ToGcUtils.gcj02_To_Gps84(32.094341, 118.8872644);
        LatLng latLng = new LatLng(gcWgs[0], gcWgs[1]);
        //lat 纬度  lon经度
        LatLng nanjing = new LatLng(32.094341, 118.8872644);
        aMap.addMarker(new MarkerOptions().position(nanjing).title("Marker in NanJing"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(nanjing));
    }

    private static class GimbalRotateTimerTask extends TimerTask {
        float pitchValue;

        GimbalRotateTimerTask(float pitchValue) {
            super();
            this.pitchValue = pitchValue;
        }

        @Override
        public void run() {
            if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
                DJISampleApplication.getProductInstance().getGimbal().
                        rotate(new Rotation.Builder().pitch(pitchValue)
                                .mode(RotationMode.SPEED)
                                .yaw(Rotation.NO_ROTATION)
                                .roll(Rotation.NO_ROTATION)
                                .time(0)
                                .build(), new CommonCallbacks.CompletionCallback() {

                            @Override
                            public void onResult(DJIError error) {

                            }
                        });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_point);
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISampleApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        waypointList = new ArrayList<>();

        initMapView();
        initUI();
        addListener();

        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    private void startStream() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            //wang
            Toast.makeText(getApplicationContext(), "already started!", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {

                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl("rtmp://live.speedchina.cn/fly/flytest?auth_key=1637565588-0-0-d0fcf8eb6e3bf85f92e6fc482269743a");
                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                DJISDKManager.getInstance().getLiveStreamManager()
                        .setVideoEncodingEnabled(true);
                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();

                //wang
                ToastUtils.setResultToToast("startLive:" + result +
                        "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                        "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled() +
                        "\n 视频帧率为：" + DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoFps() + "fps");
            }
        }.start();

    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            //    ToastUtils.setResultToToast("No live stream manager!");
            //wang
            Toast.makeText(getApplicationContext(), "No live stream manager!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange() {
        initFlightController();
        loginAccount();
    }

    private void initPreviewer() {

        BaseProduct product = DJISampleApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
//            ("is not connect");
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataCallBack);
            }
        }
    }

    private void loginAccount() {

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });

        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isLiveStreamManagerOn()) {
            DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isLiveStreamManagerOn()) {
            DJISDKManager.getInstance().getLiveStreamManager().unregisterListener(listener);
        }
    }


    /**
     * WaypointMissionOperatorListener 实现当前任务的执行情况，监听任务的执行进度
     */
    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
            if (waypointMissionDownloadEvent.getProgress() != null
                    && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded) {
                ToastUtils.setResultToToast("Download successful!");
            }
        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent) {
            if (waypointMissionUploadEvent.getProgress() != null
                    && waypointMissionUploadEvent.getProgress().isSummaryUploaded) {
                ToastUtils.setResultToToast("Upload successful!");
            }
        }

        //onExecutionUpdate，这个方法会在waypointmission过程中多次回调，并刷新任务的信息，这些信息对于区分采集数据至关重要。每一个航点任务执行都会回调
        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
            int index = waypointMissionExecutionEvent.getProgress().targetWaypointIndex;
            //isWaypointReached到达一个航点后为yes 当完成航点动作（如悬停）后，变成no 且targetWaypointIndex+1
            boolean state = waypointMissionExecutionEvent.getProgress().isWaypointReached;
            // executeState 是一个枚举类型，显示无人机具体动作或者执行状态
            WaypointMissionExecuteState waypointMissionExecuteState = waypointMissionExecutionEvent.getProgress().executeState;

            String msg = (waypointMissionExecutionEvent.getPreviousState() == null
                    ? ""
                    : waypointMissionExecutionEvent.getPreviousState().getName())
                    + ", " + waypointMissionExecutionEvent.getCurrentState().getName()
                    + (waypointMissionExecutionEvent.getProgress() == null
                    ? ""
                    : waypointMissionExecutionEvent.getProgress().targetWaypointIndex);
            ToastUtils.setResultToToast(msg);
        }

        @Override
        public void onExecutionStart() {
            //所有航点任务第一个开始执行
            ToastUtils.setResultToToast("Execution started!");
        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            //所有航点任务执行成功
            ToastUtils.setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd) {
            markWaypoint(point);
            //将高德地图坐标转换成大疆无人机的84坐标
            double[] pointWgs = Wgs84ToGcUtils.gcj02_To_Gps84(point.longitude, point.latitude);
//            Waypoint mWaypoint = new Waypoint(pointWgs[1], pointWgs[0], altitude);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            } else {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        } else {
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation() {

        //将无人机的经纬度坐标转换成高德地图坐标
        double[] wgsGcj = Wgs84ToGcUtils.gps84_To_Gcj02(droneLocationLat, droneLocationLng);
        LatLng gcjPos = new LatLng(wgsGcj[0], wgsGcj[1]);
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        CoordinateConverter converter = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(pos);
// 执行转换操作
        LatLng desLatLng = converter.convert();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point) {
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate: {
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add: {
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.config: {
                showSettingDialog();
                break;
            }
            case R.id.upload: {
                uploadWayPointMission();
                break;
            }
            case R.id.start: {
                startWaypointMission();
                break;
            }
            case R.id.stop: {
                stopWaypointMission();
                break;
            }
            case R.id.rise:
                if (timer == null) {
                    timer = new Timer();
                    gimbalRotationTimerTask = new GimbalRotateTimerTask(10);
                    timer.schedule(gimbalRotationTimerTask, 0, 100);
                }
                break;
            case R.id.descend:
                if (timer == null) {
                    timer = new Timer();
                    gimbalRotationTimerTask = new GimbalRotateTimerTask(-10);
                    timer.schedule(gimbalRotationTimerTask, 0, 100);
                }
                break;
            case R.id.gimbal_stop:
                if (timer != null) {
                    if (gimbalRotationTimerTask != null) {
                        gimbalRotationTimerTask.cancel();
                    }
                    timer.cancel();
                    timer.purge();
                    gimbalRotationTimerTask = null;
                    timer = null;
                }

                if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
                    DJISampleApplication.getProductInstance().getGimbal().
                            rotate(null, new CommonCallbacks.CompletionCallback() {

                                @Override
                                public void onResult(DJIError error) {

                                }
                            });
                }
                break;
            case R.id.rtmp_video:
                startStream();
                break;
            default:
                break;
        }
    }

    private void cameraUpdate() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd() {
        if (!isAdd) {
            isAdd = true;
            add.setText("Exit");
        } else {
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog() {
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        EditText wpAltitude_TV = wayPointSettings.findViewById(R.id.altitude);
        EditText home_altitude = wayPointSettings.findViewById(R.id.home_altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed) {
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone) {
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding) {
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("完成", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        String home_Altitude = home_altitude.getText().toString();
                        int homeAltitude = Integer.parseInt(nulltoIntegerDefalt(home_Altitude));
                        //设置返航高度
                        if (mFlightController != null) {
                            mFlightController.setMaxFlightHeight(homeAltitude, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                        }
                        Log.e(TAG, "altitude " + altitude);
                        Log.e(TAG, "speed " + mSpeed);
                        Log.e(TAG, "mFinishedAction " + mFinishedAction);
                        Log.e(TAG, "mHeadingMode " + mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();

    }

    String nulltoIntegerDefalt(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 设置航点任务
     */
    private void configWayPointMission() {

        if (waypointMissionBuilder == null) {

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0) {
            Random randomGenerator = new Random(System.currentTimeMillis());
            //进行随机的航点任务，比如拍照，停留，旋转，录像，调整云台
//            final int randomNumber = randomGenerator.nextInt() % 6;

            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                altitude = altitude + 5;
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
                switch (i) {
                    case 0:
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STAY, 1000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                -90));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1000));
                        break;
                    case 1:
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STAY, 1000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                0));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1000));
                        break;
                    case 2:
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STAY, 1000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                -45));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1000));
                        break;
                    case 3:
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STAY, 1000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                0));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.START_RECORD, 1000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STOP_RECORD, 1000));
                        break;
//                    case 4:
//                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
//                                randomGenerator.nextInt() % 45 - 45));
//                        break;
//                    case 5:
//                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,
//                                randomGenerator.nextInt() % 180));
//                        break;
                    default:
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.STAY, 2000));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                -20));
                        waypointMissionBuilder.getWaypointList().get(i).addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1000));
                        break;
                }
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }

    private void uploadWayPointMission() {

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission() {

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
