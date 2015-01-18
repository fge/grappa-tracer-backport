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

package com.github.parboiled1.grappa.backport;

import org.parboiled.MatcherContext;

/**
 * Base class for a parsing match event
 *
 * @param <V> type parameter of the matching context
 *
 * @see PreMatchEvent
 * @see MatchFailureEvent
 * @see MatchSuccessEvent
 * @see ParsingRunTrace
 */
public abstract class MatchContextEvent<V>
{
    protected final MatcherContext<V> context;

    protected MatchContextEvent(final MatcherContext<V> context)
    {
        this.context = context;
    }

    /**
     * Return the context associated with this parse event
     *
     * @return the context
     */
    public final MatcherContext<V> getContext()
    {
        return context;
    }
}
