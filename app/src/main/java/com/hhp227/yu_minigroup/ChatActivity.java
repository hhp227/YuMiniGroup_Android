package com.hhp227.yu_minigroup;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.*;
import com.hhp227.yu_minigroup.adapter.MessageListAdapter;
import com.hhp227.yu_minigroup.app.AppController;
import com.hhp227.yu_minigroup.app.EndPoint;
import com.hhp227.yu_minigroup.dto.MessageItem;
import com.hhp227.yu_minigroup.dto.User;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final int LIMIT = 30;

    private boolean mHasRequestedMore, mHasSelection, mIsGroupChat;

    private CardView mButtonSend;

    private DatabaseReference mDatabaseReference;

    private EditText mInputMessage;

    private List<MessageItem> mMessageItemList;

    private MessageListAdapter mAdapter;

    private RecyclerView mRecyclerView;

    private String mCursor, mSender, mReceiver, mValue, mFirstMessageKey;

    private TextView mSendText;

    private TextWatcher mTextWatcher;

    private User mUser;

    private View.OnLayoutChangeListener mOnLayoutChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mButtonSend = findViewById(R.id.cv_btn_send);
        mInputMessage = findViewById(R.id.et_input_msg);
        mRecyclerView = findViewById(R.id.rv_message);
        mSendText = findViewById(R.id.tv_btn_send);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Messages");
        mMessageItemList = new ArrayList<>();
        mUser = AppController.getInstance().getPreferenceManager().getUser();
        mSender = mUser.getUid();
        mReceiver = intent.getStringExtra("uid");
        mValue = intent.getStringExtra("value");
        mIsGroupChat = intent.getBooleanExtra("grp_chat", false);
        mAdapter = new MessageListAdapter(mMessageItemList, mSender);
        mOnLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && mHasSelection)
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(mMessageItemList.size() - 1));
        };
        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mButtonSend.setCardBackgroundColor(getResources().getColor(s.length() > 0 ? R.color.colorAccent : androidx.cardview.R.color.cardview_light_background, null));
                mSendText.setTextColor(getResources().getColor(s.length() > 0 ? android.R.color.white : android.R.color.darker_gray, null));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(intent.getStringExtra("chat_nm") + (mIsGroupChat ? " 그룹채팅방" : ""));
        }
        mButtonSend.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(mInputMessage.getText().toString().trim())) {
                sendMessage();
                if (!mIsGroupChat)
                    sendLMSMessage();
                mInputMessage.setText("");
            } else
                Toast.makeText(getApplicationContext(), "메시지를 입력하세요.", Toast.LENGTH_LONG).show();
        });
        mInputMessage.addTextChangedListener(mTextWatcher);
        mAdapter.setHasStableIds(true);
        layoutManager.setStackFromEnd(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!mRecyclerView.canScrollVertically(-1) && !mHasRequestedMore && mCursor != null) {
                    mHasRequestedMore = true;

                    fetchMessageList(mIsGroupChat ? mDatabaseReference.child(mReceiver).orderByKey().endAt(mCursor).limitToLast(LIMIT) : mDatabaseReference.child(mSender).child(mReceiver).orderByKey().endAt(mCursor).limitToLast(LIMIT), mMessageItemList.size(), mCursor);
                    mCursor = null;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mHasSelection = layoutManager.findFirstCompletelyVisibleItemPosition() + layoutManager.getChildCount() > layoutManager.getItemCount() - 2;
            }
        });
        mRecyclerView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        fetchMessageList(mIsGroupChat ? mDatabaseReference.child(mReceiver).orderByKey().limitToLast(LIMIT) : mDatabaseReference.child(mSender).child(mReceiver).orderByKey().limitToLast(LIMIT), 0, "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTextWatcher != null)
            mInputMessage.removeTextChangedListener(mTextWatcher);
        if (mOnLayoutChangeListener != null)
            mRecyclerView.removeOnLayoutChangeListener(mOnLayoutChangeListener);
        mRecyclerView.clearOnScrollListeners();
        mMessageItemList.clear();
        mTextWatcher = null;
        mOnLayoutChangeListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchMessageList(Query query, int prevCnt, String prevCursor) {
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (mFirstMessageKey != null && mFirstMessageKey.equals(dataSnapshot.getKey()))
                    return;
                else if (s == null)
                    mFirstMessageKey = dataSnapshot.getKey();
                if (mCursor == null)
                    mCursor = s;
                else if (prevCursor.equals(dataSnapshot.getKey())) {
                    mHasRequestedMore = false;
                    return;
                }
                MessageItem messageItem = dataSnapshot.getValue(MessageItem.class);

                mMessageItemList.add(mMessageItemList.size() - prevCnt, messageItem); // 새로 추가하면 prevCnt는 0으로 됨
                mAdapter.notifyDataSetChanged();
                //mAdapter.notifyItemRangeChanged(mMessageItemList.size() > 1 ? mMessageItemList.size() - 2 : 0, 2);
                if (mHasSelection || mHasRequestedMore)
                    if (prevCnt == 0)
                        mRecyclerView.scrollToPosition(mMessageItemList.size() - 1);
                    else
                        ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(mMessageItemList.size() - prevCnt, 10);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void sendMessage() {
        Map<String, Object> map = new HashMap<>();

        map.put("from", mSender);
        map.put("name", mUser.getName());
        map.put("message", mInputMessage.getText().toString());
        map.put("type", "text");
        map.put("seen", false);
        map.put("timestamp", System.currentTimeMillis());
        if (mIsGroupChat) {
            mDatabaseReference.child(mReceiver).push().setValue(map);
        } else {
            String receiverPath = mReceiver + "/" + mSender + "/";
            String senderPath = mSender + "/" + mReceiver + "/";
            String pushId = mDatabaseReference.child(mSender).child(mReceiver).push().getKey();
            Map<String, Object> messageMap = new HashMap<>();

            if (pushId != null) {
                messageMap.put(receiverPath.concat(pushId), map);
                messageMap.put(senderPath.concat(pushId), map);
            }
            mDatabaseReference.updateChildren(messageMap);
        }
    }

    private void sendLMSMessage() {
        AppController.getInstance().addToRequestQueue(new JsonObjectRequest(Request.Method.POST, EndPoint.SEND_MESSAGE, null, response -> {
            try {
                if (!response.getBoolean("isError"))
                    Log.d("채팅", response.getString("message"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> VolleyLog.e(error.getMessage())) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();

                headers.put("Cookie", AppController.getInstance().getCookieManager().getCookie(EndPoint.LOGIN_LMS));
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
            }

            @Override
            public byte[] getBody() {
                Map<String, String> params = new HashMap<>();

                params.put("TXT", mInputMessage.getText().toString());
                params.put("send_msg", "Y");
                params.put("USERS", mValue);
                if (params.size() > 0) {
                    StringBuilder encodedParams = new StringBuilder();

                    try {
                        params.forEach((k, v) -> {
                            try {
                                encodedParams.append(URLEncoder.encode(k, getParamsEncoding()));
                                encodedParams.append('=');
                                encodedParams.append(URLEncoder.encode(v, getParamsEncoding()));
                                encodedParams.append('&');
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        });
                        return encodedParams.toString().getBytes(getParamsEncoding());
                    } catch (UnsupportedEncodingException uee) {
                        throw new RuntimeException("Encoding not supported: " + getParamsEncoding(), uee);
                    }
                }
                return null;
            }
        }, "req_send_msg");
    }
}
