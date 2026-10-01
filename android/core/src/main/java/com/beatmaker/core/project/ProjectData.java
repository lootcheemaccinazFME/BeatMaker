package com.beatmaker.core.project;

import com.beatmaker.core.utils.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A whole beat project: a named library of patterns plus a song arrangement
 * (an ordered list of pattern blocks).
 */
public class ProjectData {

    private String name = "";
    private List<PatternData> patterns = new ArrayList<>();
    private List<SongBlock> arrangement = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PatternData> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<PatternData> patterns) {
        this.patterns = patterns;
    }

    public List<SongBlock> getArrangement() {
        return arrangement;
    }

    public void setArrangement(List<SongBlock> arrangement) {
        this.arrangement = arrangement;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("formatVersion", 1);
        map.put("name", name);

        List<Object> patternsList = new ArrayList<>();
        for (PatternData pattern : patterns) {
            patternsList.add(pattern.toMap());
        }
        map.put("patterns", patternsList);

        List<Object> arrangementList = new ArrayList<>();
        for (SongBlock block : arrangement) {
            arrangementList.add(block.toMap());
        }
        map.put("arrangement", arrangementList);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static ProjectData fromMap(Map<String, Object> map) {
        ProjectData data = new ProjectData();
        data.name = Json.getString(map, "name", "");

        List<Object> patternsList = Json.getList(map, "patterns");
        for (Object item : patternsList) {
            data.patterns.add(PatternData.fromMap((Map<String, Object>) item));
        }

        List<Object> arrangementList = Json.getList(map, "arrangement");
        for (Object item : arrangementList) {
            data.arrangement.add(SongBlock.fromMap((Map<String, Object>) item));
        }

        return data;
    }

    public String toJson() {
        return Json.write(toMap());
    }

    public static ProjectData fromJson(String json) {
        Map<String, Object> map = Json.asMap(Json.parse(json));
        return fromMap(map);
    }
}
