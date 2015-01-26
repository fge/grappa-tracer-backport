package com.github.parboiled1.grappa.backport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.parboiled.buffers.InputBuffer;

public final class ParseRunInfo
{
    private final long startDate;
    private final int nrLines;
    private final int nrChars;
    private final int nrCodePoints;

    @JsonIgnore
    public ParseRunInfo(final long startDate, final InputBuffer buffer)
    {
        this.startDate = startDate;
        nrLines = buffer.getLineCount();

        final String contents = bufferToString(buffer);

        nrChars = contents.length();
        nrCodePoints = contents.codePointCount(0, nrChars);
    }

    @JsonCreator
    public ParseRunInfo(@JsonProperty("startDate") final long startDate,
        @JsonProperty("nrLines") final int nrLines,
        @JsonProperty("nrChars") final int nrChars,
        @JsonProperty("nrCodePoints") final int nrCodePoints)
    {
        this.startDate = startDate;
        this.nrLines = nrLines;
        this.nrChars = nrChars;
        this.nrCodePoints = nrCodePoints;
    }

    public long getStartDate()
    {
        return startDate;
    }

    public int getNrLines()
    {
        return nrLines;
    }

    public int getNrChars()
    {
        return nrChars;
    }

    public int getNrCodePoints()
    {
        return nrCodePoints;
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
