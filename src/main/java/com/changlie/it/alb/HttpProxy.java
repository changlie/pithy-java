package com.changlie.it.alb;

import com.changlie.it.common.HttpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class HttpProxy implements HttpMessage {
    static String[] hostList = {"127.0.0.1:8777","127.0.0.1:8999",};
    static IntervalBorder[] intervals;
    static {
        int length = hostList.length;
        int interval = 100/ length;
        intervals  = new IntervalBorder[length];
        for(int i=0; i< length; i++){
            int start = i*interval;
            int end;
            if(i==length-1){
                end = 100;
            }else{
                end = (start+1) * interval;
            }
            intervals[i] = new IntervalBorder(start, end);
        }
    }

    private InputStream inputStream = null;
    String method;
    String url;
    String protocal;

    int contentLength;

    /** 请求头缓冲区 */
    private StringBuffer header=new StringBuffer();
    /** 请求数据  */
    private List<Byte> bytes = new LinkedList<>();
    private byte[] reqData;


    public HttpProxy(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void doParse(){
        boolean headEnd = false;
        int bodyIndex = 0;
        try {
            byte[] buf = new byte[2048];
            int len = 0;
            while (((len = inputStream.read(buf)) > 0)) {
                for(int i=0; i<len; i++){
                    byte b = buf[i];
                    char c = (char) b;
                    bytes.add(b);

                    boolean isCRLF = c == LF && header.charAt(header.length()-1) == CR;
                    if(url == null && isCRLF){
                        String firstLine = getFirstLine(bytes);
                        String[] split = firstLine.split("\\s+");
                        method = split[0];
                        url = split[1];
                        protocal = split[2];
                    }

                    if(!headEnd){
                        if(isCRLF && url != null){
                            int index = header.lastIndexOf(CRLF);
                            int startIndex = CRLF.length() + index;
                            String headerLine = header.substring(startIndex, header.length() - 1);
                            String[] split = headerLine.split(":");
                            String headerName = split[0];
                            if(CONTENT_LENGTH.equals(headerName)){
                                contentLength = Integer.parseInt(split[1].trim());
                            }
                            if("".equals(headerLine)){
                                headEnd = true;
                            }
                        }
                        header.append(c);
                    }else{
                        bodyIndex ++ ;
                    }
                }
                if(headEnd && bodyIndex>=contentLength){
                    break;
                }
            }

            reqData = toArray(bytes);

            System.out.println("request: ");
            System.out.println(new String(reqData, CHARSET_UTF8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFirstLine(List<Byte> bytes) {
        int endIndex = bytes.size() - CRLF.length();
        byte[] buf = new byte[endIndex];
        for(int i=0; i<endIndex; i++){
            buf[i] = bytes.get(i);
        }
        return new String(buf, 0, endIndex);
    }

    private static boolean endMatch(String key, List<Byte> bytes) {
        int length = key.length();
        int startIndex = bytes.size() - length;
        for(int i=startIndex, j=0; j<length; i++, j++){
            byte b = bytes.get(i);
            char c = (char) b;
            if(c != key.charAt(j)){
                return false;
            }
        }
        return true;
    }

    public byte[] toArray(List<Byte> bytes){
        int len= bytes.size();
        byte[] res = new byte[len];
        for(int i=0; i<len; i++){
            res[i] = bytes.get(i);
        }
        return res;
    }

    private static String getHostByRandom() {
        int random = (int)(Math.random()*100);
        int length = hostList.length;
        for(int i=0; i<length; i++){
            IntervalBorder border = intervals[i];
            if(border.start<=random && border.end > random){
                return hostList[i];
            }
        }
        return null;
    }

    static class IntervalBorder{
        int start;
        int end;

        public IntervalBorder(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "IntervalBorder{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }

    public byte[] getServiceResponse(){
        String host = getHostByRandom();
        String[] split = host.split(":");
        int port;
        if(split.length<2) {
            port = 80;
        }else{
            port = Integer.parseInt(split[1]);
        }
        String domainOrIp = split[0];
        System.out.println("domainOrIp: "+ domainOrIp+", port: "+port);

        try(Socket client = new Socket(domainOrIp, port)){
            // 发送参数
            OutputStream out = client.getOutputStream();
            out.write(reqData, 0, reqData.length);
            out.flush();
            // 获取返回值
            InputStream in = client.getInputStream();
            ResponseParser parser = new ResponseParser(in);
            byte[] bytes = parser.doParse();

            System.out.println("response: ");
            System.out.println(new String(bytes));

            return bytes;
        }catch (Exception e){
            e.printStackTrace();
        }
        return new byte[0];
    }

}