/*
 * Copyright (C) 2015 Francis Galiegue <fgaliegue@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.parboiled1.grappa.backport.events;

import org.parboiled.support.ParsingResult;

/**
 * Event posted after the parsing run has finished
 *
 * @param <V> type parameter of the parser
 */
public final class PostParseEvent<V>
{
    private final ParsingResult<V> result;

    public PostParseEvent(final ParsingResult<V> result)
    {
        this.result = result;
    }

    public ParsingResult<V> getResult()
    {
        return result;
    }
}
