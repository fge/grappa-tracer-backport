/*
 * Copyright (C) 2014 Francis Galiegue <fgaliegue@gmail.com>
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

package com.github.parboiled1.grappa.backport.buffers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.parboiled.support.Position;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Tainted;
import java.util.List;

// TODO: get rid of edge cases
@SuppressWarnings({ "AutoBoxing", "AutoUnboxing" })
@ParametersAreNonnullByDefault
public final class LineCounter
{
    // TODO: replace with IntRange from largetext
    private final List<Range<Integer>> lines = Lists.newArrayList();
    private final int nrLines;
    private final int len;

    public LineCounter(final CharSequence input)
    {
        int lowerBound = 0;
        int index = 0;
        len = input.length();

        while (index < len) {
            if (input.charAt(index++) != '\n')
                continue;
            lines.add(Range.closedOpen(lowerBound, index));
            lowerBound = index;
        }
        lines.add(Range.closedOpen(lowerBound, index));
        nrLines = lines.size();
    }

    @VisibleForTesting
    LineCounter(final List<Range<Integer>> ranges)
    {
        lines.addAll(ranges);
        nrLines = ranges.size();
        len = ranges.get(nrLines - 1).upperEndpoint();
    }

    public int getNrLines()
    {
        return nrLines;
    }

    public Range<Integer> getLineRange(@Tainted final int lineNr)
    {
        // Edge case: unfortunately, we can get an illegal line number
        return lines.get(Math.min(lineNr, nrLines) - 1);
    }

    public Position toPosition(@Tainted final int index)
    {
        if (index < 0)
            throw new IllegalStateException();

        final Range<Integer> range;

        // Edge case: unfortunately, we can get an illegal index
        if (index >= len) {
            range = lines.get(nrLines - 1);
            return new Position(nrLines, len - range.lowerEndpoint() + 1);
        }

        final int lineNr = binarySearch(index);

        range = lines.get(lineNr);
        return new Position(lineNr + 1, index - range.lowerEndpoint() + 1);
    }

    @VisibleForTesting
    int binarySearch(final int index)
    {
        return doBinarySearch(0, nrLines - 1, index);
    }

    private int doBinarySearch(final int low, final int high, final int index)
    {
        // Guaranteed to always succeed at this point
        if (high - low <= 1)
            return lines.get(low).contains(index) ? low : high;

        final int middle = (low + high) / 2;
        final Range<Integer> range = lines.get(middle);
        if (range.contains(index))
            return middle;

        return index < range.lowerEndpoint()
            ? doBinarySearch(low, middle, index)
            : doBinarySearch(middle, high, index);
    }
}
