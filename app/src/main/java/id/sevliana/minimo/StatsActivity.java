package id.sevliana.minimo;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private BarChart barChart;
    private FirebaseFirestore db;
    private String uid;
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (findViewById(R.id.toolbar) != null) {
            setSupportActionBar(findViewById(R.id.toolbar));
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Statistik Per Kategori");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // Listener harus dipasang SEBELUM data dimuat
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e == null) return;

                // Debugging: Muncul di Logcat untuk memastikan klik tembus
                Log.d("MINIMO_STATS", "Batang diklik pada posisi X: " + e.getX());

                int index = (int) e.getX();
                if (index >= 0 && index < categoryNames.size()) {
                    String label = categoryNames.get(index);
                    int val = (int) e.getY();
                    Toast.makeText(StatsActivity.this, label + ": " + val + " Tugas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        loadCategoryStats();
    }

    private void loadCategoryStats() {
        if (uid == null) return;
        db.collection("tasks").whereEqualTo("userId", uid).get()
                .addOnSuccessListener(value -> {
                    Map<String, Integer> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Task task = doc.toObject(Task.class);
                        String name = (task.getCategoryName() == null || task.getCategoryName().isEmpty()) ? "Lainnya" : task.getCategoryName();
                        map.put(name, map.getOrDefault(name, 0) + 1);
                    }
                    if (!map.isEmpty()) {
                        setupChartData(map);
                    } else {
                        Toast.makeText(this, "Tidak ada data tugas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupChartData(Map<String, Integer> dataMap) {
        List<BarEntry> entries = new ArrayList<>();
        categoryNames.clear();

        int i = 0;
        for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
            categoryNames.add(entry.getKey());
            entries.add(new BarEntry(i, entry.getValue()));
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tugas");
        dataSet.setColors(new int[]{Color.parseColor("#42A5F5"), Color.parseColor("#66BB6A"), Color.parseColor("#FFA726"), Color.parseColor("#AB47BC"), Color.parseColor("#EF5350")});

        // PENTING: Pengaturan Klik pada Level Data
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);

        // PENTING: Pengaturan Klik pada Level Chart
        barChart.setTouchEnabled(true);
        barChart.setClickable(true);
        barChart.setHighlightPerTapEnabled(true);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(categoryNames));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-20);

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.animateY(1000);

        // Paksa sistem merespon klik baru
        barChart.highlightValues(null);
        barChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}