package com.wjy.mybatis.desensitize.spi;

public interface ISensitize {

    /**
     * �������ݻָ�
     */
    String resensitize(Object data);

    /**
     * ����
     */
    String desensitize(Object data);

    /**
     * �Ƿ�����
     */
    boolean isEnable();

}
