package com.example.administrator.testz.activity;



        import android.os.Bundle;
        import android.os.CountDownTimer;
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.appcompat.app.AppCompatActivity;
        import android.util.Log;
        import android.view.View;
        import android.widget.Toast;


        import com.example.administrator.testz.DJISampleApplication;
        import com.example.administrator.testz.R;
        import com.example.administrator.testz.utils.ToastUtils;

        import dji.common.error.DJIError;
        import dji.common.flightcontroller.FlightControllerState;
        import dji.common.mission.hotpoint.HotpointMission;
        import dji.common.mission.hotpoint.HotpointMissionEvent;
        import dji.common.model.LocationCoordinate2D;
        import dji.common.util.CommonCallbacks;
        import dji.sdk.base.BaseProduct;
        import dji.sdk.flightcontroller.FlightController;
        import dji.sdk.mission.hotpoint.HotpointMissionOperator;
        import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener;
        import dji.sdk.products.Aircraft;
        import dji.sdk.sdkmanager.DJISDKManager;

        import static dji.common.mission.hotpoint.HotpointHeading.ALONG_CIRCLE_LOOKING_FORWARDS;
        import static dji.common.mission.hotpoint.HotpointStartPoint.EAST;

/**
 * hotPoint 启动的条件是，先要启动飞机，才能使用hotPoint去执行任务
 * <p>
 * hotPoint的执行过程是，先拿到operator，然后设置mission任务，然后start执行任务
 * <p>
 * 与wayPoint不同的是，wayPoit是先upload到aircraft，然后再执行mission任务，无需takeoff
 * <p>
 * 1、先take off
 * 2、hotpoint mission A （定时一圈，时间到结束mission）
 * 3、hotpoint mission B （定时一圈，时间到结束mission）
 * 4、startGoHome 返回起飞点
 */
public class HotPointActivity extends AppCompatActivity {

    FlightController mFlightController;
    HotpointMissionOperator operator;

    LocationCoordinate2D locationCoordinate2D_A = new LocationCoordinate2D(30.278154, 120.123897);
    LocationCoordinate2D locationCoordinate2D_B = new LocationCoordinate2D(30.278501, 120.124471);

    boolean isConnect;

    private String currentMissionStr = MISSION_A;

