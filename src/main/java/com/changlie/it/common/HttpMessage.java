package com.changlie.it.common;

/**
 * 解决数据用的常量定义集合
 */
public interface HttpMessage {
	/** 回车 */
	char CR='\r';
	/** 换行 */
	char LF='\n';
	/** 回车换行 */
	String CRLF="\r\n";

	/** 请求/响应 body长度(头名称) */
	String CONTENT_LENGTH="Content-Length";

	/** 响应分块头 的名称 */
	String TRANSFER_ENCODING = "Transfer-Encoding";

	/** 响应分块头 的值 */
	String BODY_TYPE = "chunked";

	/** 请求body参数类型(头名称) */
	String CONTENT_TYPE = "Content-Type";


	/** 默认编码 */
	String DEFAULT_CHARSET = "utf-8";
//	String DEFAULT_CHARSET = "ISO-8859-1";

	/** utf8编码 */
	String CHARSET_UTF8 = "utf-8";

	/** form表单带文件请求类型 */
	String MULTIPART_FORM_DATA = "multipart/form-data";


	default String getValByUTF8(String origin){
		try {
			byte[] bytes = origin.getBytes(DEFAULT_CHARSET);
			return new String(bytes, CHARSET_UTF8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return origin;
	}
}