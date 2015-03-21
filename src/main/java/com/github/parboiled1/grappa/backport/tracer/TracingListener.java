package com.github.parboiled1.grappa.backport.tracer;

import com.github.parboiled1.grappa.backport.EventBasedParseRunner;
import com.github.parboiled1.grappa.backport.ParseRunnerListener;
import com.github.parboiled1.grappa.backport.buffers.CharSequenceInputBuffer;
import com.github.parboiled1.grappa.backport.buffers.InputBuffer;
import com.github.parboiled1.grappa.backport.events.MatchFailureEvent;
import com.github.parboiled1.grappa.backport.events.MatchSuccessEvent;
import com.github.parboiled1.grappa.backport.events.PostParseEvent;
import com.github.parboiled1.grappa.backport.events.PreMatchEvent;
import com.github.parboiled1.grappa.backport.events.PreParseEvent;
import com.github.parboiled1.grappa.backport.type.MatcherType;
import com.github.parboiled1.grappa.backport.type.MatcherTypeProvider;
import com.github.parboiled1.grappa.exceptions.GrappaException;
import org.parboiled.MatcherContext;
import org.parboiled.matchers.Matcher;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The tracing listener
 *
 * <p>This listener is intended to be used with the <a
 * href="https://github.com/fge/grappa-debugger">debugger</a>. It collects all
 * parsing nodes and matcher information plus:</p>
 *
 * <ul>
 *     <li>the input text,</li>
 *     <li>other, generic parsing run information (date, time spent etc).</li>
 * </ul>
 *
 * @param <V> parameter type of the parser
 *
 * @see EventBasedParseRunner#registerListener(ParseRunnerListener)
 */
