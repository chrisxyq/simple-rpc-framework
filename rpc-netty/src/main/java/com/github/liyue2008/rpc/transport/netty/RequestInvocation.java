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

import com.github.liyue2008.rpc.transport.RequestHandler;
import com.github.liyue2008.rpc.transport.RequestHandlerRegistry;
import com.github.liyue2008.rpc.transport.command.Command;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
@ChannelHandler.Sharable
public class RequestInvocation extends SimpleChannelInboundHandler<Command> {
    private static final Logger logger = LoggerFactory.getLogger(RequestInvocation.class);
    private final RequestHandlerRegistry requestHandlerRegistry;

    RequestInvocation(RequestHandlerRegistry requestHandlerRegistry) {
        this.requestHandlerRegistry = requestHandlerRegistry;
    }

    /**
     * RPC 框架的服务端如何来处理客户端发送的 RPC 请求
     * 根据请求命令的 Hdader 中的请求类型 type，
     * 去 requestHandlerRegistry 中查找对应的请求处理器 RequestHandler，
     * 然后调用请求处理器去处理请求，最后把结果发送给客户端
     * @param channelHandlerContext
     * @param request
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Command request) throws Exception {
        RequestHandler handler = requestHandlerRegistry.get(request.getHeader().getType());
        if(null != handler) {
            Command response = handler.handle(request);
            if(null != response) {
                channelHandlerContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        logger.warn("Write response failed!", channelFuture.cause());
                        channelHandlerContext.channel().close();
                    }
                });
            } else {
                logger.warn("Response is null!");
            }
        } else {
            throw new Exception(String.format("No handler for request with type: %d!", request.getHeader().getType()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Exception: ", cause);

        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if(channel.isActive())ctx.close();
    }
}
