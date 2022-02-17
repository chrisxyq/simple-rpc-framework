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

import com.github.liyue2008.rpc.transport.command.Command;

import java.util.concurrent.CompletableFuture;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
public interface Transport {
    /**
     * 发送请求命令
     * 请求和响应数据都抽象成了一个 Command 类
     * CompletableFuture 作为返回值，我们可以直接调用它的 get 方法来获取响应数据，这就相当于同步调用；
     * 也可以使用以 then 开头的一系列异步方法，指定当响应返回的时候，需要执行的操作，就等同于异步调用。
     * @param request 请求命令
     * @return 返回值是一个Future，Future
     */
    CompletableFuture<Command> send(Command request);
}
