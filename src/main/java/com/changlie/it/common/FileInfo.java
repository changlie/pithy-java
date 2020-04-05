package com.changlie.it.common;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 用于记录上传文件信息。
 */
public class FileInfo{
	private String name;
	private byte[] data;

	public FileInfo() {
	}

	public FileInfo(String name, byte[] data) {
		this.name = name;
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void saveFile(String path){
		File f = new File(path);
		try (FileOutputStream os = new FileOutputStream(f)) {
			os.write(data, 0, data.length);
			os.flush();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}

