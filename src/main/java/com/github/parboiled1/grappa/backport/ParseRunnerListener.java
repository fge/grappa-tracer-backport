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

import com.github.parboiled1.grappa.backport.events.MatchFailureEvent;
import com.github.parboiled1.grappa.backport.events.MatchSuccessEvent;
import com.github.parboiled1.grappa.backport.events.PostParseEvent;
import com.github.parboiled1.grappa.backport.events.PreMatchEvent;
import com.github.parboiled1.grappa.backport.events.PreParseEvent;
import com.google.common.eventbus.Subscribe;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Basic parse runner listener implementation
 *
 * <p>You have five possible hooks:</p>
 *
 * <ul>
 *     <li>before the parsing run starts (see {@link PreParseEvent});</li>
 *     <li>before a rule attemps a match (see {@link PreMatchEvent});</li>
 *     <li>a rule has successfully matched (see {@link MatchSuccessEvent});</li>
 *     <li>a rule has failed to match (see {@link MatchFailureEvent});</li>
 *     <li>after the parsing run has completed, whether the run has succeeded or
 *     not (see {@link PostParseEvent}).</li>
 * </ul>
 *
 * <p>This base implementation does nothing.</p>
 *
 * @param <V> type parameter of the running parser
 *
 * @see EventBasedParseRunner
 */
@ParametersAreNonnullByDefault
public class ParseRunnerListener<V>
{
    @Subscribe
    public void beforeParse(final PreParseEvent<V> event)
    {
    }

    @Subscribe
    public void beforeMatch(final PreMatchEvent<V> event)
    {
    }

    @Subscribe
    public void matchSuccess(final MatchSuccessEvent<V> event)
    {
    }

    @Subscribe
    public void matchFailure(final MatchFailureEvent<V> event)
    {
    }

    @Subscribe
    public void afterParse(final PostParseEvent<V> event)
    {
    }
}
