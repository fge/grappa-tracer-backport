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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.parboiled.MatcherContext;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.matchers.Matcher;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    private static final ThreadFactory THREAD_FACTORY
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("event-writer-%d").build();

    private static final Map<String, ?> ZIPFS_ENV
        = Collections.singletonMap("create", "true");

    private static final TraceEvent END_EVENT
        = new TraceEvent(TraceEventType.BEFORE_MATCH, -1L, 0, "", "",
        MatcherType.ACTION, "", -1);

    /*
     * We have to do that, since we write to a temporary file
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(Feature.AUTO_CLOSE_TARGET);
    private static final int BUFSIZE = 16384;

    private final MatcherTypeProvider typeProvider;

    private final ExecutorService executor;
    private final Future<Closeable> future;
    private final BlockingQueue<TraceEvent> eventQueue
        = new LinkedBlockingQueue<>();

    private final Path traceFile;
    private final BufferedWriter traceWriter;
    private final Path zipPath;
    private final JsonGenerator generator;

    private InputBuffer inputBuffer;
    private long startDate;

    public TracingParseRunnerListener(final Path zipPath,
        final MatcherTypeProvider typeProvider)
    {
        this.typeProvider = Objects.requireNonNull(typeProvider);
        Objects.requireNonNull(zipPath);

        if (Files.exists(zipPath, LinkOption.NOFOLLOW_LINKS))
            throw new RuntimeException("file " + zipPath + " already exists");

        this.zipPath = zipPath;

        try {
            traceFile = Files.createTempFile("trace", ".json");
            traceWriter = Files.newBufferedWriter(traceFile, UTF_8);
            generator = MAPPER.getFactory().createGenerator(traceWriter);
        } catch (IOException e) {
            throw new RuntimeException("failed to initialize trace", e);
        }

        executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
        future = executor.submit(new EventWriter(generator, eventQueue));
    }

    public TracingParseRunnerListener(final Path zipPath)
    {
        this(zipPath, new MatcherTypeProvider());
    }

    public TracingParseRunnerListener(final String zipPath)
    {
        this(Paths.get(zipPath), new MatcherTypeProvider());
    }

    public TracingParseRunnerListener(final File file)
    {
        this(file.toPath(), new MatcherTypeProvider());
    }

    @Override
    public void beforeParse(final PreParseEvent<V> event)
    {
        inputBuffer = event.getContext().getInputBuffer();
        startDate = System.currentTimeMillis();
    }

    @Override
    public void beforeMatch(final PreMatchEvent<V> event)
    {
        final MatcherContext<V> context = event.getContext();
        final Class<? extends Matcher> c = context.getMatcher().getClass();
        final MatcherType type = typeProvider.getType(c);
        final TraceEvent traceEvent = TraceEvent.before(context, type);
        final long nanoseconds = System.nanoTime();
        traceEvent.setNanoseconds(nanoseconds);
        eventQueue.offer(traceEvent);
    }

    @Override
    public void matchSuccess(final MatchSuccessEvent<V> event)
    {
        final long nanos = System.nanoTime();
        final MatcherContext<V> context = event.getContext();
        final Class<? extends Matcher> c = context.getMatcher().getClass();
        final MatcherType type = typeProvider.getType(c);
        final TraceEvent traceEvent = TraceEvent.success(context, type);
        traceEvent.setNanoseconds(nanos);
        eventQueue.offer(traceEvent);
    }

    @Override
    public void matchFailure(final MatchFailureEvent<V> event)
    {
        final long nanos = System.nanoTime();
        final MatcherContext<V> context = event.getContext();
        final Class<? extends Matcher> c = context.getMatcher().getClass();
        final MatcherType type = typeProvider.getType(c);
        final TraceEvent traceEvent = TraceEvent.failure(context, type);
        traceEvent.setNanoseconds(nanos);
        eventQueue.offer(traceEvent);
    }

    @Override
    public void afterParse(final PostParseEvent<V> event)
    {
        eventQueue.offer(END_EVENT);

        try (
            final Closeable gen = future.get();
        ) {
            traceWriter.flush();
            executor.shutdown();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException("failed to generate trace file", e);
        }

        final ParseRunInfo runInfo = new ParseRunInfo(startDate, inputBuffer);
        final URI uri = URI.create("jar:" + zipPath.toUri());

        try (
            final FileSystem zipfs = FileSystems.newFileSystem(uri, ZIPFS_ENV);
        ) {
            Files.move(traceFile, zipfs.getPath("/trace.json"));
            copyRunInfo(zipfs, runInfo);
            copyInputText(zipfs);
        } catch (IOException e) {
            throw new RuntimeException("failed to generate zip file", e);
        }
    }

    private void copyRunInfo(final FileSystem zipfs, final ParseRunInfo runInfo)
        throws IOException
    {
        final Path path = zipfs.getPath("/info.json");

        try (
            final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8);
        ) {
            MAPPER.writeValue(writer, runInfo);
            writer.flush();
        }
    }

    private void copyInputText(final FileSystem zipfs)
        throws IOException
    {
        final Path path = zipfs.getPath("/input.txt");
        final int length = bufferToString(inputBuffer).length();

        int start = 0;
        String s;

        try (
            final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8);
        ) {
            while (start < length) {
                // Note: relies on the fact that boundaries are adjusted
                s = inputBuffer.extract(start, start + BUFSIZE);
                writer.write(s);
                start += BUFSIZE;
            }

            writer.flush();
        }
    }

    private static final class EventWriter
        implements Callable<Closeable>
    {
        private final JsonGenerator generator;
        private final BlockingQueue<TraceEvent> eventQueue;

        private EventWriter(final JsonGenerator generator,
            final BlockingQueue<TraceEvent> eventQueue)
        {
            this.generator = generator;
            this.eventQueue = eventQueue;
        }

        @Override
        public Closeable call()
            throws IOException
        {
            TraceEvent event;
            generator.writeStartArray();
            try {
                while ((event = eventQueue.take()) != END_EVENT)
                    generator.writeObject(event);
                generator.writeEndArray();
            } catch (InterruptedException ignored) {
            }
            return generator;
        }
    }

    private static String bufferToString(final InputBuffer buffer)
    {
        final int bufsize = 4096;
        String s;
        final StringBuilder sb = new StringBuilder();

        int len;
        int index = 0;

        do {
            s = buffer.extract(index, index + bufsize);
            len = s.length();
            sb.append(s);
            index += bufsize;
        } while (len == bufsize);

        return sb.toString();
    }
}
