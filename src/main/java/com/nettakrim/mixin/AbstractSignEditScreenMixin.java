package com.nettakrim.mixin;

import com.nettakrim.SignEditingInfo;
import com.nettakrim.SignedPaintingsClient;
import com.nettakrim.access.AbstractSignEditScreenAccessor;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin implements AbstractSignEditScreenAccessor {
    @Shadow
    private SignText text;

    @Final
    @Shadow
    private String[] messages;

    @Final
    @Shadow
    private SignBlockEntity blockEntity;

    @Final
    @Shadow
    private boolean front;

    @Shadow
    private int currentRow;

    @Shadow protected abstract void setCurrentRowMessage(String message);

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V")
    private void onScreenOpen(SignBlockEntity blockEntity, boolean front, boolean filtered, Text title, CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = new SignEditingInfo(blockEntity, this);
        SignedPaintingsClient.LOGGER.info("SCREEN OPENED");
    }

    @Inject(at = @At("TAIL"), method = "finishEditing")
    private void onScreenClose(CallbackInfo ci) {
        SignedPaintingsClient.currentSignEdit = null;
        SignedPaintingsClient.LOGGER.info("SCREEN CLOSED");
    }

    @Override
    public int tryPaste(String pasteString, int selectionStart, int selectionEnd) {
        String[] newMessages = new String[messages.length];
        System.arraycopy(messages, 0, newMessages, 0, messages.length);

        int maxWidthPerLine = this.blockEntity.getMaxTextWidth();
        TextRenderer textRenderer = SignedPaintingsClient.client.textRenderer;

        selectionStart = MathHelper.clamp(selectionStart, 0, newMessages[currentRow].length());
        selectionEnd = MathHelper.clamp(selectionEnd, 0, newMessages[currentRow].length());
        newMessages[currentRow] = newMessages[currentRow].substring(0,selectionStart)+pasteString+newMessages[currentRow].substring(selectionEnd);
        int currentWidth = textRenderer.getWidth(newMessages[currentRow]);

        int cursor = selectionStart+pasteString.length();

        if (currentWidth < maxWidthPerLine) {
            setCurrentRowMessage(newMessages[currentRow]);
            return cursor;
        }

        int cursorRow = currentRow;

        while (true) {
            String line = newMessages[currentRow];
            int index = getMaxFittingIndex(line, maxWidthPerLine, textRenderer);
            newMessages[currentRow] = line.substring(0, index);
            if (currentRow == messages.length-1 || line.length() <= index) {
                break;
            }
            if (currentRow == cursorRow && cursor > index) {
                cursorRow++;
                cursor -= index;
            }
            currentRow++;
            newMessages[currentRow] = line.substring(index)+newMessages[currentRow];
        }
        cursor = MathHelper.clamp(cursor, 0, newMessages[cursorRow].length());

        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = newMessages[i];
            this.text = this.text.withMessage(i, Text.literal(this.messages[i]));
        }
        this.blockEntity.setText(this.text, this.front);

        currentRow = cursorRow;
        return cursor;
    }

    @Unique
    public int getMaxFittingIndex(String reference, int budgetWidth, TextRenderer textRenderer) {
        //the string->width function can be considered as a sorted array where array[N] is the width of the first N characters of our string
        //this means it can be binary searched, resulting in an index representing the most first N characters that are at or below the budget width

        //the code
        //  index = reference.length();
        //  while (textRenderer.getWidth(reference.substring(0, index)) > budgetWidth) index--;
        //  return index;
        //should function identically

        int low = 0;
        int high = reference.length();
        int index = Integer.MAX_VALUE;

        while (low <= high) {
            int mid = low  + ((high - low) / 2);
            int currentWidth = textRenderer.getWidth(reference.substring(0, mid));
            if (currentWidth < budgetWidth) {
                low = mid + 1;
            } else if (currentWidth > budgetWidth) {
                high = mid - 1;
            } else if (currentWidth == budgetWidth) {
                return mid;
            }
            index = mid;
        }
        //length was not directly achievable, so use the next smallest length instead
        if (textRenderer.getWidth(reference.substring(0, index)) > budgetWidth) index--;
        return index;
    }
}
