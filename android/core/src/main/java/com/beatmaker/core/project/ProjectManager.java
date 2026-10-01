package com.beatmaker.core.project;

import com.beatmaker.config.Constants;
import com.beatmaker.core.midi.MidiNote;
import com.beatmaker.core.sequencer.ElementConfig;
import com.beatmaker.core.sequencer.Sequencer;
import com.beatmaker.core.sequencer.SequencerMetrics;
import com.beatmaker.core.sequencer.SequencerStep;
import com.beatmaker.core.sequencer.SequencerTrack;
import com.beatmaker.core.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bridges the live {@link Sequencer} state with the serializable
 * {@link ProjectData} model: capturing the current 16-step pattern into a
 * named snapshot, applying a stored pattern back onto the sequencer,
 * managing a simple song arrangement (an ordered list of pattern blocks),
 * and saving/loading whole projects as JSON files.
 */
public class ProjectManager {

    private static final String TAG = "ProjectManager";

    private static ProjectManager instance_;

    public static synchronized ProjectManager instance() {
        if (null == instance_) {
            instance_ = new ProjectManager();
        }
        return instance_;
    }

    private String projectName = "Untitled Project";
    private final Map<String, PatternData> patterns = new LinkedHashMap<>();
    private final List<SongBlock> arrangement = new ArrayList<>();
    private String currentPatternName;

    public ProjectManager() {}

    // ------------------------------------------------------------- patterns

    public synchronized PatternData capturePattern(String name) {
        Sequencer sequencer = Sequencer.instance();
        if (null == sequencer) return null;

        PatternData pattern = new PatternData();
        pattern.setName(name);
        pattern.setBpm(SequencerMetrics.instance().getBpm());

        int numSteps = SequencerMetrics.instance().getNumSteps();
        pattern.setNumSteps(numSteps);

        List<TrackData> trackDataList = new ArrayList<>();

        int numTracks = SequencerMetrics.instance().getNumTracks();
        for (int t = 0; t < numTracks; t++) {
            SequencerTrack track = sequencer.getTrack(t);
            if (null == track) continue;

            TrackData trackData = new TrackData();
            trackData.setName(track.hasName() ? track.getName() : ("Track" + (t + 1)));

            ElementConfig trackConfig = track.getConfig();
            if (null != trackConfig) {
                trackData.setChannel(trackConfig.getChannel());
                trackData.setDefaultNotes(toNoteDataList(trackConfig.getNotes()));
            }

            boolean[] stepActive = new boolean[numSteps];
            Map<Integer, StepOverrideData> stepOverrides = new TreeMap<>();

            for (int s = 0; s < numSteps; s++) {
                SequencerStep step = track.getElement(s);
                if (null == step) continue;

                stepActive[s] = step.isActive();

                if (step.hasConfig()) {
                    ElementConfig stepConfig = step.getConfig();
                    StepOverrideData override = new StepOverrideData();
                    override.setChannel(stepConfig.getChannel());
                    override.setNotes(toNoteDataList(stepConfig.getNotes()));
                    stepOverrides.put(s, override);
                }
            }

            trackData.setStepActive(stepActive);
            trackData.setStepOverrides(stepOverrides);

            trackDataList.add(trackData);
        }

        pattern.setTracks(trackDataList);

        patterns.put(name, pattern);
        currentPatternName = name;

        return pattern;
    }

    public synchronized void applyPattern(PatternData pattern) {
        if (null == pattern) return;

        Sequencer sequencer = Sequencer.instance();
        if (null == sequencer) return;

        if (pattern.getBpm() > 0) {
            SequencerMetrics.instance().setBpm(pattern.getBpm());
        }

        List<TrackData> trackDataList = pattern.getTracks();
        int numTracks = Math.min(trackDataList.size(), SequencerMetrics.instance().getNumTracks());

        for (int t = 0; t < numTracks; t++) {
            SequencerTrack track = sequencer.getTrack(t);
            if (null == track) continue;

            TrackData trackData = trackDataList.get(t);

            if (null != trackData.getName() && trackData.getName().length() > 0) {
                track.setName(trackData.getName());
            }

            ElementConfig trackConfig = new ElementConfig();
            trackConfig.setChannel(trackData.getChannel());
            trackConfig.setNotes(toMidiNoteList(trackData.getDefaultNotes()));
            track.setConfig(trackConfig);

            boolean[] stepActive = trackData.getStepActive();
            Map<Integer, StepOverrideData> stepOverrides = trackData.getStepOverrides();

            int numSteps = Math.min(stepActive.length, track.getNumElements());
            for (int s = 0; s < numSteps; s++) {
                SequencerStep step = track.getElement(s);
                if (null == step) continue;

                step.setActive(stepActive[s]);

                StepOverrideData override = stepOverrides.get(s);
                if (null != override) {
                    ElementConfig stepConfig = new ElementConfig();
                    stepConfig.setChannel(override.getChannel());
                    stepConfig.setNotes(toMidiNoteList(override.getNotes()));
                    step.setConfig(stepConfig);
                } else {
                    step.clearConfig();
                }
            }

            // clear out any remaining steps beyond the stored pattern length
            for (int s = numSteps; s < track.getNumElements(); s++) {
                SequencerStep step = track.getElement(s);
                if (null != step) {
                    step.setActive(false);
                    step.clearConfig();
                }
            }
        }

        currentPatternName = pattern.getName();
    }

