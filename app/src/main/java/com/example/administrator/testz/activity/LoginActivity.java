package com.example.administrator.testz.activity;




        import android.os.Bundle;
        import androidx.appcompat.app.AppCompatActivity;
        import android.util.Log;
        import android.view.View;


        import com.example.administrator.testz.R;
        import com.example.administrator.testz.utils.ToastUtils;

        import dji.common.error.DJIError;
        import dji.common.useraccount.UserAccountState;
        import dji.common.util.CommonCallbacks;
        import dji.sdk.useraccount.UserAccountManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    public void login(View view) {
        // Launches a login dialog
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(UserAccountState userAccountState) {
                        ToastUtils.setResultToToast("Login Success");
                        updateLoginState(userAccountState);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        ToastUtils.setResultToToast("error:" + djiError.getDescription());
                    }
                });
    }

    // After the user logs in or out, update the enabled state of the buttons
    private void updateLoginState(final UserAccountState userAccountState) {
//        ToastUtils.setResultToToast("Account State: " + userAccountState);
        Log.i(TAG,"Account State: " + userAccountState);
    }




    public void logout(View v){
//        UserAccountManager.getInstance().logoutOfDJIUserAccount(error -> {
//            if (null == error) {
//                ToastUtils.setResultToToast("Logout Success");
//            } else {
//                ToastUtils.setResultToToast("Logout Error:"
//                        + error.getDescription());
//            }
//        });

        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {

            }

            @Override
            public void onFailure(DJIError djiError) {
                ToastUtils.setResultToToast("error:" + djiError.getDescription());
            }
        });
    }
}
