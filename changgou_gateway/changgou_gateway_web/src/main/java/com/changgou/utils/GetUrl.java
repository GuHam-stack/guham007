package com.changgou.utils;

public class GetUrl {
    public static boolean getUrl(String url){
        String arr = "/api/user/login,/api/user/add";
        String[] strings = arr.split(",");
        for (String string : strings) {
            if (url.equals(string)){
                return true;
            }
        }
        return false;
    }
}
