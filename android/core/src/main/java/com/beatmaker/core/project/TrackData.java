package com.beatmaker.core.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Serializable representation of a single {@code SequencerTrack}: its default
 * channel/notes, and for each of the 16 steps whether it is active and
 * (optionally) a step-specific note override.
 */
public class TrackData {

    private String name = "";
    private int channel;
    private List<NoteData> defaultNotes = new ArrayList<>();
    private boolean[] stepActive = new boolean[0];
    private Map<Integer, StepOverrideData> stepOverrides = new TreeMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public List<NoteData> getDefaultNotes() {
        return defaultNotes;
    }

    public void setDefaultNotes(List<NoteData> defaultNotes) {
        this.defaultNotes = defaultNotes;
    }

    public boolean[] getStepActive() {
        return stepActive;
    }

    public void setStepActive(boolean[] stepActive) {
        this.stepActive = stepActive;
    }

    public Map<Integer, StepOverrideData> getStepOverrides() {
        return stepOverrides;
    }

    public void setStepOverrides(Map<Integer, StepOverrideData> stepOverrides) {
        this.stepOverrides = stepOverrides;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("channel", channel);

        List<Object> notesList = new ArrayList<>();
        for (NoteData note : defaultNotes) {
            notesList.add(note.toMap());
        }
        map.put("defaultNotes", notesList);

        List<Object> activeList = new ArrayList<>();
        for (boolean active : stepActive) {
            activeList.add(active);
        }
        map.put("stepActive", activeList);

        Map<String, Object> overridesMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, StepOverrideData> entry : stepOverrides.entrySet()) {
            overridesMap.put(String.valueOf(entry.getKey()), entry.getValue().toMap());
        }
        map.put("stepOverrides", overridesMap);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static TrackData fromMap(Map<String, Object> map) {
        TrackData data = new TrackData();
        data.name = com.beatmaker.core.utils.Json.getString(map, "name", "");
        data.channel = com.beatmaker.core.utils.Json.getInt(map, "channel", 0);

        List<Object> notesList = com.beatmaker.core.utils.Json.getList(map, "defaultNotes");
        for (Object item : notesList) {
            data.defaultNotes.add(NoteData.fromMap((Map<String, Object>) item));
        }

        List<Object> activeList = com.beatmaker.core.utils.Json.getList(map, "stepActive");
        data.stepActive = new boolean[activeList.size()];
        for (int i = 0; i < activeList.size(); i++) {
            Object value = activeList.get(i);
            data.stepActive[i] = (value instanceof Boolean) && (Boolean) value;
        }

        Map<String, Object> overridesMap = com.beatmaker.core.utils.Json.getMap(map, "stepOverrides");
        for (Map.Entry<String, Object> entry : overridesMap.entrySet()) {
            int step = Integer.parseInt(entry.getKey());
            data.stepOverrides.put(step, StepOverrideData.fromMap((Map<String, Object>) entry.getValue()));
        }

        return data;
    }
}
