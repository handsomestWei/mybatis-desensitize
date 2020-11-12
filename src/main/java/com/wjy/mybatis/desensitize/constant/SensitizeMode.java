package com.wjy.mybatis.desensitize.constant;

public enum SensitizeMode {
    
    /**
     * 脱敏
     */
    Desensitize(),
    /**
     * 已脱敏的数据恢复
     */
    Resensitize();

    SensitizeMode() {
    }
}
