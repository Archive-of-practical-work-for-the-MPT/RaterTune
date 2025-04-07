package com.example.ratertune.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.model.Release;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ReleasesPagerAdapter extends RecyclerView.Adapter<ReleasesPagerAdapter.ReleaseViewHolder> {
    private List<Release> releases;

    public ReleasesPagerAdapter(List<Release> releases) {
        this.releases = releases;
    }

    @NonNull
    @Override
    public ReleaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_release, parent, false);
        return new ReleaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseViewHolder holder, int position) {
        Release release = releases.get(position);
        holder.titleTextView.setText(release.getTitle());
        holder.artistTextView.setText(release.getArtist());
        holder.ratingTextView.setText(String.format("%.1f", release.getRating()));

        // Загрузка изображения с помощью Picasso
        Picasso.get()
                .load(release.getImageUrl())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return releases.size();
    }

    static class ReleaseViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView artistTextView;
        TextView ratingTextView;

        ReleaseViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.releaseImage);
            titleTextView = itemView.findViewById(R.id.releaseTitle);
            artistTextView = itemView.findViewById(R.id.releaseArtist);
            ratingTextView = itemView.findViewById(R.id.releaseRating);
        }
    }
} 