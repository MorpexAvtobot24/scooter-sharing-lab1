package com.course.app.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScooterAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ScooterAsyncExceptionHandler.class);

    private final AtomicReference<AsyncFailure> lastFailure = new AtomicReference<>();
    private final CountDownLatch failureSignal = new CountDownLatch(1);

    @Override
    public void handleUncaughtException(Throwable exception, Method method, Object... params) {
        lastFailure.set(new AsyncFailure(method.getName(), params.clone(), exception));
        failureSignal.countDown();
        log.error("Uncaught exception in async method {} with arguments {}",
                method.getName(), Arrays.toString(params), exception);
    }

    public AsyncFailure getLastFailure() {
        return lastFailure.get();
    }

    public boolean awaitFailure(long timeout, TimeUnit unit) throws InterruptedException {
        return failureSignal.await(timeout, unit);
    }

    public record AsyncFailure(String methodName, Object[] parameters, Throwable exception) {
    }
}
