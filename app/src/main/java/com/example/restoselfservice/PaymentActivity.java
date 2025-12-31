package com.example.restoselfservice;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.example.restoselfservice.api.ApiHelper;
import com.example.restoselfservice.helper.CartHelper;
import com.example.restoselfservice.model.CartItem;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class PaymentActivity extends AppCompatActivity {
	// UI Pilihan
	private LinearLayout layoutSelection;
	private CardView btnPayCash, btnPayQris;

	// UI QRIS Detail
	private CardView cardQrisDetail;
	private ImageView imgQrisCode;
	private TextView tvPaymentAmount, tvTimer;
	private Button btnCheckStatus, btnCreateNewQr, btnCancelQris;

	private ApiHelper apiHelper;
	private CountDownTimer countDownTimer;
	private CountDownTimer pollingTimer;

	private DecimalFormat priceFormatter = new DecimalFormat("#,###");
	private double totalAmount;
	private String currentExternalId = "";
	private String currentIdQRIS = "";


	private static final String TAG = "PAYMENT_FLOW";
	private static final long POLLING_INTERVAL_MS = 5000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_payment);

		apiHelper = new ApiHelper(this);
		totalAmount = CartHelper.getInstance().getTotalPrice();

		initUI();
		setupClickListeners();
	}

	private void initUI() {
		// Bagian Selection
		layoutSelection = findViewById(R.id.layoutSelection);
		btnPayCash = findViewById(R.id.btnPayCash);
		btnPayQris = findViewById(R.id.btnPayQris);

		// Bagian Detail QRIS
		cardQrisDetail = findViewById(R.id.cardQrisDetail);
		imgQrisCode = findViewById(R.id.imgQrisCode);
		tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
		tvTimer = findViewById(R.id.tvTimer);
		btnCheckStatus = findViewById(R.id.btnCheckStatus);
		btnCreateNewQr = findViewById(R.id.btnCreateNewQr);
		btnCancelQris = findViewById(R.id.btnCancelQris);

		// Set total harga di awal
		tvPaymentAmount.setText("Rp " + priceFormatter.format(totalAmount));
	}

	private void setupClickListeners() {
		// 1. PILIH CASH
		btnPayCash.setOnClickListener(v -> {
			new AlertDialog.Builder(this)
				.setTitle("Konfirmasi Tunai")
				.setMessage("Silakan bayar di kasir setelah ini. Lanjutkan?")
				.setPositiveButton("Ya, Lanjut", (d, w) -> processCashPayment())
				.setNegativeButton("Batal", null)
				.show();
		});

		// 2. PILIH QRIS
		btnPayQris.setOnClickListener(v -> {
			// Sembunyiin menu pilihan, munculin QRIS
			layoutSelection.setVisibility(View.GONE);
			cardQrisDetail.setVisibility(View.VISIBLE);
			btnCheckStatus.setVisibility(View.VISIBLE);

			// Baru tembak API buat bikin QRIS
			createQrisCharge(totalAmount);
		});

		// 3. CEK STATUS MANUAL
		btnCheckStatus.setOnClickListener(v -> checkPaymentStatus(true));

		// 4. BUAT QR BARU (Kalo Expired)
		btnCreateNewQr.setOnClickListener(v -> {
			createQrisCharge(totalAmount);
			btnCreateNewQr.setVisibility(View.GONE);
			btnCheckStatus.setVisibility(View.VISIBLE);
		});

		btnCancelQris.setOnClickListener(v -> {
			// 1. Matiin semua timer biar nggak boros baterai & RAM
			cancelAllTimers();

			// 2. Balikin visibilitas (Tuker posisi)
			layoutSelection.setVisibility(View.VISIBLE); // Munculin pilihan Tunai/QRIS lagi
			cardQrisDetail.setVisibility(View.GONE);     // Sembunyiin detail QRIS
			btnCheckStatus.setVisibility(View.GONE);     // Sembunyiin tombol cek status

			// 3. Reset tampilan QR (opsional)
			imgQrisCode.setImageBitmap(null);
			imgQrisCode.setAlpha(1.0f);
		});
	}

	// ==========================================================
	// PENANGANAN CASH (TUNAI)
	// ==========================================================
	private void processCashPayment() {
		// Untuk Cash, kita buat External ID manual aja pake timestamp
		currentExternalId = "CASH-" + System.currentTimeMillis();
		String itemsJson = CartHelper.getInstance().getCartItemsAsJson();

		saveOrderToDatabase(currentExternalId, totalAmount, itemsJson, true);
	}

	// ==========================================================
	// PENANGANAN QRIS
	// ==========================================================
	private void createQrisCharge(double amount) {
		tvTimer.setText("Membuat Tagihan...");
		btnCheckStatus.setEnabled(false);

		apiHelper.postQrisCharge(amount, new ApiHelper.QrisChargeCallback() {
			@Override
			public void onSuccess(String qrisUrl, String externalId, long remainingSeconds, String id) {
				currentExternalId = externalId;
				currentIdQRIS = id;

				displayQris(qrisUrl);
				startExpirationTimer(remainingSeconds * 1000);
				startAutoPolling();
				btnCheckStatus.setEnabled(true);
			}

			@Override
			public void onError(String message) {
				Toast.makeText(PaymentActivity.this, "Gagal QRIS: " + message, Toast.LENGTH_LONG).show();
				layoutSelection.setVisibility(View.VISIBLE); // Balikin ke menu pilihan
				cardQrisDetail.setVisibility(View.GONE);
			}
		});
	}

	private void displayQris(String qrString) {
		try {
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
			Bitmap bitmap = barcodeEncoder.encodeBitmap(qrString, BarcodeFormat.QR_CODE, 600, 600);
			imgQrisCode.setImageBitmap(bitmap);
		} catch (Exception e) {
			Log.e("QR_ERROR", e.getMessage());
		}
	}

	private void startExpirationTimer(long durationMillis) {
		if (countDownTimer != null) countDownTimer.cancel();
		countDownTimer = new CountDownTimer(durationMillis, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				long min = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
				long sec = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
				tvTimer.setText(String.format("Sisa Waktu: %02d:%02d", min, sec));
			}
			@Override
			public void onFinish() { handleExpiredUI(); }
		}.start();
	}

	private void handleExpiredUI() {
		tvTimer.setText("EXPIRED");
		tvTimer.setTextColor(Color.RED);
		imgQrisCode.setAlpha(0.3f); // Bikin pudar biar jelas kalo expired
		btnCheckStatus.setVisibility(View.GONE);
		btnCreateNewQr.setVisibility(View.VISIBLE);
		if (pollingTimer != null) pollingTimer.cancel();
	}

	private void startAutoPolling() {
		if (pollingTimer != null) pollingTimer.cancel();
		pollingTimer = new CountDownTimer(Long.MAX_VALUE, POLLING_INTERVAL_MS) {
			@Override
			public void onTick(long millisUntilFinished) { checkPaymentStatus(false); }
			@Override public void onFinish() {}
		}.start();
	}

	private void checkPaymentStatus(boolean isManualCheck) {
		if (currentIdQRIS == null || currentIdQRIS.isEmpty()) return;

		// Kita pake satu Callback aja karena onError biasanya udah include di dalamnya
		apiHelper.checkPaymentStatus(currentIdQRIS, new ApiHelper.PaymentStatusCallback() {
			@Override
			public void onStatusCheck(String status) {
				switch (status) {
					case "PAID":
					case "SETTLED":
						handlePaymentSuccess();
						break;
					case "EXPIRED":
						handleExpiredUI();
						break;
					case "PENDING":
						if (isManualCheck) {
							Snackbar.make(findViewById(android.R.id.content), "Belum dibayar nih, Bro.", Snackbar.LENGTH_SHORT)
								.setBackgroundTint(Color.parseColor("#FFA500")).show();
						}
						break;
				}
			}

			@Override
			public void onError(String message) {
				// Ini method yang diminta sama error "must implement abstract method" tadi
				Log.e(TAG, "Polling Error: " + message);
			}
		});
	}

	private void handlePaymentSuccess() {
		cancelAllTimers();
		String itemsJson = CartHelper.getInstance().getCartItemsAsJson();
		saveOrderToDatabase(currentExternalId, totalAmount, itemsJson, false);
	}

	private void saveOrderToDatabase(String externalId, double total, String itemsJson, boolean isCash) {
		ProgressDialog pd = new ProgressDialog(this);
		pd.setMessage("Menyimpan pesanan...");
		pd.setCancelable(false);
		pd.show();

		apiHelper.savePesanan(externalId, total, itemsJson, (isCash ? "CASH" : "QRIS"), new ApiHelper.SimpleCallback() {
			@Override
			public void onSuccess() {
				pd.dismiss();

				if (isCash) {
					// KALO CASH: Munculin pop-up struk di sini
					showReceiptDialog();
				}else {
					Intent intent = new Intent(PaymentActivity.this, OrderStatusActivity.class);
					intent.putExtra("EXTERNAL_ID", externalId);
					startActivity(intent);
					finish();
				}
			}

			@Override
			public void onError(String message) {
				pd.dismiss();
				Toast.makeText(PaymentActivity.this, "Gagal simpan: " + message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void showReceiptDialog() {
		// 1. Inflate Layout
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

		// 3. Isi Data dari CartHelper
		StringBuilder sb = new StringBuilder();
		for (CartItem item : CartHelper.getInstance().getCartItems()) {
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
			receiptDialog.dismiss();
			CartHelper.getInstance().clearCart(); // Kosongin keranjang

			// Balik ke MainActivity biar bisa buat pesanan baru
			Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
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

	private void cancelAllTimers() {
		if (countDownTimer != null) countDownTimer.cancel();
		if (pollingTimer != null) pollingTimer.cancel();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancelAllTimers();
	}
}
