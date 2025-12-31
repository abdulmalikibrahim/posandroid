package com.example.restoselfservice;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.example.restoselfservice.model.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.restoselfservice.api.ApiHelper; // Pastiin import ini
import com.example.restoselfservice.helper.CartHelper;

import java.util.ArrayList;
import java.text.DecimalFormat;
import androidx.appcompat.app.AlertDialog;

public class OrderStatusActivity extends AppCompatActivity {
	// 1. Deklarasiin semua view yang ada di XML (PASTIIN SEMUA ADA DI SINI)
	private TextView tvStatusTitle, tvStatusDesc;
	private ImageView imgStatus;
	private ToneGenerator toneGen;
	private DecimalFormat priceFormatter = new DecimalFormat("#,###");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_order_status);

		// 2. Hubungin variabel Java sama ID di XML (Pake ID yang baru ya!)
		imgStatus = findViewById(R.id.imgStatus);
		tvStatusTitle = findViewById(R.id.tvStatusTitle);
		tvStatusDesc = findViewById(R.id.tvStatusDesc);
		toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

		// HAPUS bagian imgStatusIcon yang tadi error, ganti pake ini:
		Glide.with(this).asGif().load(R.drawable.ic_processing).into(imgStatus);

		updateToChecking();
	}

	private void updateToChecking() {
		// Pake imgStatus aja biar konsisten
		Glide.with(this).asGif().load(R.drawable.check_payment).into(imgStatus);
		tvStatusTitle.setText("Menghubungi Server...");
		tvStatusDesc.setText("Sabar ya, lagi ngecek status pembayaran lo.");

		new Handler().postDelayed(this::updateToSuccess, 2000);
	}

	private void updateToSuccess() {
		Glide.with(this).asGif().load(R.drawable.success).into(imgStatus);
		tvStatusTitle.setText("Pembayaran Berhasil.");
		tvStatusDesc.setText("Pesanan lo udah masuk sistem!");

		// --- DISINI KITA MUNCULIN STRUKNYA ---
		showReceiptDialog();
	}

	private void showReceiptDialog() {
		// 1. Inflate Layout (Pake layout yang sama dengan PaymentActivity tadi)
		View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_struk, null);
		AlertDialog receiptDialog = new AlertDialog.Builder(this)
			.setView(dialogView)
			.setCancelable(false)
			.create();

		// 2. Hubungkan View
		TextView tvItems = dialogView.findViewById(R.id.tvStrukItems);
		TextView tvTotal = dialogView.findViewById(R.id.tvStrukTotal);
		TextView btnPrint = dialogView.findViewById(R.id.btnPrintStruk);
		Button btnDone = dialogView.findViewById(R.id.btnDoneStruk);

		// Update teks Metode Pembayaran biar user nggak bingung
		TextView tvMetode = dialogView.findViewById(R.id.tvMetode); // Pastiin ada ID ini di XML struk lo
		if(tvMetode != null) tvMetode.setText("Metode: QRIS (PAID)");

		// 3. Isi Data dari CartHelper
		StringBuilder sb = new StringBuilder();
		// Kita ambil data terakhir dari CartHelper sebelum di-clear
		for (com.example.restoselfservice.model.CartItem item : CartHelper.getInstance().getCartItems()) {
			sb.append(item.getQty()).append("x ")
				.append(item.getName()).append("\n");
		}
		tvItems.setText(sb.toString());
		tvTotal.setText("Rp " + priceFormatter.format(CartHelper.getInstance().getTotalPrice()));

		// 4. Action Tombol
		btnPrint.setOnClickListener(v -> {
			// DISINI TEMPATNYA!
			String printerAddr = getSharedPreferences("PRINTER_SETTINGS", MODE_PRIVATE)
				.getString("printer_address", null);

			if (printerAddr != null) {
				Toast.makeText(this, "Mengirim data ke printer...", Toast.LENGTH_SHORT).show();
				new Thread(() -> {
					// Logika ESC/POS lo di sini
					// printStrukBluetooth(printerAddr);
				}).start();
			} else {
				Toast.makeText(this, "Setting printer dulu di menu Setting, Bro!", Toast.LENGTH_SHORT).show();
			}
		});

		btnDone.setOnClickListener(v -> {
			// 1. Tutup dialog struknya
			receiptDialog.dismiss();

			// 2. Kosongin keranjang belanjaan
			CartHelper.getInstance().clearCart();

			// 3. Paksa balik ke halaman utama dan hapus semua history layar sebelumnya
			Intent intent = new Intent(OrderStatusActivity.this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);

			// 4. Tutup halaman status ini
			finish();
		});

		if (receiptDialog.getWindow() != null) {
			receiptDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		receiptDialog.show();
		// Tambahin ini SETELAH show()
		if (receiptDialog.getWindow() != null) {
			receiptDialog.getWindow().setLayout(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (toneGen != null) {
			toneGen.release();
		}
	}
}
