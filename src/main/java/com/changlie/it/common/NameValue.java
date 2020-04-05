package com.changlie.it.common;

/**
 * 用于临时记录 multipart/form-data 类型的httpbody的  bodypart信息
 */
public class NameValue {
	public String name;
	public Object value;

	public String fileName;

	boolean isFinish(){
		return name != null && value != null;
	}
}



