package id.sevliana.minimo;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
    private final String[] colors;
    private final OnColorSelected listener;
    private int selectedPosition = 0;

    public interface OnColorSelected {
        void onSelected(String color);
    }

    public ColorAdapter(String[] colors, OnColorSelected listener) {
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        String colorHex = colors[position];

        // Membuat bentuk lingkaran secara dinamis
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor(colorHex));

        // Memberi border hitam tebal jika warna tersebut sedang dipilih
        if (selectedPosition == position) {
            shape.setStroke(6, Color.parseColor("#000000"));
        } else {
            shape.setStroke(2, Color.parseColor("#CCCCCC"));
        }

        holder.colorView.setBackground(shape);

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onSelected(colorHex);
        });
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.viewColorCircle);
        }
    }
}