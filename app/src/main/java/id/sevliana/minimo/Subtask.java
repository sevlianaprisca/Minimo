package id.sevliana.minimo;

import java.io.Serializable;

public class Subtask implements Serializable {
    private String id;
    private String title;
    private boolean done = false;

    // Wajib ada untuk Firebase
    public Subtask() {}

    public Subtask(String title) {
        this.title = title;
        this.done = false;
    }

    // Constructor Lengkap
    public Subtask(String id, String title, boolean done) {
        this.id = id;
        this.title = title;
        this.done = done;
    }

    // Getter dan Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}