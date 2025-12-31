package com.example.restoselfservice;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView; // Untuk tampilan UI yang lebih baik

import com.example.restoselfservice.helper.CartHelper;
import com.example.restoselfservice.model.CartItem;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.example.restoselfservice.adapter.CartAdapter;

public class CartActivity extends AppCompatActivity {

	private androidx.recyclerview.widget.RecyclerView cartRecyclerView;
	private TextView tvTotal;
	private Button btnCheckout;
	private DecimalFormat priceFormatter = new DecimalFormat("#,###");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cart); // Asumsi layout sudah ada

		cartRecyclerView = findViewById(R.id.cartRecyclerView);
		cartRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
		tvTotal = findViewById(R.id.tvTotal);
		btnCheckout = findViewById(R.id.btnCheckout);

		ImageButton btnBack = findViewById(R.id.btnBack);
		btnBack.setOnClickListener(v -> finish());

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				finish();
			}
		});

		loadCart();

		btnCheckout.setOnClickListener(v -> {
			if (CartHelper.getInstance().getCartItems().isEmpty()) {
				Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show();
				return;
			}

			// 💡 Perubahan: Pindah ke PaymentActivity
			Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
			// Lo bisa kirim total harga jika diperlukan, tapi CartHelper sudah bisa menghitungnya
			startActivity(intent);
		});

		Button btnCancelOrder = findViewById(R.id.btnCancelOrder);
		btnCancelOrder.setOnClickListener(v -> showCancelConfirmation());
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadCart(); // Pastikan keranjang di-reload jika kembali dari activity lain
	}

	private void showCancelConfirmation() {
		new AlertDialog.Builder(CartActivity.this)
			.setTitle("Konfirmasi Cancel Order")
			.setMessage("Apakah kamu yakin ingin membatalkan semua order?")
			.setPositiveButton("Ya", (dialog, which) -> {
				CartHelper.getInstance().clearCart();
				Toast.makeText(CartActivity.this, "Order dibatalkan", Toast.LENGTH_SHORT).show();

				// Kembali ke menu utama (MainActivity)
				Intent intent = new Intent(CartActivity.this, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			})
			.setNegativeButton("Tidak", null)
			.show();
	}

	private void loadCart() {
		ArrayList<CartItem> items = CartHelper.getInstance().getCartItems();
		double total = 0;

		if (items.isEmpty()) {
			// Tampilkan teks kosong (Bisa pake View khusus di XML biar lebih estetik)
			cartRecyclerView.setVisibility(View.GONE);
			btnCheckout.setEnabled(false);
			btnCheckout.setAlpha(0.5f); // Biar keliatan disable
		} else {
			cartRecyclerView.setVisibility(View.VISIBLE);
			btnCheckout.setEnabled(true);
			btnCheckout.setAlpha(1.0f);

			// Kasih data ke adapter
			CartAdapter adapter = new CartAdapter(items, this, () -> {
				// Ini dipanggil kalau ada perubahan Qty (Update Total)
				updateTotal();
			});
			cartRecyclerView.setAdapter(adapter);

			// Hitung Total
			for (CartItem item : items) {
				total += item.getPrice() * item.getQty();
			}
		}

		tvTotal.setText("Total: Rp " + priceFormatter.format(total));
	}

	// Tambahin method ini buat refresh total doang
	private void updateTotal() {
		double total = 0;
		for (CartItem item : CartHelper.getInstance().getCartItems()) {
			total += item.getPrice() * item.getQty();
		}
		tvTotal.setText("Total: Rp " + priceFormatter.format(total));
	}

	private View createCartItemRow(CartItem item) {
		// --- Struktur CardView & Layouts (Menggunakan UI yang lebih menarik) ---
		CardView cardView = new CardView(this);
		LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		);
		cardParams.setMargins(0, 8, 0, 8);
		cardView.setLayoutParams(cardParams);
		cardView.setRadius(10f);
		cardView.setElevation(4f);
		cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));

		LinearLayout mainRow = new LinearLayout(this);
		mainRow.setOrientation(LinearLayout.VERTICAL);
		mainRow.setPadding(16, 16, 16, 16);

		// Bagian 1: Nama Menu dan Subtotal
		LinearLayout namePriceLayout = new LinearLayout(this);
		namePriceLayout.setOrientation(LinearLayout.HORIZONTAL);

		TextView tvName = new TextView(this);
		tvName.setText(item.getName());
		tvName.setTextSize(16f);
		tvName.setTextColor(getResources().getColor(android.R.color.black));
		tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvSubtotal = new TextView(this);
		tvSubtotal.setId(View.generateViewId()); // ID unik untuk update
		double subtotal = item.getPrice() * item.getQty();
		tvSubtotal.setText("Rp " + priceFormatter.format(subtotal));
		tvSubtotal.setTextSize(16f);
		tvSubtotal.setTextColor(getResources().getColor(R.color.colorPrimary));
		tvSubtotal.setGravity(Gravity.END);

		namePriceLayout.addView(tvName);
		namePriceLayout.addView(tvSubtotal);

		// Bagian 2: Qty Kontrol dan Tombol Hapus
		LinearLayout controlsLayout = new LinearLayout(this);
		controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
		controlsLayout.setGravity(Gravity.CENTER_VERTICAL);
		controlsLayout.setPadding(0, 10, 0, 0);

		ImageButton btnDelete = new ImageButton(this);
		btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
		btnDelete.setBackground(null);
		btnDelete.setPadding(0, 0, 16, 0);
		btnDelete.setOnClickListener(v -> showDeleteConfirmation(item));

		LinearLayout qtyController = new LinearLayout(this);
		qtyController.setOrientation(LinearLayout.HORIZONTAL);
		qtyController.setGravity(Gravity.CENTER_VERTICAL);
		qtyController.setBackgroundResource(R.drawable.rounded_border);

		Button btnMinus = new Button(this);
		btnMinus.setText("-");
		setupQtyButton(btnMinus);

		TextView tvQty = new TextView(this);
		tvQty.setId(View.generateViewId());
		tvQty.setText(String.valueOf(item.getQty()));
		tvQty.setGravity(Gravity.CENTER);
		tvQty.setPadding(10, 5, 10, 5);
		tvQty.setTextSize(16f);
		tvQty.setTextColor(getResources().getColor(android.R.color.black));

		Button btnPlus = new Button(this);
		btnPlus.setText("+");
		setupQtyButton(btnPlus);

		qtyController.addView(btnMinus);
		qtyController.addView(tvQty);
		qtyController.addView(btnPlus);

		View spacer = new View(this);
		spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));

		controlsLayout.addView(btnDelete);
		controlsLayout.addView(spacer);
		controlsLayout.addView(qtyController);

		mainRow.addView(namePriceLayout);
		mainRow.addView(controlsLayout);
		cardView.addView(mainRow);

		// Set listener untuk tombol + dan -
		btnPlus.setOnClickListener(v -> updateItemQty(item, 1, tvQty, tvSubtotal));
		btnMinus.setOnClickListener(v -> updateItemQty(item, -1, tvQty, tvSubtotal));

		return cardView;
	}

	private void setupQtyButton(Button button) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, 50);
		params.gravity = Gravity.CENTER_VERTICAL;
		button.setLayoutParams(params);
		button.setPadding(0, 0, 0, 0);
		button.setTextSize(16f);
		button.setBackgroundResource(R.drawable.rounded_button_qty);
		button.setTextColor(getResources().getColor(android.R.color.black));
	}

	private void showDeleteConfirmation(CartItem item) {
		new AlertDialog.Builder(this)
			.setTitle("Hapus Item")
			.setMessage("Yakin ingin menghapus " + item.getName() + " dari keranjang?")
			.setPositiveButton("Hapus", (dialog, which) -> {
				CartHelper.getInstance().removeItem(item);
				Toast.makeText(this, item.getName() + " dihapus.", Toast.LENGTH_SHORT).show();
				loadCart(); // Reload keranjang
			})
			.setNegativeButton("Batal", null)
			.show();
	}

	private void updateItemQty(CartItem item, int delta, TextView tvQty, TextView tvSubtotal) {
		int newQty = item.getQty() + delta;

		if (newQty <= 0) {
			showDeleteConfirmation(item);
			return;
		}

		CartHelper.getInstance().updateItemQty(item, newQty);

		// Update UI Qty dan Subtotal item ini saja
		tvQty.setText(String.valueOf(newQty));
		double newSubtotal = item.getPrice() * newQty;
		tvSubtotal.setText("Rp " + priceFormatter.format(newSubtotal));

		// Update Total Harga Keranjang di footer
		updateTotalDisplay();
	}

	private void updateTotalDisplay() {
		double total = 0;
		for (CartItem item : CartHelper.getInstance().getCartItems()) {
			total += item.getPrice() * item.getQty();
		}
		tvTotal.setText("Total: Rp " + priceFormatter.format(total));

		boolean isEmpty = CartHelper.getInstance().getCartItems().isEmpty();
		btnCheckout.setEnabled(!isEmpty);
		if (isEmpty) {
			loadCart(); // Reload jika jadi kosong
		}
	}
}
