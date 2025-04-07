package com.example.ratertune.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.model.Review;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        
        // Загрузка аватара пользователя
        Picasso.get()
                .load(review.getUserAvatarUrl())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(holder.userAvatar);
        
        holder.userName.setText(review.getUserName());
        holder.reviewDate.setText(review.getDate());
        holder.reviewTitle.setText(review.getTitle());
        holder.reviewText.setText(review.getText());
        holder.reviewRating.setText(String.format("%.1f", review.getRating()));
        holder.releaseInfo.setText(String.format("%s - %s", 
                review.getReleaseArtist(), review.getReleaseTitle()));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView userName;
        TextView reviewDate;
        TextView reviewTitle;
        TextView reviewText;
        TextView reviewRating;
        TextView releaseInfo;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            reviewDate = itemView.findViewById(R.id.reviewDate);
            reviewTitle = itemView.findViewById(R.id.reviewTitle);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewRating = itemView.findViewById(R.id.reviewRating);
            releaseInfo = itemView.findViewById(R.id.releaseInfo);
        }
    }
} 