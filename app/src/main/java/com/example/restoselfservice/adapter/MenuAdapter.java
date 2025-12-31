package com.example.restoselfservice.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.restoselfservice.R;
import com.example.restoselfservice.helper.CartHelper;
import com.example.restoselfservice.model.CartItem;
import com.example.restoselfservice.model.Menu;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

	private ArrayList<Menu> menuList;
	private Context context;
	private MenuClickListener listener;
	private DecimalFormat formatter = new DecimalFormat("#,###");

	public interface MenuClickListener {
		void onMenuAddedToCart(Menu menu);
	}

	public MenuAdapter(ArrayList<Menu> menuList, Context context, MenuClickListener listener) {
		this.menuList = menuList;
		this.context = context;
		this.listener = listener;
	}

	public void updateData(ArrayList<Menu> newList) {
		this.menuList = newList;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Pastikan inflate layout item_menu yang baru (tanpa button)
		View v = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
		return new MenuViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
		Menu menu = menuList.get(position);

		holder.tvName.setText(menu.getName());
		holder.tvPrice.setText("Rp " + formatter.format(Double.parseDouble(menu.getPrice())));
		holder.tvId.setText("#" + menu.getId());

		// Load Gambar pake Glide
		Glide.with(context)
			.load(menu.getLinkImage())
			.placeholder(R.drawable.ic_barcode_scanner) // Contoh placeholder
			.into(holder.imgMenu);

		// --- LOGIC BADGE QTY ---
		// Cek apakah item ini ada di keranjang buat munculin angka di pojok kartu
		int currentQty = 0;
		for (CartItem ci : CartHelper.getInstance().getCartItems()) {
			if (ci.getId().equals(menu.getId())) {
				currentQty = ci.getQty();
				break;
			}
		}

		if (currentQty > 0) {
			holder.tvBadgeQty.setVisibility(View.VISIBLE);
			holder.tvBadgeQty.setText(String.valueOf(currentQty));
		} else {
			holder.tvBadgeQty.setVisibility(View.GONE);
		}

		int stock = Integer.parseInt(menu.getStock());
		if (stock <= 0) {
			holder.itemView.setAlpha(0.5f); // Bikin agak buram
			// holder.tvStockStatus.setText("Habis"); // Kalau lo ada TextView status stok
			holder.itemView.setOnClickListener(null); // Matiin kliknya
		} else {
			holder.itemView.setAlpha(1.0f);
			holder.itemView.setOnClickListener(v -> listener.onMenuAddedToCart(menu));
		}
	}

	@Override
	public int getItemCount() {
		return (menuList != null) ? menuList.size() : 0;
	}

	// DI SINI PERUBAHANNYA: Hapus kata 'static' agar bisa akses listener & menuList
	public class MenuViewHolder extends RecyclerView.ViewHolder {
		CardView cardMenuItem;
		ImageView imgMenu;
		TextView tvName, tvPrice, tvBadgeQty, tvId;

		public MenuViewHolder(@NonNull View itemView) {
			super(itemView);
			cardMenuItem = itemView.findViewById(R.id.cardMenuItem);
			imgMenu = itemView.findViewById(R.id.imgMenu);
			tvName = itemView.findViewById(R.id.tvMenuName);
			tvPrice = itemView.findViewById(R.id.tvMenuPrice);
			tvBadgeQty = itemView.findViewById(R.id.tvBadgeQty);
			tvId = itemView.findViewById(R.id.tvMenuId);

			// Klik pada seluruh area item (CardView)
			itemView.setOnClickListener(v -> {
				int pos = getAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && listener != null) {
					// Langsung panggil listener untuk tambah ke keranjang
					listener.onMenuAddedToCart(menuList.get(pos));

					// Refresh item ini biar angkanya langsung update
					notifyItemChanged(pos);
				}
			});
		}
	}
}
