package id.sevliana.minimo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import id.sevliana.minimo.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(name, email, password);
            }
        });

        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser(String name, String email, String password) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menciptakan akun MIGO...");
        pd.setCancelable(false);
        pd.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        // 1. Simpan Nama Lengkap ke Profil
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                            // 2. Kirim Link Verifikasi
                            user.sendEmailVerification().addOnSuccessListener(aVoid -> {
                                pd.dismiss();
                                // 3. Logout Otomatis (Agar user wajib login manual nanti)
                                mAuth.signOut();
                                showVerificationDialog(email);
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showVerificationDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verifikasi Email Dikirim");
        builder.setMessage("Halo! Kami telah mengirimkan link verifikasi ke: " + email + "\n\nSilakan klik link tersebut di aplikasi Gmail Anda, lalu kembali ke aplikasi MIGO untuk masuk.");
        builder.setCancelable(false);

        builder.setPositiveButton("Buka Gmail", (dialog, which) -> {
            // Coba buka aplikasi Gmail secara spesifik
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");

            if (intent != null) {
                // Jika aplikasi Gmail ditemukan, langsung buka
                startActivity(intent);
            } else {
                // JIKA GMAIL TIDAK ADA (User mungkin pakai Outlook/Yahoo/dll)
                // Maka buka aplikasi email general yang ada di HP
                try {
                    Intent intentAllEmail = new Intent(Intent.ACTION_MAIN);
                    intentAllEmail.addCategory(Intent.CATEGORY_APP_EMAIL);
                    intentAllEmail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentAllEmail);
                } catch (Exception e) {
                    // Jika benar-benar tidak ada aplikasi email sama sekali
                    Toast.makeText(this, "Tidak dapat menemukan aplikasi Gmail atau Email", Toast.LENGTH_SHORT).show();
                }
            }
            finish(); // Tetap tutup halaman daftar agar kembali ke Login
        });

        builder.setNegativeButton("Nanti Saja", (dialog, which) -> {
            finish(); // Kembali ke halaman Login
        });

        builder.create().show();
    }
}