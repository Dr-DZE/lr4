package com.example.tryme.aspect; 

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.example.tryme.services..*)")
    public void serviceMethods() {
    }

    @Pointcut("within(com.example.tryme.Controller..*)") 
    public void controllerMethods() {
    }

    @Pointcut("within(com.example.tryme.Repository..*)") 
    public void repositoryMethods() {
    }

    @Before("controllerMethods() || serviceMethods() || repositoryMethods()")
    public void logMethodCall(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("Вызов метода: {}.{}() с аргументами: {}", className, methodName, Arrays.toString(args));
    }

    @AfterReturning(pointcut = "controllerMethods() || serviceMethods() || repositoryMethods()", returning = "result")
    public void logMethodReturn(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        String resultString = "null";
        if (result != null) {
            resultString = result.toString();
            if (resultString.length() > 200) { 
                resultString = resultString.substring(0, 197) + "... (trimmed)";
            }
        }
        logger.info("Метод {}.{}() успешно выполнен с результатом: {}", className, methodName, resultString);
    }

    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods() || repositoryMethods()", throwing = "exception")
    public void logMethodException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        logger.error("Ошибка в методе {}.{}(): {}", className, methodName, exception.getMessage(), exception);
    }

    @Around("serviceMethods()")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result; 
        try {
            result = joinPoint.proceed(); 
            return result; 
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            logger.debug("Метод сервиса {}.{}() выполнен за {} мс", className, methodName, executionTime);
        }
    }
}