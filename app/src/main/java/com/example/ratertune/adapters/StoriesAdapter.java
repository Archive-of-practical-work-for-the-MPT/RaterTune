package com.example.ratertune.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.models.Story;
import com.example.ratertune.utils.PicassoCache;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {
    private final List<Story> stories;
    private final OnStoryClickListener listener;
    private final Picasso picasso;

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public StoriesAdapter(List<Story> stories, OnStoryClickListener listener, Context context) {
        this.stories = stories;
        this.listener = listener;
        this.picasso = PicassoCache.getInstance(context);
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = stories.get(position);
        // Предварительно загружаем следующее изображение
        if (position + 1 < stories.size()) {
            PicassoCache.preloadImage(holder.itemView.getContext(), stories.get(position + 1).getImageUrl());
        }
        holder.bind(story, listener);
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        MaterialCardView cardView;

        StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imageView = itemView.findViewById(R.id.storyImage);
        }

        void bind(final Story story, final OnStoryClickListener listener) {
            // Очищаем изображение перед новой загрузкой, чтобы избежать смешивания
            imageView.setImageDrawable(null);
            
            // Загрузка изображения с помощью кэшированного Picasso
            picasso.load(story.getImageUrl())
                    .noFade() // Отключаем анимацию загрузки
                    .noPlaceholder() // Не показываем placeholder
                    .tag(story.getId()) // Добавляем тег для уникальной идентификации
                    .into(imageView);
            
            // Устанавливаем состояние просмотра
            cardView.setSelected(story.isViewed());
            
            // Обработка клика на элемент
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(story);
                }
            });
        }
    }
    
    /**
     * Обновляет статус просмотра для сториза
     * @param storyId ID сториза
     * @param isViewed статус просмотра
     */
    public void updateStoryViewedStatus(String storyId, boolean isViewed) {
        for (int i = 0; i < stories.size(); i++) {
            Story story = stories.get(i);
            if (story.getId().equals(storyId)) {
                story.setViewed(isViewed);
                notifyItemChanged(i);
                break;
            }
        }
    }
} 