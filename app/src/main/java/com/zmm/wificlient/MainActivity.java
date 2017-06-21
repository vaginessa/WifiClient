package com.zmm.wificlient;

import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zmm.wificlient.adapter.WifiListAdapter;
import com.zmm.wificlient.client.SocketClient;
import com.zmm.wificlient.object.object.ConstantC;
import com.zmm.wificlient.object.object.ScanResultWithLock;
import com.zmm.wificlient.wifitool.SortResultsByLevel;
import com.zmm.wificlient.wifitool.WifiReceiver;
import com.zmm.wificlient.wifitool.WifiTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SocketClient.ConnectListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    private RecyclerView rvWifiList;
    private SwipeRefreshLayout srlWifiList;
    private TextView mRead;
    private SocketClient socketClient;
    private WifiTools wifiTools;
    private WifiReceiver wifiReceiver;
    private List<ScanResultWithLock> scanResultWithLocks;
    private WifiListAdapter wifiListAdapter;
    private AlertDialog mAlertDialog;
    private EditText mEditWrite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvWifiList = (RecyclerView) findViewById(R.id.rv_wifi_list);
        srlWifiList = (SwipeRefreshLayout) findViewById(R.id.srl_wifi_list);
        Button write = (Button) findViewById(R.id.btn_client_write);
        mRead = (TextView) findViewById(R.id.tv_client_read);
        mEditWrite = (EditText) findViewById(R.id.et_write);

        initParameter();

        initWifiListAdapter();


        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

    }


    private void initParameter() {
        wifiTools = WifiTools.getInstance(this);
        socketClient = new SocketClient();
        wifiTools.closeHotSpot();
        wifiTools.openWifi();

        wifiReceiver = new WifiReceiver();
        wifiReceiver.setOnWifiChangedListener(new WifiReceiver.OnWifiChangedListener() {
            @Override
            public void onWifiOpened() {
                new RefreshWifiList().start();
            }

            @Override
            public void onWifiClosed() {

            }
        });
    }

    /**
     * 刷新wifi列表的线程
     */
    class RefreshWifiList extends Thread {
        @Override
        public void run() {
            try {
                Log.i(TAG, "RecycleView 刷新");

                wifiTools.startScanWifi();
                Thread.sleep(1500);
                List<ScanResult> scanResults = wifiTools.scanWifi();
                Log.i(TAG, "扫描结果大小：" + scanResults.size());
                Collections.sort(scanResults, new SortResultsByLevel());
                scanResultWithLocks = new ArrayList<>();
                for (ScanResult scanResult : scanResults) {
                    boolean isLocked;
                    if (scanResult.capabilities.length() <= 5) {
                        isLocked = false;
                    } else {
                        isLocked = true;
                    }
                    ScanResultWithLock scanResultWithLock = new ScanResultWithLock(scanResult, isLocked);
                    scanResultWithLocks.add(scanResultWithLock);
                }

                Log.i(TAG, "扫描结果大小：" + scanResultWithLocks.size());

                Message msg = new Message();
                msg.what = ConstantC.WIFI_LIST_REFRESHED;
                listHandler.sendMessage(msg);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Handler listHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                //wifi列表刷新处理

                case ConstantC.WIFI_LIST_REFRESHED:

                    int size = scanResultWithLocks.size();
                    Log.i(TAG, "主线程中获取到扫描结果大小：" + size);
                    if (size == 0) {
                        Log.d(TAG,"请确定wifi是否打开!");
                    }
                    wifiListAdapter.setData(scanResultWithLocks);
                    wifiTools.getConfiguration();
                    wifiListAdapter.notifyDataSetChanged();
                    srlWifiList.setRefreshing(false);

                    break;
                //wifi连接失败
                case ConstantC.WIFI_CONNECT_FAILED:
                    Toast.makeText(getApplicationContext(),"进入聊天室失败，请确认密码是否正确！",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 初始化wifi列表的
     */
    void initWifiListAdapter() {

        scanResultWithLocks = new ArrayList<>();
        wifiListAdapter = new WifiListAdapter(this, scanResultWithLocks);

        wifiListAdapter.setOnRItemClickListener(new WifiListAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onNameClick(int position) {
                Log.i(TAG, position + ":tvName Clicked");
            }

            @Override
            public void onImageClick(int position) {
                Log.i(TAG, position + ":ivIcon Clicked");
            }

            @Override
            public void onConnectClick(final int position) {
                Log.i(TAG, position + ":tvConnect Clicked");

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(false);
                View view = View.inflate(MainActivity.this,R.layout.dialog_connect_wifi , null);

                final TextInputEditText textInputEditText = (TextInputEditText) view.findViewById(R.id.tie_connect_input_password);
                final TextInputLayout textInputLayout = (TextInputLayout) view.findViewById(R.id.til_connect_input_password);
                TextView textView = (TextView) view.findViewById(R.id.tv_connect_ssid);
                textView.setText("SSID: " + scanResultWithLocks.get(position).getScanResult().SSID);
                final boolean isLocked = scanResultWithLocks.get(position).isLocked();
                if (!isLocked) {
                    textInputLayout.setVisibility(View.GONE);
                }

                textInputEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                        textInputLayout.setError(getResources().getString(R.string.dialog_connect_less_than8));
//                        textInputLayout.setErrorEnabled(true);
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() < 8) {
                            textInputLayout.setError(getResources().getString(R.string.dialog_connect_less_than8));
                            textInputLayout.setErrorEnabled(true);
                        } else {
                            textInputLayout.setErrorEnabled(false);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (scanResultWithLocks.get(position).getScanResult().SSID.startsWith("HOT-")) {
                            final String SSID = scanResultWithLocks.get(position).getScanResult().SSID;
                            final String passWord = textInputEditText.getText().toString();

                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    Log.i(TAG, "从来没有连过这个聊天室");
                                    int netId = wifiTools.AddWifiConfig(scanResultWithLocks, SSID, passWord);
                                    if (netId != -1) {
                                        Log.i(TAG, "创建配置信息成功");
                                        wifiTools.getConfiguration();//添加了配置信息，要重新得到配置信息
                                        wifiTools.connectWifi(netId);
                                        System.out.println(wifiTools.getConnectedHotIP());
                                        new ConnectServer().start();

                                    } else {
                                        Message msg = new Message();
                                        msg.what = ConstantC.WIFI_CONNECT_FAILED;
                                        listHandler.sendMessage(msg);
                                    }
                                }
                            }.start();
                        }

                    }
                });
                builder.setNegativeButton("取消", null);

                builder.setView(view);
                mAlertDialog = builder.create();
                mAlertDialog.show();

            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //设置布局管理器
        rvWifiList.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        rvWifiList.setAdapter(wifiListAdapter);
        //设置增加或删除条目的动画
        rvWifiList.setItemAnimator(new DefaultItemAnimator());


//        srlWifiList.setColorSchemeColors();  //开始用这个方法，没有用
//        srlWifiList.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

        srlWifiList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                Log.i(TAG, "WifiList 刷新");
                new RefreshWifiList().start();

            }
        });


    }

    class ConnectServer extends Thread {

        @Override
        public void run() {
            super.run();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            while (wifiTools.getConnectedHotIP().size() < 2) {
                try {
                    Thread.sleep(500);
                    System.out.println("wait");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            socketClient = new SocketClient();
            socketClient.createClient(wifiTools.getConnectedHotIP().get(1));

            socketClient.setOnConnectListener(MainActivity.this);
        }
    }



    @Override
    public void onReceiveData(final String msg) {
        Log.i(TAG, "客户端监听收到消息" + msg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRead.setText(msg);
            }
        });
    }

    @Override
    public void onNotify(String msg) {
        Log.i(TAG, "设置客户端给出通知" + msg);
    }


    private void sendMessage() {
        if (wifiTools.getConnectedHotIP().size() >= 2) {
            if (socketClient != null) {
                try {
                    String data = mEditWrite.getText().toString().trim();
                    if(data != null){
                        socketClient.sendMessageToServer(data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(TAG, "socketClient is null");
                Toast.makeText(getApplicationContext(),"socketClient is null",Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.i(TAG, "没有连接服务器");
        }
    }
}
