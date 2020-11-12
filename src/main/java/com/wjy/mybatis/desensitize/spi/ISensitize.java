package com.wjy.mybatis.desensitize.spi;

public interface ISensitize {

    /**
     * Õ—√Ù ˝æ›ª÷∏¥
     */
    String resensitize(Object data);

    /**
     * Õ—√Ù
     */
    String desensitize(Object data);

    /**
     *  «∑Ò∆Ù”√
     */
    boolean isEnable();

}
