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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.parboiled.MatcherContext;
import org.parboiled.matchers.Matcher;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class TraceEvent
{
    private static final MatcherTypeProvider TYPE_PROVIDER
        = new MatcherTypeProvider();

    private final TraceEventType type;
    private long nanoseconds;
    private final int index;
    private final String matcher;
    private final MatcherType matcherType;
    private final String matcherClass;
    private final int level;

    public static TraceEvent before(final MatcherContext<?> context)
    {
        return new TraceEvent(TraceEventType.BEFORE_MATCH,
            context.getCurrentIndex(), context.getMatcher(),
            context.getLevel());
    }

    public static TraceEvent failure(final MatcherContext<?> context)
    {
        return new TraceEvent(TraceEventType.MATCH_FAILURE,
            context.getCurrentIndex(), context.getMatcher(),
            context.getLevel());
    }

    public static TraceEvent success(final MatcherContext<?> context)
    {
        return new TraceEvent(TraceEventType.MATCH_SUCCESS,
            context.getCurrentIndex(), context.getMatcher(),
            context.getLevel());
    }

    @JsonCreator
    public TraceEvent(@JsonProperty("type") final TraceEventType type,
        @JsonProperty("nanoseconds") final long nanoseconds,
        @JsonProperty("index") final int index,
        @JsonProperty("matcher") final String matcher,
        @JsonProperty("matcherClass") final String matcherClass,
        @JsonProperty("matcherType") final MatcherType matcherType,
        @JsonProperty("level") final int level)
    {
        this.type = type;
        this.nanoseconds = nanoseconds;
        this.index = index;
        this.matcher = matcher;
        this.matcherType = matcherType;
        this.matcherClass = matcherClass;
        this.level = level;
    }

    @JsonIgnore
    private TraceEvent(final TraceEventType type, final int index,
        final Matcher matcher, final int level)
    {
        this.type = type;
        this.index = index;
        this.matcher = matcher.toString();
        this.level = level;

        final String name = matcher.getClass().getSimpleName();
        matcherClass = name.isEmpty() ? "(anonymous)" : name;
        matcherType = TYPE_PROVIDER.getType(matcher.getClass());
    }

    @JsonIgnore
    public TraceEvent(final TraceEventType type,
        final MatcherContext<?> context)
    {
        nanoseconds = System.nanoTime();
        this.type = type;
        index = context.getCurrentIndex();
        // TODO: .getMatcher() normally never returns null
        final Matcher m = context.getMatcher();
        final String name = m.getClass().getSimpleName();
        matcher = m.toString();
        matcherClass = name.isEmpty() ? "(anonymous)" : name;
        matcherType = TYPE_PROVIDER.getType(m.getClass());
        level = context.getLevel();
    }

    public TraceEventType getType()
    {
        return type;
    }

    public long getNanoseconds()
    {
        return nanoseconds;
    }

    public void setNanoseconds(final long nanoseconds)
    {
        this.nanoseconds = nanoseconds;
    }

    public int getIndex()
    {
        return index;
    }

    public String getMatcher()
    {
        return matcher;
    }

    public MatcherType getMatcherType()
    {
        return matcherType;
    }

    public String getMatcherClass()
    {
        return matcherClass;
    }

    public int getLevel()
    {
        return level;
    }

    @Override
    @Nonnull
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("nanoseconds", nanoseconds)
            .add("index", index)
            .add("matcher", matcher)
            .add("matcherClass", matcherClass)
            .add("matcherType", matcherType)
            .add("level", level)
            .toString();
    }
}
