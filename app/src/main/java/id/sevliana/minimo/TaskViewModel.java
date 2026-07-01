package id.sevliana.minimo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TaskViewModel extends ViewModel {
    private final MutableLiveData<List<Task>> tasks = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getUid();

    private ListenerRegistration taskListener;
    private ListenerRegistration catListener;

    public TaskViewModel() {
        // Langsung muat data saat ViewModel diciptakan
        loadTasks();
        loadCategories();
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    private void loadTasks() {
        if (userId == null) userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Kita hapus listener lama jika ada untuk menghindari duplikasi
        if (taskListener != null) taskListener.remove();

        taskListener = db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Urutkan berdasarkan waktu buat
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    List<Task> taskList = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Task task = doc.toObject(Task.class);
                            task.setId(doc.getId());
                            taskList.add(task);
                        }
                    }
                    // Menggunakan postValue agar aman jika terjadi di background thread
                    tasks.postValue(taskList);
                });
    }

    private void loadCategories() {
        if (userId == null) userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        if (catListener != null) catListener.remove();

        catListener = db.collection("categories")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    List<Category> categoryList = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Category cat = doc.toObject(Category.class);
                            cat.setId(doc.getId());
                            categoryList.add(cat);
                        }
                    }
                    categories.postValue(categoryList);
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (taskListener != null) taskListener.remove();
        if (catListener != null) catListener.remove();
    }
}