package com.wjy.mybatis.desensitize.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wjy.mybatis.desensitize.constant.SensitizeMode;

public class InterceptorContext {

    private static Logger log = LoggerFactory.getLogger(BaseSensitizeInterceptor.class);

    private boolean isRollBack; // 是否回滚
    private SensitizeMode mode; // 模式
    private HashMap<Integer, HashMap<Method, Object>> rollBackEntityMap; // 记录pojo对象的set方法和属性变更前的值，回滚用
    private HashMap<Integer, Object> rollBackParamsMap; // 记录基础类型参数的位置索引和变更前的值，回滚用
    private int index; // 遍历list时当前索引

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
     * 缓存pojo对象修改前的值
     * 
     * @param md pojo对象属性的set方法
     * @param obj pojo对象属性修改前的值
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
     * 缓存基础类型修改前的值
     * 
     * @param index 参数列表索引位置
     * @param obj 参数修改前的值
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
     * 回滚单个pojo对象，恢复修改前的值
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
     * 回滚pojo对象列表，恢复修改前的值
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
     * 回滚基础类型参数列表，恢复修改前的值
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
     * 回滚基础类型参数列表，恢复修改前的值
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
