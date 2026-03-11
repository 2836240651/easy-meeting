package com.easymeeting.utils;

public class TableSplitUtils {
    private static final String SPLIT_TABLE_MEETING_CHAT_MESSAGE="meeting_chat_message";
    private static final String CREATE_TABLE_TEMP="CREATE TABLE IF NOT EXISTS %s like %s ";
    private static final Integer SPLIT_TABLE_COUNT = 32;




    public static String getCreateTableSql(String templateTableName,Integer tableIndex,Integer tableCount){
        Integer padLen = String.valueOf(tableCount).length(); // 获取tableCount的位数
        String tableName = templateTableName+"_"+String.format("%0"+padLen+"d",tableIndex); // 格式化tableIndex，使其位数与tableCount一致
        return String.format(CREATE_TABLE_TEMP,tableName,SPLIT_TABLE_MEETING_CHAT_MESSAGE);//创建表的SQL语句
    }
    private static String getTableName(String prefix,Integer tableCount,String key){
        int hashCode = Math.abs(murmurHash(key));
        int tableNum=hashCode % SPLIT_TABLE_COUNT+1;
        int tableNumLength = String.valueOf(tableCount).length();
        return prefix+"_"+String.format("%0"+tableNumLength+"d",tableNum); //%0xd    % 开头表示后面跟的是格式修改符。
       // 0 表示如果格式化的数的位数不足，会用零填充。
        //X 是 tableNumLength，这个变量定义了总的宽度，即生成的字符串的长度。
        //d 表示格式化成一个十进制整数。
    }
    public static String getMeetingChatMessageTable(String meetingId){
        return getTableName(SPLIT_TABLE_MEETING_CHAT_MESSAGE,SPLIT_TABLE_COUNT,meetingId);
    }
    private static void getSplitTableCreateSql(){
        for (Integer i = 1; i <= SPLIT_TABLE_COUNT; i++) {
            System.out.println(getCreateTableSql(SPLIT_TABLE_MEETING_CHAT_MESSAGE, i, SPLIT_TABLE_COUNT));
        }
    }







    private static int murmurHash(String key) {
        final byte[] data = key.getBytes();
        final int length = data.length;
        final int seed = 0x9747b28c;
        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ length;
        int len_4 = length >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k = k << 8;
            k = k | (data[i_4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 1] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 0] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        int len_m = len_4 << 2;
        int left = length - len_m;

        if (left != 0) {
            if (left >= 3) h ^= (data[length - 3] & 0xff) << 16;
            if (left >= 2) h ^= (data[length - 2] & 0xff) << 8;
            if (left >= 1) h ^= (data[length - 1] & 0xff);

            h *= m;
        } 

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
}
