package com.wjy.mybatis.desensitize.aop;


import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import com.wjy.mybatis.desensitize.annotation.ReturnFeild;
import com.wjy.mybatis.desensitize.constant.SensitizeMode;
import com.wjy.mybatis.desensitize.spi.ISensitize;

public class BaseSensitizeInterceptor {

    private static Logger log = LoggerFactory.getLogger(BaseSensitizeInterceptor.class);

    private ISensitize spi;

    private HashSet<String> feildSet = new HashSet<String>(); // 要处理的目标字段
    private boolean enable = true; // 是否启用
    private boolean isRollBack = false; // 是否回滚
    private HashMap<String, HashSet<String>> reEntityHitFieldSet; // 恢复用：缓存pojo对象目标字段，按方法全名区分
    private HashMap<String, HashSet<String>> deEntityHitFieldSet; // 脱敏用：缓存pojo对象目标字段，按方法全名区分

    /**
     * 脱敏：基础类型参数
     */
    public Object[] desensitizeParams(Method md, Object[] args) {
        if (args == null) {
            return args;
        }
        InterceptorContext context = InterceptorContext.Of(SensitizeMode.Desensitize, isRollBack);
        try {
            args = desensitizeParamList(md, args, context);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            args = context.rollBackParam(args);
        }
        return args;
    }

    /**
     * 脱敏：pojo对象属性
     */
    public Object[] desensitizeEntitys(Method md, Object[] args) {
        // TODO 只处理第一个对象
        if (args == null || args[0] == null) {
            return args;
        }
        InterceptorContext context = InterceptorContext.Of(SensitizeMode.Desensitize, isRollBack);
        try {
            args[0] = deOrResensitizeEntity(md.toGenericString(), args[0], context);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            args[0] = context.rollBackEntity(args[0]);
        }
        return args;
    }

