package com.changlie.demo;

public class CharTest1 {
    public static void main(String[] args) {
        String s = "天上𥊍𪚥";
        System.out.println("len: "+ s.length());
        for(int i=0; i<s.length(); i++){
            System.out.println(i+": "+s.charAt(i));
        }
        System.out.println("source: "+ s);
    }
}
