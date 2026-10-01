package com.hackscroll.v2;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
/* loaded from: classes.dex */
public class ArticlePager extends ViewGroup {
    private int currentPage;
    private float downY;
    private boolean dragging;
    private int lastNotifiedPage;
    private float lastY;
    private OnPageChangeListener listener;
    private final int minFling;
    private final Scroller scroller;
    private final int touchSlop;
    private VelocityTracker velocity;

    /* loaded from: classes.dex */
    public interface OnPageChangeListener {
        void onPageChanged(int i);
    }

    public ArticlePager(Context context) {
        super(context);
        this.lastNotifiedPage = -1;
        this.scroller = new Scroller(context);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.touchSlop = viewConfiguration.getScaledTouchSlop();
        this.minFling = viewConfiguration.getScaledMinimumFlingVelocity() * 4;
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        this.listener = onPageChangeListener;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int makeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, 1073741824);
        int makeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(size2, 1073741824);
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            getChildAt(i3).measure(makeMeasureSpec, makeMeasureSpec2);
        }
        setMeasuredDimension(size, size2);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if (i6 > 0) {
            for (int i7 = 0; i7 < getChildCount(); i7++) {
                getChildAt(i7).layout(0, i7 * i6, i5, (i7 * i6) + i6);
            }
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i2 != i4 && i2 > 0) {
            this.scroller.forceFinished(true);
            scrollTo(0, this.currentPage * i2);
            requestLayout();
        }
    }

    @Override // android.view.View
    public void scrollTo(int i, int i2) {
        super.scrollTo(0, Math.max(0, Math.min(i2, Math.max(0, (getChildCount() * getHeight()) - getHeight()))));
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.scroller.forceFinished(true);
                float y = motionEvent.getY();
                this.lastY = y;
                this.downY = y;
                this.dragging = false;
                if (this.velocity == null) {
                    this.velocity = VelocityTracker.obtain();
                } else {
                    this.velocity.clear();
                }
                this.velocity.addMovement(motionEvent);
                break;
            case 1:
            case 3:
                this.dragging = false;
                break;
            case 2:
                if (!this.dragging && Math.abs(motionEvent.getY() - this.downY) > this.touchSlop) {
                    this.dragging = true;
                    this.lastY = motionEvent.getY();
                }
                if (this.dragging) {
                    this.velocity.addMovement(motionEvent);
                    return true;
                }
                break;
        }
        return false;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.velocity == null) {
            this.velocity = VelocityTracker.obtain();
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.scroller.forceFinished(true);
                float y = motionEvent.getY();
                this.lastY = y;
                this.downY = y;
                this.velocity.clear();
                this.velocity.addMovement(motionEvent);
                break;
            case 1:
                if (this.dragging) {
                    this.velocity.computeCurrentVelocity(1000);
                    float yVelocity = this.velocity.getYVelocity();
                    int nearestPage = nearestPage();
                    if (yVelocity < (-this.minFling)) {
                        nearestPage = this.currentPage + 1;
                    } else if (yVelocity > this.minFling) {
                        nearestPage = this.currentPage - 1;
                    }
                    setPage(nearestPage, true);
                    this.dragging = false;
                }
                recycleVelocity();
                break;
            case 2:
                this.velocity.addMovement(motionEvent);
                float y2 = motionEvent.getY();
                if (!this.dragging && Math.abs(y2 - this.downY) > this.touchSlop) {
                    this.dragging = true;
                    this.lastY = y2;
                }
                if (this.dragging) {
                    scrollBy(0, (int) (this.lastY - y2));
                    this.lastY = y2;
                    break;
                }
                break;
            case 3:
                if (this.dragging) {
                    setPage(nearestPage(), true);
                }
                this.dragging = false;
                recycleVelocity();
                break;
        }
        return true;
    }

    private void recycleVelocity() {
        if (this.velocity != null) {
            this.velocity.recycle();
            this.velocity = null;
        }
    }

    private int nearestPage() {
        int height = getHeight();
        if (height <= 0 || getChildCount() == 0) {
            return 0;
        }
        return clamp((int) Math.round(getScrollY() / height));
    }

    private int clamp(int i) {
        int childCount = getChildCount() - 1;
        if (childCount < 0) {
            return 0;
        }
        return Math.max(0, Math.min(i, childCount));
    }

    public void setPage(int i, boolean z) {
        this.currentPage = clamp(i);
        int height = this.currentPage * getHeight();
        this.scroller.forceFinished(true);
        this.scroller.startScroll(0, getScrollY(), 0, height - getScrollY(), z ? 320 : 0);
        postInvalidate();
        notifyIfChanged();
    }

    private void notifyIfChanged() {
        if (this.listener != null && this.currentPage != this.lastNotifiedPage) {
            this.lastNotifiedPage = this.currentPage;
            this.listener.onPageChanged(this.currentPage);
        }
    }

    @Override // android.view.View
    public void computeScroll() {
        if (this.scroller.computeScrollOffset()) {
            scrollTo(0, this.scroller.getCurrY());
            postInvalidate();
        }
    }
}
