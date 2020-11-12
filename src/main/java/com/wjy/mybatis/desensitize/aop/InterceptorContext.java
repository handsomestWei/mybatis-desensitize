package com.wjy.mybatis.desensitize.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wjy.mybatis.desensitize.constant.SensitizeMode;

public class InterceptorContext {

    private static Logger log = LoggerFactory.getLogger(BaseSensitizeInterceptor.class);

    private boolean isRollBack; // �Ƿ�ع�
    private SensitizeMode mode; // ģʽ
    private HashMap<Integer, HashMap<Method, Object>> rollBackEntityMap; // ��¼pojo�����set���������Ա��ǰ��ֵ���ع���
    private HashMap<Integer, Object> rollBackParamsMap; // ��¼�������Ͳ�����λ�������ͱ��ǰ��ֵ���ع���
    private int index; // ����listʱ��ǰ����

    private InterceptorContext(SensitizeMode mode, boolean isRollBack) {
        this.mode = mode;
        this.rollBackEntityMap = new HashMap<>();
        this.rollBackParamsMap = new HashMap<>();
        this.isRollBack = isRollBack;
    }

    public static InterceptorContext Of(SensitizeMode mode, boolean isRollBack) {
        return new InterceptorContext(mode, isRollBack);
    }

    public InterceptorContext setIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * ����pojo�����޸�ǰ��ֵ
     * 
     * @param md pojo�������Ե�set����
     * @param obj pojo���������޸�ǰ��ֵ
     */
    public void cacheEntity(Method md, Object obj) {
        if (isRollBack) {
            HashMap<Method, Object> mp = this.rollBackEntityMap.get(this.index);
            if (mp == null) {
                mp = new HashMap<Method, Object>();
                mp.put(md, obj);
                this.rollBackEntityMap.put(this.index, mp);
            } else {
                mp.put(md, obj);
            }
        }
    }

    /**
     * ������������޸�ǰ��ֵ
     * 
     * @param index �����б�����λ��
     * @param obj �����޸�ǰ��ֵ
     */
    public void cacheParam(Integer index, Object obj) {
        if (isRollBack) {
            rollBackParamsMap.put(index, obj);
        }
    }

    public SensitizeMode getMode() {
        return this.mode;
    }

    /**
     * �ع�����pojo���󣬻ָ��޸�ǰ��ֵ
     */
    public Object rollBackEntity(Object obj) {
        if (!isRollBack || obj == null) {
            return obj;
        }
        try {
            for (Method md : rollBackEntityMap.get(0).keySet()) {
                md.invoke(obj, rollBackEntityMap.get(0).get(md));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return obj;
    }

    /**
     * �ع�pojo�����б��ָ��޸�ǰ��ֵ
     */
    @SuppressWarnings("rawtypes")
    public List rollBackEntity(List objs) {
        if (!isRollBack || objs == null || objs.size() == 0) {
            return objs;
        }
        try {
            for (Integer index : rollBackEntityMap.keySet()) {
                for (Method md : rollBackEntityMap.get(index).keySet()) {
                    md.invoke(objs.get(index), rollBackEntityMap.get(index).get(md));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return objs;
    }

    /**
     * �ع��������Ͳ����б��ָ��޸�ǰ��ֵ
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List rollBackParam(List objs) {
        if (!isRollBack || objs == null || objs.size() == 0) {
            return objs;
        }
        for (Integer index : rollBackParamsMap.keySet()) {
            objs.set(index, rollBackParamsMap.get(index));
        }
        return objs;
    }

    /**
     * �ع��������Ͳ����б��ָ��޸�ǰ��ֵ
     */
    public Object[] rollBackParam(Object[] objs) {
        if (!isRollBack || objs == null || objs.length == 0) {
            return objs;
        }
        for (Integer index : rollBackParamsMap.keySet()) {
            objs[index] = rollBackParamsMap.get(index);
        }
        return objs;
    }

}
