package com.example.habiaral.Adapters;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.habiaral.R;

public class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideViewHolder> {

    private final String[] slideUrls;
    private final Context context;

    public SlideAdapter(Context context, String[] slideUrls) {
        this.context = context;
        this.slideUrls = slideUrls;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.slide_item, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        Glide.with(context)
                .load(slideUrls[position])
                .into(holder.slideImage);
    }

    @Override
    public int getItemCount() {
        return slideUrls.length;
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView slideImage;
        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            slideImage = itemView.findViewById(R.id.slideImage);
        }
    }
}
