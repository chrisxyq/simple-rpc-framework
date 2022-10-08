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
package com.github.liyue2008.rpc;

import com.github.liyue2008.rpc.spi.ServiceSupport;

import java.io.Closeable;
import java.net.URI;
import java.util.Collection;

/**
 * 把 RPC 框架对外提供的所有服务定义在一个接口 RpcAccessPoint 中
 * 方法 startServer 和 close（在父接口 Closeable 中定义）用于服务端启动和停止服务
 * @author LiYue
 * Date: 2019/9/20
 */
public interface RpcAccessPoint extends Closeable{
    /**
     * 客户端获取远程服务的引用
     * getRemoteService 供客户端来使用，这个方法的作用和我们上面例子中 Dubbo 的 @Reference 注解是一样的，
     * 客户端调用这个方法可以获得远程服务的实例
     * @param uri 远程服务地址
     * @param serviceClass 服务的接口类的Class
     * @param <T> 服务接口的类型
     * @return 远程服务引用
     */
    <T> T getRemoteService(URI uri, Class<T> serviceClass);

    /**
     * 服务端注册服务的实现实例
     * addServiceProvider 供服务端来使用，这个方法的作用和 Dubbo 的 @Service 注解是一样的，
     * 服务端通过调用这个方法向rpc框架来注册服务的实现,得到服务地址uri之后
     * 再调用 nameServer.registerService 方法，在注册中心注册服务的地址。
     *
     * @param service 实现实例
     * @param serviceClass 服务的接口类的Class
     * @param <T> 服务接口的类型
     * @return 服务地址
     */
    <T> URI addServiceProvider(T service, Class<T> serviceClass);

    /**
     * 获取注册中心的引用
     * @param nameServiceUri 注册中心URI
     * @return 注册中心引用
     */
    default NameService getNameService(URI nameServiceUri) {
        /**
         * 就是通过 SPI 机制加载所有的 NameService 的实现类，然后根据给定的 URI 中的协议，
         * 去匹配支持这个协议的实现类，然后返回这个实现的引用就可以了。
         * 这样我们就实现了一个可扩展的注册中心接口，系统可以根据 URI 中的协议，
         * 动态地来选择不同的注册中心实现。增加一种注册中心的实现，也不需要修改任何代码，
         * 只要按照 SPI 的规范，把协议的实现加入到运行时 CLASSPATH 中就可以了。
         * （这里设置 CLASSPATH 的目的，在于告诉 Java 执行环境，
         * 在哪些目录下可以找到你所要执行的 Java 程序所需要的类或者包。）
         */
        Collection<NameService> nameServices = ServiceSupport.loadAll(NameService.class);
        for (NameService nameService : nameServices) {
            //nameService.supportedSchemes(): "file"
            if(nameService.supportedSchemes().contains(nameServiceUri.getScheme())) {
                nameService.connect(nameServiceUri);
                return nameService;
            }
        }
        return null;
    }

    /**
     * 服务端启动RPC框架，监听接口，开始提供远程服务。
     * @return 服务实例，用于程序停止的时候安全关闭服务。
     */
    Closeable startServer() throws Exception;
}
