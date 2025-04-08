package com.example.ratertune.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.models.Release;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ReleasesAdapter extends RecyclerView.Adapter<ReleasesAdapter.ReleaseViewHolder> {
    private List<Release> releases;
    private OnReleaseClickListener listener;

    public interface OnReleaseClickListener {
        void onReleaseClick(Release release);
    }

    public ReleasesAdapter(List<Release> releases, OnReleaseClickListener listener) {
        this.releases = releases;
        this.listener = listener;
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
        holder.bind(release, listener);
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

        void bind(final Release release, final OnReleaseClickListener listener) {
            titleTextView.setText(release.getTitle());
            artistTextView.setText(release.getArtist());
            
            // Показываем рейтинг, если он установлен
            float rating = release.getRating();
            ratingTextView.setText(String.format("%.1f", rating));

            // Загрузка изображения с помощью Picasso
            Picasso.get()
                    .load(release.getImageUrl())
                    .placeholder(R.drawable.ic_album_placeholder)
                    .error(R.drawable.ic_album_placeholder)
                    .into(imageView);
                    
            // Обработка клика на элемент
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReleaseClick(release);
                }
            });
        }
    }
} 