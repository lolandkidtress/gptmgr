package com.openapi.tools;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ScheduleThreadPool {

  private static Logger logger = LogManager.getLogger(ScheduleThreadPool.class);

  static ThreadPoolExecutor executor;

  static ThreadPoolExecutor chatroomExecutor;

  static ThreadPoolExecutor momentExecutor;

  static ThreadPoolExecutor friendExecutor;

  static ThreadPoolExecutor backExecutor;

  static ThreadPoolExecutor redirectExecutor;

  static {
    init();
  }

  static void init(){
    int corePoolSize = 1;
    int maximumPoolSize = 100;
    long keepAliveTime = 10;
    TimeUnit unit = TimeUnit.SECONDS;
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1);
    ThreadFactory threadFactory = new NameTreadFactory();
    RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

    RejectedExecutionHandler chatroomHandler = new RerunPolicy();
    RejectedExecutionHandler momentHandler = new RerunPolicy();
    RejectedExecutionHandler friendHandler = new RerunPolicy();

    executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, handler);
    executor.prestartAllCoreThreads(); // 预启动所有核心线程

    chatroomExecutor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, chatroomHandler);
    chatroomExecutor.prestartAllCoreThreads();

    momentExecutor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, momentHandler);
    momentExecutor.prestartAllCoreThreads();

    friendExecutor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, friendHandler);
    friendExecutor.prestartAllCoreThreads();

    backExecutor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, handler);
    backExecutor.prestartAllCoreThreads();

    redirectExecutor= new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, threadFactory, handler);
    redirectExecutor.prestartAllCoreThreads();

  }

  static class NameTreadFactory implements ThreadFactory {

    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "my-thread-" + mThreadNum.getAndIncrement());
      logger.info(t.getName() + " has been created");
      return t;
    }
  }

  public static class RerunPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
      doLog(r, e);
    }

    private void doLog(Runnable r, ThreadPoolExecutor e) {
      // 可做日志记录等
      logger.error( r.toString() + " rejected");
      //扔到队列
      backExecutor.execute(r);
    }
  }

  public static ThreadPoolExecutor getExcecutor(){
    return executor;
  }

  public static ThreadPoolExecutor getChatroomExcecutor(){
    return chatroomExecutor;
  }

  public static ThreadPoolExecutor getMomentExcecutor(){
    return momentExecutor;
  }

  public static ThreadPoolExecutor getFriendExcecutor(){
    return friendExecutor;
  }

  public static ThreadPoolExecutor getRedirectExecutor() {
    return redirectExecutor;
  }

  public static void main(String[] args) {

  }

}
