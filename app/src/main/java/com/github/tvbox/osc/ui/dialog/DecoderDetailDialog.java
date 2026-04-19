package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
//import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DecoderDetailDialog extends BaseDialog {

    private final DecoderInfoDialog.DecoderInfo decoderInfo;

    public DecoderDetailDialog(@NonNull Context context, DecoderInfoDialog.DecoderInfo decoderInfo) {
        super(context);
        this.decoderInfo = decoderInfo;
        setContentView(R.layout.dialog_decoder_detail);
        initView();
    }

    private void initView() {
        TextView tvDecoderName = findViewById(R.id.tvDecoderName);
        TextView tvDetailInfo = findViewById(R.id.tvDetailInfo);
        Button btnSaveDetail = findViewById(R.id.btnSaveDetail);
        Button btnClose = findViewById(R.id.btnClose);

        if (tvDecoderName != null) {
            tvDecoderName.setText(decoderInfo.getName());
        }

        if (tvDetailInfo != null) {
            tvDetailInfo.setText(decoderInfo.getDetailedInfo());
        }

        if (btnSaveDetail != null) {
            btnSaveDetail.setOnClickListener(v -> {
                saveDetailToFile();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                dismiss();
            });
        }
    }

    /**
     * 保存详细信息到文件
     */
    private void saveDetailToFile() {
        try {
            // 创建文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timeStamp = sdf.format(new Date());
            String fileName = "解码器详情_" + decoderInfo.getName().replace("/", "_") + "_" + timeStamp + ".txt";

            // 获取保存目录
            File saveDir = new File(getContext().getExternalFilesDir(null), "DecoderDetails");

            // 确保目录存在
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // 创建文件
            File file = new File(saveDir, fileName);

            // 写入文件
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decoderInfo.getDetailedInfo().getBytes("UTF-8"));
            fos.close();

            Toast.makeText(getContext(), "详细信息已保存到:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}