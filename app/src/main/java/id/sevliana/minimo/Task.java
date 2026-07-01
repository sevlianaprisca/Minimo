package id.sevliana.minimo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task {
    private String id;
    private String title;
    private String desc;
    private Date deadline;
    private String userId;
    private boolean done = false;

    private String categoryId;
    private String categoryName;
    private String categoryColor;

    // Inisialisasi list agar tidak terjadi NullPointerException
    private List<Subtask> subtasks = new ArrayList<>();

    private String attachmentUrl;
    private String attachmentName;

    @ServerTimestamp
    private Date createdAt;

    // Constructor kosong wajib untuk Firebase
    public Task() {}

    public Task(String title, String desc, Date deadline, String userId) {
        this.title = title;
        this.desc = desc;
        this.deadline = deadline;
        this.userId = userId;
        this.subtasks = new ArrayList<>(); // Pastikan selalu siap
    }

    // --- FUNGSI LOGIKA (Penting untuk Progress Ring) ---

    // Mengambil total jumlah rincian tugas
    public int getTotalSubtasks() {
        return (subtasks != null) ? subtasks.size() : 0;
    }

    // Menghitung jumlah rincian tugas yang sudah dicentang
    public int getCompletedSubtasks() {
        if (subtasks == null || subtasks.isEmpty()) return 0;
        int count = 0;
        for (Subtask s : subtasks) {
            if (s.isDone()) count++;
        }
        return count;
    }

    // --- GETTER & SETTER ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

    public List<Subtask> getSubtasks() {
        if (subtasks == null) subtasks = new ArrayList<>();
        return subtasks;
    }
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = (subtasks != null) ? subtasks : new ArrayList<>();
    }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    private boolean isPrivate; // true = Privat, false = Publik

    // Tambahkan di Constructor dan buat Getter/Setter-nya
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
}
