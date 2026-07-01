package id.sevliana.minimo;

public class Category {
    private String id;
    private String name;
    private String colorHex;
    private String userId;

    // --- TAMBAHKAN INI (Constructor Kosong) ---
    // Tanpa ini, Firebase akan menyebabkan aplikasi CRASH saat load data
    public Category() {
    }
    // ------------------------------------------

    public Category(String name, String colorHex, String userId) {
        this.name = name;
        this.colorHex = colorHex;
        this.userId = userId;
    }

    // Getter Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}