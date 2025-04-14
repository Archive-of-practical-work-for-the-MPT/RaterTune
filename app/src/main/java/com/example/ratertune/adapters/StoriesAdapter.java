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

        StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.storyImage);
        }

        void bind(final Story story, final OnStoryClickListener listener) {
            // Загрузка изображения с помощью кэшированного Picasso
            picasso.load(story.getImageUrl())
                    .noFade() // Отключаем анимацию загрузки
                    .noPlaceholder() // Не показываем placeholder
                    .into(imageView);
            
            // Обработка клика на элемент
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(story);
                }
            });
        }
    }
} 