package id.sevliana.minimo;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import id.sevliana.minimo.databinding.BottomSheetAddTaskBinding;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetAddTaskBinding binding;
    private OnTaskAddedListener listener;
    private Date selectedDate = null;
    private String taskId = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
    private List<Category> categoryList;
    private Category selectedCategory = null;
    private List<Subtask> subtaskList = new ArrayList<>();
    private SubtaskAdapter subtaskAdapter;
    private Uri fileUri = null;
    private String base64Image = null;
    private String uploadedName = null;
    private ActivityResultLauncher<String> filePickerLauncher;

    public interface OnTaskAddedListener {
        void onTaskAdded(Task task);
    }

    // Instance untuk Tambah Tugas Baru
    public static AddTaskBottomSheet newInstance(List<Category> categories) {
        AddTaskBottomSheet fragment = new AddTaskBottomSheet();
        fragment.categoryList = categories;
        return fragment;
    }

    // Instance untuk Edit Tugas Lama
    public static AddTaskBottomSheet newInstance(Task task, List<Category> categories) {
        AddTaskBottomSheet fragment = new AddTaskBottomSheet();
        Bundle args = new Bundle();
        args.putString("id", task.getId());
        args.putString("title", task.getTitle());
        args.putString("desc", task.getDesc());
        args.putBoolean("isPrivate", task.isPrivate());
        if (task.getDeadline() != null) args.putLong("deadline", task.getDeadline().getTime());
        args.putString("categoryId", task.getCategoryId());
        args.putString("attachmentUrl", task.getAttachmentUrl());
        args.putString("attachmentName", task.getAttachmentName());
        fragment.setArguments(args);
        fragment.categoryList = categories;
        fragment.subtaskList = task.getSubtasks() != null ? new ArrayList<>(task.getSubtasks()) : new ArrayList<>();
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnTaskAddedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTaskAddedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup Picker Gambar
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        fileUri = uri;
                        uploadedName = "image_" + System.currentTimeMillis() + ".jpg";
                        binding.tvAttachmentName.setText("Gambar siap diunggah");
                        binding.layoutAttachment.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupCategoryDropdown();
        setupSubtaskRecycler();

        // Jika dalam mode EDIT, isi field dengan data lama
        if (getArguments() != null) {
            taskId = getArguments().getString("id");
            binding.tvSheetTitle.setText("Edit Tugas"); // Judul Header
            binding.etTitle.setText(getArguments().getString("title"));
            binding.etDesc.setText(getArguments().getString("desc"));

            boolean isPrivate = getArguments().getBoolean("isPrivate", false);
            binding.switchVisibility.setChecked(isPrivate);

            long deadlineMillis = getArguments().getLong("deadline", 0);
            if (deadlineMillis != 0) {
                selectedDate = new Date(deadlineMillis);
                binding.etDeadline.setText(sdf.format(selectedDate));
            }

            String catId = getArguments().getString("categoryId");
            if (catId != null && categoryList != null) {
                for (Category cat : categoryList) {
                    if (cat.getId().equals(catId)) {
                        selectedCategory = cat;
                        binding.actvCategory.setText(cat.getName(), false);
                        break;
                    }
                }
            }

            base64Image = getArguments().getString("attachmentUrl");
            uploadedName = getArguments().getString("attachmentName");
            if (base64Image != null && !base64Image.isEmpty()) {
                binding.tvAttachmentName.setText("Gambar tersimpan");
                binding.layoutAttachment.setVisibility(View.VISIBLE);
            }
            binding.btnSave.setText("Update Tugas");
        }

        // Click Listeners
        binding.etDeadline.setFocusable(false);
        binding.etDeadline.setOnClickListener(v -> showDatePicker());
        binding.btnAddSubtask.setOnClickListener(v -> addSubtask());
        binding.btnUpload.setOnClickListener(v -> filePickerLauncher.launch("image/*"));
        binding.btnRemoveAttachment.setOnClickListener(v -> removeAttachment());
        binding.btnSave.setOnClickListener(v -> saveTask());
    }

    private void setupCategoryDropdown() {
        if (categoryList == null) categoryList = new ArrayList<>();
        List<String> names = new ArrayList<>();
        names.add("Tanpa Kategori");
        for (Category cat : categoryList) names.add(cat.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        binding.actvCategory.setAdapter(adapter);
        binding.actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) selectedCategory = null;
            else selectedCategory = categoryList.get(position - 1);
        });
    }

    private void setupSubtaskRecycler() {
        subtaskAdapter = new SubtaskAdapter(subtaskList, new SubtaskAdapter.OnSubtaskListener() {
            @Override
            public void onDeleted(int pos) {
                subtaskList.remove(pos);
                subtaskAdapter.notifyDataSetChanged();
                updateTaskInFirestore(); // Langsung update cloud
            }

            @Override
            public void onCheckedChanged(int pos, boolean isDone) {
                updateTaskInFirestore();
            }
        });
        binding.rvSubtasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSubtasks.setAdapter(subtaskAdapter);
    }

    private void addSubtask() {
        String title = binding.etSubtask.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Tulis rincian dulu", Toast.LENGTH_SHORT).show();
            return;
        }
        subtaskList.add(new Subtask(title));
        subtaskAdapter.notifyItemInserted(subtaskList.size() - 1);
        binding.etSubtask.setText("");
        updateTaskInFirestore(); // Simpan jika sedang mode edit
    }

    private void removeAttachment() {
        fileUri = null;
        base64Image = null;
        uploadedName = null;
        binding.layoutAttachment.setVisibility(View.GONE);
    }

    // Fungsi Kunci untuk Sinkronisasi Real-time
    private void updateTaskInFirestore() {
        if (taskId != null) {
            FirebaseFirestore.getInstance()
                    .collection("tasks")
                    .document(taskId)
                    .update("subtasks", subtaskList)
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Gagal sinkron rincian", Toast.LENGTH_SHORT).show());
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) calendar.setTime(selectedDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            selectedDate = calendar.getTime();
            binding.etDeadline.setText(sdf.format(selectedDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTask() {
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDesc.getText().toString().trim();
        boolean isPrivateStatus = binding.switchVisibility.isChecked();

        if (title.isEmpty()) {
            binding.etTitle.setError("Judul tugas wajib ada!");
            return;
        }
        binding.btnSave.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Jika ada gambar baru yang dipilih, convert dulu
        if (fileUri != null) {
            base64Image = uriToBase64(fileUri);
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDesc(desc);
        task.setDeadline(selectedDate);
        task.setPrivate(isPrivateStatus); // SET VISIBILITAS DI SINI

        if (taskId != null) task.setId(taskId);
        if (selectedCategory != null) {
            task.setCategoryId(selectedCategory.getId());
            task.setCategoryName(selectedCategory.getName());
            task.setCategoryColor(selectedCategory.getColorHex());
        }
        task.setSubtasks(subtaskList);
        task.setAttachmentUrl(base64Image);
        task.setAttachmentName(uploadedName);

        if (listener != null) {
            listener.onTaskAdded(task);
        }
        dismiss();
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Kompresi gambar agar tidak terlalu besar di Firestore
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
