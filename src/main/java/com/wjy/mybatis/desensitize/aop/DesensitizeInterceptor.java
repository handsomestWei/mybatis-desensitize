package com.wjy.mybatis.desensitize.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Õ—√Ù
 */
public class DesensitizeInterceptor extends BaseSensitizeInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    }

    public Object beforeByParam(ProceedingJoinPoint jp) throws Throwable {
        if (isEnable()) {
            return jp.proceed(desensitizeParams(((MethodSignature) jp.getSignature()).getMethod(), jp.getArgs()));
        }
        return jp.proceed();
    }

    public Object beforeByEntity(ProceedingJoinPoint jp) throws Throwable {
        if (isEnable()) {
            return jp.proceed(desensitizeEntitys(((MethodSignature) jp.getSignature()).getMethod(), jp.getArgs()));
        }
        return jp.proceed();
    }

}
