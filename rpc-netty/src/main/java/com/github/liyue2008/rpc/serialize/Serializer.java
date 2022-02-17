package com.github.liyue2008.rpc.serialize;
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

public interface Serializer<T> {
    /**
     * 计算对象序列化后的长度，主要用于申请存放序列化数据的字节数组
     * @param entry 待序列化的对象
     * @return 对象序列化后的长度
     */
    int size(T entry);

    /**
     * 序列化对象。将给定的对象序列化成字节数组
     * @param entry 待序列化的对象
     * @param bytes 存放序列化数据的字节数组
     * @param offset 数组的偏移量，从这个位置开始写入序列化数据
     * @param length 对象序列化后的长度，也就是{@link Serializer#size(java.lang.Object)}方法的返回值。
     */
    void serialize(T entry, byte[] bytes, int offset, int length);

    /**
     * 反序列化对象
     * @param bytes 存放序列化数据的字节数组
     * @param offset 数组的偏移量，从这个位置开始写入序列化数据
     * @param length 对象序列化后的长度
     * @return 反序列化之后生成的对象
     */
    T parse(byte[] bytes, int offset, int length);

    /**
     * 用一个字节标识对象类型，每种类型的数据应该具有不同的类型值
     * type 方法定义每种序列化实现的类型，这个类型值也会写入到序列化之后的数据中，
     * 主要的作用是在反序列化的时候，能够识别是什么数据类型的，以便找到对应的反序列化实现类
     */
    byte type();

    /**
     * getSerializeClass 这个方法返回这个序列化实现类对应的对象类型，
     * 目的是，在执行序列化的时候，通过被序列化的对象类型找到对应序列化实现类。
     */
    Class<T> getSerializeClass();
}
