/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy.support;

import io.r2dbc.proxy.util.Assert;

/**
 * Utility class for formatters.
 *
 * @author Tadaya Tsuyukubo
 */
final class FormatterUtils {

    private FormatterUtils() {
    }

    /**
     * Remove the matching {@code String} from the end of the given {@code StringBuilder}
     *
     * @param sb {@code StringBuilder}
     * @param s  removing {@code String} from tail
     * @throws IllegalArgumentException if {@code sb} is {@code null}
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
    public static void chompIfEndWith(StringBuilder sb, String s) {
        Assert.requireNonNull(sb, "sb must not be null");
        Assert.requireNonNull(s, "s must not be null");

        if (sb.length() < s.length()) {
            return;
        }
        final int startIndex = sb.length() - s.length();
        if (sb.substring(startIndex, sb.length()).equals(s)) {
            sb.delete(startIndex, sb.length());
        }
    }

}
