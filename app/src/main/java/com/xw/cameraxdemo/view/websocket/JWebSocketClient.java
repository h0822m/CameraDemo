package com.xw.cameraxdemo.view.websocket;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import javax.net.ssl.SSLParameters;

/**
 * Create by zcr on 2023/2/2
 */
public class JWebSocketClient extends WebSocketClient {

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
//        super.onSetSSLParameters(sslParameters);
    }

    public JWebSocketClient(URI serverUri) {
        super(serverUri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {//在webSocket连接开启时调用
        Log.d("JWebSocketClient---", "open");
    }

    @Override
    public void onMessage(String message) {//接收到消息时调用
        Log.d("JWebSocketClient---", "onMessage");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {//在连接断开时调用
        Log.d("JWebSocketClient---", "onClose");
    }

    @Override
    public void onError(Exception ex) {//在连接出错时调用
        Log.d("JWebSocketClient---", "onError");
    }
}
