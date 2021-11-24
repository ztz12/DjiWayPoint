package com.example.administrator.testz.view;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.administrator.testz.DJISampleApplication;
import com.example.administrator.testz.R;
import com.example.administrator.testz.activity.TestActivity;
import com.example.administrator.testz.utils.ToastUtils;
import com.example.administrator.testz.utils.ViewWrapper;
import com.example.administrator.testz.activity.MainDjiActivity;
import com.example.administrator.testz.utils.GeneralUtils;
import com.squareup.otto.Subscribe;

import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

/**
 * Created by dji on 15/12/18.
 */
public class MainContent extends RelativeLayout {

    public static final String TAG = MainContent.class.getName();
    private static BluetoothProductConnector connector = null;
    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private TextView mTextModelAvailable;
    private Button mBtnOpen;
    private Button mBtnBluetooth;
    //    private ViewWrapper componentList = new ViewWrapper(new (getContext()), R.string.activity_component_list);
    private ViewWrapper bluetoothView;
    private EditText mBridgeModeEditText;
    private Handler mHandler;
    private Handler mHandlerUI;
    private HandlerThread mHandlerThread = new HandlerThread("Bluetooth");

    private BaseProduct mProduct;
    private DJIKey firmwareKey;
    private KeyListener firmwareVersionUpdater;
    private boolean hasStartedFirmVersionListener = false;
    private AtomicBoolean hasAppActivationListenerStarted = new AtomicBoolean(false);
    private static final int MSG_UPDATE_BLUETOOTH_CONNECTOR = 0;
    private static final int MSG_INFORM_ACTIVATION = 1;
    private static final int ACTIVATION_DALAY_TIME = 1000;
    private AppActivationState.AppActivationStateListener appActivationStateListener;