    private static final String MISSION_A = "A";
    private static final String MISSION_B = "B";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_point);
        BaseProduct baseProduct = DJISampleApplication.getProductInstance();
        operator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();

        if (baseProduct != null && baseProduct.isConnected()) {
            if (baseProduct instanceof Aircraft) {
                isConnect = true;
                mFlightController = ((Aircraft) baseProduct).getFlightController();
                //设置飞行控制的回调，可以拿到当前无人机的经纬度
                mFlightController.setStateCallback(
                        new FlightControllerState.Callback() {
                            @Override
                            public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                                double droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                                double droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();

                                Log.i("tag", "latitude = " + droneLocationLat + "  longitude" + droneLocationLng);

                            }
                        });
            }
        } else {
            ToastUtils.setResultToToast("disConnect");
        }

        registerHotPointListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterListener();
    }

    public void onClick(View view) {
        if (!isConnect) {
            ToastUtils.setResultToToast("disConnect");
            return;
        }
        switch (view.getId()) {
            case R.id.hot_take_off:
                takeOff();
                break;
            case R.id.start_hot_point:
                startPoint();
                break;
            case R.id.stop_hot_point:
                stopPoint();
                break;
            case R.id.twice_hot_point:
                doTwiceHotPoint();
                break;
            case R.id.start_hot_point_A:
                startPoint(locationCoordinate2D_A);
                currentMissionStr = MISSION_A;
                break;
            case R.id.start_hot_point_B:
                startPoint(locationCoordinate2D_B);
                break;
        }
    }


    public void doTwiceHotPoint() {
        startPoint(locationCoordinate2D_A);
    }

    public void takeOff() {
        mFlightController.startTakeoff(
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Toast.makeText(HotPointActivity.this, djiError.getDescription(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HotPointActivity.this, "Take off Success", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * 注册监听
     */
    private void registerHotPointListener() {
        operator.addListener(new HotpointMissionOperatorListener() {
            @Override
            public void onExecutionUpdate(@NonNull HotpointMissionEvent hotpointMissionEvent) {
                ToastUtils.setResultToToast(hotpointMissionEvent.getCurrentState().getName());

                if (hotpointMissionEvent.getCurrentState().getName().equals("EXECUTING")) {
                    // 开始倒计时
                    switch (currentMissionStr) {
                        case MISSION_A:
                            countDownTimerA.cancel();
                            countDownTimerA.start();
                            break;

                        case MISSION_B:
                            countDownTimerB.cancel();
                            countDownTimerB.start();
                            break;
                    }

                }
            }

            @Override
            public void onExecutionStart() {
                ToastUtils.setResultToToast("onExecutionStart");
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("onExecutionFinish");
                    switch (currentMissionStr) {
                        case MISSION_A:
                            break;

                        case MISSION_B:
                            goBackHome();
                            break;
                    }
                } else {
                    ToastUtils.setResultToToast("onExecutionFinish, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    private void unRegisterListener() {
        operator.removeAllListeners();
    }

    /**
     * CountDownTimer 实现倒计时
     */
    private CountDownTimer countDownTimerA = new CountDownTimer(360 / 20 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            stopPointA();
        }
    };

    /**
     * CountDownTimer 实现倒计时
     */
    private CountDownTimer countDownTimerB = new CountDownTimer(360 / 20 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            stopPointB();
        }
    };

    /**
     * CountDownTimer 实现倒计时
     */
    private CountDownTimer countDownTimer2Delay = new CountDownTimer(5 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            // 延迟一段时间去执行B点的hotpoint, 实际项目中需要采用回调或者轮询检测的方式（推测是等待硬件初始化完毕才能执行下一个任务，不然会任务紊乱）
            startPoint(locationCoordinate2D_B);
            currentMissionStr = MISSION_B;
        }
    };

    /**
     * 设置HotpointMission任务时，以下参数必须要设置，不然会报failure错误
     * 设置范围有限定，以下参数都会引起飞行器限制飞行，可以参考文档合理设置范围
     */
    public void startPoint() {
        HotpointMission hotpointMission = new HotpointMission();
        //设置海拔
        hotpointMission.setAltitude(30);
        //设置顺时针方向
        hotpointMission.setClockwise(true);
        //设置角速率
        hotpointMission.setAngularVelocity(20);
        //设置范围
        hotpointMission.setRadius(10);
        hotpointMission.setHeading(ALONG_CIRCLE_LOOKING_FORWARDS);
        //当前热点的经纬度
        hotpointMission.setHotpoint(new LocationCoordinate2D(30.278154, 120.123897));
        //起飞的方向
        hotpointMission.setStartPoint(EAST);

        operator.startMission(hotpointMission, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission start successfully!");
                } else {
                    ToastUtils.setResultToToast("Mission start failed, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    public void startPoint(LocationCoordinate2D locationCoordinate2D) {
        HotpointMission hotpointMission = new HotpointMission();
        //设置海拔
        hotpointMission.setAltitude(10);
        //设置顺时针方向
        hotpointMission.setClockwise(true);
        //设置角速率
        hotpointMission.setAngularVelocity(20);
        //设置范围
        hotpointMission.setRadius(10);
        hotpointMission.setHeading(ALONG_CIRCLE_LOOKING_FORWARDS);
        //当前热点的经纬度
        hotpointMission.setHotpoint(locationCoordinate2D);
        //起飞的方向
        hotpointMission.setStartPoint(EAST);

        operator.startMission(hotpointMission, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission start successfully!");
                } else {
                    ToastUtils.setResultToToast("Mission start failed, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    public void stopPoint() {
        operator.stop(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission stop successfully!");
                    countDownTimer2Delay.start();
                } else {
                    ToastUtils.setResultToToast("Mission stop failed, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    public void stopPointA() {
        operator.stop(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission A stop successfully!");
                    countDownTimer2Delay.start();
                } else {
                    ToastUtils.setResultToToast("Mission A stop failed, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    public void stopPointB() {
        operator.stop(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission B stop successfully!");
                } else {
                    ToastUtils.setResultToToast("Mission B stop failed, error: " + djiError.getDescription() + " retrying...");
                }
            }
        });
    }

    /**
     * 控制飞机返航
     */
    private void goBackHome() {
        mFlightController.startGoHome(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("go home success!");
                } else {
                    ToastUtils.setResultToToast("go home failed, error: " + djiError.getDescription());
                }
            }
        });
    }
}
