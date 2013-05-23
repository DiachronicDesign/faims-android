package au.org.intersect.faims.android.nutiteq;

import android.graphics.Color;
import android.graphics.Typeface;

import com.nutiteq.style.StyleSet;
import com.nutiteq.style.TextStyle;

public class GeometryTextStyle {
	
	public int color;
	public int size;
	public Typeface font;
	public int minZoom;
	
	public GeometryTextStyle(int minZoom) {
		this.minZoom = minZoom;
	}
	
	public TextStyle toStyle() {
		return TextStyle.builder().setColor(color).setSize(size).setFont(font).build();
	}
	
	public StyleSet<TextStyle> toStyleSet() {
		StyleSet<TextStyle> styleSet = new StyleSet<TextStyle>();
		styleSet.setZoomStyle(minZoom, toStyle());
		return styleSet;
	}

	public static GeometryTextStyle defaultStyle() {
		GeometryTextStyle textStyle = new GeometryTextStyle(12);
		textStyle.color = Color.WHITE;
		textStyle.size = 40;
		textStyle.font = Typeface.DEFAULT;
		return textStyle;
	}

}
