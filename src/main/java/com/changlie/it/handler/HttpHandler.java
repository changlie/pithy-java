package com.changlie.it.handler;


import com.changlie.it.common.FileInfo;

import java.io.InputStream;
import java.net.Socket;

public class HttpHandler implements Runnable{
    final String lineSeperator = "\r\n";

    private Socket req;

    public HttpHandler(Socket req) {
        this.req = req;
    }

    @Override
    public void run() {
        try {
            InputStream in = req.getInputStream();
            HttpRequest request=new HttpRequest(in);
            request.parseRequest();
            System.out.print(request.getHeaders());
            postTest(request);

            HttpResponse resp = new HttpResponse(req.getOutputStream());
            resp.setReq(request);
            resp.setEntity("submit successfully!");
            resp.success();

            req.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postTest(HttpRequest request) {
        String uri = request.getUri();
        if(!uri.endsWith("saveUser")){
            return;
        }

        String hobby = request.getParamString("hobby");
        String name = request.getParamString("myName");
        System.out.println("hobby: "+hobby);
        System.out.println("name: "+name);

        FileInfo f = request.getParamFile("deIndex");
        if(f == null){
            return;
        }
        String fileName = f.getName();
        System.out.println("fileName: "+fileName);
        String path = "d:/"+fileName;
        f.saveFile(path);
        System.out.println("save file("+path+") successfully!!");
    }
}