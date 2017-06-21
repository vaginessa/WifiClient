package com.zmm.wificlient.client;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketClient {

    Socket socket;
    static final String TAG = "SocketClient";
    private ConnectListener mListener;

    public void createClient(final String ip) {
        new Thread() {
            @Override
            public void run() {
                try {

                    socket = new Socket(ip, 12345);
                    Log.i(TAG, "已经连接上了服务器");
                    InputStream inputStream = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len;

                    while ((len = inputStream.read(buffer)) != -1) {
                        String data = new String(buffer, 0, len);

                        //通过回调接口将获取到的数据推送出去
                        if (mListener != null) {
                            handleMessageFromServer(data);
                        } else {
                            System.out.println("null");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 处理来自服务器的生消息
     *
     * @param data
     */
    private void handleMessageFromServer(String data) {
        if (mListener != null) {
            mListener.onReceiveData(data);
        }
    }

    /**
     * 发送消息给服务器
     *
     * @param msg  消息内容
     * @throws IOException
     */
    public void sendMessageToServer(String msg) throws IOException {

        Log.i(TAG, "发出消息函数");
        OutputStream outputStream = socket.getOutputStream();
//        msg = type + socketUser.getSign() + msg;
        if (mListener != null) {
            mListener.onNotify(msg);
        } else {
            Log.i(TAG, "客户端自己显示消息错误");
        }
        outputStream.write(msg.getBytes("utf-8"));
        outputStream.flush();

        Log.i(TAG, "发出消息" + msg);

    }

    public void setOnConnectListener(ConnectListener listener) {
        this.mListener = listener;
    }

    /**
     * 数据接收回调接口
     */
    public interface ConnectListener {
        void onReceiveData(String msg);

        void onNotify(String msg);
    }


}
