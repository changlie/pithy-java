package com.changlie.demo;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class CharTest {
    public static void main(String[] args) {
        try(InputStream is = new FileInputStream("/home/changlie/demo");){
            int len = is.available();
            byte[] bytes = new byte[len];
            is.read(bytes, 0, len);
            System.out.println(Arrays.toString(bytes));
            System.out.println(new String(bytes, "utf-8"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
