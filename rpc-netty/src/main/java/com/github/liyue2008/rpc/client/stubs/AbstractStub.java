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
package com.github.liyue2008.rpc.client.stubs;

import com.github.liyue2008.rpc.client.RequestIdSupport;
import com.github.liyue2008.rpc.client.ServiceStub;
import com.github.liyue2008.rpc.client.ServiceTypes;
import com.github.liyue2008.rpc.serialize.SerializeSupport;
import com.github.liyue2008.rpc.transport.Transport;
import com.github.liyue2008.rpc.transport.command.Code;
import com.github.liyue2008.rpc.transport.command.Command;
import com.github.liyue2008.rpc.transport.command.Header;
import com.github.liyue2008.rpc.transport.command.ResponseHeader;

import java.util.concurrent.ExecutionException;

/**
 * @author LiYue
 * Date: 2019/9/27
 */

/**
 * 我们需要动态生成的这个桩，它每个方法的逻辑都是一样的，
 * 都是把类名、方法名和方法的参数封装成请求，然后发给服务端，
 * 收到服务端响应之后再把结果作为返回值，返回给调用方。
 * 所以，我们定义一个 AbstractStub 的抽象类，
 * 在这个类中实现大部分通用的逻辑，让所有动态生成的桩都继承这个抽象类，这样动态生成桩的代码会更少一些。
 */
public abstract class AbstractStub implements ServiceStub {
    /**
     *  Transport 这个接口的实现 NettyTransport 类。这个 send 方法的实现，
     *  本质上就是一个异步方法，在把请求数据发出去之后就返回了，并不会阻塞当前这个线程去等待响应返回来。
     */
    protected Transport transport;

    /**
     * 把接口的类名、方法名和序列化后的参数封装成一个 RpcRequest 对象，
     * 调用父类 AbstractStub 中的 invokeRemote 方法，发送给服务端。
     * invokeRemote 方法的返回值就是序列化的调用结果，
     * 我们在模板中把这个结果反序列化之后，直接作为返回值返回给调用方就可以了。
     *
     *
     *     RPC 框架提供统一的泛化调用接口，
     *      调用端在创建 GenericService 代理时指定真正需要调用的接口的接口名以及分组名，
     *      通过调用 GenericService 代理的 $invoke 方法将服务端所需要的所有信息，
     *      包括接口名、业务分组名、方法名以及参数信息等封装成请求消息，发送给服务端，实现在没有接口的情况下进行 RPC 调用的功能
     * @param request
     * @return
     */
    protected byte [] invokeRemote(RpcRequest request) {
        /**
         * request: {"interfaceName":"com.github.liyue2008.rpc.hello.HelloService",
         * "methodName":"hello","serializedArguments":"AE1hc3RlciBNUQ=="}
         *
         * header: {"requestId":0,"type":0,"version":1}
         *
         * requestCommand:{"header":{"requestId":0,"type":0,"version":1},
         * "payload":"ZQAAACtjb20uZ2l0aHViLmxpeXVlMjAwOC5ycGMuaGVsbG8uSGVsbG9TZXJ2aWNlAAAABWhlbGxvAAAACgBNYXN0ZXIgTVE="}
         *
         * responseCommand: {"header":{"code":0,"error":"","requestId":0,"type":0,"version":1},"payload":"AEhlbGxvLCBNYXN0ZXIgTVE="}
         */
        Header header = new Header(ServiceTypes.TYPE_RPC_REQUEST, 1, RequestIdSupport.next());
        byte [] payload = SerializeSupport.serialize(request);
        Command requestCommand = new Command(header, payload);
        try {
            //NettyTransport类实现通信
            /**
             * 所谓的同步调用，不过是 RPC 框架在调用端的处理逻辑中主动执行了这个 Future 的 get 方法，
             * 让动态代理等待返回值；而异步调用则是 RPC 框架没有主动执行这个 Future 的 get 方法，
             * 用户可以从请求上下文中得到这个 Future，自己决定什么时候执行这个 Future 的 get 方法。
             */
            Command responseCommand = transport.send(requestCommand).get();
            ResponseHeader responseHeader = (ResponseHeader) responseCommand.getHeader();
            if(responseHeader.getCode() == Code.SUCCESS.getCode()) {
                return responseCommand.getPayload();
            } else {
                throw new Exception(responseHeader.getError());
            }

        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setTransport(Transport transport) {
        this.transport = transport;
    }
}
