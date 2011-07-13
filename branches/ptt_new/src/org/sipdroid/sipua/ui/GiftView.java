package org.sipdroid.sipua.ui;

import java.io.InputStream;

import org.sipdroid.sipua.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class GiftView extends ImageView{
    
    private Movie mMovie;
    private long mMovieStart;
    
    public GiftView(Context context, int id) {
    	super(context);
    	
    	setFocusable(true);

    	InputStream is;

    	is = context.getResources().openRawResource(R.drawable.chat_icon);
    	mMovie = Movie.decodeStream(is);
    }
    public GiftView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);

    	InputStream is;

    	is = context.getResources().openRawResource(R.drawable.chat_icon);
    	mMovie = Movie.decodeStream(is);
    }

    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);

        
        Paint p = new Paint();
        p.setAntiAlias(true);
        
       
        long now = android.os.SystemClock.uptimeMillis();
        if (mMovieStart == 0) {   // first time
            mMovieStart = now;
        }
        if (mMovie != null) {
            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int)((now - mMovieStart) % dur);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, 0, 0);
            invalidate();
        }
    }



}