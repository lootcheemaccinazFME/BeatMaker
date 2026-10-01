package com.beatmaker.core.project;

import com.beatmaker.core.midi.MidiNote;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializable representation of a single {@link MidiNote}.
 */
public class NoteData {

    private int channel;
    private int pitch;
    private int velocity;

    public NoteData() {}

    public NoteData(int channel, int pitch, int velocity) {
        this.channel = channel;
        this.pitch = pitch;
        this.velocity = velocity;
    }

    public NoteData(MidiNote note) {
        this(note.getChannel(), note.getPitch(), note.getVelocity());
    }

    public int getChannel() {
        return channel;
    }

    public int getPitch() {
        return pitch;
    }

    public int getVelocity() {
        return velocity;
    }

    public MidiNote toMidiNote() {
        return new MidiNote(channel, pitch, velocity);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("channel", channel);
        map.put("pitch", pitch);
        map.put("velocity", velocity);
        return map;
    }

    public static NoteData fromMap(Map<String, Object> map) {
        NoteData note = new NoteData();
        note.channel = com.beatmaker.core.utils.Json.getInt(map, "channel", 0);
        note.pitch = com.beatmaker.core.utils.Json.getInt(map, "pitch", 0);
        note.velocity = com.beatmaker.core.utils.Json.getInt(map, "velocity", 0);
        return note;
    }
}