    public MainContent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        DJISampleApplication.getEventBus().register(this);
        initUI();
    }

    private void initUI() {
        Log.v(TAG, "initUI");

        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextModelAvailable = (TextView) findViewById(R.id.text_model_available);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBridgeModeEditText = (EditText) findViewById(R.id.edittext_bridge_ip);
        mBtnBluetooth = (Button) findViewById(R.id.btn_bluetooth);
        //mBtnBluetooth.setEnabled(false);

        mBtnOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // BattryActivity
                Intent intent = new Intent(getContext(), TestActivity.class);
                getContext().startActivity(intent);
                if (GeneralUtils.isFastDoubleClick()) {
                    return;
                }
                //intent
                //  DJISampleApplication.getEventBus().post(componentList);
            }
        });
        mBtnBluetooth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            /*    if (GeneralUtils.isFastDoubleClick()) {
                    return;
                }
                if (DJISampleApplication.getBluetoothProductConnector() == null) {
                    ToastUtils.setResultToToast("pls wait the sdk initiation finished");
                    return;
                }
                bluetoothView =
                        new ViewWrapper(new BluetoothView(getContext()), R.string.component_listview_bluetooth);
                DJISampleApplication.getEventBus().post(bluetoothView);*/
            }
        });
        mBridgeModeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event != null && event.isShiftPressed()) {
                        return false;
                    } else {
                        // the user is done typing.
                        handleBridgeIPTextChange();
                    }
                }
                return false; // pass on to other listeners.
            }
        });
        mBridgeModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    // remove new line characcter
                    final String currentText = mBridgeModeEditText.getText().toString();
                    mBridgeModeEditText.setText(currentText.substring(0, currentText.indexOf('\n')));
                    handleBridgeIPTextChange();
                }
            }
        });
        ((TextView) findViewById(R.id.text_version)).setText(getResources().getString(R.string.sdk_version,
                DJISDKManager.getInstance()
                        .getSDKVersion()));
    }

    private void handleBridgeIPTextChange() {
        // the user is done typing.
        final String bridgeIP = mBridgeModeEditText.getText().toString();
        DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
        if (!TextUtils.isEmpty(bridgeIP)) {
            ToastUtils.setResultToToast("BridgeMode ON!\nIP: " + bridgeIP);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(TAG, "Comes into the onAttachedToWindow");
        refreshSDKRelativeUI();

        mHandlerThread.start();
        final long currentTime = System.currentTimeMillis();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_BLUETOOTH_CONNECTOR:
                        //connected = DJISampleApplication.getBluetoothConnectStatus();
                        connector = DJISampleApplication.getBluetoothProductConnector();

                        if (connector != null) {
                            mBtnBluetooth.post(new Runnable() {
                                @Override
                                public void run() {
                                    mBtnBluetooth.setEnabled(true);
                                }
                            });
                            return;
                        } else if ((System.currentTimeMillis() - currentTime) >= 5000) {
//                            DialogUtils.showDialog(getContext(),
//                                    "Fetch Connector failed, reboot if you want to connect the Bluetooth");
                            return;
                        } else if (connector == null) {
                            sendDelayMsg(0, MSG_UPDATE_BLUETOOTH_CONNECTOR);
                        }
                        break;
                    case MSG_INFORM_ACTIVATION:
                        loginToActivationIfNeeded();
                        break;
                }
            }
        };
        mHandlerUI = new Handler(Looper.getMainLooper());
        super.onAttachedToWindow();
    }

    private void sendDelayMsg(int msg, long delayMillis) {
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(msg, delayMillis);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeFirmwareVersionListener();
        removeAppActivationListenerIfNeeded();
        mHandler.removeCallbacksAndMessages(null);
        mHandlerUI.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mHandlerThread.quitSafely();
        } else {
            mHandlerThread.quit();
        }
        mHandlerUI = null;
        mHandler = null;
        super.onDetachedFromWindow();
    }

    private void updateVersion() {
        String version = null;
        if (mProduct != null) {
            version = DJISampleApplication.getProductInstance().getFirmwarePackageVersion();
        }

        if (TextUtils.isEmpty(version)) {
            mTextModelAvailable.setText("Firmware version:N/A"); //Firmware version:
        } else {
            mTextModelAvailable.setText("Firmware version:" + version); //"Firmware version: " +
            removeFirmwareVersionListener();
        }
    }

    @Subscribe
    public void onConnectivityChange(MainDjiActivity.ConnectivityChangeEvent event) {
        if (mHandlerUI != null) {
            mHandlerUI.post(new Runnable() {
                @Override
                public void run() {
                    refreshSDKRelativeUI();
                }
            });
        }
    }

    private void refreshSDKRelativeUI() {
        mProduct = DJISampleApplication.getProductInstance();
        Log.d(TAG, "mProduct: " + (mProduct == null ? "null" : "unnull"));
        if (null != mProduct) {
            if (mProduct.isConnected()) {
                mBtnOpen.setEnabled(true);
                String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
                mTextConnectionStatus.setText("Status: " + str + " connected");
                tryUpdateFirmwareVersionWithListener();
                if (mProduct instanceof Aircraft) {
                    addAppActivationListenerIfNeeded();
                }

                if (null != mProduct.getModel()) {
                    mTextProduct.setText("" + mProduct.getModel().getDisplayName());
                } else {
                    mTextProduct.setText(R.string.product_information);
                }
            } else if (mProduct instanceof Aircraft) {
                Aircraft aircraft = (Aircraft) mProduct;
                if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                    mTextConnectionStatus.setText(R.string.connection_only_rc);
                    mTextProduct.setText(R.string.product_information);
                    mBtnOpen.setEnabled(false);
                    mTextModelAvailable.setText("Firmware version:N/A");
                }
            }
        } else {
            mBtnOpen.setEnabled(false);
            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
            mTextModelAvailable.setText("Firmware version:N/A");
        }
    }

    private void tryUpdateFirmwareVersionWithListener() {
        if (!hasStartedFirmVersionListener) {
            firmwareVersionUpdater = new KeyListener() {
                @Override
                public void onValueChange(final Object o, final Object o1) {
                    mHandlerUI.post(new Runnable() {
                        @Override
                        public void run() {
                            updateVersion();
                        }
                    });
                }
            };
            firmwareKey = ProductKey.create(ProductKey.FIRMWARE_PACKAGE_VERSION);
            if (KeyManager.getInstance() != null) {
                KeyManager.getInstance().addListener(firmwareKey, firmwareVersionUpdater);
            }
            hasStartedFirmVersionListener = true;
        }
        updateVersion();
    }

    private void removeFirmwareVersionListener() {
        if (hasStartedFirmVersionListener) {
            if (KeyManager.getInstance() != null) {
                KeyManager.getInstance().removeListener(firmwareVersionUpdater);
            }
        }
        hasStartedFirmVersionListener = false;
    }

    private void addAppActivationListenerIfNeeded() {
        if (AppActivationManager.getInstance().getAppActivationState() != AppActivationState.ACTIVATED) {
            sendDelayMsg(MSG_INFORM_ACTIVATION, ACTIVATION_DALAY_TIME);
            if (hasAppActivationListenerStarted.compareAndSet(false, true)) {
                appActivationStateListener = new AppActivationState.AppActivationStateListener() {

                    @Override
                    public void onUpdate(AppActivationState appActivationState) {
                        if (mHandler != null && mHandler.hasMessages(MSG_INFORM_ACTIVATION)) {
                            mHandler.removeMessages(MSG_INFORM_ACTIVATION);
                        }
                        if (appActivationState != AppActivationState.ACTIVATED) {
                            sendDelayMsg(MSG_INFORM_ACTIVATION, ACTIVATION_DALAY_TIME);
                        }
                    }
                };
                AppActivationManager.getInstance().addAppActivationStateListener(appActivationStateListener);
            }
        }
    }

    private void removeAppActivationListenerIfNeeded() {
        if (hasAppActivationListenerStarted.compareAndSet(true, false)) {
            AppActivationManager.getInstance().removeAppActivationStateListener(appActivationStateListener);
        }
    }

    private void loginToActivationIfNeeded() {
        if (AppActivationManager.getInstance().getAppActivationState() == AppActivationState.LOGIN_REQUIRED) {
            UserAccountManager.getInstance()
                    .logIntoDJIUserAccount(getContext(),
                            new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                                @Override
                                public void onSuccess(UserAccountState userAccountState) {
                                    ToastUtils.setResultToToast("Login Successed!");
                                }

                                @Override
                                public void onFailure(DJIError djiError) {
                                    ToastUtils.setResultToToast("Login Failed!");
                                }
                            });
        }
    }
}