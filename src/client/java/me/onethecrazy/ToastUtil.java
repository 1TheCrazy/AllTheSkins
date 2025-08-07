package me.onethecrazy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class ToastUtil {
    public static void showFileTooLargeToast(){
        // Get the singleton client instance
        MinecraftClient client = MinecraftClient.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        client.getToastManager().add(
                SystemToast.create(
                        client,
                        SystemToast.Type.FILE_DROP_FAILURE,
                        Text.translatable("gui.alltheskins.title.file_too_large"),
                        Text.translatable("gui.alltheskins.description.file_too_large")
                )
        );
    }

    public static void showModerationNoticeToast(){
        // Get the singleton client instance
        MinecraftClient client = MinecraftClient.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        client.getToastManager().add(
                SystemToast.create(
                        client,
                        SystemToast.Type.UNSECURE_SERVER_WARNING,
                        Text.translatable("gui.alltheskins.title.moderation_notice"),
                        Text.translatable("gui.alltheskins.description.moderation_notice")
                )
        );
    }
}
