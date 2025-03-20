package com.example.couponcore.component;

import com.example.couponcore.exception.LockAcquisitionException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class DistributeLockExecutor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final RedissonClient redissonClient;

    public void execute(Runnable runnable, String lockName, long waitMs, long leaseMs) {
        RLock lock = redissonClient.getLock(lockName);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitMs, leaseMs, TimeUnit.MILLISECONDS);
            if (!isLocked) {
                throw new LockAcquisitionException(lockName, null);
            }

            runnable.run();

            lock.unlock();
            isLocked = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockName, e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
