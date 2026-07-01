package id.sevliana.minimo;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rvCategory;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Kategori");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_migo));
        }

        if (findViewById(R.id.toolbarCategory) != null) {
            setSupportActionBar(findViewById(R.id.toolbarCategory));
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Kategori");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rvCategory = findViewById(R.id.rvCategory);
        adapter = new CategoryAdapter(categoryList, this::showEditDialog);
        rvCategory.setLayoutManager(new LinearLayoutManager(this));
        rvCategory.setAdapter(adapter);
        FloatingActionButton fab = findViewById(R.id.fabAddCategory);
        fab.setOnClickListener(v -> showAddDialog());
        loadCategories();
    }

    private void loadCategories() {
        db.collection("categories")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    categoryList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Category cat = doc.toObject(Category.class);
                            cat.setId(doc.getId());
                            categoryList.add(cat);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_add_category, null);
        bottomSheetDialog.setContentView(view);
        EditText etName = view.findViewById(R.id.etCategoryName);
        RecyclerView rvColor = view.findViewById(R.id.rvColorPicker);
        Button btnSave = view.findViewById(R.id.btnSaveCategory);

        String[] colors = {
                "#6366F1", // Indigo (Kuliah/Kerja) - Lebih lembut dari biru biasa
                "#FB7185", // Rose (Pribadi) - Pengganti merah yang lebih modern
                "#10B981", // Emerald (Kesehatan) - Hijau yang segar
                "#F59E0B", // Amber (Penting) - Kuning mustard yang elegan
                "#8B5CF6", // Violet (Hobi) - Ungu yang mewah
                "#0EA5E9", // Sky Blue (Santai) - Biru langit yang cerah
                "#64748B", // Slate (Lain-lain) - Abu-abu metalik yang pro
                "#334155"  // Charcoal (Urgent) - Hitam arang yang minimalis
        };
        final String[] selectedColor = {colors[0]};

        ColorAdapter colorAdapter = new ColorAdapter(colors, color -> selectedColor[0] = color);
        rvColor.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvColor.setAdapter(colorAdapter);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Nama wajib diisi");
                return;
            }
            Category newCat = new Category(name, selectedColor[0], uid);
            db.collection("categories").add(newCat).addOnSuccessListener(doc -> {
                Toast.makeText(this, "Kategori ditambahkan", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });
        });
        bottomSheetDialog.show();
    }

    private void showEditDialog(Category category) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText etName = view.findViewById(R.id.etCategoryName);
        View viewPreview = view.findViewById(R.id.viewColorPreview);
        Button btnPickColor = view.findViewById(R.id.btnPickColor);

        etName.setText(category.getName());
        final String[] selectedColor = {category.getColorHex()};
        if (viewPreview != null) viewPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));

        btnPickColor.setOnClickListener(v -> {
            String[] names = {"Biru", "Merah", "Hijau", "Kuning", "Oranye", "Ungu", "Hitam", "Cokelat", "Abu-abu"};
            String[] hexs = {"#2196F3", "#F44336", "#4CAF50", "#FFEB3B", "#FF9800", "#9C27B0", "#000000", "#795548", "#9E9E9E"};

            new AlertDialog.Builder(this)
                    .setTitle("Pilih Warna")
                    .setItems(names, (dialog, which) -> {
                        selectedColor[0] = hexs[which];
                        if (viewPreview != null) viewPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
                    }).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Update Kategori")
                .setView(view)
                .setPositiveButton("Update", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        category.setName(name);
                        category.setColorHex(selectedColor[0]);
                        db.collection("categories").document(category.getId()).set(category);
                    }
                })
                .setNegativeButton("Batal", null)
                .setNeutralButton("Hapus", (d, w) -> confirmDeleteCategory(category))
                .create();

        dialog.show();
        Button btnHapus = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (btnHapus != null) btnHapus.setTextColor(Color.RED);
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Kategori?")
                .setMessage("Yakin ingin menghapus '" + category.getName() + "'?")
                .setPositiveButton("Hapus", (d, w) -> {
                    db.collection("categories").document(category.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Kategori dihapus", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ADAPTER DALAM (INNER CLASS)
    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        List<Category> list;
        OnItemClick listener;

        interface OnItemClick { void onClick(Category cat); }

        CategoryAdapter(List<Category> list, OnItemClick listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Category cat = list.get(position);
            holder.tvName.setText(cat.getName());
            holder.tvHex.setText(cat.getColorHex());

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            try {
                shape.setColor(Color.parseColor(cat.getColorHex()));
                holder.tvName.setTextColor(Color.parseColor(cat.getColorHex()));
            } catch (Exception e) {
                shape.setColor(Color.GRAY);
                holder.tvName.setTextColor(Color.BLACK);
            }
            holder.viewColorCircle.setBackground(shape);
            holder.itemView.setOnClickListener(v -> listener.onClick(cat));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvHex;
            View viewColorCircle;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCategoryNameText);
                tvHex = itemView.findViewById(R.id.tvCategoryHexText);
                viewColorCircle = itemView.findViewById(R.id.viewCategoryColorCircle);
            }
        }
    }
}