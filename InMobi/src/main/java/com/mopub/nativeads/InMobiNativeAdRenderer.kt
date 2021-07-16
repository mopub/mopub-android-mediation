package com.mopub.nativeads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.mopub.common.Preconditions
import com.mopub.nativeads.InMobiNative.InMobiNativeAd
import java.util.*

class InMobiNativeAdRenderer(
    /**
     * A view binder containing the layout resource and views to be rendered by the renderer.
     */
    private val mViewBinder: ViewBinder
) : MoPubAdRenderer<InMobiNativeAd> {
    /**
     * A weak hash map used to keep track of view holder so that the views can be properly recycled.
     */
    private val mViewHolderMap: WeakHashMap<View, InMobiNativeViewHolder?> = WeakHashMap()
    private var mAdView: View? = null

    /**
     * Creates a new view to be used as an ad.
     *
     *
     * This method is called when you call [MoPubStreamAdPlacer.getAdView]
     * and the convertView is null. You must return a valid view.
     *
     * @param context The context. Useful for creating a view. This is recommended to be an
     * Activity. If you have custom themes defined in your Activity, not passing
     * in that Activity will result in the default Application theme being used
     * when creating the ad view.
     * @param parent  The parent that the view will eventually be attached to. You might use the
     * parent to determine layout parameters, but should return the view without
     * attaching it to the parent.
     * @return A new ad view.
     */
    override fun createAdView(context: Context, parent: ViewGroup?): View {
        mAdView = LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false)
        val mainImageView = mAdView?.findViewById<View>(mViewBinder.mainImageId) ?: return mAdView!!
        val mainImageViewLayoutParams = mainImageView.layoutParams
        val primaryViewLayoutParams = RelativeLayout.LayoutParams(mainImageViewLayoutParams.width, mainImageViewLayoutParams.height)

        if (mainImageViewLayoutParams is MarginLayoutParams) {
            mainImageViewLayoutParams.apply { primaryViewLayoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin) }
            primaryViewLayoutParams.addRule(RelativeLayout.BELOW, mViewBinder.mainImageId)
        }
        if (mainImageViewLayoutParams is RelativeLayout.LayoutParams) {
            val rules = mainImageViewLayoutParams.rules
            for (i in rules.indices) {
                primaryViewLayoutParams.addRule(i, rules[i])
            }
        }
        mainImageView.visibility = View.INVISIBLE

        mViewBinder.extras[VIEW_BINDER_KEY_PRIMARY_AD_VIEW_LAYOUT]?.let { layoutId ->
            val primaryAdLayout = mAdView?.findViewById<ViewGroup>(layoutId)
            if (parent is RelativeLayout && primaryAdLayout is RelativeLayout) {
                primaryAdLayout.setLayoutParams(primaryViewLayoutParams)
            }
        }
        return mAdView!!
    }

    /**
     * Renders a view created by [.createAdView] by filling it with ad data.
     *
     * @param view           The ad [View]
     * @param inMobiNativeAd The ad data that should be bound to the view.
     */
    override fun renderAdView(view: View, inMobiNativeAd: InMobiNativeAd) {
        var inMobiNativeViewHolder = mViewHolderMap[view]
        if (inMobiNativeViewHolder == null) {
            inMobiNativeViewHolder = InMobiNativeViewHolder.fromViewBinder(view, mViewBinder)
            mViewHolderMap[view] = inMobiNativeViewHolder
        }
        update(inMobiNativeViewHolder, inMobiNativeAd)
        setViewVisibility(inMobiNativeViewHolder, View.VISIBLE)
    }

    /**
     * Determines if this renderer supports the type of native ad passed in.
     *
     * @param nativeAd The native ad to render.
     * @return True if the renderer can render the native ad and false if it cannot.
     */
    override fun supports(nativeAd: BaseNativeAd): Boolean {
        Preconditions.checkNotNull(nativeAd)
        return nativeAd is InMobiNativeAd
    }

    private fun update(inMobiNativeViewHolder: InMobiNativeViewHolder, inMobiNativeAd: InMobiNativeAd) {
        val mainImageView = inMobiNativeViewHolder.mainImageView
        NativeRendererHelper.addTextView(inMobiNativeViewHolder.titleView, inMobiNativeAd.adTitle)
        NativeRendererHelper.addTextView(inMobiNativeViewHolder.textView, inMobiNativeAd.adDescription)
        NativeRendererHelper.addCtaButton(
            inMobiNativeViewHolder.callToActionView,
            inMobiNativeViewHolder.mainView,
            inMobiNativeAd.adCtaText
        )
        NativeImageHelper.loadImageView(inMobiNativeAd.adIconUrl, inMobiNativeViewHolder.iconImageView)

        mAdView?.setOnClickListener { inMobiNativeAd.onCtaClick() }

        val primaryAdViewLayout = inMobiNativeViewHolder.primaryAdViewLayout
        if (primaryAdViewLayout != null && mainImageView != null) {
            //removed child views and setting native ad primary View to avoid mismatch between native ad and primary
            // view which is caused, because android recycles the view
            primaryAdViewLayout.removeAllViews()
            primaryAdViewLayout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    primaryAdViewLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val primaryAdView = inMobiNativeAd.getPrimaryAdView(inMobiNativeViewHolder.primaryAdViewLayout)
                    primaryAdViewLayout?.addView(primaryAdView)
                }
            })
            primaryAdViewLayout.visibility = View.VISIBLE
            mainImageView.layoutParams = primaryAdViewLayout.layoutParams
        }
    }

    internal class InMobiNativeViewHolder  // Use fromViewBinder instead of a constructor
    private constructor(
        private val mStaticNativeViewHolder: StaticNativeViewHolder,
        val primaryAdViewLayout: ViewGroup?,
        val isMainImageViewInRelativeView: Boolean
    ) {
        val mainView: View?
            get() = mStaticNativeViewHolder.mainView
        val titleView: TextView?
            get() = mStaticNativeViewHolder.titleView
        val textView: TextView?
            get() = mStaticNativeViewHolder.textView
        val callToActionView: TextView?
            get() = mStaticNativeViewHolder.callToActionView
        val mainImageView: ImageView?
            get() = mStaticNativeViewHolder.mainImageView
        val iconImageView: ImageView?
            get() = mStaticNativeViewHolder.iconImageView

        companion object {
            fun fromViewBinder(view: View, viewBinder: ViewBinder): InMobiNativeViewHolder {
                val staticNativeViewHolder = StaticNativeViewHolder.fromViewBinder(view, viewBinder)
                val mainImageView: View? = staticNativeViewHolder.mainImageView
                var mainImageViewInRelativeView = false
                if (mainImageView != null) {
                    val mainImageParent = mainImageView.parent as ViewGroup
                    if (mainImageParent is RelativeLayout) {
                        mainImageViewInRelativeView = true
                    }
                }

                val primaryAdViewLayout: ViewGroup? = viewBinder.extras[VIEW_BINDER_KEY_PRIMARY_AD_VIEW_LAYOUT]?.let { layoutId ->
                    view.findViewById<View>(layoutId) as ViewGroup
                }
                return InMobiNativeViewHolder(staticNativeViewHolder, primaryAdViewLayout, mainImageViewInRelativeView)
            }
        }
    }

    companion object {
        /**
         * Key to set and get star primary ad view as an extra in the view binder.
         */
        const val VIEW_BINDER_KEY_PRIMARY_AD_VIEW_LAYOUT = "primary_ad_view_layout"

        private fun setViewVisibility(inMobiNativeViewHolder: InMobiNativeViewHolder?, visibility: Int) {
            if (inMobiNativeViewHolder?.mainView != null) {
                inMobiNativeViewHolder.mainView?.visibility = visibility
            }
        }
    }

}