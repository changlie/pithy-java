package com.changlie.it.alb;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class HttpProxyHandler implements  Runnable{
    private Socket req;

    public HttpProxyHandler(Socket req) {
        this.req = req;
    }

    @Override
    public void run() {
        try {
            InputStream in = req.getInputStream();
            HttpProxy hp = new HttpProxy(in);
            hp.doParse();
            byte[] bytes = hp.getServiceResponse();

            OutputStream out = req.getOutputStream();
            out.write(bytes, 0, bytes.length);
            out.flush();

            req.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}