package id.sevliana.minimo;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {
    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // 1. Setup ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Kalender Tugas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 2. Inisialisasi View
        calendarView = findViewById(R.id.calendarView);
        rvTasks = findViewById(R.id.rvCalendarTasks);

        // 3. Paksa refresh Text Appearance (Opsional agar lebih mantap)
        calendarView.setHeaderTextAppearance(R.style.CalendarEliteTitle);
        calendarView.setDateTextAppearance(R.style.CalendarEliteText);
        calendarView.setWeekDayTextAppearance(R.style.CalendarEliteText);

        // 4. Setup RecyclerView
        adapter = new TaskAdapter(new ArrayList<>(), null);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);

        loadTasksFromFirestore();

        // 5. Listener Klik Tanggal
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            filterTasksByDate(date);
        });
    }

    private void loadTasksFromFirestore() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("tasks").whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    allTasks.clear();
                    HashSet<CalendarDay> daysWithTasks = new HashSet<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null && task.getDeadline() != null) {
                            task.setId(doc.getId());
                            allTasks.add(task);

                            // Ambil tanggal untuk dekorasi titik di kalender
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(task.getDeadline());
                            CalendarDay day = CalendarDay.from(
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH) + 1,
                                    cal.get(Calendar.DAY_OF_MONTH)
                            );
                            daysWithTasks.add(day);
                        }
                    }
                    // Tambahkan titik merah pada tanggal yang ada tugas
                    calendarView.addDecorator(new EventDecorator(androidx.core.content.ContextCompat.getColor(this, R.color.primary_migo), daysWithTasks));
                    // Filter otomatis untuk hari ini saat pertama buka
                    calendarView.setSelectedDate(CalendarDay.today());
                    filterTasksByDate(CalendarDay.today());
                });
    }

    private void filterTasksByDate(CalendarDay date) {
        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(t.getDeadline());

            if (cal.get(Calendar.DAY_OF_MONTH) == date.getDay() &&
                    cal.get(Calendar.MONTH) + 1 == date.getMonth() &&
                    cal.get(Calendar.YEAR) == date.getYear()) {
                filtered.add(t);
            }
        }
        adapter.updateData(filtered);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}