@ParametersAreNonnullByDefault
public final class TracingListener<V>
    extends ParseRunnerListener<V>
{
    /*
     * Zip info
     */
    private static final Map<String, ?> ENV
        = Collections.singletonMap("create", "true");
    private static final String NODE_PATH = "/nodes.csv";
    private static final String MATCHERS_PATH = "/matchers.csv";
    private static final String INPUT_TEXT_PATH = "/input.txt";
    private static final String INFO_PATH = "/info.csv";

    /*
     * The input buffer
     */
    private InputBuffer inputBuffer = null;

    /*
     * General parse info
     */
    private long startTime = 0L;
    private int nrLines = 0;
    private int nrChars = 0;
    private int nrCodePoints = 0;

    /*
     * Matcher type provider (matchers in 1.0.x don't have .getType())
     */
    private final MatcherTypeProvider typeProvider = new MatcherTypeProvider();

    /*
     * Matchers found during tracing, plus their ids as written in the CSV
     */
    private final Map<Matcher, MatcherDescriptor> matcherDescriptors
        = new IdentityHashMap<>();

    private final Map<Matcher, Integer> matcherIds = new IdentityHashMap<>();
    private int nextMatcherId = 0;

    /*
     * Map of parsing nodes, plus id
     */
    private final Map<Integer, Integer> nodeIds = new HashMap<>();
    private int nextNodeId = 0;

    /*
     * Data collected in pre match events
     */
    private final Map<Integer, Integer> prematchMatcherIds = new HashMap<>();
    private final Map<Integer, Integer> prematchIndices = new HashMap<>();
    private final Map<Integer, Long> prematchTimes = new HashMap<>();

    /*
     * The path to the zip, and the parse tree node file
     */
    private final Path zipPath;
    private final FileSystem zipfs;
    private final BufferedWriter writer;
    private final StringBuilder sb = new StringBuilder();

    public TracingListener(final Path zipPath, final boolean delete)
        throws IOException
    {
        this.zipPath = zipPath;
        if (delete)
            Files.deleteIfExists(zipPath);
        final URI uri = URI.create("jar:" + zipPath.toUri());
        zipfs = FileSystems.newFileSystem(uri, ENV);
        writer = Files.newBufferedWriter(zipfs.getPath(NODE_PATH), UTF_8);
    }

    @Override
    public void beforeParse(final PreParseEvent<V> event)
    {
        nodeIds.put(-1, -1);
        final org.parboiled.buffers.InputBuffer legacyBuffer
            = event.getContext().getInputBuffer();
        inputBuffer = CharSequenceInputBuffer.fromLegacy(legacyBuffer);
        nrChars = inputBuffer.length();
        nrLines = inputBuffer.getLineCount();
        startTime = System.currentTimeMillis();
    }

    @Override
    public void beforeMatch(final PreMatchEvent<V> event)
    {
        final MatcherContext<V> context = event.getContext();
        final Matcher matcher = context.getMatcher();

        Integer id = matcherIds.get(matcher);

        if (id == null) {
            //noinspection UnnecessaryBoxing
            id = Integer.valueOf(nextMatcherId);
            matcherIds.put(matcher, id);
            final MatcherType type = typeProvider.getType(matcher.getClass());
            final MatcherDescriptor descriptor
                = new MatcherDescriptor(nextMatcherId, type, matcher);
            matcherDescriptors.put(matcher, descriptor);
            nextMatcherId++;
        }

        final int level = context.getLevel();

        nodeIds.put(level, nextNodeId);
        nextNodeId++;

        prematchMatcherIds.put(level, id);
        final int startIndex = Math.min(nrChars, context.getCurrentIndex());
        prematchIndices.put(level, startIndex);
        prematchTimes.put(level, System.nanoTime());
    }

    @SuppressWarnings({ "AutoBoxing", "AutoUnboxing" })
    @Override
    public void matchSuccess(final MatchSuccessEvent<V> event)
    {
        final long endTime = System.nanoTime();
        final MatcherContext<V> context = event.getContext();
        final int level = context.getLevel();

        final Integer parentNodeId = nodeIds.get(level - 1);
        final Integer nodeId = nodeIds.get(level);

        final int startIndex = prematchIndices.get(level);
        final int endIndex
            = Math.min(nrChars, context.getCurrentIndex());

        final Integer matcherId = prematchMatcherIds.get(level);

        final long time = endTime - prematchTimes.get(level);

        // Write:
        // parent;id;level;success;matcherId;start;end;time
        sb.setLength(0);
        sb.append(parentNodeId).append(';')
            .append(nodeId).append(';')
            .append(level).append(";1;")
            .append(matcherId).append(';')
            .append(startIndex).append(';')
            .append(endIndex).append(';')
            .append(time).append('\n');
        try {
            writer.append(sb);
        } catch (IOException e) {
            throw cleanup(e);
        }
    }

    @SuppressWarnings({ "AutoBoxing", "AutoUnboxing" })
    @Override
    public void matchFailure(final MatchFailureEvent<V> event)
    {
        final long endTime = System.nanoTime();
        final MatcherContext<V> context = event.getContext();
        final int level = context.getLevel();

        final Integer parentNodeId = nodeIds.get(level - 1);
        final Integer nodeId = nodeIds.get(level);

        final int startIndex = prematchIndices.get(level);
        final int endIndex = context.getCurrentIndex();

        final Integer matcherId = prematchMatcherIds.get(level);

        final long time = endTime - prematchTimes.get(level);

        // Write:
        // parent;id;level;success;matcherId;start;end;time
        sb.setLength(0);
        sb.append(parentNodeId).append(';')
            .append(nodeId).append(';')
            .append(level).append(";0;")
            .append(matcherId).append(';')
            .append(startIndex).append(';')
            .append(endIndex).append(';')
            .append(time).append('\n');
        try {
            writer.append(sb);
        } catch (IOException e) {
            throw cleanup(e);
        }
    }

    @Override
    public void afterParse(final PostParseEvent<V> event)
    {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw cleanup(e);
        }

        try (
            final Closeable closeable = zipfs;
        ) {
            copyInputText();
            copyMatcherInfo();
            copyParseInfo();
        } catch (IOException e) {
            throw cleanup(e);
        }
    }

    private void copyInputText()
        throws IOException
    {
        final Path path = zipfs.getPath(INPUT_TEXT_PATH);

        final String s = inputBuffer.extract(0, nrChars);
        nrCodePoints = s.codePointCount(0, nrChars);

        try (
            final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8);
        ) {
            writer.write(s);
            writer.flush();
        }
    }

    private void copyMatcherInfo()
    {
        final Path path = zipfs.getPath(MATCHERS_PATH);

        try (
            final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8);
        ) {
            for (final MatcherDescriptor descriptor:
                matcherDescriptors.values()) {
                sb.setLength(0);
                sb.append(descriptor.getId()).append(';')
                    .append(descriptor.getClassName()).append(';')
                    .append(descriptor.getType()).append(';')
                    .append(descriptor.getName()).append('\n');
                writer.append(sb);
            }
            writer.flush();
        } catch (IOException e) {
            throw cleanup(e);
        }
    }

    // MUST be called after copyInputText!
    private void copyParseInfo()
        throws IOException
    {
        final Path path = zipfs.getPath(INFO_PATH);
        try (

            final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8);
        ) {
            sb.setLength(0);
            sb.append(startTime).append(';')
                .append(prematchIndices.size()).append(';')
                .append(nextMatcherId).append(';')
                .append(nrLines).append(';')
                .append(nrChars).append(';')
                .append(nrCodePoints).append(';')
                .append(nextNodeId).append('\n');
            writer.append(sb);
            writer.flush();
        }
    }

    private GrappaException cleanup(final IOException e)
    {
        final GrappaException ret
            = new GrappaException("failed to write event", e);

        try {
            writer.close();
        } catch (IOException e1) {
            ret.addSuppressed(e1);
        }

        try {
            zipfs.close();
        } catch (IOException e2) {
            ret.addSuppressed(e2);
        }

        return ret;
    }
}
