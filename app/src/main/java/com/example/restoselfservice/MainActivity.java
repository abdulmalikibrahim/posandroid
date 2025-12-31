package com.example.restoselfservice;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.restoselfservice.adapter.MenuAdapter;
import com.example.restoselfservice.api.ApiHelper;
import com.example.restoselfservice.helper.CartHelper;
import com.example.restoselfservice.model.CartItem;
import com.example.restoselfservice.model.Category;
import com.example.restoselfservice.model.Menu;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MenuAdapter.MenuClickListener {

	private androidx.cardview.widget.CardView cartFooter;
	private LinearLayout categoryContainer;
	private RecyclerView menuRecyclerView;
	private MenuAdapter menuAdapter;
	private ArrayList<Category> categories;
	private ApiHelper apiHelper;
	private TextView cartCount, cartTotal;
	private Button btnCheckout;
	private ProgressBar menuLoadingSpinner;
	private DecimalFormat priceFormatter = new DecimalFormat("#,###");
	private com.google.android.material.textfield.TextInputEditText etBarcode;
	private ArrayList<Menu> masterMenus = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_main);

		// --- 1. INIT UI ---
		etBarcode = findViewById(R.id.etBarcode);
		cartFooter = findViewById(R.id.cartFooter);
		categoryContainer = findViewById(R.id.categoryContainer);
		cartCount = findViewById(R.id.cartCount);
		cartTotal = findViewById(R.id.cartTotal);
		btnCheckout = findViewById(R.id.btnCheckout);
		menuLoadingSpinner = findViewById(R.id.menuLoading);
		ImageButton btnOpenSettings = findViewById(R.id.btnOpenSettings);
		SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);

		// --- 2. SETUP SCANNER & KEYBOARD ---
		setupBarcodeLogic();

		// --- 3. RECYCLERVIEW SETUP ---
		menuRecyclerView = findViewById(R.id.menuRecyclerView);
		menuRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
		menuRecyclerView.setNestedScrollingEnabled(false);
		menuAdapter = new MenuAdapter(new ArrayList<>(), this, this);
		menuRecyclerView.setAdapter(menuAdapter);

		// --- 4. API & DATA LOAD ---
		apiHelper = new ApiHelper(this);
		fetchCategoriesAndLoadFirstMenu();
		loadMasterDataForBarcode();

		// --- 5. EVENT LISTENERS ---
		swipeRefresh.setOnRefreshListener(() -> {
			fetchCategoriesAndLoadFirstMenu();
			swipeRefresh.setRefreshing(false);
		});

		btnOpenSettings.setOnClickListener(v -> showPinDialog());

		btnCheckout.setOnClickListener(v -> {
			if (CartHelper.getInstance().getTotalItems() > 0) {
				startActivity(new Intent(MainActivity.this, CartActivity.class));
			} else {
				Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show();
			}
		});

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				showExitDialog();
			}
		});
	}

	private void setupBarcodeLogic() {
		// Fokus otomatis ke input barcode
		etBarcode.requestFocus();

		// Listener buat nangkep "ENTER" dari Scanner Hardware
		etBarcode.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
				String code = etBarcode.getText().toString().trim();
				if (!code.isEmpty()) {
					tambahBarangKeKeranjang(code);
					etBarcode.setText(""); // Reset kolom
				}
				return true;
			}
			return false;
		});

		// Listener buat FILTER list barang pas kasir ngetik manual nama barang
		etBarcode.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				filterMenus(s.toString().trim());
			}
			@Override
			public void afterTextChanged(Editable s) {}
		});

		// Paksa keyboard muncul (opsional, buat user yang nggak pake scanner hardware)
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) imm.showSoftInput(etBarcode, InputMethodManager.SHOW_IMPLICIT);
		}, 800);
	}

	private void tambahBarangKeKeranjang(String barcode) {
		Menu found = null;
		for (Menu m : masterMenus) {
			if (m.getId().equalsIgnoreCase(barcode)) {
				found = m;
				break;
			}
		}

		if (found != null) {
			// Panggil onMenuAddedToCart biar logic cek stoknya jalan di satu pintu
			onMenuAddedToCart(found);
		} else {
			Toast.makeText(this, "Barang gak ada di database, Bro!", Toast.LENGTH_SHORT).show();
		}
	}

	private void filterMenus(String query) {
		if (query.isEmpty()) {
			// Kalau kosong, biarin tampil apa adanya (atau reload kategori aktif)
			return;
		}
		ArrayList<Menu> filtered = new ArrayList<>();
		for (Menu m : masterMenus) {
			if (m.getName().toLowerCase().contains(query.toLowerCase()) ||
				m.getId().equalsIgnoreCase(query)) {
				filtered.add(m);
			}
		}
		menuAdapter.updateData(filtered);
	}

	private void loadMasterDataForBarcode() {
		apiHelper.getMenus("1", new ApiHelper.ApiMenuCallback() {
			@Override
			public void onSuccess(ArrayList<Menu> menus) {
				masterMenus.clear();
				masterMenus.addAll(menus);
			}
			@Override
			public void onError(Exception e) {
				Log.e("API", "Gagal load master data barcode");
			}
		});
	}

	// --- LOGIC UI & API (Kategori, Footer, Dialog) ---

	private void fetchCategoriesAndLoadFirstMenu() {
		menuLoadingSpinner.setVisibility(View.VISIBLE);
		apiHelper.getCategories(new ApiHelper.ApiCallback() {
			@Override
			public void onSuccess(ArrayList<Category> fetchedCategories) {
				menuLoadingSpinner.setVisibility(View.GONE);
				categories = fetchedCategories;
				createCategoryButtons(categories);
				if (!categories.isEmpty()) fetchMenus(categories.get(0).getId());
			}
			@Override
			public void onError(Exception e) {
				menuLoadingSpinner.setVisibility(View.GONE);
				Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void fetchMenus(String categoryId) {
		menuLoadingSpinner.setVisibility(View.VISIBLE);
		apiHelper.getMenus(categoryId, new ApiHelper.ApiMenuCallback() {
			@Override
			public void onSuccess(ArrayList<Menu> menus) {
				menuLoadingSpinner.setVisibility(View.GONE);
				menuAdapter.updateData(menus);
			}
			@Override
			public void onError(Exception e) {
				menuLoadingSpinner.setVisibility(View.GONE);
			}
		});
	}

	@Override
	public void onMenuAddedToCart(Menu menu) {
		// Ambil stok dari model Menu (kita asumsikan datanya String dari API)
		int stockTersedia = Integer.parseInt(menu.getStock());

		// Cek di keranjang sekarang udah ada berapa biji barang ini
		int qtyDiKeranjang = 0;
		for (CartItem item : CartHelper.getInstance().getCartItems()) {
			if (item.getId().equals(menu.getId())) {
				qtyDiKeranjang = item.getQty();
				break;
			}
		}

		// VALIDASI: Kalau (keranjang + 1) ngelebihi stok, blokir!
		if (qtyDiKeranjang + 1 > stockTersedia) {
			Toast.makeText(this, "Waduh Bro, stok sisa " + stockTersedia + ". Gak bisa nambah lagi!", Toast.LENGTH_SHORT).show();
			return; // Stop di sini, jangan lanjut ke bawah
		}

		// Kalau aman, baru masukin ke Helper
		CartHelper.getInstance().addToCart(new CartItem(
			menu.getId(),
			menu.getName(),
			Double.parseDouble(menu.getPrice()),
			menu.getLinkImage()
		));

		updateFooterCart();
		menuAdapter.notifyDataSetChanged(); // Biar angka di badge berubah
	}

	private void updateFooterCart() {
		int items = CartHelper.getInstance().getTotalItems();
		double price = CartHelper.getInstance().getTotalPrice();
		cartCount.setText(items + " item");
		cartTotal.setText("Rp " + priceFormatter.format(price));

		if (items > 0) showFooter(); else hideFooter();
	}

	private void showFooter() {
		if (cartFooter.getVisibility() == View.VISIBLE) return;
		cartFooter.setVisibility(View.VISIBLE);
		TranslateAnimation slideUp = new TranslateAnimation(0, 0, 200, 0);
		slideUp.setDuration(300);
		cartFooter.startAnimation(slideUp);
	}

	private void hideFooter() {
		if (cartFooter.getVisibility() == View.GONE) return;
		TranslateAnimation slideDown = new TranslateAnimation(0, 0, 0, 200);
		slideDown.setDuration(300);
		cartFooter.startAnimation(slideDown);
		cartFooter.postDelayed(() -> cartFooter.setVisibility(View.GONE), 300);
	}

	private void createCategoryButtons(ArrayList<Category> categories) {
		categoryContainer.removeAllViews();
		for (int i = 0; i < categories.size(); i++) {
			Category cat = categories.get(i);
			TextView tv = new TextView(this);
			tv.setText(cat.getName());
			tv.setTextColor(Color.WHITE);
			tv.setBackgroundResource(R.drawable.button_category);
			tv.setPadding(40, 20, 40, 20);
			tv.setTypeface(null, Typeface.BOLD);

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
			lp.setMargins(0, 0, 16, 0);
			tv.setLayoutParams(lp);

			tv.setAlpha(i == 0 ? 1.0f : 0.6f);
			tv.setOnClickListener(v -> {
				for (int j = 0; j < categoryContainer.getChildCount(); j++) categoryContainer.getChildAt(j).setAlpha(0.6f);
				v.setAlpha(1.0f);
				fetchMenus(cat.getId());
				etBarcode.requestFocus(); // Balikin fokus ke barcode setiap ganti kategori
			});
			categoryContainer.addView(tv);
		}
	}

	private void showPinDialog() {
		final EditText etPin = new EditText(this);
		etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
		etPin.setHint("PIN Admin");
		etPin.setGravity(Gravity.CENTER);

		new AlertDialog.Builder(this)
			.setTitle("Admin Only")
			.setView(etPin)
			.setPositiveButton("Buka", (d, w) -> {
				String savedPin = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("app_pin", "123123");
				if (etPin.getText().toString().equals(savedPin)) {
					startActivity(new Intent(this, SettingsActivity.class));
				} else {
					Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show();
				}
			}).show();
	}

	private void showExitDialog() {
		View v = getLayoutInflater().inflate(R.layout.dialog_exit, null);
		AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
		v.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
		v.findViewById(R.id.btnExit).setOnClickListener(view -> finishAffinity());
		dialog.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateFooterCart();
		if (menuAdapter != null) menuAdapter.notifyDataSetChanged();
		etBarcode.requestFocus(); // Pastiin siap scan lagi pas balik ke sini
	}
}
