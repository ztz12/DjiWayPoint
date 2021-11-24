package com.example.administrator.testz;


import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.example.administrator.testz.error.CrashHandler;
import com.secneo.sdk.Helper;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Main application
 */
public class DJISampleApplication extends Application {

    public static final String TAG = DJISampleApplication.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private static BaseProduct product;
    private static BluetoothProductConnector bluetoothConnector = null;
    private static Bus bus = new Bus(ThreadEnforcer.ANY);
    private static Application app = null;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized BluetoothProductConnector getBluetoothProductConnector() {
        bluetoothConnector = DJISDKManager.getInstance().getBluetoothProductConnector();
        return bluetoothConnector;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    public static synchronized HandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) {
            return null;
        }
        return (HandHeld) getProductInstance();
    }

    //电池
    public static synchronized Battery getBatteryInstance(){
        if (getProductInstance() == null){
            return null;
        }
        Battery battery = null;
        BaseProduct productInstance = getProductInstance();
        if (productInstance instanceof Aircraft){
            battery = ((Aircraft)productInstance).getBattery();
            //手持云台
        }else if (productInstance instanceof HandHeld){

        }
        return battery;
    }


    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }

    public static Application getInstance() {
        return DJISampleApplication.app;
    }

    public static Bus getEventBus() {
        return bus;
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        Helper.install(this);
        app = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }
}