package com.techsenger.jeditermfx.app.debug;

import com.techsenger.jeditermfx.core.util.CharUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ControlSequenceVisualizer {

    @NotNull
    public static final String getVisualizedString(int logStart, @NotNull List<char[]> arrayChunks,
                                                   @NotNull ControlSequenceSettings settings) {
        Objects.requireNonNull(arrayChunks, "arrayChunks");
        Objects.requireNonNull(settings, "settings");
        List<String> originalChunks = arrayChunks.stream().map((c) -> new String(c)).collect(Collectors.toList());
        List<String> chunks = originalChunks;
        if (settings.isUseTeseq()) {
            chunks = new TeseqVisualizer().apply(chunks);
        }
        if (settings.isShowInvisibleCharacters()) {
            chunks = chunks.stream()
                    .map(s -> CharUtils.toHumanReadableText(toHumanReadableSpace(s)))
                    .collect(Collectors.toList());
        }
        if (settings.isShowChunkId()) {
            chunks = withChunkId(logStart, chunks, originalChunks);
        }
        return chunks.stream().collect(Collectors.joining(""));
    }

    private static String toHumanReadableSpace(String escSeq) {
        return makeCharHumanReadable(escSeq, ' ', "S");
    }

    @SuppressWarnings("SameParameterValue")
    private static final String makeCharHumanReadable(String escSeq, char ch, String presentable) {
        var pattern = Pattern.compile(Pattern.quote(String.valueOf(ch)) + "+");
        var matcher = pattern.matcher(escSeq);
        var builder = new StringBuilder();
        var lastInd = 0;
        while (matcher.find()) {
            var startInd = matcher.start();
            var endInd = matcher.end();
            var spaces = escSeq.substring(startInd, endInd);
            if (!spaces.equals(String.valueOf(ch).repeat(endInd - startInd))) {
                throw new IllegalStateException("Not spaces");
            }
            builder.append(escSeq.substring(lastInd, startInd));
            if (spaces.length() > 1) {
                builder.append("<$presentable:").append(spaces.length()).append(">");
            } else {
                builder.append("<$presentable>");
            }
            lastInd = endInd;
        }
        return builder.toString();
    }

    private static final List<String> withChunkId(int logStart, List<String> chunks, List<String> originalChunks) {
        if (chunks.size() != originalChunks.size()) {
            throw new IllegalStateException("Check failed.");
        } else {
            var result = new ArrayList<String>();
            for (int id = 0; id < chunks.size(); id++) {
                var label = "--- #" + (id + 1 + logStart) + " (received " + originalChunks.get(id).length() + " chars) ---\n";
                if (id == 0) {
                    result.add(label);
                } else {
                    result.add("\n" + label);
                }
                var chunk = chunks.get(id);
                result.add(chunk);
            }
            return result;
        }
    }
}
