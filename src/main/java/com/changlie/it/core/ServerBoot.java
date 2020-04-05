package com.changlie.it.core;

import com.changlie.it.handler.HttpHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerBoot {
    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        int port = 7272;

        try(ServerSocket server = new ServerSocket(port)){
            System.out.println("server:["+port+"] is running!!!");
            while (true){
                Socket req = server.accept();
                threadPool.execute(new HttpHandler(req));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
