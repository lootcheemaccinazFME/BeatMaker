package com.beatmaker.beatmaker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.beatmaker.beatmaker.databinding.FragmentMainBinding;
import com.beatmaker.core.application.ApplicationBase;
import com.beatmaker.core.application.ApplicationListener;
import com.beatmaker.core.midi.MidiNote;
import com.beatmaker.core.midi.export.MidiFileExporter;
import com.beatmaker.core.project.PatternData;
import com.beatmaker.core.project.ProjectManager;
import com.beatmaker.core.project.SongBlock;
import com.beatmaker.core.sequencer.Sequencer;
import com.beatmaker.core.sequencer.SequencerControl;
import com.beatmaker.core.sequencer.SequencerListener;
import com.beatmaker.core.sequencer.SequencerMetrics;
import com.beatmaker.core.sequencer.SequencerPosition;
import com.beatmaker.core.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements ApplicationListener, SequencerListener {

    private static final String TAG = "MainFragment";
    private static final String PROJECT_FILE_NAME = "project.json";
    private static final String DEFAULT_PATTERN_NAME = "Pattern 1";

    private FragmentMainBinding ui;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ui = FragmentMainBinding.inflate(inflater, container, false);
        View view = ui.getRoot();

        ui.btnRewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Sequencer sequencer = Sequencer.instance();
                if (null != sequencer) {
                    sequencer.rewind();
                }

            }
        });

        ui.btnStartStop.setOnClickListener(v -> {
            Sequencer sequencer = Sequencer.instance();
            if (null == sequencer) return;

            int mode = sequencer.getMode();
            if (mode == SequencerControl.MODE_PLAYING) {
                sequencer.stop();
            } else if (mode == SequencerControl.MODE_PAUSED) {
                sequencer.resume();
            } else if (mode == SequencerControl.MODE_STOPPED) {
                sequencer.start();
            }
        });

        ui.btnProject.setOnClickListener(v -> showProjectMenu());

        ApplicationBase.instance().addListener(this);
        Sequencer.instance().addListener(this);

        loadProjectFromDisk();

        return view;
    }

    // ------------------------------------------------------------ project

    private File getProjectFile() {
        return new File(requireContext().getFilesDir(), PROJECT_FILE_NAME);
    }

    private File getExportDir() {
        File base = requireContext().getExternalFilesDir(null);
        if (null == base) {
            base = requireContext().getFilesDir();
        }
        File dir = new File(base, "exports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void loadProjectFromDisk() {
        File file = getProjectFile();
        if (!file.exists()) return;

        try {
            ProjectManager.instance().load(file);
            refreshSequencerView();
        } catch (IOException e) {
            Logger.d(TAG, "Failed to load project: " + e.getMessage());
            toast("Failed to load saved project");
        }
    }

    private void saveProjectToDisk() {
        try {
            ProjectManager.instance().save(getProjectFile());
        } catch (IOException e) {
            Logger.d(TAG, "Failed to save project: " + e.getMessage());
            toast("Failed to save project");
        }
    }

    private void refreshSequencerView() {
        if (null == ui) return;
        if (null != ui.mainView) {
            ui.mainView.setDirty();
        }
        if (null != ui.textBpm) {
            ui.textBpm.setText(SequencerMetrics.instance().getBpm() + " bpm");
        }
    }

    private void toast(String message) {
        if (null == getContext()) return;
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showProjectMenu() {
        String[] items = {
                "Save Pattern As…",
                "Load Pattern",
                "New Pattern",
                "Song Arrangement",
                "Export Pattern as MIDI"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Project")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showSavePatternDialog();
                            break;
                        case 1:
                            showLoadPatternDialog();
                            break;
                        case 2:
                            showNewPatternDialog();
                            break;
                        case 3:
                            showArrangementDialog();
                            break;
                        case 4:
                            exportCurrentPatternAsMidi();
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void showSavePatternDialog() {
        String currentName = ProjectManager.instance().getCurrentPatternName();

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(null != currentName ? currentName : DEFAULT_PATTERN_NAME);

        new AlertDialog.Builder(requireContext())
                .setTitle("Save Pattern As")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Pattern name cannot be empty");
                        return;
                    }
                    ProjectManager.instance().capturePattern(name);
                    saveProjectToDisk();
                    toast("Saved pattern \"" + name + "\"");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoadPatternDialog() {
        List<String> names = ProjectManager.instance().getPatternNames();
        if (names.isEmpty()) {
            toast("No saved patterns yet");
            return;
        }

        String[] items = names.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Load Pattern")
                .setItems(items, (dialog, which) -> {
                    String name = items[which];
                    if (ProjectManager.instance().loadPatternByName(name)) {
                        refreshSequencerView();
                        toast("Loaded pattern \"" + name + "\"");
                    } else {
                        toast("Could not load pattern \"" + name + "\"");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNewPatternDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(DEFAULT_PATTERN_NAME);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Pattern")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Pattern name cannot be empty");
                        return;
                    }
                    ProjectManager.instance().newPattern(name);
                    saveProjectToDisk();
                    refreshSequencerView();
                    toast("Created pattern \"" + name + "\"");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showArrangementDialog() {
        List<SongBlock> arrangement = ProjectManager.instance().getArrangement();

        List<String> items = new ArrayList<>();
        for (int i = 0; i < arrangement.size(); i++) {
            SongBlock block = arrangement.get(i);
            items.add((i + 1) + ". " + block.getPatternName() + " (tap to remove)");
        }
        items.add("+ Add Pattern Block");

        new AlertDialog.Builder(requireContext())
                .setTitle("Song Arrangement")
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    if (which == items.size() - 1) {
                        showAddArrangementBlockDialog();
                    } else {
                        ProjectManager.instance().removeArrangementBlock(which);
                        saveProjectToDisk();
                        showArrangementDialog();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddArrangementBlockDialog() {
        List<String> names = ProjectManager.instance().getPatternNames();
        if (names.isEmpty()) {
            toast("Save a pattern first, then add it to the arrangement");
            return;
        }

        String[] items = names.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Pattern Block")
                .setItems(items, (dialog, which) -> {
                    ProjectManager.instance().addArrangementBlock(items[which]);
                    saveProjectToDisk();
                    showArrangementDialog();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportCurrentPatternAsMidi() {
        String name = ProjectManager.instance().getCurrentPatternName();
        if (null == name || name.isEmpty()) {
            name = DEFAULT_PATTERN_NAME;
        }

        PatternData pattern = ProjectManager.instance().capturePattern(name);
        saveProjectToDisk();

        File outFile = new File(getExportDir(), sanitizeFileName(name) + ".mid");

        try {
            MidiFileExporter.export(pattern, outFile);
            toast("Exported MIDI to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            Logger.d(TAG, "Failed to export MIDI: " + e.getMessage());
            toast("Failed to export MIDI file");
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_ ]", "_").trim();
    }

    public void onSequencerPositionUpdate(SequencerPosition position, boolean stepChange) {
        if (!stepChange) return;

        if (null == ui) return;

        if (null != ui.mainView) {
            ui.mainView.setDirty();
        }

        if (null != ui.textPosition) {
            String posInfo = "" + (position.getQuarterInMeasure()+1) + "." + (position.getStepInQuarter()+1);
            ui.textPosition.setText(posInfo);
        }

        if (null != ui.textBpm) {
            String bpmInfo = SequencerMetrics.instance().getBpm() + " bpm";
            ui.textBpm.setText(bpmInfo);
        }
    }

    public void onSequencerCaptureFinished(List<MidiNote> capturedNotes) {
    }

    @Override
    public void onApplicationStartup() {
        //ui.mainView.start();
    }

    @Override
    public void onApplicationShutdown() {
        ui.mainView.stop();
    }

    @Override
    public void onApplicationPause() {
        ui.mainView.stop();
    }

    @Override
    public void onApplicationResume() {
        ui.mainView.start();
    }


}
