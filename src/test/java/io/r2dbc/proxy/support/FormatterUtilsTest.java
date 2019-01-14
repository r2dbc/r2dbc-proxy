/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.r2dbc.proxy.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class FormatterUtilsTest {

    @Test
    void chompIfEndWith() {
        StringBuilder sb;

        sb = new StringBuilder("fooFOOfoo");
        FormatterUtils.chompIfEndWith(sb, "foo");
        assertThat(sb.toString()).isEqualTo("fooFOO");

        sb = new StringBuilder("fooFOOfoo");
        FormatterUtils.chompIfEndWith(sb, "bar");
        assertThat(sb.toString()).isEqualTo("fooFOOfoo");

        sb = new StringBuilder("fooFOOfoo");
        FormatterUtils.chompIfEndWith(sb, "");
        assertThat(sb.toString()).isEqualTo("fooFOOfoo");
    }
}
