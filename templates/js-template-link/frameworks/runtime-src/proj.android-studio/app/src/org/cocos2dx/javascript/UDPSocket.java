package org.cocos2dx.javascript;

import android.app.Activity;
import android.util.Log;

import org.cocos2dx.lib.Cocos2dxActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kivlin on 2018/8/28.
 */

public class UDPSocket {
    private static final String TAG = "UDPSocket";
    private static final int POOL_SIZE = 5;
    private static final int BUFFER_LENGTH = 1024;
    private static byte[] receiveByte = new byte[BUFFER_LENGTH];
    private static final String BROADCAST_IP = "192.168.1.138";

    public static final int PORT = 8888;

    private static boolean isThreadRunning = false;
    private static DatagramSocket client;
    private static DatagramPacket receivePacket;

    private static ExecutorService mThreadPool;
    private static Thread clientThread;

    public static Cocos2dxActivity activity;

    public static void initialize() {
        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
        startUDPSocket();
    }

//    public UDPSocket() {
//        int cpuNumbers = Runtime.getRuntime().availableProcessors();
//        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
//    }

    private static void startUDPSocket() {
        if(client != null) return;
        try {
            client = new DatagramSocket();
            //client = new DatagramSocket();

            if(receivePacket == null) {
                receivePacket = new DatagramPacket(receiveByte, BUFFER_LENGTH);
            }

            startSocketThread();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static void startSocketThread() {
        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "clientThread is running...");
                receiveMessage();
            }
        });
        isThreadRunning = true;
        clientThread.start();
    }

    private static void receiveMessage() {
        while(isThreadRunning) {
            try {
                if(client != null) {
                    client.receive(receivePacket);
                }
                Log.d(TAG, "receive packet success...");
            } catch(IOException e) {
                Log.e(TAG, "UDP数据包接受失败！线程停止");
                stopUDPSocket();
                e.printStackTrace();
                return;
            }

            if(receivePacket == null || receivePacket.getLength() == 0 ) {
                Log.e(TAG, "无法接受UDP数据或者收到的UDP数据为空");
                continue;
            }

            final byte[] data = receivePacket.getData();
            final int len = receivePacket.getLength();
            Log.d(TAG, receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort() + " " + data);
            activity.runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    nativeReceiveMessage(data, len);
                }
            });
            if(receivePacket != null){
                receivePacket.setLength(BUFFER_LENGTH);
            }
        }


    }

    private static void stopUDPSocket() {
        isThreadRunning = false;
        receivePacket= null;
        if(clientThread != null) {
            clientThread.interrupt();
        }
        if(client != null) {
            client.close();
            client = null;
        }
    }

    public static void sendMessage(final byte[] message) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(BROADCAST_IP);

                    DatagramPacket packet = new DatagramPacket(message,message.length,targetAddress, PORT);

                    client.send(packet);

                    Log.d(TAG, "数据发送成功");
                }catch(UnknownHostException e) {
                    e.printStackTrace();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static native void nativeReceiveMessage(byte[] data, int length);
}
