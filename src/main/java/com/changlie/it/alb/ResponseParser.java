package com.changlie.it.alb;

import com.changlie.it.common.HttpMessage;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class ResponseParser implements HttpMessage {
    InputStream in;

    StringBuilder header = new StringBuilder();
    StringBuilder body = new StringBuilder();

    List<Byte> bytes = new LinkedList<>();
    byte[] respData;

    public ResponseParser(InputStream in) {
        this.in = in;
    }

    public byte[] doParse(){
        String firstLine = null;
        boolean headEnd = false;
        int bodyIndex = 0;
        int contentLength = 0;
        boolean isChunked = false;
        int chunkedSize = -1;
        int chunkedIndex = 0;
        boolean chunkecBodyEnd = false;

        try{
            byte[] buf = new byte[2048];
            int len = 0;
            while((len = in.read(buf)) > 0){
                for(int i=0; i<len; i++){
                    byte b = buf[i];
                    char c = (char) b;
                    bytes.add(b);

                    if(!headEnd){
                        boolean isCRLF = c == LF && header.charAt(header.length()-1) == CR;
                        if(firstLine == null && isCRLF){
                            firstLine = header.substring(0, header.length()-1);
                        }
                        if(firstLine != null && isCRLF){
                            int headerStartIndex = header.lastIndexOf(CRLF) + CRLF.length();
                            String headerLine = header.substring(headerStartIndex, header.length() - 1);
                            String[] split = headerLine.split(":");
                            String headerName = split[0];
                            if(CONTENT_LENGTH.equals(headerName)){
                                contentLength = Integer.parseInt(split[1].trim());
                            }else if(TRANSFER_ENCODING.equals(headerName) && BODY_TYPE.equals(split[1].trim())){
                                isChunked = true;
                            }else if("".equals(headerLine)){
                                headEnd = true;
                            }
                        }
                        header.append(c);
                    }else{
                        if(!isChunked){
                            bodyIndex ++;
                        }else{
                            boolean isCRLF = c == LF && body.charAt(body.length()-1) == CR;
                            if(isCRLF && chunkedSize == -1){
                                int startIndex = body.lastIndexOf(CRLF);
                                if(startIndex==-1){
                                    startIndex = 0;
                                }else{
                                    startIndex = startIndex+CRLF.length();
                                }
                                String tmp = body.substring(startIndex, body.length() - 1);
                                chunkedSize = Integer.parseInt(tmp.trim(), 16);
                            }else if(chunkedSize > -1 && chunkedIndex<chunkedSize){
                                chunkedIndex++;
                            }else if(isCRLF && chunkedIndex>=chunkedSize){
                                chunkedIndex = 0;
                                chunkedSize = -1;

                                chunkecBodyEnd = body.charAt(body.length()-2) == LF
                                                && body.charAt(body.length()-3) == CR;
                            }
                        }
                        body.append(c);
                    }
                }

                if((!isChunked && headEnd && bodyIndex>=contentLength) || chunkecBodyEnd){
                    break;
                }
            }
            System.out.println("isChunked: "+isChunked);

            respData = toArray(bytes);
            return respData;
        }catch (Exception e){
            e.printStackTrace();
        }

        return new byte[0];
    }


    public byte[] toArray(List<Byte> bytes){
        int len= bytes.size();
        byte[] res = new byte[len];
        for(int i=0; i<len; i++){
            res[i] = bytes.get(i);
        }
        return res;
    }
}