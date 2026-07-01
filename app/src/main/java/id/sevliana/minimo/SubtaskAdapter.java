package id.sevliana.minimo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.VH> {

    // Interface untuk komunikasi ke AddTaskBottomSheet
    public interface OnSubtaskListener {
        void onDeleted(int position);
        void onCheckedChanged(int position, boolean isDone);
    }

    private List<Subtask> list;
    private OnSubtaskListener listener;

    public SubtaskAdapter(List<Subtask> list, OnSubtaskListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Memastikan layout item_subtask terpasang dengan benar
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtask, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Subtask sub = list.get(position);
        holder.tvTitle.setText(sub.getTitle());

        // 1. Reset listener ke null sebelum setChecked untuk menghindari bug saat scrolling/recycling
        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(sub.isDone());

        // 2. Pasang kembali listener untuk mendeteksi centang
        holder.cb.setOnCheckedChangeListener((v, isChecked) -> {
            int currentPos = holder.getBindingAdapterPosition(); // Lebih aman daripada 'position'
            if (currentPos != RecyclerView.NO_POSITION) {
                list.get(currentPos).setDone(isChecked);
                if (listener != null) {
                    listener.onCheckedChanged(currentPos, isChecked);
                }
            }
        });

        // 3. Listener untuk tombol hapus rincian
        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDeleted(currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (list != null) ? list.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvTitle;
        ImageButton btnDelete;

        VH(View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.checkboxSubtask);
            tvTitle = itemView.findViewById(R.id.tvSubtaskTitle);
            btnDelete = itemView.findViewById(R.id.btnDeleteSubtask);
        }
    }
}