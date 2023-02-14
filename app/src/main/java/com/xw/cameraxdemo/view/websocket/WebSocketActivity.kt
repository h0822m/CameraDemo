package com.xw.cameraxdemo.view.websocket

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.xw.cameraxdemo.R
import java.net.InetSocketAddress
import java.net.URI

/**
 * Create by zcr on 2023/2/2
 */
class WebSocketActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_websocket_layout)
        val testBtn = findViewById<Button>(R.id.socket_test_btn)

        // 启动服务器
        startMyWebSocketServer()
        // 测试服务器
        testBtn.setOnClickListener {
            textSocket()
        }
    }

    private fun textSocket() {
        val uri: URI = URI.create("ws://172.16.123.180:8080")
        val client: JWebSocketClient = object : JWebSocketClient(uri) {
            override fun onMessage(message: String) {
                //message就是接收到的消息
                Log.e("JWebSClientService", message)
            }
        }

        try {
            client.connectBlocking()
            client.send("哈喽")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // 实现方法，在服务中或者OnCreate()方法调用此方法
    private fun startMyWebSocketServer() {
        // 192.168.1.101为安卓服务端，需要连接wifi后 高级选项ip设置为静态,输入自定义地址
        // 方便客户端 找 服务端,不需要用getHostAddress等，可能连接不上
        // 9090为端口
        val myHost = InetSocketAddress("172.16.123.180", 9090)
        val myWebSocketServer = MyWebSocketServer(myHost)
        myWebSocketServer.connectionLostTimeout = 0
        myWebSocketServer.start()
    }
}