package com.example.ratertune.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.models.Review;
import com.example.ratertune.utils.PicassoCache;
import com.example.ratertune.utils.SessionManager;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.Locale;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private static final String TAG = "ReviewsAdapter";
    private List<Review> reviews;
    private OnReviewClickListener listener;
    private OnReviewLikeListener likeListener;

    public interface OnReviewClickListener {
        void onReviewClick(Review review);
    }

    public interface OnReviewLikeListener {
        void onReviewLike(Review review, boolean liked);
    }

    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public ReviewsAdapter(List<Review> reviews, OnReviewClickListener listener) {
        this.reviews = reviews;
        this.listener = listener;
    }

    public ReviewsAdapter(List<Review> reviews, OnReviewClickListener listener, OnReviewLikeListener likeListener) {
        this.reviews = reviews;
        this.listener = listener;
        this.likeListener = likeListener;
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
        holder.bind(review, listener, likeListener);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    // Метод для обновления счетчика лайков и статуса
    public void updateLikeStatus(long reviewId, boolean isLiked, int likesCount) {
        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            if (review.getId() == reviewId) {
                review.setLikedByUser(isLiked);
                review.setLikesCount(likesCount);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView usernameText;
        TextView ratingText;
        TextView reviewText;
        TextView dateText;
        TextView releaseNameText;
        TextView likeCountText;
        ImageButton likeButton;
        SupabaseClient supabaseClient;
        SessionManager sessionManager;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.reviewAvatar);
            usernameText = itemView.findViewById(R.id.reviewUsername);
            ratingText = itemView.findViewById(R.id.reviewRating);
            reviewText = itemView.findViewById(R.id.reviewText);
            dateText = itemView.findViewById(R.id.reviewDate);
            releaseNameText = itemView.findViewById(R.id.reviewReleaseName);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            likeButton = itemView.findViewById(R.id.likeButton);
            
            supabaseClient = SupabaseClient.getInstance();
            sessionManager = new SessionManager(itemView.getContext());
            supabaseClient.setSessionManager(sessionManager);
        }

        void bind(Review review, OnReviewClickListener listener, OnReviewLikeListener likeListener) {
            usernameText.setText(review.getUserName());
            ratingText.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));
            reviewText.setText(review.getText());
            
            // Загрузка аватара с кэшированием
            if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
                Picasso picasso = PicassoCache.getInstance(itemView.getContext());
                picasso.load(review.getUserAvatarUrl())
                        .noFade()
                        .noPlaceholder()
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_profile);
            }
            
            // Используем метод getFormattedDate() из модели Review
            String formattedDate = review.getFormattedDate();
            Log.d(TAG, "Using formatted date from model: " + formattedDate);
            dateText.setText(formattedDate);
            
            // Добавляем информацию о релизе
            if (review.getReleaseName() != null && !review.getReleaseName().isEmpty()) {
                releaseNameText.setText(review.getReleaseName());
                releaseNameText.setVisibility(View.VISIBLE);
            } else {
                releaseNameText.setVisibility(View.GONE);
            }
            
            // Устанавливаем количество лайков
            likeCountText.setText(String.valueOf(review.getLikesCount()));
            
            // Устанавливаем иконку в зависимости от статуса лайка
            updateLikeButtonAppearance(review.isLikedByUser());
            
            // Обработчик нажатия на кнопку лайка
            likeButton.setOnClickListener(v -> {
                toggleLike(review, likeListener);
            });
            
            // Добавляем обработчик нажатия на элемент
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onReviewClick(review));
            }
            
            // Загружаем актуальную информацию о лайках
            loadLikeInfo(review);
        }
        
        private void updateLikeButtonAppearance(boolean isLiked) {
            if (isLiked) {
                likeButton.setImageResource(R.drawable.ic_like_filled);
                likeButton.setColorFilter(itemView.getContext().getResources().getColor(R.color.accent));
            } else {
                likeButton.setImageResource(R.drawable.ic_like);
                likeButton.setColorFilter(itemView.getContext().getResources().getColor(R.color.text_secondary));
            }
        }
        
        private void toggleLike(Review review, OnReviewLikeListener likeListener) {
            String token = sessionManager.getAccessToken();
            boolean isCurrentlyLiked = review.isLikedByUser();
            
            if (isCurrentlyLiked) {
                // Удаляем лайк
                supabaseClient.unlikeReview(review.getId(), token, new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // Обновляем UI
                        review.setLikedByUser(false);
                        review.setLikesCount(Math.max(0, review.getLikesCount() - 1));
                        
                        itemView.post(() -> {
                            updateLikeButtonAppearance(false);
                            likeCountText.setText(String.valueOf(review.getLikesCount()));
                            
                            if (likeListener != null) {
                                likeListener.onReviewLike(review, false);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        itemView.post(() -> 
                            Toast.makeText(itemView.getContext(), 
                                "Ошибка: " + errorMessage, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } else {
                // Добавляем лайк
                supabaseClient.likeReview(review.getId(), token, new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // Обновляем UI
                        review.setLikedByUser(true);
                        review.setLikesCount(review.getLikesCount() + 1);
                        
                        itemView.post(() -> {
                            updateLikeButtonAppearance(true);
                            likeCountText.setText(String.valueOf(review.getLikesCount()));
                            
                            if (likeListener != null) {
                                likeListener.onReviewLike(review, true);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        itemView.post(() -> 
                            Toast.makeText(itemView.getContext(), 
                                "Ошибка: " + errorMessage, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }
        }
        
        private void loadLikeInfo(Review review) {
            String token = sessionManager.getAccessToken();
            
            // Проверяем, лайкнул ли текущий пользователь эту рецензию
            supabaseClient.isReviewLikedByUser(review.getId(), token, new SupabaseClient.LikeCheckCallback() {
                @Override
                public void onSuccess(boolean isLiked) {
                    review.setLikedByUser(isLiked);
                    itemView.post(() -> updateLikeButtonAppearance(isLiked));
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error checking like status: " + errorMessage);
                }
            });
            
            // Получаем общее количество лайков
            supabaseClient.getReviewLikesCount(review.getId(), token, new SupabaseClient.LikesCountCallback() {
                @Override
                public void onSuccess(int likesCount) {
                    review.setLikesCount(likesCount);
                    itemView.post(() -> likeCountText.setText(String.valueOf(likesCount)));
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error getting likes count: " + errorMessage);
                }
            });
        }
    }
}
