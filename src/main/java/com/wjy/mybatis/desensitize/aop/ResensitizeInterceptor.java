package com.wjy.mybatis.desensitize.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * ÍÑÃôÊý¾Ý»Ö¸´
 */
public class ResensitizeInterceptor extends BaseSensitizeInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    }

    public Object afterReturnByParam(ProceedingJoinPoint jp) throws Throwable {
        if (isEnable()) {
            return resensitizeParams(((MethodSignature) jp.getSignature()).getMethod(), jp.proceed());
        }
        return jp.proceed();
    }

    public Object afterReturnByEntity(ProceedingJoinPoint jp) throws Throwable {
        if (isEnable()) {
            return resensitizeEntitys(((MethodSignature) jp.getSignature()).getMethod(), jp.proceed());
        }
        return jp.proceed();
    }

}
