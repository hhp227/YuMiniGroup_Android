package com.hhp227.yu_minigroup.adapter;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.hhp227.yu_minigroup.PictureActivity;
import com.hhp227.yu_minigroup.R;
import com.hhp227.yu_minigroup.app.AppController;
import com.hhp227.yu_minigroup.app.EndPoint;
import com.hhp227.yu_minigroup.dto.ArticleItem;
import com.hhp227.yu_minigroup.dto.ReplyItem;

import java.util.ArrayList;
import java.util.List;

public class ReplyListAdapter extends RecyclerView.Adapter {
    private static final int TYPE_ARTICLE = 0;
    private static final int TYPE_REPLY = 1;
    private Activity mActivity;
    private List<String> mReplyItemKeys;
    private List<Object> mReplyItemValues;

    public ReplyListAdapter(Activity activity, List<String> replyItemKeys, List<Object> replyItemValues) {
        this.mActivity = activity;
        this.mReplyItemKeys = replyItemKeys;
        this.mReplyItemValues = replyItemValues;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_ARTICLE:
                View articleItemView = LayoutInflater.from(mActivity).inflate(R.layout.article_detail, parent, false);
                return new HeaderHolder(articleItemView);
            case TYPE_REPLY:
                View replyItemView = LayoutInflater.from(mActivity).inflate(R.layout.reply_item, parent, false);
                return new ItemHolder(replyItemView);
        }
        throw new NullPointerException();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            if (mReplyItemValues.get(position) instanceof ArticleItem) {
                ArticleItem articleItem = (ArticleItem) mReplyItemValues.get(position);

                Glide.with(mActivity)
                        .load(articleItem.getUid() != null ? EndPoint.USER_IMAGE.replace("{UID}", articleItem.getUid()) : null)
                        .apply(RequestOptions.errorOf(R.drawable.profile_img_circle).circleCrop())
                        .into(((HeaderHolder) holder).articleProfile);
                ((HeaderHolder) holder).articleTitle.setText(articleItem.getTitle() + " - " + articleItem.getName());
                ((HeaderHolder) holder).articleTimeStamp.setText(articleItem.getDate());
                if (!TextUtils.isEmpty(articleItem.getContent())) {
                    ((HeaderHolder) holder).articleContent.setText(articleItem.getContent());
                    ((HeaderHolder) holder).articleContent.setVisibility(View.VISIBLE);
                } else
                    ((HeaderHolder) holder).articleContent.setVisibility(View.GONE);

                List<String> images = articleItem.getImages();
                if (images != null && images.size() > 0) {
                    for (String imageUrl : images) {
                        if (((HeaderHolder) holder).articleImages.getChildCount() > images.size() - 1)
                            break;
                        ImageView articleImage = new ImageView(mActivity);
                        articleImage.setAdjustViewBounds(true);
                        articleImage.setPadding(0, 0, 0, 30);
                        articleImage.setScaleType(ImageView.ScaleType.FIT_XY);
                        articleImage.setOnClickListener(v -> {
                            Intent intent = new Intent(mActivity, PictureActivity.class);
                            intent.putStringArrayListExtra("images", (ArrayList<String>) images);
                            intent.putExtra("position", mReplyItemValues.size());
                            mActivity.startActivity(intent);
                        });
                        Glide.with(mActivity).load(imageUrl).apply(RequestOptions.errorOf(R.drawable.ic_launcher_background)).into(articleImage);
                        ((HeaderHolder) holder).articleImages.addView(articleImage);
                    }
                    ((HeaderHolder) holder).articleImages.setVisibility(View.VISIBLE);
                } else
                    ((HeaderHolder) holder).articleImages.setVisibility(View.GONE);

                ((HeaderHolder) holder).itemView.setOnLongClickListener(View::showContextMenu);
            }
        } else if (holder instanceof ItemHolder) {
            if (mReplyItemValues.get(position) instanceof ReplyItem) {
                ReplyItem replyItem = (ReplyItem) mReplyItemValues.get(position);

                Glide.with(mActivity)
                        .load(replyItem.getUid() != null ? new GlideUrl(EndPoint.USER_IMAGE.replace("{UID}", replyItem.getUid()), new LazyHeaders.Builder()
                                .addHeader("Cookie", AppController.getInstance().getPreferenceManager().getCookie())
                                .build()) : null)
                        .apply(new RequestOptions().circleCrop().error(R.drawable.profile_img_circle))
                        .into(((ItemHolder) holder).profileImage);
                ((ItemHolder) holder).name.setText(replyItem.getName());
                ((ItemHolder) holder).reply.setText(replyItem.getReply());
                ((ItemHolder) holder).timeStamp.setText(replyItem.getDate());

                ((ItemHolder) holder).itemView.setOnLongClickListener(View::showContextMenu);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mReplyItemValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ARTICLE : TYPE_REPLY;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class HeaderHolder extends RecyclerView.ViewHolder {
        public ImageView articleProfile;
        public LinearLayout articleImages;
        public TextView articleTitle, articleTimeStamp, articleContent;

        public HeaderHolder(View itemView) {
            super(itemView);
            articleProfile = itemView.findViewById(R.id.iv_profile_image);
            articleTitle = itemView.findViewById(R.id.tv_title);
            articleTimeStamp = itemView.findViewById(R.id.tv_timestamp);
            articleContent = itemView.findViewById(R.id.tv_content);
            articleImages = itemView.findViewById(R.id.ll_image);
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder {
        private ImageView profileImage;
        private TextView name, reply, timeStamp;

        public ItemHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile_image);
            name = itemView.findViewById(R.id.tv_name);
            reply = itemView.findViewById(R.id.tv_reply);
            timeStamp = itemView.findViewById(R.id.tv_timestamp);
        }
    }
}
