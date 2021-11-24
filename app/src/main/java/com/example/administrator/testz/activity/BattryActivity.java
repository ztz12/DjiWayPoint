package com.example.administrator.testz.activity;




        import android.os.Bundle;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.util.Date;
        import java.util.Timer;
        import java.util.TimerTask;

        import androidx.appcompat.app.AppCompatActivity;

        import com.example.administrator.testz.DJISampleApplication;
        import com.example.administrator.testz.R;

        import dji.common.battery.BatteryState;
        import dji.common.error.DJIError;
        import dji.common.util.CommonCallbacks;
        import dji.sdk.battery.Battery;

public class BattryActivity extends AppCompatActivity implements BatteryState.Callback {
    /*
     * view
     */
    private TextView createTimeTv;
    private TextView createFreTv;

    private TextView batteryFullChargeCapacityTv;
    private TextView batteryChargeRemainingTv;
    private TextView batteryChargeRemainingInPercentTv;
    private TextView batteryIsBeingChargedTv;
    private TextView batterySelfHeatingStateTv;
    private TextView batteryVoltageTv;
    private TextView batteryCurrentTv;
    private TextView batteryLifetimeRemainingTv;
    private TextView batteryTemperatureTv;
    private TextView batteryNumberOfDischargesTv;
    private TextView batterySnTv;

    /*
        流程参数
     */
    private Date createTime = new Date();//当前更新时间

    //读取电池信息状态
    private Timer timer = new Timer();
    private Battery mBattery;
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
                BattryActivity.this.mBattery = DJISampleApplication.getBatteryInstance();
            if (mBattery != null) {
                mBattery.setStateCallback(BattryActivity.this);
                mBattery.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                    @Override
                    public void onSuccess(final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                batterySnTv.setText("SN:" + s);
                            }
                        });
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
                task.cancel();
                timer.cancel();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showbattery);

        createFreTv = findViewById(R.id.tv_create_fre);
        createTimeTv = findViewById(R.id.tv_create_time);
        batteryFullChargeCapacityTv = findViewById(R.id.bfcctv);
        batteryChargeRemainingTv = findViewById(R.id.bcrtv);
        batteryChargeRemainingInPercentTv = findViewById(R.id.bcriptv);
        batteryIsBeingChargedTv = findViewById(R.id.bibctv);
        batterySelfHeatingStateTv = findViewById(R.id.bshstv);
        batteryVoltageTv = findViewById(R.id.bvtv);
        batteryCurrentTv = findViewById(R.id.bctv);
        batteryLifetimeRemainingTv = findViewById(R.id.blrtv);
        batteryTemperatureTv = findViewById(R.id.bttv);
        batteryNumberOfDischargesTv = findViewById(R.id.bnodtv);
        batterySnTv = findViewById(R.id.tv_sn);
        //获取时间
        timer.schedule(task, 100, 1000);
    }

    @Override
    public void onUpdate(final BatteryState batteryState) {
        if (batteryState == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //刷新当前更新时间
                Date nowDate = new Date();
                createTimeTv.setText("更新时间: " + android.text.format.DateFormat.format("yyyy年MM月dd日,kk:mm:ss", nowDate));
                long fre = nowDate.getTime() - createTime.getTime();
                createFreTv.setText("刷新频率:" + (fre / 1000) + "s/次");
                createTime = nowDate;
                //更新状态
                try {
                    //总能量
                    batteryFullChargeCapacityTv.setText("总电量:" + batteryState.getFullChargeCapacity() + " mAh");
                    //剩余能量
                    batteryChargeRemainingTv.setText("剩余电量:" + batteryState.getChargeRemaining() + " mAh");
                    //剩余电量百分比
                    batteryChargeRemainingInPercentTv.setText("剩余电量百分比:" + batteryState.getChargeRemainingInPercent() + " %");
                    //是否正在充电
                    batteryIsBeingChargedTv.setText("是否正在充电:" + (batteryState.isBeingCharged() ? "是" : "否"));
                    //自发热状态
                    if (batteryState.getSelfHeatingState() != null) {
                        batterySelfHeatingStateTv.setText("自发热状态:" + batteryState.getSelfHeatingState().value());
                    }
                    //电压
                    batteryVoltageTv.setText("当前电压:" + batteryState.getVoltage() + " mV");
                    //当前状态
                    batteryCurrentTv.setText("当前状态:" + (batteryState.getCurrent() > 0 ? "正在充电" : "正在放电"));
                    //剩余寿命
                    batteryLifetimeRemainingTv.setText("剩余寿命:" + batteryState.getLifetimeRemaining() + "%");
                    //电池温度
                    batteryTemperatureTv.setText("电池温度:" + batteryState.getTemperature() + " ℃");
                    //总放电次数
                    batteryNumberOfDischargesTv.setText("总放电次数:" + batteryState.getNumberOfDischarges());
                } catch (Exception e) {
                    Toast.makeText(BattryActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
