package com.example.administrator.testz.nettydji;




        import java.util.HashMap;
        import java.util.List;

/**
 * Created by wrs on 2019/6/25,17:35
 * projectName: Testz
 * packageName: com.example.administrator.testz
 */
public class HeartBeatBean {
    // 通过快捷键Alt+Insert

    String action;
    HashMap<String, Object> data;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public HeartBeatBean(String mAction, HashMap<String, Object> data) {
        this.action = mAction;
        this.data = data;
    }
}