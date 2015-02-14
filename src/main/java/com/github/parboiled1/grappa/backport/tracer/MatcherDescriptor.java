package com.github.parboiled1.grappa.backport.tracer;

import com.github.parboiled1.grappa.backport.type.MatcherType;
import org.parboiled.matchers.Matcher;

final class MatcherDescriptor
{
    private final int id;
    private final String className;
    private final MatcherType type;
    private final String name;

    MatcherDescriptor(final int id, final MatcherType type,
        final Matcher matcher)
    {
        this.id = id;
        className = matcher.getClass().getSimpleName();
        this.type = type;
        name = matcher.getLabel();
    }

    int getId()
    {
        return id;
    }

    String getClassName()
    {
        return className;
    }

    MatcherType getType()
    {
        return type;
    }

    String getName()
    {
        return name;
    }
}
