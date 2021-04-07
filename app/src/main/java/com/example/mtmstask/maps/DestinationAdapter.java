package com.example.mtmstask.maps;

import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mtmstask.R;

import java.util.ArrayList;
import java.util.List;


public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.MyViewHolder> {

    private List<Address> items;
    onClickCallback onClickCallback;

    public DestinationAdapter(List<Address> items, onClickCallback onClickCallback) {
        this.items = items;
        this.onClickCallback=onClickCallback;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.single_item_view, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Address item = items.get(position);
        holder.name.setText(item.getFeatureName());

        holder.itemView.setOnClickListener(v ->{
            onClickCallback.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        public MyViewHolder(final View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.nameTv);
        }
    }

    public interface onClickCallback{
        void onClick(Address item);
    }
}
