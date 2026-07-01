package id.sevliana.minimo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import id.sevliana.minimo.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // 1. Logika Tombol Login
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Logika Pindah ke Halaman Daftar
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // 3. FIX: Logika Tombol Lupa Sandi
        binding.tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    private void loginUser(String email, String password) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Memasuki dashboard...");
        pd.setCancelable(false);
        pd.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    pd.dismiss();

                    if (user != null) {
                        // Cek apakah user sudah verifikasi email
                        if (user.isEmailVerified()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            mAuth.signOut(); // Logout kembali karena belum verif
                            showNotVerifiedAlert();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Login Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Fitur Reset Password (Lupa Sandi)
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lupa Kata Sandi?");
        builder.setMessage("Masukkan email terdaftar untuk menerima link reset sandi.");

        // Buat input email di dalam dialog
        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("email@contoh.com");
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Memberikan margin/padding pada input di dalam dialog
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        inputEmail.setLayoutParams(lp);
        builder.setView(inputEmail);

        builder.setPositiveButton("Kirim", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Link reset dikirim ke email Anda", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Masukkan email Anda", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void showNotVerifiedAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Email Belum Terverifikasi")
                .setMessage("Silakan klik link verifikasi di Gmail (spam) Anda terlebih dahulu.")
                .setPositiveButton("Buka Gmail", (dialog, which) -> {
                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
                    if (intent != null) {
                        startActivity(intent);
                    } else {
                        // Jika tidak ada aplikasi Gmail, buka aplikasi email yang tersedia
                        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
                        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
                        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(emailIntent);
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }
}