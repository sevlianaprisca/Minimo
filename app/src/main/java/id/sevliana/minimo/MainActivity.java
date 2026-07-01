package id.sevliana.minimo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import id.sevliana.minimo.databinding.ActivityMainBinding;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AddTaskBottomSheet.OnTaskAddedListener, TaskAdapter.OnTaskClickListener {

    private ActivityMainBinding binding;
    private TaskViewModel viewModel;
    private TaskAdapter adapter;

    private List<Task> allTasks = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();

    private String currentFilter = "ALL";
    private String currentSort = "DEADLINE";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Dark Mode
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // 2. Cek Login
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // 3. View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Styling Status Bar & Toolbar (Elite Look)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_migo));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.full_app_name);
        }

        // 4. Inisialisasi UI & Data
        setupRecyclerView();
        setupViewModel();
        setupFilter();
        setupSwipe();

        binding.fabAdd.setOnClickListener(v -> {
            AddTaskBottomSheet bottomSheet = AddTaskBottomSheet.newInstance(categoryList);
            bottomSheet.show(getSupportFragmentManager(), "AddTaskBottomSheet");
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // OBSERVASI REAL-TIME: Data tugas akan otomatis ter-update di sini
        // saat Firebase mendeteksi perubahan rincian tugas di database.
        viewModel.getTasks().observe(this, tasks -> {
            this.allTasks = tasks;
            applyFilterAndSort();
        });

        viewModel.getCategories().observe(this, categories -> {
            this.categoryList = categories;
        });
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(new ArrayList<>(), this);
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTasks.setAdapter(adapter);
    }

    private void applyFilterAndSort() {
        List<Task> filtered = new ArrayList<>();
        Date now = new Date();

        for (Task task : allTasks) {
            boolean matchFilter = false;
            switch (currentFilter) {
                case "ALL": matchFilter = true; break;
                case "ACTIVE": matchFilter = !task.isDone(); break;
                case "DONE": matchFilter = task.isDone(); break;
                case "OVERDUE": matchFilter = !task.isDone() && task.getDeadline() != null && task.getDeadline().before(now); break;
            }

            boolean matchSearch = searchQuery.isEmpty() || task.getTitle().toLowerCase().contains(searchQuery.toLowerCase());
            if (matchFilter && matchSearch) filtered.add(task);
        }

        // Sorting
        sortTasks(filtered);

        // UPDATE ADAPTER: Ini yang memicu tampilan angka Rincian 1/2 berubah
        adapter.updateData(filtered);

        // Empty State Logic
        if (filtered.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvTasks.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvTasks.setVisibility(View.VISIBLE);
        }
    }

    private void sortTasks(List<Task> filtered) {
        if (currentSort.equals("DEADLINE")) {
            Collections.sort(filtered, (t1, t2) -> {
                if (t1.getDeadline() == null) return 1;
                if (t2.getDeadline() == null) return -1;
                return t1.getDeadline().compareTo(t2.getDeadline());
            });
        } else if (currentSort.equals("TITLE")) {
            Collections.sort(filtered, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
        } else {
            Collections.sort(filtered, (t1, t2) -> {
                if (t1.getCreatedAt() == null) return 1;
                if (t2.getCreatedAt() == null) return -1;
                return t2.getCreatedAt().compareTo(t1.getCreatedAt());
            });
        }
    }

    private void setupSwipe() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                Task task = adapter.getTaskAt(pos);
                if (direction == ItemTouchHelper.LEFT) {
                    deleteTask(task);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    toggleDone(task);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_light))
                        .addSwipeLeftActionIcon(android.R.drawable.ic_menu_delete)
                        .addSwipeRightBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_light))
                        .addSwipeRightActionIcon(android.R.drawable.checkbox_on_background)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(binding.rvTasks);
    }

    private void deleteTask(Task task) {
        FirebaseFirestore.getInstance().collection("tasks").document(task.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    cancelNotification(task);
                    com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Tugas dihapus", 3000)
                            .setAction("UNDO", v -> onTaskAdded(task)).show();
                });
    }

    private void toggleDone(Task task) {
        boolean newStatus = !task.isDone();
        FirebaseFirestore.getInstance().collection("tasks").document(task.getId()).update("done", newStatus);
    }

    @Override
    public void onTaskAdded(Task task) {
        String uid = FirebaseAuth.getInstance().getUid();
        task.setUserId(uid);
        if (task.getId() != null) {
            FirebaseFirestore.getInstance().collection("tasks").document(task.getId()).set(task);
        } else {
            FirebaseFirestore.getInstance().collection("tasks").add(task).addOnSuccessListener(doc -> {
                task.setId(doc.getId());
                scheduleNotification(task);
            });
        }
    }

    @Override
    public void onCheckChanged(Task task, boolean isChecked) {
        FirebaseFirestore.getInstance().collection("tasks").document(task.getId()).update("done", isChecked);
    }

    @Override
    public void onItemClick(Task task) {
        AddTaskBottomSheet bottomSheet = AddTaskBottomSheet.newInstance(task, categoryList);
        bottomSheet.show(getSupportFragmentManager(), "EditTaskBottomSheet");
    }

    @Override
    public void onShareClick(Task task) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "📌 Nama Tugas: " + task.getTitle() + "\nStatus: " + (task.isDone() ? "Selesai" : "Aktif"));
        startActivity(Intent.createChooser(shareIntent, "Bagikan Rincian"));
    }

    private void setupFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll) currentFilter = "ALL";
            else if (id == R.id.chipActive) currentFilter = "ACTIVE";
            else if (id == R.id.chipDone) currentFilter = "DONE";
            else if (id == R.id.chipOverdue) currentFilter = "OVERDUE";
            applyFilterAndSort();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Cari tugas...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                applyFilterAndSort();
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_calendar) startActivity(new Intent(this, CalendarActivity.class));
        else if (id == R.id.action_statistik) startActivity(new Intent(this, StatsActivity.class));
        else if (id == R.id.action_kategori) startActivity(new Intent(this, CategoryActivity.class));
        else if (id == R.id.action_sort) showSortDialog();
        else if (id == R.id.action_logout) { FirebaseAuth.getInstance().signOut(); goToLogin(); }
        else if (id == R.id.action_dark_mode) toggleDarkMode();
        return super.onOptionsItemSelected(item);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        boolean newMode = !isDark;

        prefs.edit().putBoolean("dark_mode", newMode).apply();

        // Ubah mode
        AppCompatDelegate.setDefaultNightMode(newMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // TRIK ELITE: Matikan animasi di level Window sebelum recreate
        getWindow().setWindowAnimations(0);

        recreate();
    }

    private void showSortDialog() {
        String[] options = {"Deadline Terdekat", "Nama A-Z", "Terbaru"};
        new AlertDialog.Builder(this).setTitle("Urutkan").setItems(options, (dialog, which) -> {
            if (which == 0) currentSort = "DEADLINE";
            else if (which == 1) currentSort = "TITLE";
            else currentSort = "CREATED";
            applyFilterAndSort();
        }).show();
    }

    private void scheduleNotification(Task task) {
        if (task.getDeadline() == null || task.isDone()) return;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("title", task.getTitle());
        PendingIntent pi = PendingIntent.getBroadcast(this, task.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.getDeadline().getTime(), pi);
    }

    private void cancelNotification(Task task) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, task.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}