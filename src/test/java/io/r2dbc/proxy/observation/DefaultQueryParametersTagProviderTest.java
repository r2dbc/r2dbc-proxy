/*
 * Copyright 2022 the original author or authors.
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

package io.r2dbc.proxy.observation;

import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DefaultQueryParametersTagProvider}.
 *
 * @author Tadaya Tsuyukubo
 */
class DefaultQueryParametersTagProviderTest {

    @ParameterizedTest
    @MethodSource
    void getTagValue(List<Bindings> bindings, String expected) {
        DefaultQueryParametersTagProvider provider = new DefaultQueryParametersTagProvider();
        String result = provider.getTagValue(bindings);
        assertThat(result).isEqualTo(expected);
    }


    static Stream<Arguments> getTagValue() {
        return Stream.of(
            Arguments.of(Named.of("empty", new ArrayList<>()), "()"),
            Arguments.of(Named.of("bindByIndexSingle", bindingsByIndexSingle()), "(foo,100)"),
            Arguments.of(Named.of("bindByIndexMulti", bindingsByIndexMulti()), "(foo,100),(bar,200),(baz,300)"),
            Arguments.of(Named.of("bindByNameSingle", bindingsByNameSingle()), "(id=100,name=foo)"),
            Arguments.of(Named.of("bindByNameMulti", bindingsByNameMulti()), "(id=100,name=foo),(id=200,name=bar),(id=300,name=baz)")
        );
    }

    static List<Bindings> bindingsByIndexSingle() {
        Bindings first = new Bindings();
        first.addIndexBinding(Bindings.indexBinding(0, BoundValue.value("foo")));
        first.addIndexBinding(Bindings.indexBinding(1, BoundValue.value(100)));
        return Arrays.asList(first);
    }

    static List<Bindings> bindingsByIndexMulti() {
        Bindings first = new Bindings();
        first.addIndexBinding(Bindings.indexBinding(0, BoundValue.value("foo")));
        first.addIndexBinding(Bindings.indexBinding(1, BoundValue.value(100)));
        Bindings second = new Bindings();
        second.addIndexBinding(Bindings.indexBinding(0, BoundValue.value("bar")));
        second.addIndexBinding(Bindings.indexBinding(1, BoundValue.value(200)));
        Bindings third = new Bindings();
        third.addIndexBinding(Bindings.indexBinding(0, BoundValue.value("baz")));
        third.addIndexBinding(Bindings.indexBinding(1, BoundValue.value(300)));
        return Arrays.asList(first, second, third);
    }

    static List<Bindings> bindingsByNameSingle() {
        Bindings first = new Bindings();
        first.addNamedBinding(Bindings.namedBinding("name", BoundValue.value("foo")));
        first.addNamedBinding(Bindings.namedBinding("id", BoundValue.value(100)));
        return Arrays.asList(first);
    }

    static List<Bindings> bindingsByNameMulti() {
        Bindings first = new Bindings();
        first.addNamedBinding(Bindings.namedBinding("name", BoundValue.value("foo")));
        first.addNamedBinding(Bindings.namedBinding("id", BoundValue.value(100)));
        Bindings second = new Bindings();
        second.addNamedBinding(Bindings.namedBinding("name", BoundValue.value("bar")));
        second.addNamedBinding(Bindings.namedBinding("id", BoundValue.value(200)));
        Bindings third = new Bindings();
        third.addNamedBinding(Bindings.namedBinding("name", BoundValue.value("baz")));
        third.addNamedBinding(Bindings.namedBinding("id", BoundValue.value(300)));
        return Arrays.asList(first, second, third);
    }
}
