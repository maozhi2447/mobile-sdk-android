package com.appnexus.opensdk;

import com.appnexus.opensdk.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public abstract class AdView extends FrameLayout {

	protected AdFetcher mAdFetcher;
	private String placementID;
	protected int measuredWidth;
	protected int measuredHeight;
	private boolean measured=false;
	protected int width=-1;
	protected int height=-1;
	private boolean mraid_expand=false;
	
	
	/** Begin Construction **/

	public AdView(Context context){
		super(context, null);
		setup(context, null);
	}
	
	public AdView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context, attrs);

	}

	public AdView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context, attrs);
	}
	
	public AdView(Context context, String placement_id){
		super(context);
		this.setPlacementID(placement_id);
		setup(context, null);
	}
	
	public AdView(Context context, String placement_id, int ad_width, int ad_height){
		super(context);
		this.setAdHeight(ad_height);
		this.setAdWidth(ad_width);
		setup(context, null);
	}

	protected void setup(Context context, AttributeSet attrs) {
		// Store self.context in the settings for errors
		Clog.error_context = this.getContext();
		
		Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.new_adview));
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("opensdk_first_launch", true)) {
			// This is the first launch, store a value to remember
			Clog.v(Clog.baseLogTag,Clog.getString(R.string.first_opensdk_launch));
			Settings.getSettings().first_launch = true;
			prefs.edit().putBoolean("opensdk_first_launch", false).commit();
		} else {
			// Found the stored value, this is NOT the first launch
			Clog.v(Clog.baseLogTag,Clog.getString(R.string.not_first_opensdk_launch));
			Settings.getSettings().first_launch = false;
		}

		// Store the UA in the settings
		Settings.getSettings().ua = new WebView(context).getSettings()
				.getUserAgentString();
		Clog.v(Clog.baseLogTag, Clog.getString(R.string.ua, Settings.getSettings().ua));

		// Store the AppID in the settings
		Settings.getSettings().app_id = context.getApplicationContext()
				.getPackageName();
		Clog.v(Clog.baseLogTag, Clog.getString(R.string.appid, Settings.getSettings().app_id));
		
		Clog.v(Clog.baseLogTag, Clog.getString(R.string.making_adman));
		// Make an AdFetcher - Continue the creation pass
		mAdFetcher = new AdFetcher(this);
		// Load user variables only if attrs isn't null
		if(attrs!=null) loadVariablesFromXML(context, attrs);
		
		//We don't start the ad requesting here, since the view hasn't been sized yet.
	}
	
	@Override
	public void onLayout(boolean changed, int left, int top, int right, int bottom){
		super.onLayout(changed, left, top, right, bottom);
		if(mraid_expand){
			mraid_expand=false;
			return;
		}
		if(!measured || changed){
			//Convert to dips
			float density = getContext().getResources().getDisplayMetrics().density;
			measuredWidth = (int)((right - left)/density + 0.5f);
			measuredHeight = (int)((bottom - top)/density + 0.5f);
			if(measuredHeight<height || measuredWidth<width ){
				Clog.e(Clog.baseLogTag, Clog.getString(R.string.adsize_too_big, measuredWidth, measuredHeight, width, height));
				//Hide the space, since no ad will be loaded due to error
				hide();
				//Stop any request in progress
				if(mAdFetcher!=null) mAdFetcher.stop();
				//Returning here allows the SDK to re-request when the layout next changes, and maybe the error will be amended. 
				return;
			}
			measured = true;
			
			// Hide the adview
			hide();

			onFirstLayout();
			
			
		}
	}
	
	// If single-use mode, we must manually start the fetcher
	protected void onFirstLayout(){
		mAdFetcher.start();
	}
	
	public void loadAd(){
		if(this.getWindowVisibility()==VISIBLE && mAdFetcher!=null){
			//Reload Ad Fetcher to get new ad at user's request
			mAdFetcher.stop();
			mAdFetcher.start();
		}
	}

	public void loadAd(String placementID){
		this.setPlacementID(placementID);
		loadAd();
	}
	
	public void loadAd(String placementID, int width, int height){
		this.setAdHeight(height);
		this.setAdWidth(width);
		this.setPlacementID(placementID);
		loadAd();
	}

	protected abstract void loadVariablesFromXML(Context context, AttributeSet attrs);

	/** End Construction **/

	protected void display(Displayable d) {
		if (d.failed())
			return; // The displayable has failed to be parsed or turned into a
					// View.
		this.removeAllViews();
		this.addView(d.getView());
		Clog.d("MRAID", "Adding view: w:"+d.getView().getLayoutParams().width+" h:"+d.getView().getLayoutParams().height);
		unhide();
	}

	protected void unhide() {
		if (getVisibility() != VISIBLE){
			setVisibility(VISIBLE);
		}
	}

	protected void hide() {
		if (getVisibility() != GONE)
			setVisibility(GONE);
	}



	public String getPlacementID() {
		Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.get_placement_id, placementID));
		return placementID;
	}

	public void setPlacementID(String placementID) {
		Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.set_placement_id, placementID));
		this.placementID = placementID;
	}

	@Override
	protected void finalize() {
		try {
			super.finalize();
		} catch (Throwable e){}
		// Just in case, kill the adfetcher's service
		if (mAdFetcher != null)
			mAdFetcher.stop();
	}
	
	public void setAdHeight(int h){
		Clog.d(Clog.baseLogTag, Clog.getString(R.string.set_height, h));
		height=h;
	}
	
	public void setAdWidth(int w){
		Clog.d(Clog.baseLogTag, Clog.getString(R.string.set_width, w));
		width=w;
	}
	
	public int getAdHeight(){
		Clog.d(Clog.baseLogTag, Clog.getString(R.string.get_height, height));
		return height;
	}
	
	public int getAdWidth(){
		Clog.d(Clog.baseLogTag, Clog.getString(R.string.get_width, width));
		return width;
	}
	
	protected int getContainerWidth(){
		return measuredWidth;
	}
	
	protected int getContainerHeight(){
		return measuredHeight;
	}
	
	//Used only by MRAID
	private ImageButton close;
	protected void expand(int w, int h, boolean custom_close, final MRAIDImplementation caller){
		//Only expand w and h if they are >0, otherwise they are match_parent or something
		mraid_expand=true;
		if(getLayoutParams().width>0) getLayoutParams().width=w;
		if(getLayoutParams().height>0) getLayoutParams().height=h;
		if(!custom_close && close==null){
			//Add a stock close button to the top right corner
			close = new ImageButton(this.getContext());
			close.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
			FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP);
			blp.rightMargin=(this.getMeasuredWidth()-this.getChildAt(0).getMeasuredWidth())/2;
			close.setLayoutParams(blp);
			close.setBackgroundColor(Color.TRANSPARENT);
			close.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					caller.close();
					
				}
			});
			this.addView(close);
		}else if(custom_close && close!=null){
			close.setVisibility(GONE);
		}else if(!custom_close && close!=null){
			close.setVisibility(VISIBLE);
		}
	}
}