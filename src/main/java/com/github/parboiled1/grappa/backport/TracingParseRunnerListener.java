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

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.buffers.InputBuffer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tracing parse runner listener
 *
 * <p>This listener will collect information about the parsing process and
 * generate a list of {@link TraceEvent}.</p>
 *
 * <p>When the parsing process is done, it saves both the full input text and
 * the list of events (as a JSON file) in a zip file, whose name is given as an
 * argument to the listener's constructor.</p>
 *
 * <p>Sample usage:</p>
 *
 * <pre>
 *     final MyParser parser = Parboiled.createParser(MyParser.class);
 *     final String zipPath = "/path/to/zipfile.zip";
 *     final TracingParseRunnerListener&lt;Whatever&gt; listener
 *         = new TracingParseRunner&lt;&gt;(zipPath);
 *     final ParseRunner&lt;Whatever&gt; runner
 *         = new EventBasedParseRunner(parser.theRule());
 *     runner.run(someInput);
 * </pre>
 *
 * <p>You can then use the created zip file in the <a
 * href="https://github.com/fge/grappa-debugger">GUI debugger application</a>
 * to analyze the parsing process.</p>
 *
 * <p>Note that there is <strong>no need</strong> for your parser to be
 * annotated with {@link BuildParseTree} for this tracer to work.</p>
 *
 * <p><strong>Important:</strong> unfortunately, the parse runner does not allow
 * checked exceptions to be thrown; this parser therefore throws a {@link
 * RuntimeException} if:</p>
 *
 * <ul>
 *     <li>the zip file already exists (constructor), or</li>
 *     <li>the zip file cannot be created (post parse).</li>
 * </ul>
 *
 * @param <V> parameter type of the parser
 */
@SuppressWarnings("ProhibitedExceptionThrown")
@ParametersAreNonnullByDefault
public final class TracingParseRunnerListener<V>
    extends ParseRunnerListener<V>
{
    private static final Map<String, ?> ZIPFS_ENV
        = Collections.singletonMap("create", "true");
    /*
     * We have to do that, otherwise a corrupt zip file is created :(
     *
     * See https://github.com/FasterXML/jackson-databind/issues/680
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(Feature.AUTO_CLOSE_TARGET);

    private final URI zipUri;
    private final List<TraceEvent> events = new ArrayList<>();

    private InputBuffer buffer;
    private long startDate;

    public TracingParseRunnerListener(final String zipPath)
    {
        final Path path = Paths.get(zipPath);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS))
            throw new RuntimeException("path " + zipPath + "already exists");
        zipUri = URI.create("jar:" + path.toUri());
    }

    @Override
    public void beforeParse(final PreParseEvent<V> event)
    {
        buffer = event.getContext().getInputBuffer();
        startDate = System.currentTimeMillis();
    }

    @Override
    public void beforeMatch(final PreMatchEvent<V> event)
    {
        final TraceEvent traceEvent = TraceEvent.before(event.getContext());
        events.add(traceEvent);
        traceEvent.setNanoseconds(System.nanoTime());
    }

    @Override
    public void matchSuccess(final MatchSuccessEvent<V> event)
    {
        final long nanos = System.nanoTime();
        final TraceEvent traceEvent = TraceEvent.success(event.getContext());
        traceEvent.setNanoseconds(nanos);
        events.add(traceEvent);
    }

    @Override
    public void matchFailure(final MatchFailureEvent<V> event)
    {
        final long nanos = System.nanoTime();
        final TraceEvent traceEvent = TraceEvent.failure(event.getContext());
        traceEvent.setNanoseconds(nanos);
        events.add(traceEvent);
    }

    @Override
    public void afterParse(final PostParseEvent<V> event)
    {
        try (
            final FileSystem zipfs
                = FileSystems.newFileSystem(zipUri, ZIPFS_ENV);
        ) {
            final ParsingRunTrace trace
                = new ParsingRunTrace(startDate, events);
            writeInputText(zipfs);
            writeTrace(zipfs, trace);
        } catch (IOException e) {
            throw new RuntimeException("failed to write trace file", e);
        }
    }

    private void writeTrace(final FileSystem zipfs, final ParsingRunTrace trace)
        throws IOException
    {
        final Path path = zipfs.getPath("/trace.json");

        try (
            final BufferedWriter writer = Files.newBufferedWriter(path,
                StandardCharsets.UTF_8);
        ) {
            MAPPER.writeValue(writer, trace);
            writer.flush();
        }
    }

    private void writeInputText(final FileSystem zipfs)
        throws IOException
    {
        final Path path = zipfs.getPath("/input.txt");
        final String text = buffer.extract(0, Integer.MAX_VALUE);
        try (
            final BufferedWriter writer = Files.newBufferedWriter(path,
                StandardCharsets.UTF_8);
        ) {
            writer.append(text);
            writer.flush();
        }
    }
}
