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

import com.github.liyue2008.rpc.NameService;
import com.github.liyue2008.rpc.RpcAccessPoint;
import com.github.liyue2008.rpc.hello.HelloService;
import com.github.liyue2008.rpc.spi.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.net.URI;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static void main(String [] args) throws Exception {

        String serviceName = HelloService.class.getCanonicalName();
        File tmpDirFile = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tmpDirFile, "simple_rpc_name_service.data");
        HelloService helloService = new HelloServiceImpl();
        logger.info("创建并启动RpcAccessPoint...");
        try(RpcAccessPoint rpcAccessPoint = ServiceSupport.load(RpcAccessPoint.class);
            //把 RPC 框架对外提供的所有服务定义在一个接口 RpcAccessPoint 中
            Closeable ignored = rpcAccessPoint.startServer()) {
            //file.toURI(): file:/D:/Users/yuanqixu/AppData/Local/Temp/simple_rpc_name_service.data
            NameService nameService = rpcAccessPoint.getNameService(file.toURI());
            assert nameService != null;
            //serviceName: com.github.liyue2008.rpc.hello.HelloService
            logger.info("向RpcAccessPoint注册{}服务...", serviceName);
            //服务端step1：利用RPC 框架提供的服务 RpcAccessPoint，注册 helloService 服务 得到服务的地址uri: rpc://localhost:9999
            URI uri = rpcAccessPoint.addServiceProvider(helloService, HelloService.class);
            logger.info("服务名: {}, 向NameService注册...", serviceName);
            //服务端step2：利用注册中心服务 NameService，在注册中心注册服务的地址。
            //serviceName: com.github.liyue2008.rpc.hello.HelloService
            //uri: rpc://localhost:9999
            nameService.registerService(serviceName, uri);
            logger.info("开始提供服务，按任何键退出.");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
            logger.info("Bye!");
        }
    }

}