    public synchronized void newPattern(String name) {
        Sequencer sequencer = Sequencer.instance();
        if (null != sequencer) {
            int numTracks = SequencerMetrics.instance().getNumTracks();
            for (int t = 0; t < numTracks; t++) {
                SequencerTrack track = sequencer.getTrack(t);
                if (null == track) continue;
                // clears step activity/overrides, keeps each track's own
                // default channel/pitch configuration intact
                track.clear();
            }
        }

        currentPatternName = name;
        patterns.put(name, capturePattern(name));
    }

    public synchronized boolean loadPatternByName(String name) {
        PatternData pattern = patterns.get(name);
        if (null == pattern) return false;
        applyPattern(pattern);
        return true;
    }

    public synchronized List<String> getPatternNames() {
        return new ArrayList<>(patterns.keySet());
    }

    public synchronized PatternData getPattern(String name) {
        return patterns.get(name);
    }

    public synchronized String getCurrentPatternName() {
        return currentPatternName;
    }

    public synchronized void removePattern(String name) {
        patterns.remove(name);
        if (null != currentPatternName && currentPatternName.equals(name)) {
            currentPatternName = null;
        }
    }

    // --------------------------------------------------------- arrangement

    public synchronized List<SongBlock> getArrangement() {
        return arrangement;
    }

    public synchronized void addArrangementBlock(String patternName) {
        if (!patterns.containsKey(patternName)) return;
        arrangement.add(new SongBlock(patternName));
    }

    public synchronized void removeArrangementBlock(int index) {
        if (index < 0 || index >= arrangement.size()) return;
        arrangement.remove(index);
    }

    public synchronized void clearArrangement() {
        arrangement.clear();
    }

    // -------------------------------------------------------------- project

    public synchronized String getProjectName() {
        return projectName;
    }

    public synchronized void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public synchronized ProjectData toProjectData() {
        ProjectData data = new ProjectData();
        data.setName(projectName);
        data.setPatterns(new ArrayList<>(patterns.values()));
        data.setArrangement(new ArrayList<>(arrangement));
        return data;
    }

    public synchronized void loadProjectData(ProjectData data) {
        patterns.clear();
        for (PatternData pattern : data.getPatterns()) {
            patterns.put(pattern.getName(), pattern);
        }

        arrangement.clear();
        arrangement.addAll(data.getArrangement());

        projectName = data.getName();

        currentPatternName = null;
        if (!patterns.isEmpty()) {
            String firstPatternName = patterns.keySet().iterator().next();
            loadPatternByName(firstPatternName);
        }
    }

    public synchronized void save(File file) throws IOException {
        if (null != currentPatternName) {
            // refresh the live snapshot before writing to disk
            capturePattern(currentPatternName);
        }

        ProjectData data = toProjectData();
        String json = data.toJson();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(json);
        }

        Logger.d(TAG, "Saved project to " + file.getAbsolutePath());
    }

    public synchronized void load(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                sb.append(buffer, 0, read);
            }
        }

        ProjectData data = ProjectData.fromJson(sb.toString());
        loadProjectData(data);

        Logger.d(TAG, "Loaded project from " + file.getAbsolutePath());
    }

    // ---------------------------------------------------------------- utils

    private List<NoteData> toNoteDataList(List<MidiNote> notes) {
        List<NoteData> result = new ArrayList<>();
        if (null != notes) {
            for (MidiNote note : notes) {
                result.add(new NoteData(note));
            }
        }
        return result;
    }

    private List<MidiNote> toMidiNoteList(List<NoteData> notes) {
        List<MidiNote> result = new ArrayList<>();
        if (null != notes) {
            for (NoteData note : notes) {
                result.add(note.toMidiNote());
            }
        }
        return result;
    }
}
