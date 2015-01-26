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

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.matchers.Matcher;
import org.parboiled.parserunners.AbstractParseRunner;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.ParsingResult;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link ParseRunner} implementation with hooks at different points in the
 * parsing process
 *
 * <p>This parser performs like a {@link BasicParseRunner}, except that you are
 * allowed to {@link #registerListener(ParseRunnerListener) register} one or
 * more {@link ParseRunnerListener}s.</p>
 *
 * <p>This package comes with one listener implementation, which listens for all
 * events and builds a trace file for use with <a
 * href="https://github.com/fge/grappa-debugger">the GUI debugger</a>.</p>
 *
 * @see ParseRunnerListener
 * @see EventBus
 */
@SuppressWarnings("DesignForExtension")
@ParametersAreNonnullByDefault
public class EventBasedParseRunner<V>
    extends AbstractParseRunner<V>
    implements MatchHandler
{
    // TODO: does it need to be volatile?
    private volatile Throwable throwable = null;

    private final EventBus bus = new EventBus(new SubscriberExceptionHandler()
    {
        @Override
        public void handleException(final Throwable exception,
            final SubscriberExceptionContext context)
        {
                throwable = exception;
        }
    });

    public EventBasedParseRunner(final Rule rule)
    {
        super(rule);
    }

    /**
     * Register one listener to this parse runner
     *
     * @param listener the listener
     */
    public final void registerListener(final ParseRunnerListener<V> listener)
    {
        bus.register(listener);
    }

    @Override
    public ParsingResult<V> run(final InputBuffer inputBuffer)
    {
        Preconditions.checkNotNull(inputBuffer, "inputBuffer");
        resetValueStack();

        final MatcherContext<V> rootContext
            = createRootContext(inputBuffer, this, true);

        bus.post(new PreParseEvent<V>(rootContext));

        if (throwable != null)
            throw new RuntimeException("parse listener error", throwable);

        final boolean matched = rootContext.runMatcher();
        final ParsingResult<V> result
            = createParsingResult(matched, rootContext);

        bus.post(new PostParseEvent<V>(result));

        if (throwable != null)
            throw new RuntimeException("parse listener error", throwable);

        return result;
    }

    @Override
    public <T> boolean match(final MatcherContext<T> context)
    {
        final Matcher matcher = context.getMatcher();

        final PreMatchEvent<T> preMatchEvent = new PreMatchEvent<T>(context);

        bus.post(preMatchEvent);

        if (throwable != null)
            throw new RuntimeException("parse listener error", throwable);

        // FIXME: is there any case at all where context.getMatcher() is null?
        @SuppressWarnings("ConstantConditions")
        final boolean match = matcher.match(context);

        final MatchContextEvent<T> postMatchEvent = match
            ? new MatchSuccessEvent<T>(context)
            : new MatchFailureEvent<T>(context);

        bus.post(postMatchEvent);

        if (throwable != null)
            throw new RuntimeException("parse listener error", throwable);

        return match;
    }
}
