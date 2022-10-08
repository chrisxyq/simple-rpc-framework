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

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * 定一个注册中心的接口 NameService
 * @author LiYue
 * Date: 2019/9/20
 */
public interface NameService {

    /**
     * 所有支持的协议
     */
    Collection<String> supportedSchemes();

    /**
     * 连接注册中心
     * 给定注册中心服务端的 URI，去建立与注册中心服务端的连接。
     * @param nameServiceUri 注册中心地址
     */
    void connect(URI nameServiceUri);
    /**
     * 供服务端使用
     * 注册服务
     * @param serviceName 服务名称
     * @param uri 服务地址
     */
    void registerService(String serviceName, URI uri) throws IOException;

    /**
     * 供客户端使用
     * 向注册中心查询serviceName的服务地址
     * @param serviceName 服务名称
     * @return 服务地址
     */
    URI lookupService(String serviceName) throws IOException;
}
