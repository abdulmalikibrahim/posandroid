package com.example.restoselfservice.adapter; // Ubah packagenage-nya

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.restoselfservice.R;
import com.example.restoselfservice.model.CartItem;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
	private ArrayList<CartItem> items;
	private Context context;
	private OnCartChangeListener listener;

	public interface OnCartChangeListener {
		void onUpdate();
	}

	public CartAdapter(ArrayList<CartItem> items, Context context, OnCartChangeListener listener) {
		this.items = items;
		this.context = context;
		this.listener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		CartItem item = items.get(position);
		holder.tvName.setText(item.getName());
		holder.tvPrice.setText("Rp " + new DecimalFormat("#,###").format(item.getPrice()));
		holder.tvQty.setText(String.valueOf(item.getQty()));

		// Tombol Kurang Qty
		holder.itemView.findViewById(R.id.btnMinus).setOnClickListener(v -> {
			if (item.getQty() > 1) {
				item.setQty(item.getQty() - 1);
				notifyItemChanged(position);
				if (listener != null) listener.onUpdate();
			}
		});

		// Tombol Tambah Qty
		holder.itemView.findViewById(R.id.btnPlus).setOnClickListener(v -> {
			item.setQty(item.getQty() + 1);
			notifyItemChanged(position);
			if (listener != null) listener.onUpdate();
		});

		// Tombol Hapus (Icon Tong Sampah)
		holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
			items.remove(position);
			notifyItemRemoved(position);
			notifyItemRangeChanged(position, items.size());
			if (listener != null) listener.onUpdate();
		});
	}

	@Override
	public int getItemCount() { return items.size(); }

	public static class ViewHolder extends RecyclerView.ViewHolder {
		TextView tvName, tvPrice, tvQty;
		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			tvName = itemView.findViewById(R.id.tvCartItemName);
			tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
			tvQty = itemView.findViewById(R.id.tvQty);
		}
	}
}
