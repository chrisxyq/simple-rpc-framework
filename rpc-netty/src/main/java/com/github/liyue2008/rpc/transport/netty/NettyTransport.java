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
package com.github.liyue2008.rpc.transport.netty;

import com.github.liyue2008.rpc.transport.InFlightRequests;
import com.github.liyue2008.rpc.transport.ResponseFuture;
import com.github.liyue2008.rpc.transport.Transport;
import com.github.liyue2008.rpc.transport.command.Command;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.CompletableFuture;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
public class NettyTransport implements Transport {
    private final Channel channel;
    /**
     * NettyTransport类使用 inFlightRequests 维护在途的所有请求CompletableFuture
     */
    private final InFlightRequests inFlightRequests;

    NettyTransport(Channel channel, InFlightRequests inFlightRequests) {
        this.channel = channel;
        this.inFlightRequests = inFlightRequests;
    }


    /**
     * 这个 send 方法的实现，本质上就是一个异步方法，在把请求数据发出去之后就返回了，并不会阻塞当前这个线程去等待响应返回来
     * @param request 请求命令
     * @return
     */
    @Override
    public  CompletableFuture<Command> send(Command request) {
        // 构建返回值
        CompletableFuture<Command> completableFuture = new CompletableFuture<>();
        try {
            /**
             * 第一件事儿是把请求中的 requestId 和返回的 completableFuture 一起，构建了一个 ResponseFuture 对象，
             * 然后把这个对象放到了 inFlightRequests 这个变量中。inFlightRequests 中存放了所有在途的请求，
             * 也就是已经发出了请求但还没有收到响应的这些 responseFuture 对象
             */
            inFlightRequests.put(new ResponseFuture(request.getHeader().getRequestId(), completableFuture));
            /**
             * 第二件事儿就是调用 netty 发送数据的方法，把这个 request 命令发给对方。
             * 这里面需要注意的一点是，已经发出去的请求，有可能会因为网络连接断开或者对方进程崩溃等各种异常情况，
             * 永远都收不到响应。那为了确保这些孤儿 ResponseFuture 不会在内存中越积越多，
             * 我们必须要捕获所有的异常情况，结束对应的 ResponseFuture。所以，我们在上面代码中，
             * 两个地方都做了异常处理，分别应对发送失败和发送异常两种情况。
             */
            channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                // 处理发送失败的情况
                if (!channelFuture.isSuccess()) {
                    completableFuture.completeExceptionally(channelFuture.cause());
                    channel.close();
                }
            });
        } catch (Throwable t) {
            // 处理发送异常
            inFlightRequests.remove(request.getHeader().getRequestId());
            completableFuture.completeExceptionally(t);
        }
        return completableFuture;
    }


}
