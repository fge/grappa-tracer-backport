package com.github.parboiled1.grappa.backport;


import com.github.parboiled1.grappa.helpers.ValueBuilder;

import javax.annotation.Nonnull;

public final class TraceEventBuilder
    implements ValueBuilder<TraceEvent>
{
    private final TraceEventType[] EVENT_TYPES = TraceEventType.values();
    private final MatcherType[] MATCHER_TYPES = MatcherType.values();

    private TraceEventType type;
    private int index;
    private int level;
    private String matcher;
    private MatcherType matcherType;
    private String matcherClass;
    private long nanoseconds;

    public boolean setType(final String match)
    {
        type = EVENT_TYPES[Integer.parseInt(match)];
        return true;
    }

    public boolean setIndex(final String match)
    {
        index = Integer.parseInt(match);
        return true;
    }

    public boolean setLevel(final String match)
    {
        level = Integer.parseInt(match);
        return true;
    }


    public boolean setMatcher(final String match)
    {
        matcher = match.replace("\\;", ";");
        return true;
    }

    public boolean setMatcherType(final String match)
    {
        matcherType = MATCHER_TYPES[Integer.parseInt(match)];
        return true;
    }

    public boolean setMatcherClass(final String match)
    {
        matcherClass = match;
        return true;
    }

    public boolean setNanoseconds(final String match)
    {
        nanoseconds = Long.parseLong(match);
        return true;
    }

    @Nonnull
    @Override
    public TraceEvent build()
    {
        return new TraceEvent(type, nanoseconds, index, matcher,
            matcherClass, matcherType, level);
    }

    @Override
    public boolean reset()
    {
        return true;
    }
}
