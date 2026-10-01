package com.beatmaker.core.project;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single block in the song arrangement: a reference to a named pattern and
 * how many times it should repeat before moving to the next block.
 */
public class SongBlock {

    private String patternName;
    private int repeatCount = 1;

    public SongBlock() {}

    public SongBlock(String patternName) {
        this(patternName, 1);
    }

    public SongBlock(String patternName, int repeatCount) {
        this.patternName = patternName;
        this.repeatCount = Math.max(1, repeatCount);
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = Math.max(1, repeatCount);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patternName", patternName);
        map.put("repeatCount", repeatCount);
        return map;
    }

    public static SongBlock fromMap(Map<String, Object> map) {
        String patternName = com.beatmaker.core.utils.Json.getString(map, "patternName", "");
        int repeatCount = com.beatmaker.core.utils.Json.getInt(map, "repeatCount", 1);
        return new SongBlock(patternName, repeatCount);
    }
}
