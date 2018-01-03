package org.sonar.plugins.kt.advance.batch;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.plugins.kt.advance.model.GoodForCache;

public class IPOTextRange implements GoodForCache {

    private static final long serialVersionUID = 7972064605806555395L;

    public final Integer line;
    final Integer offset;

    final Integer endLine;
    final Integer endLineOffset;

    public IPOTextRange(Integer line, Integer offset, Integer endLine, Integer endLineOffset) {
        super();
        this.line = line;
        this.offset = offset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IPOTextRange other = (IPOTextRange) obj;
        if (endLine == null) {
            if (other.endLine != null) {
                return false;
            }
        } else if (!endLine.equals(other.endLine)) {
            return false;
        }
        if (endLineOffset == null) {
            if (other.endLineOffset != null) {
                return false;
            }
        } else if (!endLineOffset.equals(other.endLineOffset)) {
            return false;
        }
        if (line == null) {
            if (other.line != null) {
                return false;
            }
        } else if (!line.equals(other.line)) {
            return false;
        }
        if (offset == null) {
            if (other.offset != null) {
                return false;
            }
        } else if (!offset.equals(other.offset)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endLine == null) ? 0 : endLine.hashCode());
        result = prime * result + ((endLineOffset == null) ? 0 : endLineOffset.hashCode());
        result = prime * result + ((line == null) ? 0 : line.hashCode());
        result = prime * result + ((offset == null) ? 0 : offset.hashCode());
        return result;
    }

    /**
     * means, it is not bound to the entire line of code, but to some specific
     * fragment
     *
     * @return
     */
    public boolean isBoundToVar() {
        return !(offset == null || (offset == endLineOffset && line == endLine));
    }

    public String key() {
        return line + "-" + offset + "-" + endLine + "-" + endLineOffset;
    }

    @Override
    public String toString() {
        return "IPOTextRange [line=" + line + ", offset=" + offset + ", endLine=" + endLine + ", endLineOffset="
                + endLineOffset + "]";
    }

    public TextRange toTextRange(final InputFile inputFile) {
        if (isBoundToVar()) {
            return inputFile.newRange(line, offset, endLine, endLineOffset);
        } else {
            return inputFile.selectLine(line);
        }
    }
}