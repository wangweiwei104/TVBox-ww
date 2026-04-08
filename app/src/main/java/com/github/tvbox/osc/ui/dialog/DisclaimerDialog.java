package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.R;

public class DisclaimerDialog extends BaseDialog {

    public DisclaimerDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_disclaimer);
        initView();
    }

    private void initView() {
        // 获取布局中的组件
        TextView disclaimerContent = findViewById(R.id.disclaimerContent);

        if (disclaimerContent != null) {
            // 获取资源中的内容
            Context context = getContext();
            String[] englishParts = context.getResources().getStringArray(R.array.about_disclaimers_content);

            // 使用StringBuilder拼接
            StringBuilder disclaimerBuilder = new StringBuilder();
            for (int i = 0; i < englishParts.length; i++) {
                disclaimerBuilder.append(englishParts[i]);
                if (i < englishParts.length - 1) {
                    disclaimerBuilder.append("\n\n");
                }
            }
            // 设置免责声明文本
            String disclaimerText = disclaimerBuilder.toString();
//            // 设置免责声明文本
//            String disclaimerText = "本软件只提供聚合展示功能，所有资源来自网上，软件不参与任何制作、上传、储存、下载等内容。\n\n" +
//                    "软件仅供学习参考，请于安装后24小时内删除。\n\n" +
//                    "本软件为开源项目，遵循相关开源协议。使用者应遵守当地法律法规，不得用于非法用途。\n\n" +
//                    "如因使用本软件产生的任何问题，开发者不承担任何责任。" ;

            disclaimerContent.setText(disclaimerText);
        }

        // 设置确定按钮点击事件
        setOnConfirmClickListener(v -> {
            // 可以在这里添加点击确定后的逻辑
            Toast.makeText(getContext(), "我已阅读并同意免责声明", Toast.LENGTH_SHORT).show();
        });
    }

    // 可选的：添加关闭对话框的方法
    public void setOnConfirmClickListener(View.OnClickListener listener) {
        View view = findViewById(R.id.btnConfirm); // 如果需要按钮，可以在布局中添加
        if (view != null) {
            view.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
    }
}