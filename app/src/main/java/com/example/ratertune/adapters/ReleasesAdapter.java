package com.example.ratertune.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.models.Release;
import com.example.ratertune.utils.PicassoCache;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ReleasesAdapter extends RecyclerView.Adapter<ReleasesAdapter.ReleaseViewHolder> {
    private final List<Release> releases;
    private final OnReleaseClickListener listener;
    private final Context context;

    public interface OnReleaseClickListener {
        void onReleaseClick(Release release);
    }

    public ReleasesAdapter(List<Release> releases, OnReleaseClickListener listener, Context context) {
        this.releases = releases;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ReleaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_release, parent, false);
        return new ReleaseViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseViewHolder holder, int position) {
        Release release = releases.get(position);
        // Предварительно загружаем следующее изображение
        if (position + 1 < releases.size()) {
            PicassoCache.preloadImage(holder.itemView.getContext(), releases.get(position + 1).getImageUrl());
        }
        holder.bind(release, listener);
    }

    @Override
    public int getItemCount() {
        return releases.size();
    }

    static class ReleaseViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView artistView;
        private final TextView ratingView;
        private final Picasso picasso;

        ReleaseViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            imageView = itemView.findViewById(R.id.releaseImage);
            titleView = itemView.findViewById(R.id.releaseTitle);
            artistView = itemView.findViewById(R.id.releaseArtist);
            ratingView = itemView.findViewById(R.id.releaseRating);
            picasso = PicassoCache.getInstance(context);
        }

        void bind(final Release release, final OnReleaseClickListener listener) {
            // Очищаем изображение перед новой загрузкой, чтобы избежать смешивания
            imageView.setImageDrawable(null);
            
            // Загрузка изображения с помощью кэшированного Picasso
            picasso.load(release.getImageUrl())
                    .noFade() // Отключаем анимацию загрузки
                    .noPlaceholder() // Не показываем placeholder
                    .tag(release.getId()) // Добавляем тег для уникальной идентификации
                    .into(imageView);
            
            titleView.setText(release.getTitle());
            artistView.setText(release.getArtist());
            
            // Отображение рейтинга всегда, даже если он равен нулю
            String rating = String.format("%.1f", release.getRating());
            ratingView.setText(rating);
            ratingView.setVisibility(View.VISIBLE);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReleaseClick(release);
                }
            });
        }
    }
} 