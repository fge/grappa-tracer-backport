package com.github.parboiled1.grappa.backport;


import org.parboiled.MatcherContext;
import org.parboiled.matchers.Matcher;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/*
 * Order of fields:
 *
 * - event type (the ordinal!);
 * - index;
 * - level;
 * - matcher;
 * - matcher type (the ordinal!);
 * - matcher class;
 * - nanoseconds.
 *
 * No path anymore; useless, it can be built again if needed.
 *
 * The nanoseconds are written after the event only.
 *
 * Writes in CSV format, with semicolon as the separator; semicolons themselves
 * are escaped with a blackslash.
 */
@ParametersAreNonnullByDefault
public final class TraceEventWriter
{
    private static final int BEFORE_MATCH
        = TraceEventType.BEFORE_MATCH.ordinal();
    private static final int MATCH_SUCCESS
        = TraceEventType.MATCH_SUCCESS.ordinal();
    private static final int MATCH_FAILURE
        = TraceEventType.MATCH_FAILURE.ordinal();

    private final Writer writer;
    private final MatcherTypeProvider provider;
    private final StringBuilder sb = new StringBuilder();

    private boolean startSet = false;
    private long start = 0L;

    public TraceEventWriter(final Writer writer,
        final MatcherTypeProvider provider)
    {
        this.writer = Objects.requireNonNull(writer);
        this.provider = Objects.requireNonNull(provider);
    }

    public void writeBefore(final MatcherContext<?> context, final long nanos)
        throws IOException
    {
        if (!startSet) {
            start = nanos;
            startSet = true;
        }

        final Matcher matcher = context.getMatcher();
        final Class<? extends Matcher> matcherClass = matcher.getClass();
        @SuppressWarnings("ConstantConditions")
        final String name = matcherClass.getSimpleName();
        final MatcherType type = provider.getType(matcherClass);

        sb.setLength(0);
        sb.append(BEFORE_MATCH).append(';')
            .append(context.getCurrentIndex()).append(';')
            .append(context.getLevel()).append(';')
            .append(matcher.toString().replace(";", "\\;")).append(';')
            .append(type.ordinal()).append(';')
            .append(name.isEmpty() ? "(anonymous)" : name).append(';')
            .append(nanos - start).append('\n');

        writer.append(sb);
    }

    public void writeSuccess(final MatcherContext<?> context, final long nanos)
        throws IOException
    {
        final Matcher matcher = context.getMatcher();
        final Class<? extends Matcher> matcherClass = matcher.getClass();
        @SuppressWarnings("ConstantConditions")
        final String name = matcherClass.getSimpleName();
        final MatcherType type = provider.getType(matcherClass);

        sb.setLength(0);

        sb.append(MATCH_SUCCESS).append(';')
            .append(context.getCurrentIndex()).append(';')
            .append(context.getLevel()).append(';')
            .append(matcher.toString().replace(";", "\\;")).append(';')
            .append(type.ordinal()).append(';')
            .append(name.isEmpty() ? "(anonymous)" : name).append(';')
            .append(nanos - start).append('\n');

        writer.append(sb);
    }

    public void writeFailure(final MatcherContext<?> context, final long nanos)
        throws IOException
    {
        final Matcher matcher = context.getMatcher();
        final Class<? extends Matcher> matcherClass = matcher.getClass();
        @SuppressWarnings("ConstantConditions")
        final String name = matcherClass.getSimpleName();
        final MatcherType type = provider.getType(matcherClass);

        sb.setLength(0);

        sb.append(MATCH_FAILURE).append(';')
            .append(context.getCurrentIndex()).append(';')
            .append(context.getLevel()).append(';')
            .append(matcher.toString().replace(";", "\\;")).append(';')
            .append(type.ordinal()).append(';')
            .append(name.isEmpty() ? "(anonymous)" : name).append(';')
            .append(nanos - start).append('\n');

        writer.append(sb);
    }
}
