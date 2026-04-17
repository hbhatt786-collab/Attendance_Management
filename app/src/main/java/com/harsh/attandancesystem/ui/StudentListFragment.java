package com.harsh.attandancesystem.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.data.local.Student;
import com.harsh.attandancesystem.data.local.StudentOverview;
import com.harsh.attandancesystem.databinding.DialogClasswiseImportBinding;
import com.harsh.attandancesystem.databinding.DialogStudentBinding;
import com.harsh.attandancesystem.databinding.FragmentStudentListBinding;
import com.harsh.attandancesystem.ui.adapter.StudentAdapter;
import com.harsh.attandancesystem.util.SpreadsheetImportUtils;
import com.harsh.attandancesystem.util.StudentPhotoStore;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class StudentListFragment extends Fragment implements StudentAdapter.StudentActionListener {
    private static final String[] PREDEFINED_CLASSES = new String[]{
            "BCA A", "BCA B", "BCA C", "BCA D", "BTEC", "MCA", "MBA"
    };

    private FragmentStudentListBinding binding;
    private AttendanceViewModel viewModel;
    private StudentAdapter adapter;
    private List<StudentOverview> allStudents = new ArrayList<>();
    private DialogClasswiseImportBinding importDialogBinding;
    private AlertDialog importDialog;
    private int activeImportSlotIndex = -1;
    private DialogStudentBinding studentDialogBinding;
    private Uri pendingStudentPhotoUri;
    private String editingStudentEmail = "";
    private final List<PendingImportSlot> pendingImportSlots = createPendingSlots();
    private final ActivityResultLauncher<String[]> importSingleFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportSlotFile);
    private final ActivityResultLauncher<String[]> studentPhotoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleStudentPhotoPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        adapter = new StudentAdapter(this);

        binding.studentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.studentRecyclerView.setAdapter(adapter);

        binding.addStudentButton.setOnClickListener(v -> showStudentDialog(null));
        binding.importStudentsButton.setOnClickListener(v -> showImportOptions());

        viewModel.getStudentOverviews().observe(getViewLifecycleOwner(), students -> {
            allStudents = students;
            filterStudents(binding.searchEditText.getText().toString());
            updateCount(students != null ? students.size() : 0);
        });

        setupSearch();
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterStudents(String query) {
        if (allStudents == null) {
            return;
        }

        List<StudentOverview> filtered;
        if (query.isEmpty()) {
            filtered = allStudents;
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
            filtered = allStudents.stream()
                    .filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || s.getClassName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || s.getDivision().toLowerCase(Locale.ROOT).contains(lowerQuery))
                    .collect(Collectors.toList());
        }

        adapter.submitList(filtered);
        binding.emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateCount(int count) {
        binding.studentRecordsChip.setText("Total Records: " + count);
    }

    private String resolveFileName(@NonNull Uri uri) {
        android.database.Cursor cursor = requireContext().getContentResolver().query(
                uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        String name = cursor.getString(index);
                        if (name != null && !name.trim().isEmpty()) {
                            return name;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return uri.getLastPathSegment() == null ? "Selected file" : uri.getLastPathSegment();
    }

    private void showImportOptions() {
        resetPendingSlots();
        importDialogBinding = DialogClasswiseImportBinding.inflate(getLayoutInflater());
        renderPendingSlots();

        importDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_classwise_title)
                .setView(importDialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    importDialogBinding = null;
                    importDialog = null;
                })
                .setPositiveButton(R.string.preview_students, null)
                .create();

        importDialog.setOnShowListener(dialog -> {
            setupClassDropdown(importDialogBinding.slot1ClassAutoComplete, 0);
            setupClassDropdown(importDialogBinding.slot2ClassAutoComplete, 1);
            setupClassDropdown(importDialogBinding.slot3ClassAutoComplete, 2);
            setupClassDropdown(importDialogBinding.slot4ClassAutoComplete, 3);
            setupClassDropdown(importDialogBinding.slot5ClassAutoComplete, 4);
            bindSlotClick(importDialogBinding.slot1Button, 0);
            bindSlotClick(importDialogBinding.slot2Button, 1);
            bindSlotClick(importDialogBinding.slot3Button, 2);
            bindSlotClick(importDialogBinding.slot4Button, 3);
            bindSlotClick(importDialogBinding.slot5Button, 4);

            importDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> importPendingSlots());
        });
        importDialog.show();
    }

    private void setupClassDropdown(android.widget.AutoCompleteTextView autoCompleteTextView, int slotIndex) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                PREDEFINED_CLASSES
        );
        autoCompleteTextView.setAdapter(adapter);
        PendingImportSlot slot = pendingImportSlots.get(slotIndex);
        if (slot.className.isEmpty()) {
            slot.className = PREDEFINED_CLASSES[Math.min(slotIndex, PREDEFINED_CLASSES.length - 1)];
        }
        autoCompleteTextView.setText(slot.className, false);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            slot.className = String.valueOf(parent.getItemAtPosition(position));
            renderPendingSlots();
        });
    }

    private void bindSlotClick(View button, int slotIndex) {
        button.setOnClickListener(v -> {
            activeImportSlotIndex = slotIndex;
            importSingleFileLauncher.launch(getImportMimeTypes());
        });
    }

    private String[] getImportMimeTypes() {
        return new String[]{
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv",
                "text/comma-separated-values"
        };
    }

    private void handleImportSlotFile(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        if (activeImportSlotIndex < 0 || activeImportSlotIndex >= pendingImportSlots.size()) {
            return;
        }

        int slotIndex = activeImportSlotIndex;
        activeImportSlotIndex = -1;

        new Thread(() -> {
            try {
                String fileName = resolveFileName(uri);
                String selectedClassOption = pendingImportSlots.get(slotIndex).className;
                ClassDivisionSelection selection = normalizeClassAndDivision(selectedClassOption, "");
                List<Student> parsedStudents = SpreadsheetImportUtils.importStudents(
                        requireContext().getContentResolver(),
                        uri,
                        selection.className
                );
                List<Student> fileStudents = applyDivisionSelection(parsedStudents, selection);

                PendingImportSlot slot = pendingImportSlots.get(slotIndex);
                slot.fileName = fileName;
                slot.students.clear();
                slot.students.addAll(fileStudents);
                requireActivity().runOnUiThread(this::renderPendingSlots);
            } catch (Exception exception) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.import_students_error, exception.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    private void importPendingSlots() {
        List<Student> importedStudents = new ArrayList<>();
        int selectedFileCount = 0;
        for (PendingImportSlot slot : pendingImportSlots) {
            if (!slot.students.isEmpty()) {
                importedStudents.addAll(slot.students);
                selectedFileCount++;
            }
        }

        if (selectedFileCount == 0) {
            Toast.makeText(requireContext(), R.string.import_select_at_least_one, Toast.LENGTH_LONG).show();
            return;
        }

        ImportPreview preview = buildImportPreview(importedStudents);
        if (preview.totalRows == 0) {
            Toast.makeText(requireContext(), R.string.import_students_empty, Toast.LENGTH_LONG).show();
            return;
        }
        showImportPreviewDialog(preview, selectedFileCount);
    }

    private ImportPreview buildImportPreview(@NonNull List<Student> importedStudents) {
        List<Student> validStudents = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        Set<String> existingKeys = new LinkedHashSet<>();

        if (allStudents != null) {
            for (StudentOverview overview : allStudents) {
                existingKeys.add(buildKey(
                        overview.getName(),
                        overview.getClassName(),
                        overview.getDivision(),
                        overview.getEmail()
                ));
            }
        }

        int skippedExisting = 0;
        int skippedInFile = 0;

        for (Student student : importedStudents) {
            String key = buildKey(student.getName(), student.getClassName(), student.getDivision(), student.getEmail());
            if (existingKeys.contains(key)) {
                skippedExisting++;
                continue;
            }
            if (!seenKeys.add(key)) {
                skippedInFile++;
                continue;
            }
            validStudents.add(student);
        }

        return new ImportPreview(
                importedStudents.size(),
                validStudents,
                skippedExisting,
                skippedInFile
        );
    }

    private FileImportPreview buildFilePreview(@NonNull String fileName, @NonNull List<Student> students) {
        if (students.isEmpty()) {
            return new FileImportPreview(
                    fileName,
                    0,
                    getString(R.string.selected_files_empty_preview)
            );
        }

        List<String> names = new ArrayList<>();
        for (Student student : students) {
            String studentName = student.getName() == null ? "" : student.getName().trim();
            String className = student.getClassName() == null ? "" : student.getClassName().trim();
            String division = student.getDivision() == null ? "" : student.getDivision().trim();
            String classLabel = className;
            if (!division.isEmpty()) {
                classLabel = className.isEmpty() ? division : className + " - " + division;
            }

            if (classLabel.isEmpty()) {
                names.add("- " + studentName);
            } else {
                names.add("- " + studentName + " (" + classLabel + ")");
            }
        }

        return new FileImportPreview(
                fileName,
                students.size(),
                android.text.TextUtils.join("\n", names)
        );
    }

    private List<PendingImportSlot> createPendingSlots() {
        List<PendingImportSlot> slots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            slots.add(new PendingImportSlot());
        }
        return slots;
    }

    private void resetPendingSlots() {
        for (int i = 0; i < pendingImportSlots.size(); i++) {
            PendingImportSlot slot = pendingImportSlots.get(i);
            slot.fileName = "";
            slot.className = PREDEFINED_CLASSES[Math.min(i, PREDEFINED_CLASSES.length - 1)];
            slot.students.clear();
        }
    }

    private void renderPendingSlots() {
        if (importDialogBinding == null) {
            return;
        }

        renderSlot(importDialogBinding.slot1Title, importDialogBinding.slot1FileName, importDialogBinding.slot1Students, 0);
        renderSlot(importDialogBinding.slot2Title, importDialogBinding.slot2FileName, importDialogBinding.slot2Students, 1);
        renderSlot(importDialogBinding.slot3Title, importDialogBinding.slot3FileName, importDialogBinding.slot3Students, 2);
        renderSlot(importDialogBinding.slot4Title, importDialogBinding.slot4FileName, importDialogBinding.slot4Students, 3);
        renderSlot(importDialogBinding.slot5Title, importDialogBinding.slot5FileName, importDialogBinding.slot5Students, 4);
    }

    private void renderSlot(android.widget.TextView title, android.widget.TextView fileName,
                            android.widget.TextView studentsView, int index) {
        PendingImportSlot slot = pendingImportSlots.get(index);
        title.setText(getString(R.string.import_slot_title_with_class, index + 1, slot.className));
        if (slot.fileName.isEmpty()) {
            fileName.setText(R.string.import_slot_empty);
            studentsView.setText(R.string.import_slot_empty_hint);
            return;
        }
        fileName.setText(slot.fileName + " (" + slot.students.size() + " students)");
        FileImportPreview preview = buildFilePreview(slot.fileName, slot.students);
        studentsView.setText(preview.studentNamesPreview);
    }

    private void showImportPreviewDialog(@NonNull ImportPreview preview, int fileCount) {
        String message = getString(
                R.string.import_preview_message_multi,
                fileCount,
                preview.totalRows,
                preview.validStudents.size(),
                preview.skippedExisting,
                preview.skippedInFile
        );

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_preview_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        preview.validStudents.isEmpty() ? R.string.ok : R.string.save_students,
                        (dialog, which) -> {
                            if (preview.validStudents.isEmpty()) {
                                dialog.dismiss();
                                return;
                            }
                            viewModel.importStudents(preview.validStudents);
                            if (importDialog != null) {
                                importDialog.dismiss();
                                importDialog = null;
                            }
                            importDialogBinding = null;
                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.import_students_success, preview.validStudents.size()),
                                    Toast.LENGTH_LONG
                            ).show();
                        })
                .show();
    }

    private String buildKey(String name, String className, String division, String email) {
        String normalizedEmail = normalizeValue(email);
        if (!normalizedEmail.isEmpty()) {
            return "email:" + normalizedEmail;
        }
        return "name:" + normalizeValue(name)
                + "|class:" + normalizeValue(className)
                + "|division:" + normalizeValue(division);
    }

    private List<Student> applyDivisionSelection(@NonNull List<Student> students,
                                                 @NonNull ClassDivisionSelection selection) {
        List<Student> mappedStudents = new ArrayList<>();
        for (Student student : students) {
            String division = student.getDivision() == null ? "" : student.getDivision().trim();
            if (division.isEmpty()) {
                division = selection.division;
            }
            mappedStudents.add(new Student(
                    student.getId(),
                    student.getName(),
                    selection.className,
                    student.getEmail(),
                    student.getPassword(),
                    student.getMobileNo(),
                    student.getAbcId(),
                    student.getEnrollmentNo(),
                    student.getRollNo(),
                    division,
                    student.getBranch()
            ));
        }
        return mappedStudents;
    }

    private ClassDivisionSelection normalizeClassAndDivision(String rawClassName, String rawDivision) {
        String className = rawClassName == null ? "" : rawClassName.trim();
        String division = rawDivision == null ? "" : rawDivision.trim().toUpperCase(Locale.ROOT);

        if (division.isEmpty()) {
            String[] parts = className.split("\\s+");
            if (parts.length >= 2) {
                String lastPart = parts[parts.length - 1].trim();
                if (lastPart.length() == 1 && Character.isLetter(lastPart.charAt(0))) {
                    division = lastPart.toUpperCase(Locale.ROOT);
                    className = className.substring(0, className.length() - lastPart.length()).trim();
                }
            }
        }

        return new ClassDivisionSelection(className, division);
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void showStudentDialog(@Nullable StudentOverview overview) {
        studentDialogBinding = DialogStudentBinding.inflate(getLayoutInflater());
        DialogStudentBinding dialogBinding = studentDialogBinding;
        boolean isEditing = overview != null;
        pendingStudentPhotoUri = null;
        editingStudentEmail = isEditing ? overview.getEmail() : "";
        if (isEditing) {
            dialogBinding.emailEditText.setText(overview.getEmail());
            dialogBinding.passwordEditText.setText(overview.getPassword());
            dialogBinding.nameEditText.setText(overview.getName());
            dialogBinding.classEditText.setText(overview.getClassName());
            dialogBinding.rollNoEditText.setText(overview.getRollNo());
            dialogBinding.divisionEditText.setText(overview.getDivision());
            dialogBinding.branchEditText.setText(overview.getBranch());
            dialogBinding.enrollmentEditText.setText(overview.getEnrollmentNo());
            dialogBinding.mobileEditText.setText(overview.getMobileNo());
            dialogBinding.abcIdEditText.setText(overview.getAbcId());
        }
        bindStudentPhotoPreview(isEditing ? overview.getEmail() : "");
        dialogBinding.selectPhotoButton.setOnClickListener(v -> studentPhotoPickerLauncher.launch(new String[]{"image/*"}));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEditing ? R.string.edit_student : R.string.add_student)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = String.valueOf(dialogBinding.emailEditText.getText()).trim();
            String password = String.valueOf(dialogBinding.passwordEditText.getText()).trim();
            String name = String.valueOf(dialogBinding.nameEditText.getText()).trim();
            String className = String.valueOf(dialogBinding.classEditText.getText()).trim();
            String division = String.valueOf(dialogBinding.divisionEditText.getText()).trim();
            String rollNo = String.valueOf(dialogBinding.rollNoEditText.getText()).trim();
            String branch = String.valueOf(dialogBinding.branchEditText.getText()).trim();
            String enrollment = String.valueOf(dialogBinding.enrollmentEditText.getText()).trim();
            String mobile = String.valueOf(dialogBinding.mobileEditText.getText()).trim();
            String abcId = String.valueOf(dialogBinding.abcIdEditText.getText()).trim();

            ClassDivisionSelection selection = normalizeClassAndDivision(className, division);
            className = selection.className;
            division = selection.division;

            if (name.isEmpty() || className.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Email, Password, Name and Class are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditing) {
                if (!overview.getEmail().equalsIgnoreCase(email)) {
                    StudentPhotoStore.renamePhotoKey(requireContext(), overview.getEmail(), email);
                }
                persistStudentPhoto(email);
                viewModel.updateStudent(overview.getId(), name, className, email, password, rollNo, division, branch, enrollment, mobile, abcId);
                Toast.makeText(requireContext(), R.string.student_updated, Toast.LENGTH_SHORT).show();
            } else {
                persistStudentPhoto(email);
                viewModel.addStudent(name, className, email, password, rollNo, division, branch, enrollment, mobile, abcId);
                Toast.makeText(requireContext(), R.string.student_saved, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        }));
        dialog.setOnDismissListener(d -> {
            studentDialogBinding = null;
            pendingStudentPhotoUri = null;
            editingStudentEmail = "";
        });
        dialog.show();
    }

    @Override
    public void onEdit(@NonNull StudentOverview overview) {
        showStudentDialog(overview);
    }

    @Override
    public void onDelete(@NonNull StudentOverview overview) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_student_title)
                .setMessage(R.string.delete_student_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteStudent(overview);
                    Toast.makeText(requireContext(), R.string.student_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ImportPreview {
        final int totalRows;
        final List<Student> validStudents;
        final int skippedExisting;
        final int skippedInFile;

        ImportPreview(int totalRows, List<Student> validStudents, int skippedExisting, int skippedInFile) {
            this.totalRows = totalRows;
            this.validStudents = validStudents;
            this.skippedExisting = skippedExisting;
            this.skippedInFile = skippedInFile;
        }
    }

    private static class FileImportPreview {
        final String fileName;
        final int studentCount;
        final String studentNamesPreview;

        FileImportPreview(String fileName, int studentCount, String studentNamesPreview) {
            this.fileName = fileName;
            this.studentCount = studentCount;
            this.studentNamesPreview = studentNamesPreview;
        }
    }

    private static class PendingImportSlot {
        String className = "";
        String fileName = "";
        final List<Student> students = new ArrayList<>();
    }

    private static class ClassDivisionSelection {
        final String className;
        final String division;

        ClassDivisionSelection(String className, String division) {
            this.className = className;
            this.division = division;
        }
    }

    private void handleStudentPhotoPicked(@Nullable Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some providers do not expose persistable permissions.
        }

        pendingStudentPhotoUri = uri;
        bindStudentPhotoPreview(editingStudentEmail);
    }

    private void bindStudentPhotoPreview(@Nullable String email) {
        if (studentDialogBinding == null) {
            return;
        }

        String previewEmail = email == null ? "" : email;
        if (pendingStudentPhotoUri != null) {
            studentDialogBinding.studentPhotoPreview.setPadding(0, 0, 0, 0);
            studentDialogBinding.studentPhotoPreview.setBackground(null);
            studentDialogBinding.studentPhotoPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            studentDialogBinding.studentPhotoPreview.setImageURI(pendingStudentPhotoUri);
            return;
        }

        StudentPhotoStore.applyStudentPhotoOrDefault(
                studentDialogBinding.studentPhotoPreview,
                requireContext(),
                previewEmail
        );
    }

    private void persistStudentPhoto(String email) {
        if (pendingStudentPhotoUri == null || email == null || email.trim().isEmpty()) {
            return;
        }
        StudentPhotoStore.savePhotoUri(requireContext(), email, pendingStudentPhotoUri.toString());
    }
}
