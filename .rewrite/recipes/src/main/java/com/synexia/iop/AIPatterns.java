/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synexia.iop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for multiple {@link AIPattern} annotations.
 *
 * @since IOP 1.0
 * @see AIPattern
 * @m3.pattern synexia:aiop
 * @m3.algorithm Carries repeated AIPattern records for design-pattern and lifecycle-scope implementation preferences in the separate AI-prefixed API; the container performs no evaluation, planning or execution.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIPatterns {

    /**
     * Array of pattern hints.
     *
     * @return patterns array
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries AIPatterns#value advisory metadata for Array of pattern hints. The element itself neither executes nor enforces that request.
     * @m3.dataStructures AIPattern[] annotation element; explicit value required, no default.
     */
    AIPattern[] value();
}
