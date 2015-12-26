package ua.napps.scorekeeper.Models;

import com.github.lzyzsd.randomcolor.RandomColor;

import java.io.Serializable;

import static ua.napps.scorekeeper.Helpers.Constants.RIGHT;
import static ua.napps.scorekeeper.Helpers.Constants.SWIPE_STEP;

public final class Counter implements Serializable {
    private transient OnChangeListener listener;
    private String caption = "Counter";
    private int value = 0;
    private int color = getRandomColor();
    private int defValue = 0;
    private int minValue = -999;
    private int maxValue = 1000;
    private int step = 1;

    public Counter(String caption) {
        this.caption = caption;
    }

    public static Counter getClone(Counter counter) {
        Counter clon = new Counter(counter.getCaption());
        clon.caption = counter.caption;
        clon.value = counter.value;
        clon.color = counter.color;
        clon.defValue = counter.defValue;
        clon.minValue = counter.minValue;
        clon.maxValue = counter.maxValue;
        clon.step = counter.step;
        return clon;
    }

    public void setCaption(String caption) {
        this.caption = caption;

        if (listener != null) listener.onChangeValues();
    }

    public void setValue(int value) {
        this.value = value;
        if (listener != null) listener.onChangeValues();
    }

    public String getCaption() {
        return caption;
    }

    public int getValue() {
        return value;
    }

    public void setColor(int color) {
        this.color = color;
        if (listener != null) listener.onChangeColor();
    }

    public int getColor() {
        return color;
    }

    public int getDefValue() {
        return defValue;
    }

    public void setDefValue(int defValue) {
        this.defValue = defValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        if (value >= minValue) return;
        value = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        if (value <= maxValue) return;
        value = maxValue;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void step(int direction, boolean isSwipe) {
        int newValue;
        if (isSwipe) newValue = getValue() + (direction == RIGHT ? SWIPE_STEP : -1 * SWIPE_STEP);
        else newValue = getValue() + (direction == RIGHT ? step : -1 * step);
        if (newValue < getMinValue() || newValue > getMaxValue()) return;
        setValue(newValue);
    }

    public void increaseValue() {
        value += step;
    }

    public void decreaseValue() {
        value -= step;
    }

    public void setChangeListener(OnChangeListener listener) {
        this.listener = listener;
        listener.onChangeColor();
        listener.onChangeValues();
    }

    private static int getRandomColor() {
        RandomColor randomColor = new RandomColor();
        int color = randomColor.randomColor();
        return color;
    }

    public interface OnChangeListener {
        void onChangeColor();

        void onChangeValues();
    }
}
