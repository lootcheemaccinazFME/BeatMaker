package com.beatmaker.core.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializable override configuration for a single step (used only when a
 * step has its own {@code ElementConfig} that differs from the track default).
 */
public class StepOverrideData {

    private int channel;
    private List<NoteData> notes = new ArrayList<>();

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public List<NoteData> getNotes() {
        return notes;
    }

    public void setNotes(List<NoteData> notes) {
        this.notes = notes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("channel", channel);
        List<Object> notesList = new ArrayList<>();
        for (NoteData note : notes) {
            notesList.add(note.toMap());
        }
        map.put("notes", notesList);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static StepOverrideData fromMap(Map<String, Object> map) {
        StepOverrideData data = new StepOverrideData();
        data.channel = com.beatmaker.core.utils.Json.getInt(map, "channel", 0);
        List<Object> notesList = com.beatmaker.core.utils.Json.getList(map, "notes");
        for (Object item : notesList) {
            data.notes.add(NoteData.fromMap((Map<String, Object>) item));
        }
        return data;
    }
}
