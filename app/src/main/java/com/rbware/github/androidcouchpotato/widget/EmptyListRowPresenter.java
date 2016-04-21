/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.rbware.github.androidcouchpotato.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rbware.github.androidcouchpotato.R;
import com.rbware.github.androidcouchpotato.system.Settings;
import com.rbware.github.androidcouchpotato.transition.TransitionHelper;

import java.util.HashMap;

/**
 * ListRowPresenter renders {@link ListRow} using a
 * {@link HorizontalGridView} hosted in a {@link EmptyListRowView}.
 *
 * <h3>Hover card</h3>
 * Optionally, {@link #setHoverCardPresenterSelector(PresenterSelector)} can be used to
 * display a view for the currently focused list item below the rendered
 * list. This view is known as a hover card.
 *
 * <h3>Selection animation</h3>
 * ListRowPresenter disables {@link RowPresenter}'s default dimming effect and draws
 * a dim overlay on each view individually.  A subclass may override and disable
 * {@link #isUsingDefaultListSelectEffect()} and write its own dim effect in
 * {@link #onSelectLevelChanged(RowPresenter.ViewHolder)}.
 *
 * <h3>Shadow</h3>
 * ListRowPresenter applies a default shadow to each child view.  Call
 * {@link #setShadowEnabled(boolean)} to disable shadows.  A subclass may override and return
 * false in {@link #isUsingDefaultShadow()} and replace with its own shadow implementation.
 */
public class EmptyListRowPresenter extends RowPresenter {

    private static final String TAG = "ListRowPresenter";
    private static final boolean DEBUG = false;

    private static final int DEFAULT_RECYCLED_POOL_SIZE = 24;

    /**
     * ViewHolder for the ListRowPresenter.
     */
    public static class ViewHolder extends RowPresenter.ViewHolder {
        final EmptyListRowPresenter mListRowPresenter;
        final HorizontalGridView mGridView;
        final TextView mEmptyTextView;
        ItemBridgeAdapter mItemBridgeAdapter;
        final HorizontalHoverCardSwitcher mHoverCardViewSwitcher = new HorizontalHoverCardSwitcher();
        final int mPaddingTop;
        final int mPaddingBottom;
        final int mPaddingLeft;
        final int mPaddingRight;

        public ViewHolder(View rootView, HorizontalGridView gridView, TextView emptyTextView, EmptyListRowPresenter p) {
            super(rootView);
            mGridView = gridView;
            mEmptyTextView = emptyTextView;
            mListRowPresenter = p;
            mPaddingTop = mGridView.getPaddingTop();
            mPaddingBottom = mGridView.getPaddingBottom();
            mPaddingLeft = mGridView.getPaddingLeft();
            mPaddingRight = mGridView.getPaddingRight();
        }

        /**
         * Gets ListRowPresenter that creates this ViewHolder.
         * @return ListRowPresenter that creates this ViewHolder.
         */
        public final EmptyListRowPresenter getListRowPresenter() {
            return mListRowPresenter;
        }

        /**
         * Gets HorizontalGridView that shows a list of items.
         * @return HorizontalGridView that shows a list of items.
         */
        public final HorizontalGridView getGridView() {
            return mGridView;
        }

        /**
         * Gets ItemBridgeAdapter that creates the list of items.
         * @return ItemBridgeAdapter that creates the list of items.
         */
        public final ItemBridgeAdapter getBridgeAdapter() {
            return mItemBridgeAdapter;
        }

        /**
         * Gets selected item position in adapter.
         * @return Selected item position in adapter.
         */
        public int getSelectedPosition() {
            return mGridView.getSelectedPosition();
        }

