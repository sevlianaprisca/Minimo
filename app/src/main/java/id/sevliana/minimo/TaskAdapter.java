package id.sevliana.minimo;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final OnTaskClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    public interface OnTaskClickListener {
        void onCheckChanged(Task task, boolean isChecked);
        void onItemClick(Task task);
        void onShareClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = new ArrayList<>(taskList);
        this.listener = listener;
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    public void updateData(List<Task> newList) {
        // Menggunakan DiffUtil untuk mendeteksi perubahan data secara cerdas
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(this.taskList, newList));
        this.taskList.clear();
        this.taskList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Bind Nama Tugas & Catatan
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDesc());

        // Handle Checkbox tanpa memicu listener saat binding
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(task.isDone());

        // 1. Logika Tampilan Selesai (Strikethrough & Alpha)
        if (task.isDone()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
            holder.tvDesc.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
            holder.tvDesc.setAlpha(1.0f);
        }

        // 2. Logika Deadline
        if (task.getDeadline() != null) {
            holder.tvDeadline.setText(sdf.format(task.getDeadline()));
            holder.tvDeadline.setVisibility(View.VISIBLE);

            // Warnai merah jika terlambat dan belum selesai
            if (task.getDeadline().before(new java.util.Date()) && !task.isDone()) {
                holder.tvDeadline.setTextColor(Color.RED);
            } else {
                holder.tvDeadline.setTextColor(Color.GRAY);
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }

        // 3. Logika Warna Kategori & Indikator Samping
        String colorHex = task.getCategoryColor();
        int color = Color.GRAY;
        try {
            if (colorHex != null && !colorHex.isEmpty()) {
                color = Color.parseColor(colorHex);
            }
        } catch (Exception e) {
            color = Color.GRAY;
        }
        holder.viewCategoryColor.setBackgroundColor(color);

        // 4. RINCIAN TUGAS (Circular Progress & Teks)
        if (task.getTotalSubtasks() > 0) {
            holder.cpRincian.setVisibility(View.VISIBLE);
            holder.tvSubtaskProgress.setVisibility(View.VISIBLE);

            // Mengambil format dari strings.xml: "Rincian: 1/2"
            String statusRincian = holder.itemView.getContext().getString(
                    R.string.status_rincian,
                    task.getCompletedSubtasks(),
                    task.getTotalSubtasks()
            );
            holder.tvSubtaskProgress.setText(statusRincian);

            // Hitung Progres Lingkaran (%)
            int progressPercent = (int) ((double) task.getCompletedSubtasks() / task.getTotalSubtasks() * 100);
            holder.cpRincian.setProgress(progressPercent, true);
        } else {
            holder.cpRincian.setVisibility(View.GONE);
            holder.tvSubtaskProgress.setVisibility(View.GONE);
        }

        // Cari bagian ini di onBindViewHolder kamu dan ubah menjadi:
        if (task.isPrivate()) {
            holder.imgLock.setVisibility(View.VISIBLE); // Tanpa .binding
            holder.btnShare.setVisibility(View.GONE);
        } else {
            holder.imgLock.setVisibility(View.GONE);
            holder.btnShare.setVisibility(View.VISIBLE);
        }

        // Listeners
        holder.cbDone.setOnCheckedChangeListener((v, isChecked) -> {
            if (listener != null) listener.onCheckChanged(task, isChecked);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(task);
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvDeadline, tvSubtaskProgress;
        CheckBox cbDone;
        ImageButton btnShare;
        CircularProgressIndicator cpRincian;
        View viewCategoryColor;
        ImageView imgLock;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            cbDone = itemView.findViewById(R.id.checkboxDone);
            tvSubtaskProgress = itemView.findViewById(R.id.tvSubtaskProgress);
            btnShare = itemView.findViewById(R.id.btnShare);
            cpRincian = itemView.findViewById(R.id.cpRincian);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            imgLock = itemView.findViewById(R.id.imgLock); // Pastikan ID ini ada di item_task.xml
        }
    }

    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldList, newList;

        public TaskDiffCallback(List<Task> oldList, List<Task> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int op, int np) {
            return oldList.get(op).getId().equals(newList.get(np).getId());
        }

        @Override
        public boolean areContentsTheSame(int op, int np) {
            Task oldTask = oldList.get(op);
            Task newTask = newList.get(np);

            // Cek semua field yang mungkin berubah
            boolean sameStatus = oldTask.isDone() == newTask.isDone();
            boolean sameTitle = oldTask.getTitle().equals(newTask.getTitle());
            boolean sameDesc = (oldTask.getDesc() == null && newTask.getDesc() == null) ||
                    (oldTask.getDesc() != null && oldTask.getDesc().equals(newTask.getDesc()));
            boolean sameProgress = oldTask.getCompletedSubtasks() == newTask.getCompletedSubtasks() &&
                    oldTask.getTotalSubtasks() == newTask.getTotalSubtasks();
            boolean sameCategory = (oldTask.getCategoryColor() == null && newTask.getCategoryColor() == null) ||
                    (oldTask.getCategoryColor() != null && oldTask.getCategoryColor().equals(newTask.getCategoryColor()));
            boolean sameVisibility = oldTask.isPrivate() == newTask.isPrivate();

            return sameStatus && sameTitle && sameDesc && sameProgress && sameCategory && sameVisibility;
        }
    }
}