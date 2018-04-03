package remix.myplayer.ui.fragment;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.afollestad.materialdialogs.util.DialogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.interfaces.OnFirstLoadFinishListener;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 *
 */
public class CoverFragment extends BaseFragment {
    @BindView(R.id.cover_image)
    ImageView mImage;
    @BindView(R.id.cover_shadow)
    ImageView mShadow;
    @BindView(R.id.cover_container)
    View mCoverContainer;

    private int mWidth;
    private Uri mUri = Uri.EMPTY;
    private OnInflateFinishListener mInflateFinishListener;
    private OnFirstLoadFinishListener mFirstLoadFinishListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = CoverFragment.class.getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mWidth = getArguments().getInt("Width");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        mImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImage.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                int imageWidth = mImage.getWidth();
//                int imageHeigh = mImage.getHeight();
//                //如果封面宽度大于高度 需要处理下
//                if(imageWidth > imageHeigh){
//                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mImage.getLayoutParams();
//                    lp.width = lp.height = imageHeigh;
//                    mImage.setLayoutParams(lp);
//                }

                if(mInflateFinishListener != null)
                    mInflateFinishListener.onViewInflateFinish(mImage);
                return true;
            }
        });

        return rootView;
    }

    public void setInflateFinishListener(OnInflateFinishListener l){
        mInflateFinishListener = l;
    }

    /**
     * 操作为上一首歌曲时，显示往左侧消失的动画
     *       下一首歌曲时，显示往右侧消失的动画
     * @param info 需要更新的歌曲
     * @param withAnim 是否需要动画
     */
    public void updateCover(Song info, Uri uri, boolean withAnim){
        if(!isAdded())
            return;
        if (mImage == null || info == null)
            return;

        mUri = uri;

        if(withAnim){
            int operation = Global.getOperation();

            int offsetX = (mWidth +  mImage.getWidth()) >> 1;
            final double startValue = 0;
            final double endValue = operation == Constants.PREV ? offsetX : -offsetX;

            //封面移动动画
            final Spring outAnim = SpringSystem.create().createSpring();
            outAnim.addListener(new SimpleSpringListener(){
                @Override
                public void onSpringUpdate(Spring spring) {
                    if(mCoverContainer == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) spring.getCurrentValue());
                }

                @Override
                public void onSpringAtRest(Spring spring) {
                    //显示封面的动画
                    if(mImage == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) startValue);
                    setImageUriInternal();

                    float endVal = 1;
                    final Spring inAnim = SpringSystem.create().createSpring();
                    inAnim.addListener(new SimpleSpringListener(){
                        @Override
                        public void onSpringUpdate(Spring spring) {
                            if(mImage == null || spring == null)
                                return;
                            mCoverContainer.setScaleX((float) spring.getCurrentValue());
                            mCoverContainer.setScaleY((float) spring.getCurrentValue());
                        }

                        @Override
                        public void onSpringActivate(Spring spring) {

                        }

                    });
                    inAnim.setCurrentValue(0.85);
                    inAnim.setEndValue(endVal);
                }
            });
            outAnim.setOvershootClampingEnabled(true);
            outAnim.setCurrentValue(startValue);
            outAnim.setEndValue(endValue);
        } else {
            setImageUriInternal();
            mShadow.setVisibility(View.VISIBLE);
        }
    }

    public void setImageUriInternal(){
        mShadow.setVisibility(View.INVISIBLE);

        RequestOptions options = new RequestOptions()
                .centerCrop()
//                .placeholder(DialogUtils.resolveDrawable(mContext,R.attr.default_album))
                .error(DialogUtils.resolveDrawable(mContext,R.attr.default_album))
//                .override(mImage.getWidth(),mImage.getHeight())
//                .transform(new GlideCircleTransform(mContext,DensityUtil.dip2px(mContext,2),Color.WHITE))
                .dontAnimate();
        Glide.with(mContext).load(mUri)
                .apply(options)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if(mFirstLoadFinishListener != null){
                            mFirstLoadFinishListener.onFirstLoadFinish();
                            mFirstLoadFinishListener = null;
                        }
                        mShadow.setVisibility(View.VISIBLE);
                        mImage.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        if(mFirstLoadFinishListener != null){
                            mFirstLoadFinishListener.onFirstLoadFinish();
                            mFirstLoadFinishListener = null;
                        }
                        mShadow.setVisibility(View.VISIBLE);
                        mImage.setImageDrawable(errorDrawable);
                    }
                });
    }

    /**
     * activity退出时隐藏封面
     */
    public void hideImage(){
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.INVISIBLE);
    }

    /**
     * activity入场动画播放完毕时显示封面
     */
    public void showImage(){
        if(mImage != null)
            mImage.setVisibility(View.VISIBLE);
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.VISIBLE);
    }

    public void setOnFirstLoadFinishListener(OnFirstLoadFinishListener onFirstLoadFinishListener) {
        mFirstLoadFinishListener = onFirstLoadFinishListener;
    }
}