        /**
         * Gets ViewHolder at a position in adapter.  Returns null if the item does not exist
         * or the item is not bound to a view.
         * @param position Position of the item in adapter.
         * @return ViewHolder bounds to the item.
         */
        public Presenter.ViewHolder getItemViewHolder(int position) {
            ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder) mGridView
                    .findViewHolderForAdapterPosition(position);
            if (ibvh == null) {
                return null;
            }
            return ibvh.getViewHolder();
        }
    }

    /**
     * A task on the ListRowPresenter.ViewHolder that can select an item by position in the
     * HorizontalGridView and perform an optional item task on it.
     */
    public static class SelectItemViewHolderTask extends ViewHolderTask {

        private int mItemPosition;
        private boolean mSmoothScroll = true;
        private ViewHolderTask mItemTask;

        public SelectItemViewHolderTask(int itemPosition) {
            setItemPosition(itemPosition);
        }

        /**
         * Sets the adapter position of item to select.
         * @param itemPosition Position of the item in adapter.
         */
        public void setItemPosition(int itemPosition) {
            mItemPosition = itemPosition;
        }

        /**
         * Returns the adapter position of item to select.
         * @return The adapter position of item to select.
         */
        public int getItemPosition() {
            return mItemPosition;
        }

        /**
         * Sets smooth scrolling to the item or jump to the item without scrolling.  By default it is
         * true.
         * @param smoothScroll True for smooth scrolling to the item, false otherwise.
         */
        public void setSmoothScroll(boolean smoothScroll) {
            mSmoothScroll = smoothScroll;
        }

        /**
         * Returns true if smooth scrolling to the item false otherwise.  By default it is true.
         * @return True for smooth scrolling to the item, false otherwise.
         */
        public boolean isSmoothScroll() {
            return mSmoothScroll;
        }

        /**
         * Returns optional task to run when the item is selected, null for no task.
         * @return Optional task to run when the item is selected, null for no task.
         */
        public ViewHolderTask getItemTask() {
            return mItemTask;
        }

        /**
         * Sets task to run when the item is selected, null for no task.
         * @param itemTask Optional task to run when the item is selected, null for no task.
         */
        public void setItemTask(ViewHolderTask itemTask) {
            mItemTask = itemTask;
        }

        @Override
        public void run(Presenter.ViewHolder holder) {
            if (holder instanceof EmptyListRowPresenter.ViewHolder) {
                HorizontalGridView gridView = ((EmptyListRowPresenter.ViewHolder) holder).getGridView();
                com.rbware.github.androidcouchpotato.widget.ViewHolderTask task = null;
                if (mItemTask != null) {
                    task = new com.rbware.github.androidcouchpotato.widget.ViewHolderTask() {
                        final ViewHolderTask itemTask = mItemTask;
                        @Override
                        public void run(RecyclerView.ViewHolder rvh) {
                            ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder) rvh;
                            itemTask.run(ibvh.getViewHolder());
                        }
                    };
                }
                if (isSmoothScroll()) {
                    gridView.setSelectedPositionSmooth(mItemPosition, task);
                } else {
                    gridView.setSelectedPosition(mItemPosition, task);
                }
            }
        }
    }

    class ListRowPresenterItemBridgeAdapter extends ItemBridgeAdapter {
        EmptyListRowPresenter.ViewHolder mRowViewHolder;

        ListRowPresenterItemBridgeAdapter(EmptyListRowPresenter.ViewHolder rowViewHolder) {
            mRowViewHolder = rowViewHolder;
        }

        @Override
        protected void onCreate(ViewHolder viewHolder) {
            if (viewHolder.itemView instanceof ViewGroup) {
                TransitionHelper.setTransitionGroup((ViewGroup) viewHolder.itemView, true);
            }
            if (mShadowOverlayHelper != null) {
                mShadowOverlayHelper.onViewCreated(viewHolder.itemView);
            }
        }

        @Override
        public void onBind(final ViewHolder viewHolder) {
            // Only when having an OnItemClickListner, we will attach the OnClickListener.
            if (mRowViewHolder.getOnItemViewClickedListener() != null) {
                viewHolder.mHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewHolder ibh = (ViewHolder)
                                mRowViewHolder.mGridView.getChildViewHolder(viewHolder.itemView);
                        if (mRowViewHolder.getOnItemViewClickedListener() != null) {
                            mRowViewHolder.getOnItemViewClickedListener().onItemClicked(viewHolder.mHolder,
                                    ibh.mItem, mRowViewHolder, (ListRow) mRowViewHolder.mRow);
                        }
                    }
                });
            }
        }

        @Override
        public void onUnbind(ViewHolder viewHolder) {
            if (mRowViewHolder.getOnItemViewClickedListener() != null) {
                viewHolder.mHolder.view.setOnClickListener(null);
            }
        }

        @Override
        public void onAttachedToWindow(ViewHolder viewHolder) {
            if (mShadowOverlayHelper != null && mShadowOverlayHelper.needsOverlay()) {
                int dimmedColor = mRowViewHolder.mColorDimmer.getPaint().getColor();
                mShadowOverlayHelper.setOverlayColor(viewHolder.itemView, dimmedColor);
            }
            mRowViewHolder.syncActivatedStatus(viewHolder.itemView);
        }

        @Override
        public void onAddPresenter(Presenter presenter, int type) {
            mRowViewHolder.getGridView().getRecycledViewPool().setMaxRecycledViews(
                    type, getRecycledPoolSize(presenter));
        }
    }

    private int mRowHeight;
    private int mExpandedRowHeight;
    private PresenterSelector mHoverCardPresenterSelector;
    private int mFocusZoomFactor;
    private boolean mUseFocusDimmer;
    private boolean mShadowEnabled = true;
    private int mBrowseRowsFadingEdgeLength = -1;
    private boolean mRoundedCornersEnabled = true;
    private boolean mKeepChildForeground = true;
    private HashMap<Presenter, Integer> mRecycledPoolSize = new HashMap<Presenter, Integer>();
    private ShadowOverlayHelper mShadowOverlayHelper;
    private ItemBridgeAdapter.Wrapper mShadowOverlayWrapper;

    private static int sSelectedRowTopPadding;
    private static int sExpandedSelectedRowTopPadding;
    private static int sExpandedRowNoHovercardBottomPadding;

    /**
     * Constructs a ListRowPresenter with defaults.
     * Uses {@link FocusHighlight#ZOOM_FACTOR_MEDIUM} for focus zooming and
     * disabled dimming on focus.
     */
    public EmptyListRowPresenter() {
        this(FocusHighlight.ZOOM_FACTOR_MEDIUM);
    }

    /**
     * Constructs a ListRowPresenter with the given parameters.
     *
     * @param focusZoomFactor Controls the zoom factor used when an item view is focused. One of
     *         {@link FocusHighlight#ZOOM_FACTOR_NONE},
     *         {@link FocusHighlight#ZOOM_FACTOR_SMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_XSMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_MEDIUM},
     *         {@link FocusHighlight#ZOOM_FACTOR_LARGE}
     * Dimming on focus defaults to disabled.
     */
    public EmptyListRowPresenter(int focusZoomFactor) {
        this(focusZoomFactor, false);
    }

    /**
     * Constructs a ListRowPresenter with the given parameters.
     *
     * @param focusZoomFactor Controls the zoom factor used when an item view is focused. One of
     *         {@link FocusHighlight#ZOOM_FACTOR_NONE},
     *         {@link FocusHighlight#ZOOM_FACTOR_SMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_XSMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_MEDIUM},
     *         {@link FocusHighlight#ZOOM_FACTOR_LARGE}
     * @param useFocusDimmer determines if the FocusHighlighter will use the dimmer
     */
    public EmptyListRowPresenter(int focusZoomFactor, boolean useFocusDimmer) {
        if (!FocusHighlightHelper.isValidZoomIndex(focusZoomFactor)) {
            throw new IllegalArgumentException("Unhandled zoom factor");
        }
        mFocusZoomFactor = focusZoomFactor;
        mUseFocusDimmer = useFocusDimmer;
    }

    /**
     * Sets the row height for rows created by this Presenter. Rows
     * created before calling this method will not be updated.
     *
     * @param rowHeight Row height in pixels, or WRAP_CONTENT, or 0
     * to use the default height.
     */
    public void setRowHeight(int rowHeight) {
        mRowHeight = rowHeight;
    }

    /**
     * Returns the row height for list rows created by this Presenter.
     */
    public int getRowHeight() {
        return mRowHeight;
    }

    /**
     * Sets the expanded row height for rows created by this Presenter.
     * If not set, expanded rows have the same height as unexpanded
     * rows.
     *
     * @param rowHeight The row height in to use when the row is expanded,
     *        in pixels, or WRAP_CONTENT, or 0 to use the default.
     */
    public void setExpandedRowHeight(int rowHeight) {
        mExpandedRowHeight = rowHeight;
    }

    /**
     * Returns the expanded row height for rows created by this Presenter.
     */
    public int getExpandedRowHeight() {
        return mExpandedRowHeight != 0 ? mExpandedRowHeight : mRowHeight;
    }

    /**
     * Returns the zoom factor used for focus highlighting.
     */
    public final int getFocusZoomFactor() {
        return mFocusZoomFactor;
    }

    /**
     * Returns the zoom factor used for focus highlighting.
     * @deprecated use {@link #getFocusZoomFactor} instead.
     */
    @Deprecated
    public final int getZoomFactor() {
        return mFocusZoomFactor;
    }

    /**
     * Returns true if the focus dimmer is used for focus highlighting; false otherwise.
     */
    public final boolean isFocusDimmerUsed() {
        return mUseFocusDimmer;
    }

    @Override
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        final ViewHolder rowViewHolder = (ViewHolder) holder;
        Context context = holder.view.getContext();
        if (mShadowOverlayHelper == null) {
            mShadowOverlayHelper = new ShadowOverlayHelper.Builder()
                    .needsOverlay(needsDefaultListSelectEffect())
                    .needsShadow(needsDefaultShadow())
                    .needsRoundedCorner(areChildRoundedCornersEnabled())
                    .preferZOrder(isUsingZOrder(context))
                    .keepForegroundDrawable(mKeepChildForeground)
                    .options(createShadowOverlayOptions())
                    .build(context);
            if (mShadowOverlayHelper.needsWrapper()) {
                mShadowOverlayWrapper = new ItemBridgeAdapterShadowOverlayWrapper(
                        mShadowOverlayHelper);
            }
        }
        rowViewHolder.mItemBridgeAdapter = new ListRowPresenterItemBridgeAdapter(rowViewHolder);
        // set wrapper if needed
        rowViewHolder.mItemBridgeAdapter.setWrapper(mShadowOverlayWrapper);
        mShadowOverlayHelper.prepareParentForShadow(rowViewHolder.mGridView);

        FocusHighlightHelper.setupBrowseItemFocusHighlight(rowViewHolder.mItemBridgeAdapter,
                mFocusZoomFactor, mUseFocusDimmer);
        rowViewHolder.mGridView.setFocusDrawingOrderEnabled(mShadowOverlayHelper.getShadowType()
                == ShadowOverlayHelper.SHADOW_STATIC);
        rowViewHolder.mGridView.setOnChildSelectedListener(
                new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                selectChildView(rowViewHolder, view, true);
            }
        });
        rowViewHolder.mGridView.setOnUnhandledKeyListener(
                new BaseGridView.OnUnhandledKeyListener() {
            @Override
            public boolean onUnhandledKey(KeyEvent event) {
                if (rowViewHolder.getOnKeyListener() != null &&
                        rowViewHolder.getOnKeyListener().onKey(
                                rowViewHolder.view, event.getKeyCode(), event)) {
                    return true;
                }
                return false;
            }
        });
    }

    final boolean needsDefaultListSelectEffect() {
        return isUsingDefaultListSelectEffect() && getSelectEffectEnabled();
    }

    /**
     * Sets the recycled pool size for the given presenter.
     */
    public void setRecycledPoolSize(Presenter presenter, int size) {
        mRecycledPoolSize.put(presenter, size);
    }

    /**
     * Returns the recycled pool size for the given presenter.
     */
    public int getRecycledPoolSize(Presenter presenter) {
        return mRecycledPoolSize.containsKey(presenter) ? mRecycledPoolSize.get(presenter) :
                DEFAULT_RECYCLED_POOL_SIZE;
    }

    /**
     * Sets the {@link PresenterSelector} used for showing a select object in a hover card.
     */
    public final void setHoverCardPresenterSelector(PresenterSelector selector) {
        mHoverCardPresenterSelector = selector;
    }

    /**
     * Returns the {@link PresenterSelector} used for showing a select object in a hover card.
     */
    public final PresenterSelector getHoverCardPresenterSelector() {
        return mHoverCardPresenterSelector;
    }

    /*
     * Perform operations when a child of horizontal grid view is selected.
     */
    private void selectChildView(ViewHolder rowViewHolder, View view, boolean fireEvent) {
        if (view != null) {
            if (rowViewHolder.mExpanded && rowViewHolder.mSelected) {
                ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                        rowViewHolder.mGridView.getChildViewHolder(view);

                if (mHoverCardPresenterSelector != null) {
                    rowViewHolder.mHoverCardViewSwitcher.select(
                            rowViewHolder.mGridView, view, ibh.mItem);
                }
                if (fireEvent && rowViewHolder.getOnItemViewSelectedListener() != null) {
                    rowViewHolder.getOnItemViewSelectedListener().onItemSelected(
                            ibh.mHolder, ibh.mItem, rowViewHolder, rowViewHolder.mRow);
                }
            }
        } else {
            if (mHoverCardPresenterSelector != null) {
                rowViewHolder.mHoverCardViewSwitcher.unselect();
            }
            if (fireEvent && rowViewHolder.getOnItemViewSelectedListener() != null) {
                rowViewHolder.getOnItemViewSelectedListener().onItemSelected(
                        null, null, rowViewHolder, rowViewHolder.mRow);
            }
        }
    }

    private static void initStatics(Context context) {
        if (sSelectedRowTopPadding == 0) {
            sSelectedRowTopPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_selected_row_top_padding);
            sExpandedSelectedRowTopPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_expanded_selected_row_top_padding);
            sExpandedRowNoHovercardBottomPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_expanded_row_no_hovercard_bottom_padding);
        }
    }

    private int getSpaceUnderBaseline(EmptyListRowPresenter.ViewHolder vh) {
        RowHeaderPresenter.ViewHolder headerViewHolder = vh.getHeaderViewHolder();
        if (headerViewHolder != null) {
            if (getHeaderPresenter() != null) {
                return getHeaderPresenter().getSpaceUnderBaseline(headerViewHolder);
            }
            return headerViewHolder.view.getPaddingBottom();
        }
        return 0;
    }

    private void setVerticalPadding(EmptyListRowPresenter.ViewHolder vh) {
        int paddingTop, paddingBottom;
        // Note: sufficient bottom padding needed for card shadows.
        if (vh.isExpanded()) {
            int headerSpaceUnderBaseline = getSpaceUnderBaseline(vh);
            if (DEBUG) Log.v(TAG, "headerSpaceUnderBaseline " + headerSpaceUnderBaseline);
            paddingTop = (vh.isSelected() ? sExpandedSelectedRowTopPadding : vh.mPaddingTop) -
                    headerSpaceUnderBaseline;
            paddingBottom = mHoverCardPresenterSelector == null ?
                    sExpandedRowNoHovercardBottomPadding : vh.mPaddingBottom;
        } else if (vh.isSelected()) {
            paddingTop = sSelectedRowTopPadding - vh.mPaddingBottom;
            paddingBottom = sSelectedRowTopPadding;
        } else {
            paddingTop = 0;
            paddingBottom = vh.mPaddingBottom;
        }
        vh.getGridView().setPadding(vh.mPaddingLeft, paddingTop, vh.mPaddingRight,
                paddingBottom);
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        initStatics(parent.getContext());
        EmptyListRowView rowView = new EmptyListRowView(parent.getContext());
        setupFadingEffect(rowView);
        if (mRowHeight != 0) {
            rowView.getGridView().setRowHeight(mRowHeight);
        }
        return new ViewHolder(rowView, rowView.getGridView(), rowView.getEmptyTextView(), this);
    }

    /**
     * Dispatch item selected event using current selected item in the {@link HorizontalGridView}.
     * The method should only be called from onRowViewSelected().
     */
    @Override
    protected void dispatchItemSelectedListener(RowPresenter.ViewHolder holder, boolean selected) {
        ViewHolder vh = (ViewHolder)holder;
        ItemBridgeAdapter.ViewHolder itemViewHolder = (ItemBridgeAdapter.ViewHolder)
                vh.mGridView.findViewHolderForPosition(vh.mGridView.getSelectedPosition());
        if (itemViewHolder == null) {
            super.dispatchItemSelectedListener(holder, selected);
            return;
        }

        if (selected) {
            if (holder.getOnItemViewSelectedListener() != null) {
                holder.getOnItemViewSelectedListener().onItemSelected(
                        itemViewHolder.getViewHolder(), itemViewHolder.mItem, vh, vh.getRow());
            }
        }
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        super.onRowViewSelected(holder, selected);
        ViewHolder vh = (ViewHolder) holder;
        setVerticalPadding(vh);
        updateFooterViewSwitcher(vh);
    }

    /*
     * Show or hide hover card when row selection or expanded state is changed.
     */
    private void updateFooterViewSwitcher(ViewHolder vh) {
        if (vh.mExpanded && vh.mSelected) {
            if (mHoverCardPresenterSelector != null) {
                vh.mHoverCardViewSwitcher.init((ViewGroup) vh.view,
                        mHoverCardPresenterSelector);
            }
            ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                    vh.mGridView.findViewHolderForPosition(
                            vh.mGridView.getSelectedPosition());
            selectChildView(vh, ibh == null ? null : ibh.itemView, false);
        } else {
            if (mHoverCardPresenterSelector != null) {
                vh.mHoverCardViewSwitcher.unselect();
            }
        }
    }

    private void setupFadingEffect(EmptyListRowView rowView) {
        // content is completely faded at 1/2 padding of left, fading length is 1/2 of padding.
        HorizontalGridView gridView = rowView.getGridView();
        if (mBrowseRowsFadingEdgeLength < 0) {
            TypedArray ta = gridView.getContext()
                    .obtainStyledAttributes(R.styleable.LeanbackTheme);
            mBrowseRowsFadingEdgeLength = (int) ta.getDimension(
                    R.styleable.LeanbackTheme_browseRowsFadingEdgeLength, 0);
            ta.recycle();
        }
        gridView.setFadingLeftEdgeLength(mBrowseRowsFadingEdgeLength);
    }

    @Override
    protected void onRowViewExpanded(RowPresenter.ViewHolder holder, boolean expanded) {
        super.onRowViewExpanded(holder, expanded);
        ViewHolder vh = (ViewHolder) holder;
        if (getRowHeight() != getExpandedRowHeight()) {
            int newHeight = expanded ? getExpandedRowHeight() : getRowHeight();
            vh.getGridView().setRowHeight(newHeight);
        }
        setVerticalPadding(vh);
        updateFooterViewSwitcher(vh);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        ViewHolder vh = (ViewHolder) holder;
        ListRow rowItem = (ListRow) item;
        vh.mItemBridgeAdapter.setAdapter(rowItem.getAdapter());
        vh.mGridView.setAdapter(vh.mItemBridgeAdapter);
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        ViewHolder vh = (ViewHolder) holder;
        vh.mGridView.setAdapter(null);
        vh.mItemBridgeAdapter.clear();
        super.onUnbindRowViewHolder(holder);
    }

    /**
     * ListRowPresenter overrides the default select effect of {@link RowPresenter}
     * and return false.
     */
    @Override
    public final boolean isUsingDefaultSelectEffect() {
        return false;
    }

    /**
     * Returns true so that default select effect is applied to each individual
     * child of {@link HorizontalGridView}.  Subclass may return false to disable
     * the default implementation.
     * @see #onSelectLevelChanged(RowPresenter.ViewHolder)
     */
    public boolean isUsingDefaultListSelectEffect() {
        return true;
    }

    /**
     * Returns true if SDK >= 18, where default shadow
     * is applied to each individual child of {@link HorizontalGridView}.
     * Subclass may return false to disable.
     */
    public boolean isUsingDefaultShadow() {
        return ShadowOverlayHelper.supportsShadow();
    }

    /**
     * Returns true if SDK >= L, where Z shadow is enabled so that Z order is enabled
     * on each child of horizontal list.   If subclass returns false in isUsingDefaultShadow()
     * and does not use Z-shadow on SDK >= L, it should override isUsingZOrder() return false.
     */
    public boolean isUsingZOrder(Context context) {
        return !Settings.getInstance(context).preferStaticShadows();
    }

    /**
     * Enables or disables child shadow.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final void setShadowEnabled(boolean enabled) {
        mShadowEnabled = enabled;
    }

    /**
     * Returns true if child shadow is enabled.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final boolean getShadowEnabled() {
        return mShadowEnabled;
    }

    /**
     * Enables or disabled rounded corners on children of this row.
     * Supported on Android SDK >= L.
     */
    public final void enableChildRoundedCorners(boolean enable) {
        mRoundedCornersEnabled = enable;
    }

    /**
     * Returns true if rounded corners are enabled for children of this row.
     */
    public final boolean areChildRoundedCornersEnabled() {
        return mRoundedCornersEnabled;
    }

    final boolean needsDefaultShadow() {
        return isUsingDefaultShadow() && getShadowEnabled();
    }

    /**
     * When ListRowPresenter applies overlay color on the child,  it may change child's foreground
     * Drawable.  If application uses child's foreground for other purposes such as ripple effect,
     * it needs tell ListRowPresenter to keep the child's foreground.  The default value is true.
     *
     * @param keep true if keep foreground of child of this row, false ListRowPresenter might change
     *             the foreground of the child.
     */
    public final void setKeepChildForeground(boolean keep) {
        mKeepChildForeground = keep;
    }

    /**
     * Returns true if keeps foreground of child of this row, false otherwise.  When
     * ListRowPresenter applies overlay color on the child,  it may change child's foreground
     * Drawable.  If application uses child's foreground for other purposes such as ripple effect,
     * it needs tell ListRowPresenter to keep the child's foreground.  The default value is true.
     *
     * @return true if keeps foreground of child of this row, false otherwise.
     */
    public final boolean isKeepChildForeground() {
        return mKeepChildForeground;
    }

    /**
     * Create ShadowOverlayHelper Options.  Subclass may override.
     * e.g.
     * <code>
     * return new ShadowOverlayHelper.Options().roundedCornerRadius(10);
     * </code>
     *
     * @return The options to be used for shadow, overlay and rouded corner.
     */
    protected ShadowOverlayHelper.Options createShadowOverlayOptions() {
        return ShadowOverlayHelper.Options.DEFAULT;
    }

    /**
     * Applies select level to header and draw a default color dim over each child
     * of {@link HorizontalGridView}.
     * <p>
     * Subclass may override this method.  A subclass
     * needs to call super.onSelectLevelChanged() for applying header select level
     * and optionally applying a default select level to each child view of
     * {@link HorizontalGridView} if {@link #isUsingDefaultListSelectEffect()}
     * is true.  Subclass may override {@link #isUsingDefaultListSelectEffect()} to return
     * false and deal with the individual item select level by itself.
     * </p>
     */
    @Override
    protected void onSelectLevelChanged(RowPresenter.ViewHolder holder) {
        super.onSelectLevelChanged(holder);
        if (mShadowOverlayHelper != null && mShadowOverlayHelper.needsOverlay()) {
            ViewHolder vh = (ViewHolder) holder;
            int dimmedColor = vh.mColorDimmer.getPaint().getColor();
            for (int i = 0, count = vh.mGridView.getChildCount(); i < count; i++) {
                mShadowOverlayHelper.setOverlayColor(vh.mGridView.getChildAt(i), dimmedColor);
            }
            if (vh.mGridView.getFadingLeftEdge()) {
                vh.mGridView.invalidate();
            }
        }
    }

    @Override
    public void freeze(RowPresenter.ViewHolder holder, boolean freeze) {
        ViewHolder vh = (ViewHolder) holder;
        vh.mGridView.setScrollEnabled(!freeze);
    }

    @Override
    public void setEntranceTransitionState(RowPresenter.ViewHolder holder,
            boolean afterEntrance) {
        super.setEntranceTransitionState(holder, afterEntrance);
        ((ViewHolder) holder).mGridView.setChildrenVisibility(
                afterEntrance? View.VISIBLE : View.INVISIBLE);
    }
}