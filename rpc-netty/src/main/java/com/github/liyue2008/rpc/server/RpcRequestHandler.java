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
package com.github.liyue2008.rpc.server;

import com.github.liyue2008.rpc.client.ServiceTypes;
import com.github.liyue2008.rpc.client.stubs.RpcRequest;
import com.github.liyue2008.rpc.serialize.SerializeSupport;
import com.github.liyue2008.rpc.spi.Singleton;
import com.github.liyue2008.rpc.transport.RequestHandler;
import com.github.liyue2008.rpc.transport.command.Code;
import com.github.liyue2008.rpc.transport.command.Command;
import com.github.liyue2008.rpc.transport.command.Header;
import com.github.liyue2008.rpc.transport.command.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 因为我们这个 RPC 框架中只需要处理一种类型的请求：RPC 请求，
 * 所以我们只实现了一个命令处理器：RpcRequestHandler。这部分代码是这个 RPC 框架服务端最核心的部分
 * 这个类不仅实现了处理客户端请求的 RequestHandler 接口，
 * 同时还实现了注册 RPC 服务 ServiceProviderRegistry 接口，
 * 也就是说，RPC 框架服务端需要实现的两个功能——注册 RPC 服务和处理客户端 RPC 请求
 * @author LiYue
 * Date: 2019/9/23
 */
@Singleton
public class RpcRequestHandler implements RequestHandler, ServiceProviderRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
    /**
     * Key 就是服务名，Value 就是服务提供方，也就是服务实现类的实例
     */
    private Map<String/*service name*/, Object/*service provider*/> serviceProviders = new HashMap<>();

    @Override
    public Command handle(Command requestCommand) {
        Header header = requestCommand.getHeader();
        // 1.从payload中反序列化RpcRequest
        RpcRequest rpcRequest = SerializeSupport.parse(requestCommand.getPayload());
        try {
            // 2.根据 rpcRequest 中的服务名，去成员变量 serviceProviders 中查找已注册服务实现类的实例；
            Object serviceProvider = serviceProviders.get(rpcRequest.getInterfaceName());
            if(serviceProvider != null) {
                // 3.找到服务提供者，利用Java反射机制调用服务的对应方法
                String arg = SerializeSupport.parse(rpcRequest.getSerializedArguments());
                Method method = serviceProvider.getClass().getMethod(rpcRequest.getMethodName(), String.class);
                String result = (String ) method.invoke(serviceProvider, arg);
                // 4.把结果封装成响应命令并返回
                return new Command(new ResponseHeader(type(), header.getVersion(), header.getRequestId()), SerializeSupport.serialize(result));
            }
            // 如果没找到，返回NO_PROVIDER错误响应。
            logger.warn("No service Provider of {}#{}(String)!", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
            return new Command(new ResponseHeader(type(), header.getVersion(), header.getRequestId(), Code.NO_PROVIDER.getCode(), "No provider!"), new byte[0]);
        } catch (Throwable t) {
            // 发生异常，返回UNKNOWN_ERROR错误响应。
            logger.warn("Exception: ", t);
            return new Command(new ResponseHeader(type(), header.getVersion(), header.getRequestId(), Code.UNKNOWN_ERROR.getCode(), t.getMessage()), new byte[0]);
        }
    }

    @Override
    public int type() {
        return ServiceTypes.TYPE_RPC_REQUEST;
    }

    @Override
    public synchronized <T> void addServiceProvider(Class<? extends T> serviceClass, T serviceProvider) {
        serviceProviders.put(serviceClass.getCanonicalName(), serviceProvider);
        logger.info("Add service: {}, provider: {}.",
                serviceClass.getCanonicalName(),
                serviceProvider.getClass().getCanonicalName());
    }
}
