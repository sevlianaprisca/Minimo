package id.sevliana.minimo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Menghilangkan Toolbar di Splash Screen
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Delay 2 detik
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Cek apakah user sudah login atau belum
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish(); // Tutup SplashActivity agar tidak bisa di-back
        }, 2000);
    }
}