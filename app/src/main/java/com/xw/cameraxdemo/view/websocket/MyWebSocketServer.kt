package com.xw.cameraxdemo.view.websocket

import android.util.Log
import com.xw.cameraxdemo.util.ByteUtils
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer


/**
 * Create by zcr on 2023/2/3
 */
class MyWebSocketServer internal constructor(host: InetSocketAddress?) : WebSocketServer(host) {

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.d("websocket", "onOpen()一个客户端连接成功：" + conn.remoteSocketAddress + "/" + conn.localSocketAddress)
        conn.send("哈喽，IDE")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d("websocket", "onClose()服务器关闭")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        // 这里在网页测试端发过来的是文本数据 测试网页 http://www.websocket-test.com/
        // 需要保证android 和 加载网页的设备(我这边是电脑) 在同一个网段内，连同一个wifi即可
        Log.d("websocket", "onMessage()网页端来的消息->$message")
        conn.send("你好，client")
    }

    override fun onMessage(conn: WebSocket?, message: ByteBuffer) {
        // 接收到的是Byte数据，需要转成文本数据，根据你的客户端要求
        // 看看是string还是byte，工具类在下面贴出
        Log.d("websocket", "onMessage()接收到ByteBuffer的数据->" + ByteUtils.byteBufferToString(message))
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        // 异常  经常调试的话会有缓存，导致下一次调试启动时，端口未关闭,多等待一会儿
        // 可以在这里回调处理，关闭连接，开个线程重新连接调用startMyWebsocketServer()
        Log.e("websocket", "->onError()出现异常：$ex")
    }

    override fun onStart() {
        Log.d("websocket", "onStart()，WebSocket服务端启动成功")
    }
}