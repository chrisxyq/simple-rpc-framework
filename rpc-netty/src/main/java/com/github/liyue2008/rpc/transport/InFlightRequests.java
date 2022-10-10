/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liyue2008.rpc.transport;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
public class InFlightRequests implements Closeable {
    private final static long TIMEOUT_SEC = 10L;
    /**
     * 在实现异步网络传输的时候，一定要配套实现一个背压的机制，避免客户端请求速度过快，导致大量的请求失败。
     * 在服务端处理不过来的时候限制一下客户端的请求速度。
     * 这个信号量有 10 个许可，我们每次往 inFlightRequest 中加入一个 ResponseFuture 的时候，
     * 需要先从信号量中获得一个许可，如果这时候没有许可了，就会阻塞当前这个线程，也就是发送请求的这个线程，
     * 直到有人归还了许可，才能继续发送请求。我们每结束一个在途请求，就归还一个许可，
     * 这样就可以保证在途请求的数量最多不超过 10 个请求，积压在服务端正在处理或者待处理的请求也不会超过 10 个。
     */
    private final Semaphore semaphore = new Semaphore(10);
    /**
     * 用于维护所有的在途请求，key为请求id，value为返回值
     */
    private final Map<Integer, ResponseFuture> futureMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledFuture scheduledFuture;
    public InFlightRequests() {
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::removeTimeoutFutures, TIMEOUT_SEC, TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    public void put(ResponseFuture responseFuture) throws InterruptedException, TimeoutException {
        if(semaphore.tryAcquire(TIMEOUT_SEC, TimeUnit.SECONDS)) {
            futureMap.put(responseFuture.getRequestId(), responseFuture);
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * 即使是我们对所有能捕获的异常都做了处理，也不能保证所有 ResponseFuture 都能正常或者异常结束，
     * 比如说，编写对端程序的程序员写的代码有问题，收到了请求就是没给我们返回响应，
     * 为了应对这种情况，还必须有一个兜底超时的机制来保证所有情况下 ResponseFuture 都能结束，
     * 无论什么情况，只要超过了超时时间还没有收到响应，我们就认为这个 ResponseFuture 失败了，结束并删除它
     */
    private void removeTimeoutFutures() {
        futureMap.entrySet().removeIf(entry -> {
            if( System.nanoTime() - entry.getValue().getTimestamp() > TIMEOUT_SEC * 1000000000L) {
                semaphore.release();
                return true;
            } else {
                return false;
            }
        });
    }

    public ResponseFuture remove(int requestId) {
        ResponseFuture future = futureMap.remove(requestId);
        if(null != future) {
            semaphore.release();
        }
        return future;
    }

    @Override
    public void close() {
        scheduledFuture.cancel(true);
        scheduledExecutorService.shutdown();
    }
}
