# mybatis-desensitize
基于mybatis框架的数据脱敏方案，使用spring aop拦截mybatis的dao接口。功能包括：
+ 数据脱敏：数据脱敏后更新到数据库
+ 脱敏数据恢复：从数据库查询脱敏数据，恢复真实值
+ 数据回滚

# Usage

## maven依赖
```
<dependency>
	<groupId>com.wjy</groupId>
	<artifactId>mybatis-desensitize</artifactId>
	<version>1.0.0</version>
</dependency>
```

## 目标字段配置
配置文件*.properties配置，逗号`,`分割
```
## 脱敏目标字段例
de_sensitize_feilds = "mobile,idCardNo"
## 脱敏恢复字段例
re_sensitize_feilds = "mobile,bankNo"
```

## 目标字段匹配
+ 脱敏字段为基础类型：使用`mybatis`的`@param`注解指定别名
+ 脱敏字段为pojo对象：反射获取pojo对象的属性
+ 脱敏数据恢复字段为基础类型：使用自定义的`@ReturnFeild`注解指定别名
+ 脱敏恢复数据字段为pojo对象：反射获取pojo对象的属性

## 数据处理实现
使用者需提供自己的数据脱敏和脱敏恢复方法，实现`com.wjy.mybatis.desensitize.spi.ISensitize`接口

## 拦截器注册
注册数据脱敏拦截器，配置要处理的脱敏字段
```
<bean id="customSpi" class="xxxxx">

<bean id="desensitizeInterceptor" class="com.wjy.mybatis.desensitize.aop.DesensitizeInterceptor">
      <property name="spi" ref="customSpi" />
      <property name="feildSet" value="${de_sensitize_feilds}" />
</bean>
```
注册数据脱敏恢复拦截器，配置要处理的脱敏恢复字段
```
<bean id="customSpi" class="xxxxx">

<bean id="resensitizeInterceptor" class="com.wjy.mybatis.desensitize.aop.ResensitizeInterceptor">
      <property name="spi" ref="customSpi" />
      <property name="feildSet" value="${re_sensitize_feilds}" />
</bean>
```

## 拦截器配置
数据脱敏后更新到数据库，参数为pojo类型
```
<aop:aspect ref="desensitizeInterceptor">
        <aop:pointcut id="beforeByEntity" expression="execution(public * com.xxx.dao.xxxDao.insert(..))" ></aop:pointcut>
        <aop:around pointcut-ref="beforeByEntity" method="beforeByEntity"></aop:around>
</aop:aspect>
```
数据脱敏后更新到数据库，参数为基础类型
```
<aop:aspect ref="desensitizeInterceptor">
        <aop:pointcut id="beforeByParam" expression="execution(public * com.xxx.dao.xxxDao.update(..))" ></aop:pointcut>
        <aop:around pointcut-ref="beforeByParam" method="beforeByParam"></aop:around>
</aop:aspect>
```
从数据库查询脱敏数据，恢复真实值。返回值为实体类型
```
<aop:aspect ref="resensitizeInterceptor">
        <aop:pointcut id="afterReturnByEntity" expression="execution(public * com.xxx.dao.xxxDao.select(..))" ></aop:pointcut>
        <aop:around pointcut-ref="afterReturnByEntity" method="afterReturnByEntity"></aop:around>
</aop:aspect>
```
从数据库查询脱敏数据，恢复真实值。返回值为基础类型
```
<aop:aspect ref="resensitizeInterceptor">
        <aop:pointcut id="afterReturnByParam" expression="execution(public * com.xxx.dao.xxxDao.selectXX(..))"></aop:pointcut>
        <aop:around pointcut-ref="afterReturnByParam" method="afterReturnByParam"></aop:around>
</aop:aspect>
```