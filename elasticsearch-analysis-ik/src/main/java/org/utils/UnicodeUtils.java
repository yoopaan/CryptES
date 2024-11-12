package org.utils;

public class UnicodeUtils {

    /**
     * 字符串加密: 将字符串Unicode编码加1
     *
     * @param str
     * @return
     */
    public static String encryptStr(String str) {
        str = (str == null ? "" : str);
        StringBuffer sb = new StringBuffer(1000);
        char c;
        sb.setLength(0);
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            int pos = Integer.valueOf(c).intValue();
            sb.append((char)(pos+1));
        }
        return (new String(sb));
    }

    /**
     * 字符串解密: 将字符串Unicode编码减1
     *
     * @param str
     * @return
     */
    public static String decryptStr(String str) {
        str = (str == null ? "" : str);
        StringBuffer sb = new StringBuffer(1000);
        char c;
        sb.setLength(0);
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            int pos = Integer.valueOf(c).intValue();
            sb.append((char)(pos-1));
        }
        return (new String(sb));
    }


    public static void main(String[] args) {
        String s1 = "456";
        String ens1 = encryptStr(s1);
        System.out.println(ens1);
        String des1 = decryptStr(ens1);
        System.out.println(des1);
        System.out.println("\u262F");
    }

}
