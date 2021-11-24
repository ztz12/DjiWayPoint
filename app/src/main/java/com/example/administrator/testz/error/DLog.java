package com.example.administrator.testz.error;




        import android.util.Log;

        import com.example.administrator.testz.BuildConfig;

/**
 * @author DeMon
 * @date 2018/8/14
 * @description
 */
public class DLog {
    /*m默认不打印Log，如果要打印，置为true*/
    private static boolean enableLog = BuildConfig.DEBUG;

    public static void e(String tag, String msg) {
        if (enableLog)
            Log.e(tag, msg);

    }

    public static void d(String tag, String msg) {
        if (enableLog)
            Log.d(tag, msg);

    }

    public static void i(String tag, String msg) {
        if (enableLog)
            Log.i(tag, msg);

    }

    public static void v(String tag, String msg) {
        if (enableLog)
            Log.v(tag, msg);

    }

    public static void w(String tag, String msg) {
        if (enableLog)
            Log.w(tag, msg);

    }


}