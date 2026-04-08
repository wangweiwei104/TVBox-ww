package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.github.tvbox.osc.R;

public class RepoLinksDialog extends BaseDialog {

    public RepoLinksDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_repo_links);
        initView();
    }

    private void initView() {
        // 设置弹窗宽度和高度
//        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
//        int dialogWidth = (int) (screenWidth * 0.85); // 屏幕宽度的85%
//        getWindow().setLayout(dialogWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        // 设置点击事件
        LinearLayout llOriginalRepo = findViewById(R.id.llOriginalRepo);
        LinearLayout llTakagenRepo = findViewById(R.id.llTakagenRepo);
        LinearLayout llMyRepo = findViewById(R.id.llMyRepo);
        View btnClose = findViewById(R.id.btnClose);

        if (llOriginalRepo != null) {
            llOriginalRepo.setOnClickListener(v -> openUrlInBrowser("https://github.com/CatVodTVOfficial/TVBoxOSC"));
        }

        if (llTakagenRepo != null) {
            llTakagenRepo.setOnClickListener(v -> openUrlInBrowser("https://github.com/takagen99/Box"));
        }

        if (llMyRepo != null) {
            llMyRepo.setOnClickListener(v -> openUrlInBrowser("https://github.com/wangweiwei104/TVBox-ww"));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }

    // 打开浏览器访问网址
    private void openUrlInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            getContext().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}