package com.example.ratertune.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.models.PopularUser;
import com.example.ratertune.utils.PicassoCache;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class PopularUsersAdapter extends RecyclerView.Adapter<PopularUsersAdapter.ViewHolder> {
    
    private final List<PopularUser> users;
    private final Context context;
    private final OnUserClickListener listener;
    private final Picasso picasso;
    
    public interface OnUserClickListener {
        void onUserClick(PopularUser user);
    }
    
    public PopularUsersAdapter(List<PopularUser> users, Context context, OnUserClickListener listener) {
        this.users = users;
        this.context = context;
        this.listener = listener;
        this.picasso = PicassoCache.getInstance(context);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_popular_user, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopularUser user = users.get(position);
        
        // Устанавливаем имя пользователя
        holder.userName.setText(user.getUserName());
        
        // Устанавливаем аватар пользователя
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            picasso.load(user.getAvatarUrl())
                   .placeholder(R.drawable.ic_profile)
                   .error(R.drawable.ic_profile)
                   .into(holder.userAvatar);
        } else {
            holder.userAvatar.setImageResource(R.drawable.ic_profile);
        }
        
        // Устанавливаем номер позиции
        int displayPosition = position + 1; // позиции начинаются с 1
        holder.positionBadge.setText(String.valueOf(displayPosition));
        
        // Настройка цветов для разных позиций
        if (displayPosition == 1) {
            holder.positionBadge.setBackgroundResource(R.drawable.circle_background);
        } else if (displayPosition == 2) {
            holder.positionBadge.setBackgroundResource(R.drawable.silver_circle);
        } else if (displayPosition == 3) {
            holder.positionBadge.setBackgroundResource(R.drawable.bronze_circle);
        } else {
            holder.positionBadge.setBackgroundResource(R.drawable.regular_circle);
        }
        
        // Устанавливаем количество лайков и рецензий
        holder.likesCount.setText(String.valueOf(user.getLikesCount()));
        holder.reviewsCount.setText(String.valueOf(user.getReviewsCount()));
        
        // Устанавливаем обработчик нажатия
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView userAvatar;
        final TextView userName;
        final TextView positionBadge;
        final TextView likesCount;
        final TextView reviewsCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            positionBadge = itemView.findViewById(R.id.positionBadge);
            likesCount = itemView.findViewById(R.id.likesCount);
            reviewsCount = itemView.findViewById(R.id.reviewsCount);
        }
    }
}
