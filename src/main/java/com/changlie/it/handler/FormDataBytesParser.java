package com.changlie.it.handler;

import com.changlie.it.common.FileInfo;
import com.changlie.it.common.HttpMessage;
import com.changlie.it.common.NameValue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  multipart/form-data 类型的httpbody的解析类
 */
public class FormDataBytesParser implements HttpMessage {
	/** 包含整个http body 的 字节数组 */
	private byte[] bytes;
	/** 请求头的约定好的键值对分隔符。 */
	String boundary;

	/** 键值对之间的分隔符 */
	String nameValueSeperator;
	/** 键值对起始token */
	String nameValueEnd;
	/** 键值对结束token */
	String nameValueStart;

	/** 用于记录bodyPart的起始索引 */
	List<Integer> nameValueStartIndexStack = new ArrayList<>();
	/** 用于记录键值对的value的起始索引 */
	List<Integer> valueStartIndexStack = new ArrayList<>();

	/** 相应起始索引获取失败时，返回的值 */
	int NONE = -1;

	/** 本解析器，解析后的结果集 */
	Map<String, Object> params = new HashMap<>();

	public FormDataBytesParser(byte[] bytes, String boundary) {
		this.bytes = bytes;
		this.boundary = boundary;
		this.nameValueStart = boundary+CRLF;
		this.nameValueSeperator = CRLF+CRLF;
		this.nameValueEnd = CRLF+"--";
	}

	/**
	 * 开始执行对httpbody 的解析任务。
	 * @return 一个map
	 * @throws UnsupportedEncodingException
	 */
	public Map<String, Object> doParse() throws UnsupportedEncodingException {
		int len = bytes.length;


		NameValue nv = null;
		for(int i=0; i<len; i++){
			char c = (char) bytes[i];
			if(i<nameValueStart.length()){
				continue;
			}
			if(LF == c){
				if(tailMatchNameValueStart(i)){
					// 标记键值对的起始索引
					nameValueStartIndexStack.add(i+1);
					nv = new NameValue();
				}else if(tailMatchCRLF(i) && lastStartIndex() != NONE && isNew(nv)){
					// 获取键值对的键信息。
					int lastStartIndex = lastStartIndex();
					int lineLength = i-1 - lastStartIndex;
					String nameValueFirstLine = new String(bytes, lastStartIndex, lineLength, CHARSET_UTF8);

					nv.name = getName(nameValueFirstLine);
					if(nameValueFirstLine.contains("filename=\"")){
						nv.fileName = getFileName(nameValueFirstLine);
					}
				}else if(tailMatchSeperator(i) && hasName(nv)){
					// 标记键值对的值的起始索引
					valueStartIndexStack.add(i+1);
				}
			}else if('-' == c && tailMatchEnd(i) && lastValueStartIndex() != NONE){
				// 获取键值对的值信息，把解析出来的键值对信息保存到结果集
				// 并且置空临时对象NameValue nv
				int valueStartIndex = lastValueStartIndex();
				int valueEndIndex = i - (nameValueEnd.length()-1);
				byte[] valBytes = getBytes(this.bytes, valueStartIndex, valueEndIndex);
				if(nv.fileName != null){
					FileInfo f = new FileInfo(nv.fileName, valBytes);
					params.put(nv.name, f);
					continue;
				}
				params.put(nv.name, new String(valBytes, CHARSET_UTF8));
				nv = null;
			}
		}
		return params;
	}

	/**
	 * 把换行符，回车符显示出来
	 * @param raw
	 * @return
	 */
	String convert(String raw){
		StringBuilder res = new StringBuilder();
		int length = raw.length();
		for(int i=0; i<length; i++){
			char c = raw.charAt(i);
			if(CR == c){
				res.append("\\r");
			}else if(LF == c){
				res.append("\\n");
			}else{
				res.append(c);
			}
		}
		return res.toString();
	}

	/**
	 * 根据给定的起始索引与结束索引，获取一个子 字节数组
	 * @param bytes
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private byte[] getBytes(byte[] bytes, int startIndex, int endIndex) {
		int len = endIndex-startIndex;
		byte[] res = new byte[len];
		for(int i=0, j=startIndex; i<len; i++, j++){
			res[i] = bytes[j];
		}
		return res;
	}

	/**
	 * NameValue 临时对象的成员变量name是否已被初始化
	 * @param nv
	 * @return
	 */
	private boolean hasName(NameValue nv) {
		return nv != null && nv.name != null;
	}

	/**
	 * NameValue 临时对象是否为刚初始化的状态
	 * @param nv
	 * @return
	 */
	private boolean isNew(NameValue nv) {
		return nv != null && nv.name == null;
	}


	/**
	 * 获取获取键值对的键名称。
	 * @param content
	 * @return
	 */
	private String getName(String content) {
		String regex = "name=\"(?<name>[^\"]+)\"";
		Pattern compile = Pattern.compile(regex);
		Matcher matcher = compile.matcher(content);
		if(matcher.find()){
			return matcher.group("name").trim();
		}else {
			return null;
		}
	}

	/**
	 * 获取上传文件名称。
	 * @param line
	 * @return
	 */
	private String getFileName(String line){
		String regex = "filename=\"(?<name>[^\"]+)\"";
		Pattern compile = Pattern.compile(regex);
		Matcher matcher = compile.matcher(line);
		if(matcher.find()){
			return matcher.group("name");
		}else {
			return null;
		}
	}

	/**
	 * 获取当前键值对的值的起始索引
	 * @return
	 */
	private int lastValueStartIndex() {
		int len = valueStartIndexStack.size();
		if(len<1 || len != nameValueStartIndexStack.size()){
			return NONE;
		}
		return valueStartIndexStack.get(len-1);
	}

	/**
	 * 获取当前键值对的起始索引
	 * @return
	 */
	private int lastStartIndex() {
		int len = nameValueStartIndexStack.size();
		if(len>0){
			return nameValueStartIndexStack.get(len-1);
		}

		return NONE;
	}

	/**
	 * 当前位置是否为键值对的结束位置
	 * @param i
	 * @return
	 */
	private boolean tailMatchEnd(int i) {
		return tailMatch(i, nameValueEnd);
	}

	/**
	 * 当前位置是否为键值对的键与值的分隔符位置
	 * @param i
	 * @return
	 */
	private boolean tailMatchSeperator(int i) {
		return tailMatch(i, nameValueSeperator);
	}

	/**
	 * 当前位置是否匹配一个回车换行字符串
	 * @param endIndex
	 * @return
	 */
	private boolean tailMatchCRLF(int endIndex) {
		return tailMatch(endIndex, CRLF);
	}

	/**
	 * 当前位置是否为键值对的起始位置。
	 * @param endIndex
	 * @return
	 */
	private boolean tailMatchNameValueStart(int endIndex) {
		return tailMatch(endIndex, nameValueStart);
	}

	/**
	 * 从当前位置往后推(token.length()-1)个位置是否匹配token
	 * @param endIndex 当前位置
	 * @param token 标记字符串
	 * @return
	 */
	private boolean tailMatch(int endIndex, String token) {
		int startIndex = endIndex-(token.length()-1);
		for(int i=startIndex, j=0; i<= endIndex; i++, j++){
			char c = (char) bytes[i];
			if(c != token.charAt(j)){
				return false;
			}
		}
		return true;
	}
}