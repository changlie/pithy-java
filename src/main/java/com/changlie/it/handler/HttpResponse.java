package com.changlie.it.handler;

import com.changlie.it.common.HttpMessage;
import com.changlie.it.common.ResponseCode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse implements ResponseCode, HttpMessage {
    private HttpRequest req;

    private OutputStream os = null;
    /** http响应码 */
    private String statusCode;
    private Map<String, String> headers = new HashMap<>();
    private String textBody;
    private byte[] binaryBody;

    public HttpResponse(OutputStream out) {
        this.os = out;
    }

    public void setReq(HttpRequest req) {
        this.req = req;
    }

    public void setHeader(String name, String value){
        headers.put(name, value);
    }

    public void setEntity(String entity){
        this.textBody = entity;
    }

    public void setEntity(byte[] entity){
        this.binaryBody = entity;
    }

    public void success() throws Exception {
        doResponse(SUCCESS);
    }

    public void notFound() throws IOException {
        doResponse(NOT_FOUND);
    }

    public void error() throws IOException {
        doResponse(ERROR);
    }

    /**
     * 向客户端发送响应。
     * @param statusCode
     * @throws IOException
     */
    private void doResponse(String statusCode) throws IOException {
        this.statusCode = statusCode;
        sendHeaders();
        sendBody();
    }

    private void sendBody() throws IOException {
        if(textBody==null && binaryBody==null){
            return;
        }

        byte[] body;
        if(textBody!=null){
            body = textBody.getBytes(DEFAULT_CHARSET);
        }else{
            body = binaryBody;
        }

        os.write(body);
        os.flush();
    }

    private void sendHeaders() throws IOException {
        if(os == null) {
            return ;
        }
        int contentLength = getContentLength();
        String protocol = req.getProtocol();

        StringBuilder headers = new StringBuilder();
        headers.append(protocol).append(" ").append(statusCode).append(" OK").append(CRLF);
        headers.append("Content-Length: ").append(contentLength).append(CRLF);
        headers.append("Content-Type: text/html").append(CRLF);
        headers.append("Server: TK").append(CRLF);
        headers.append(CRLF);

        os.write(headers.toString().getBytes());
        os.flush();
    }

    public int getContentLength() throws UnsupportedEncodingException {
        if(textBody==null && binaryBody==null){
            return 0;
        }
        if(textBody!=null){
            return textBody.getBytes(DEFAULT_CHARSET).length;
        }
        return binaryBody.length;
    }

}

