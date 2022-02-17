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
package com.github.liyue2008.rpc.client;

import com.github.liyue2008.rpc.transport.Transport;

/**
 * @author LiYue
 * Date: 2019/9/27
 */
public interface StubFactory {
    /**
     * 它的功能就是创建一个桩的实例，这个桩实现的接口可以是任意类型的，也就是上面代码中的泛型 T。
     * 这个方法有两个参数，第一个参数是一个 Transport 对象，它是用来给服务端发请求的时候使用的。
     * 第二个参数是一个 Class 对象，它用来告诉桩工厂：我需要你给我创建的这个桩，应该是什么类型的。
     * createStub 的返回值就是由工厂创建出来的桩。
     * @param transport
     * @param serviceClass
     * @param <T>
     * @return
     */
    <T> T createStub(Transport transport, Class<T> serviceClass);
}
