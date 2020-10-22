package com.ctrip.framework.apollo.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ApolloThreadFactory implements ThreadFactory {
  private static Logger log = LoggerFactory.getLogger(ApolloThreadFactory.class);

  private final AtomicLong threadNumber = new AtomicLong(1);

  private final String namePrefix;

  private final boolean daemon;

  private static final ThreadGroup threadGroup = new ThreadGroup("Apollo");

  public static ThreadGroup getThreadGroup() {
    return threadGroup;
  }

  public static ThreadFactory create(String namePrefix, boolean daemon) {
    return new ApolloThreadFactory(namePrefix, daemon);
  }

  public static boolean waitAllShutdown(int timeoutInMillis) {
    ThreadGroup group = getThreadGroup();
    Thread[] activeThreads = new Thread[group.activeCount()];
    group.enumerate(activeThreads);
    Set<Thread> alives = new HashSet<>(Arrays.asList(activeThreads));
    Set<Thread> dies = new HashSet<>();
    log.info("Current ACTIVE thread count is: {}", alives.size());
    long expire = System.currentTimeMillis() + timeoutInMillis;
    while (System.currentTimeMillis() < expire) {
      classify(alives, dies, new ClassifyStandard<Thread>() {
        @Override
        public boolean satisfy(Thread thread) {
          return !thread.isAlive() || thread.isInterrupted() || thread.isDaemon();
        }
      });
      if (alives.size() > 0) {
        log.info("Alive apollo threads: {}", alives);
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
          // ignore
        }
      } else {
        log.info("All apollo threads are shutdown.");
        return true;
      }
    }
    log.warn("Some apollo threads are still alive but expire time has reached, alive threads: {}",
        alives);
    return false;
  }

  private interface ClassifyStandard<T> {
    boolean satisfy(T thread);
  }

  private static <T> void classify(Set<T> src, Set<T> des, ClassifyStandard<T> standard) {
    Set<T> set = new HashSet<>();
    for (T t : src) {
      if (standard.satisfy(t)) {
        set.add(t);
      }
    }
    src.removeAll(set);
    des.addAll(set);
  }

  private ApolloThreadFactory(String namePrefix, boolean daemon) {
    this.namePrefix = namePrefix;
    this.daemon = daemon;
  }

  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(threadGroup, runnable,//
        threadGroup.getName() + "-" + namePrefix + "-" + threadNumber.getAndIncrement());
    thread.setDaemon(daemon);
    if (thread.getPriority() != Thread.NORM_PRIORITY) {
      thread.setPriority(Thread.NORM_PRIORITY);
    }
    return thread;
  }
}
