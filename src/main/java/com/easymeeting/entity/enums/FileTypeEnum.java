package com.easymeeting.entity.enums;
public enum FileTypeEnum {
    IMAGE(0,new String[]{".jpeg",".jpg",".png",".gif",".bmp",".webp"},".jpg","图片"),
    VIDEO(1,new String[]{".mp4",".avi","rmvb",".mkv",".mov"},".mp4","视频");
    private Integer type;
    private String[] suffixArray;
    private String suffix;
    private String desc;
    public static FileTypeEnum getByType(Integer type) {
        for (FileTypeEnum item : FileTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

    public static FileTypeEnum getBySuffix(String suffix) {
        for (FileTypeEnum item : FileTypeEnum.values()) {
            if (item.getSuffix().equals(suffix)) {
                return item;
            }
        }
        return null;
    }


    private FileTypeEnum(Integer type, String[] suffixArray, String suffix, String desc) {
        this.type = type;
        this.suffixArray = suffixArray;
        this.suffix = suffix;
        this.desc = desc;
    }

    public String[] getSuffixArray() {
        return suffixArray;
    }

    public Integer getType() {
        return type;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getDesc() {
        return desc;
    }
}