    /**
     * 脱敏数据恢复：基础类型参数
     */
    @SuppressWarnings("rawtypes")
    public Object resensitizeParams(Method md, Object args) {
        if (args == null) {
            return args;
        }
        if (args instanceof List) {
            List ls = (List) args;
            InterceptorContext context = InterceptorContext.Of(SensitizeMode.Resensitize, isRollBack);
            try {
                args = resensitizeParamList(md, ls, context);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                args = context.rollBackParam(ls);
            }
            return args;
        } else {
            try {
                args = resensitizeParam(md, args, "");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return args;
        }
    }

    /**
     * 脱敏数据恢复：pojo对象属性
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object resensitizeEntitys(Method md, Object args) {
        if (args == null) {
            return args;
        }
        String methodGenericName = md.toGenericString();
        if (args instanceof List) {
            List ls = (List) args;
            InterceptorContext context = InterceptorContext.Of(SensitizeMode.Resensitize,isRollBack);
            try {
                for (int i = 0; i < ls.size(); i++) {
                    ls.set(i, deOrResensitizeEntity(methodGenericName, ls.get(i), context.setIndex(i)));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                ls = context.rollBackEntity(ls);
            }         
            return ls;
        } else {
            InterceptorContext context = InterceptorContext.Of(SensitizeMode.Resensitize,isRollBack);
            try {
                args = deOrResensitizeEntity(methodGenericName, args, context);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                args = context.rollBackEntity(args);
            }
            return args;
        }
    }

    /**
     * 脱敏：基础类型参数列表
     */
    private Object[] desensitizeParamList(Method md, Object[] args, InterceptorContext context) throws Exception {
        // 第一维度表示参数列表，按方法参数声明的顺序排列
        // 第二维度表示该参数的注解列表
        Annotation[][] parameterAnnotations = md.getParameterAnnotations();
        if (parameterAnnotations != null && parameterAnnotations.length > 0) {
            int i = 0;
            for (Annotation[] parameterAnnotation : parameterAnnotations) {
                for (Annotation annotation : parameterAnnotation) {
                    // 定位到mybatis的Param注解，获取参数别名
                    if (annotation instanceof org.apache.ibatis.annotations.Param) {
                        String alias = ((Param) annotation).value();
                        if (feildSet.contains(alias)) {
                            try {
                                // 修改参数
                                Object originalValue = args[i];
                                args[i] = spi.desensitize(originalValue);
                                context.cacheParam(i, originalValue);
                            } catch (Exception e) {
                                throw e;
                            }
                        }
                        break;
                    }
                }
                i++;
            }
        }
        return args;
    }

    /**
     * 脱敏数据恢复：单个基础类型参数
     */
    private Object resensitizeParam(Method md, Object obj, String alias) throws Exception {
        if (StringUtils.isEmpty(alias)) {
            ReturnFeild t = md.getAnnotation(com.wjy.mybatis.desensitize.annotation.ReturnFeild.class);
            if (t == null) {
                return obj;
            }
            alias = t.value();
        }
        if (feildSet.contains(alias)) {
            try {
                obj = spi.resensitize(obj);
            } catch (Exception e) {
                throw e;
            }
        }
        return obj;
    }

    /**
     * 脱敏数据恢复：基础类型参数List
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object resensitizeParamList(Method md, List ls, InterceptorContext context) throws Exception {
        for (int i = 0; i < ls.size(); i++) {
            ReturnFeild t = md.getAnnotation(com.wjy.mybatis.desensitize.annotation.ReturnFeild.class);
            if (t == null) {
                continue;
            }
            try {
                Object originalValue = ls.get(i);
                ls.set(i, resensitizeParam(md, originalValue, t.value()));
                context.cacheParam(i, originalValue);
            } catch (Exception e) {
                throw e;
            }
        }
        return ls;
    }

    private Object deOrResensitizeEntity(String methodGenericName, Object entity, InterceptorContext context)
            throws Exception {

        // 获取要处理的字段
        Iterator<String> it = null;
        boolean isHit = false; // 是否已缓存
        HashSet<String> hitFieldSet = null;
        if (SensitizeMode.Desensitize.equals(context.getMode()) && deEntityHitFieldSet.containsKey(methodGenericName)) {
            it = deEntityHitFieldSet.get(methodGenericName).iterator();
            isHit = true;
        } else if (SensitizeMode.Resensitize.equals(context.getMode())
                && reEntityHitFieldSet.containsKey(methodGenericName)) {
            it = reEntityHitFieldSet.get(methodGenericName).iterator();
            isHit = true;
        } else {
            it = feildSet.iterator();
            hitFieldSet = new HashSet<>();
        }

        Class<?> clazz = entity.getClass();
        while (it.hasNext()) {
            String fieldName = it.next();
            PropertyDescriptor ps = BeanUtils.getPropertyDescriptor(clazz, fieldName);
            if (ps != null && ps.getReadMethod() != null && ps.getWriteMethod() != null) {
                try {
                    if (!isHit) {
                        hitFieldSet.add(fieldName);
                    }
                    Object value = ps.getReadMethod().invoke(entity);
                    if (value != null) {
                        // 修改字段：先调用bean的get方法取值，后调用set方法回填
                        Object originalValue = value;
                        Method writeMd = ps.getWriteMethod();
                        if (SensitizeMode.Desensitize.equals(context.getMode())) {
                            writeMd.invoke(entity, spi.desensitize(originalValue));
                        } else {
                            writeMd.invoke(entity, spi.resensitize(originalValue));
                        }
                        // 缓存修改前的值，回滚用
                        context.cacheEntity(writeMd, originalValue);
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
        }

        // 缓存命中字段
        if (!isHit) {
            if (SensitizeMode.Desensitize.equals(context.getMode())) {
                deEntityHitFieldSet.put(methodGenericName, hitFieldSet);
            } else if (SensitizeMode.Resensitize.equals(context.getMode())) {
                reEntityHitFieldSet.put(methodGenericName, hitFieldSet);
            }
        }

        return entity;
    }

    public ISensitize getSpi() {
        return spi;
    }

    public void setSpi(ISensitize spi) {
        this.spi = spi;
    }

    public HashSet<String> getFeildSet() {
        return feildSet;
    }

    public void setFeildSet(String feilds) {
        String[] feildArray = feilds.split(",");
        for (String feild : feildArray) {
            this.feildSet.add(feild);
        }
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isRollBack() {
        return isRollBack;
    }

    public void setRollBack(boolean isRollBack) {
        this.isRollBack = isRollBack;
    }

}
