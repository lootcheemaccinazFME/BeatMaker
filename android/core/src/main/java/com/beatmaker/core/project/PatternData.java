package com.beatmaker.core.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializable representation of a single 16-step drum pattern: a name,
 * tempo, and the per-track step data of the sequencer.
 */
public class PatternData {

    private String name = "";
    private int bpm;
    private int numSteps;
    private List<TrackData> tracks = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getNumSteps() {
        return numSteps;
    }

    public void setNumSteps(int numSteps) {
        this.numSteps = numSteps;
    }

    public List<TrackData> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackData> tracks) {
        this.tracks = tracks;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("bpm", bpm);
        map.put("numSteps", numSteps);

        List<Object> tracksList = new ArrayList<>();
        for (TrackData track : tracks) {
            tracksList.add(track.toMap());
        }
        map.put("tracks", tracksList);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static PatternData fromMap(Map<String, Object> map) {
        PatternData data = new PatternData();
        data.name = com.beatmaker.core.utils.Json.getString(map, "name", "");
        data.bpm = com.beatmaker.core.utils.Json.getInt(map, "bpm", 0);
        data.numSteps = com.beatmaker.core.utils.Json.getInt(map, "numSteps", 0);

        List<Object> tracksList = com.beatmaker.core.utils.Json.getList(map, "tracks");
        for (Object item : tracksList) {
            data.tracks.add(TrackData.fromMap((Map<String, Object>) item));
        }

        return data;
    }
}
