/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.proxy.listener;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class LifeCycleListenerTest {

    @ParameterizedTest
    @ProxyClassesSource
    @SuppressWarnings("deprecation")
    void verifyMethodNames(Class<?> clazz) {

        String className = clazz.getSimpleName();

        Set<String> expected = Stream.of(clazz.getDeclaredMethods())
            .map(Method::getName)
            .flatMap(methodName -> {
                // beforeXxxOnYyy / afterXxxOnYyy
                String name = StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
                return Stream.of("before" + name, "after" + name);
            })
            .collect(toSet());

        Set<String> actual = Stream.of(LifeCycleListener.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(toSet());

        assertThat(actual).containsAll(expected);
    }

}
