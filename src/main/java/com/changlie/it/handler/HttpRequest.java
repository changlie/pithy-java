package com.changlie.it.handler;

import com.changlie.it.common.FileInfo;
import com.changlie.it.common.HttpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * http请求数据解析类
 */
public class HttpRequest implements HttpMessage {
	private InputStream inputStream = null;

	/** 请求头的第一行，包含请求方法，请求url, http协议 */
	private String requestLine = null;

	private String method;
	private String url;
	private String uri; // 不带queryString.
	private String protocol;

	private int contentLength = 0; // 请求体的长度
	private boolean headerEnd = false; // 标记请求头的位置。

	/** 请求头键值对 */
	private Map<String,String> headersMap=new HashMap<>();
	/** 请求头缓冲区 */
	private StringBuffer header=new StringBuffer();
	/** 请求体缓冲区 */
	private List<Byte> entity = null;

	private Map<String, Object> httpArgs = new HashMap<>();
	
	public HttpRequest(InputStream inputStream) {
		this.inputStream=inputStream;
	}

	public String getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	public String getUri() {
		return uri;
	}

	public String getProtocol() {
		return protocol;
	}

	public void parseRequest() throws IOException {
		byte[] buffer=new byte[2048]; //临时缓冲区。

		// 获取请求头数据。
		while(!headerEnd){
			// 逐个逐个字节读取，以获取请求头数据。
			int bufferSize=inputStream.read(buffer);
			for(int index=0;index<bufferSize;index++){
				byte byt = buffer[index];
				char c=(char) buffer[index];
				int length=header.length();

				// 以回车符，换行符分析，归类请求数据。
				// 并从请求头缓冲区解析出每个请求头name,value保存至一个map.
				// 标记出请求头结束位置。
				if(c==LF && header.charAt(length-1)==CR){
					doParseRequest(length);
				}

				// 收集请求头数据至请求头缓冲区，并处理请求头，请求体的边界的数据
				sortoutByte(c, byt);
			}
		}

		//如果请求头读取完毕，并且存在请求体，则继续读取请求体数据
		while(contentLength>0 && contentLength>entity.size()){
			int bufferSize=inputStream.read(buffer);

			for(int i=0; i<bufferSize; i++){
				entity.add(buffer[i]);
			}
		}

		//解析请求体。
		if(entity!=null && entity.size()>0){
			parseParams();
		}
	}

	/**
	 * 以回车符，换行符分析，归类请求数据。<br>
	 * 并从请求头缓冲区解析出每个请求头name,value保存至一个map.<br>
	 * 标记出请求头结束位置。
 	 * @param length 当前请求头缓冲区的长度
	 */
	private void doParseRequest(int length) {
		if(requestLine==null){//第一个CRLF是请求行
			requestLine=header.toString();
			// 解析首行信息。
			String[] strings = requestLine.split("\\s+");
			method = strings[0];
			url = strings[1];

			int queryStringStartIndex = url.indexOf("?");
			if(queryStringStartIndex > -1){
				uri = url.substring(0, queryStringStartIndex);
			}else{
				uri = url;
			}

			protocol = strings[2];
			return;
		}

		// 从请求头缓冲区解析出每个请求头name,value保存至一个map
		int crlf = header.lastIndexOf(CRLF);
		String headerLine=header.substring(crlf+2,length-1);
		if(!"".equals(headerLine)){//请求头
			putHeader(headerLine);
			return;
		}

		//请求数据中，第一次出现以CRLF结尾的空行, 意味着请求头已结束
		headerEnd=true;
		if(headersMap.get(CONTENT_LENGTH)!=null){
			contentLength=Integer.parseInt(headersMap.get(CONTENT_LENGTH));
		}
	}

	/**
	 * 收集请求头数据至请求头缓冲区，并处理请求头，请求体的边界的数据
	 * @param c 字符 由字节强转得来，为ASCII时不会出错。
	 * @param byt 请求数据的单字节数据
	 */
	private void sortoutByte(char c, byte byt) {
		// 收集请求头数据。
		if(!headerEnd){
			header.append(c);
			return;
		}

		// 处理请求头，请求体的边界
		if(entity==null){//请求头的最后一个LF，不放入实体部分
			header.append(c);
			entity= new ArrayList<>(contentLength);
		}else{
			// 收集请求体的数据
			entity.add(byt);
		}
	}

	/**
	 * 解析请求参数。
	 * @throws UnsupportedEncodingException
	 */
	public void parseParams() throws UnsupportedEncodingException {
		String contentType = getHeader(CONTENT_TYPE);
		if(contentType.startsWith(MULTIPART_FORM_DATA)){
			String[] split = contentType.split(";");
			String boundary = split[1].split("=")[1];
//			String content = getEntity();
//			FormDataParser p = new FormDataParser(content, boundary);

			byte[] r = getRawEntity();
			FormDataBytesParser p = new FormDataBytesParser(r, boundary);
			Map<String, Object> params = p.doParse();
			httpArgs.putAll(params);
		}
	}

	public String getParamString(String name){
		Object val = httpArgs.get(name);
		if(val==null) return null;
		return val.toString();
	}

	public FileInfo getParamFile(String name){
		Object val = httpArgs.get(name);
		if(val==null) return null;
		return (FileInfo) val;
	}

	public Map<String, Object> getHttpArgs(){
		return httpArgs;
	}
	
	private void putHeader(String header){
		int index=header.indexOf(":");
		String key=header.substring(0,index);
		String value=header.charAt(index+1)==' '?header.substring(index+2):header.substring(index+1);
		headersMap.put(key, value);
	}

	public String getHeader(String name){
		String val = headersMap.get(name);
		if(val == null){
			return null;
		}
		return val.trim();
	}
	
	public String getHeaders() {
		return header.toString();
	}
	
	public String getEntity() throws UnsupportedEncodingException {
		if(getRawEntity()==null){
			return null;
		}

		return new String(getRawEntity(), DEFAULT_CHARSET);
	}

	public byte[] getRawEntity(){
		if(entity==null){
			return null;
		}
		int len = entity.size();

		byte[] buf = new byte[len];

		for(int i = 0; i< len; i++){
			buf[i] = entity.get(i);
		}
		return buf;
	}
}