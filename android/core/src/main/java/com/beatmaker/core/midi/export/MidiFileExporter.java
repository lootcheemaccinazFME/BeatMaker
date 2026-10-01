package com.beatmaker.core.midi.export;

import com.beatmaker.config.Constants;
import com.beatmaker.core.project.NoteData;
import com.beatmaker.core.project.PatternData;
import com.beatmaker.core.project.StepOverrideData;
import com.beatmaker.core.project.TrackData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exports a {@link PatternData} as a Standard MIDI File (format 0, single
 * track), using one quarter note = {@link Constants#TICKS_PER_QUARTER_NOTE}
 * ticks as the file's time division so step timing lines up exactly with the
 * sequencer's internal tick resolution.
 */
public class MidiFileExporter {

    private static final int META_TEMPO = 0x51;
    private static final int META_TRACK_NAME = 0x03;
    private static final int META_END_OF_TRACK = 0x2F;

    private MidiFileExporter() {}

    public static void export(PatternData pattern, File outFile) throws IOException {
        byte[] data = export(pattern);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
        }
    }

    public static byte[] export(PatternData pattern) throws IOException {
        int ticksPerQuarterNote = Constants.TICKS_PER_QUARTER_NOTE;
        int ticksPerStep = Constants.TICKS_PER_STEP;

        List<MidiEvent> events = buildEvents(pattern, ticksPerStep);
        Collections.sort(events);

        byte[] trackChunk = buildTrackChunk(pattern, events);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeHeaderChunk(out, ticksPerQuarterNote);
        out.write(trackChunk);

        return out.toByteArray();
    }

    private static List<MidiEvent> buildEvents(PatternData pattern, int ticksPerStep) {
        List<MidiEvent> events = new ArrayList<>();

        int numSteps = pattern.getNumSteps();
        int noteDuration = Math.max(1, ticksPerStep - 1);

        for (TrackData track : pattern.getTracks()) {
            boolean[] stepActive = track.getStepActive();
            Map<Integer, StepOverrideData> overrides = track.getStepOverrides();

            int steps = Math.min(numSteps, stepActive.length);

            for (int s = 0; s < steps; s++) {
                if (!stepActive[s]) continue;

                int channel;
                List<NoteData> notes;

                StepOverrideData override = overrides.get(s);
                if (null != override) {
                    channel = override.getChannel();
                    notes = override.getNotes();
                } else {
                    channel = track.getChannel();
                    notes = track.getDefaultNotes();
                }

                if (null == notes || notes.isEmpty()) continue;

                long tickOn = (long) s * ticksPerStep;
                long tickOff = tickOn + noteDuration;

                for (NoteData note : notes) {
                    int noteChannel = (notes.size() == 1) ? channel : note.getChannel();
                    events.add(new MidiEvent(tickOn, 0, noteChannel, note.getPitch(), note.getVelocity()));
                    events.add(new MidiEvent(tickOff, 1, noteChannel, note.getPitch(), 0));
                }
            }
        }

        return events;
    }

    private static byte[] buildTrackChunk(PatternData pattern, List<MidiEvent> events) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        String name = pattern.getName();
        if (null != name && name.length() > 0) {
            writeVariableLength(body, 0);
            writeMetaEvent(body, META_TRACK_NAME, nameToBytes(name));
        }

        int microsPerQuarterNote = bpmToMicrosPerQuarterNote(pattern.getBpm());
        writeVariableLength(body, 0);
        writeMetaEvent(body, META_TEMPO, new byte[]{
                (byte) ((microsPerQuarterNote >> 16) & 0xFF),
                (byte) ((microsPerQuarterNote >> 8) & 0xFF),
                (byte) (microsPerQuarterNote & 0xFF)
        });

        long lastTick = 0;
        for (MidiEvent event : events) {
            long deltaTicks = event.tick - lastTick;
            lastTick = event.tick;

            writeVariableLength(body, deltaTicks);

            int status = (event.isNoteOff() ? 0x80 : 0x90) | (event.channel & 0x0F);
            body.write(status);
            body.write(event.pitch & 0x7F);
            body.write(event.velocity & 0x7F);
        }

        writeVariableLength(body, 0);
        writeMetaEvent(body, META_END_OF_TRACK, new byte[0]);

        byte[] bodyBytes = body.toByteArray();

        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        chunk.write('M');
        chunk.write('T');
        chunk.write('r');
        chunk.write('k');
        writeUint32(chunk, bodyBytes.length);
        chunk.write(bodyBytes);

        return chunk.toByteArray();
    }

    private static byte[] nameToBytes(String name) {
        try {
            return name.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return name.getBytes();
        }
    }

    private static void writeHeaderChunk(ByteArrayOutputStream out, int division) {
        out.write('M');
        out.write('T');
        out.write('h');
        out.write('d');
        writeUint32(out, 6);
        writeUint16(out, 0); // format 0: single track
        writeUint16(out, 1); // one track
        writeUint16(out, division);
    }

    private static void writeMetaEvent(ByteArrayOutputStream out, int type, byte[] data) {
        out.write(0xFF);
        out.write(type & 0x7F);
        writeVariableLength(out, data.length);
        out.write(data, 0, data.length);
    }

    private static void writeVariableLength(ByteArrayOutputStream out, long value) {
        long buffer = value & 0x7F;
        while ((value >>= 7) > 0) {
            buffer <<= 8;
            buffer |= ((value & 0x7F) | 0x80);
        }

        while (true) {
            out.write((int) (buffer & 0xFF));
            if ((buffer & 0x80) != 0) {
                buffer >>= 8;
            } else {
                break;
            }
        }
    }

    private static void writeUint16(ByteArrayOutputStream out, int value) {
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeUint32(ByteArrayOutputStream out, int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static int bpmToMicrosPerQuarterNote(int bpm) {
        if (bpm <= 0) bpm = Constants.DEFAULT_BPM;
        return 60000000 / bpm;
    }

    private static class MidiEvent implements Comparable<MidiEvent> {
        final long tick;
        final int kind; // 0 = note on, 1 = note off
        final int channel;
        final int pitch;
        final int velocity;

        MidiEvent(long tick, int kind, int channel, int pitch, int velocity) {
            this.tick = tick;
            this.kind = kind;
            this.channel = channel;
            this.pitch = pitch;
            this.velocity = velocity;
        }

        boolean isNoteOff() {
            return kind == 1 || velocity == 0;
        }

        @Override
        public int compareTo(MidiEvent other) {
            if (tick != other.tick) {
                return Long.compare(tick, other.tick);
            }
            // process note-offs before note-ons at the same tick
            return Integer.compare(kind, other.kind);
        }
    }
}
