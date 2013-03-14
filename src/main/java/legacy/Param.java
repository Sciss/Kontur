package legacy;

import java.util.prefs.Preferences;

public class Param
{
	public final double		value;
	public final int		unit;

	public Param( double value, int unit )
	{
		this.value	= value;
		this.unit	= unit;
	}

	public int hashCode()
	{
		final long v = Double.doubleToLongBits( value );

		return( (int) (v ^ (v >>> 32)) ^ unit);
	}

	public boolean equals( Object o )
	{
		if( (o != null) && (o instanceof Param) ) {
			final Param p2 = (Param) o;
			return( (Double.doubleToLongBits( this.value ) == Double.doubleToLongBits( p2.value )) &&
					(this.unit == p2.unit) );
		} else {
			return false;
		}
	}

	public static Param fromPrefs( Preferences prefs, String key, Param defaultValue )
	{
		final String str = prefs.get( key, null );
		return( str == null ? defaultValue : Param.valueOf( str ));
	}

	public static Param valueOf( String str )
	{
		final int sepIdx = str.indexOf( ' ' );
		if( sepIdx >= 0 ) {
			return new Param( Double.parseDouble( str.substring( 0, sepIdx )),
							  ParamSpace.stringToUnit( str.substring( sepIdx + 1 )));
		} else {
			return new Param( Double.parseDouble( str ), ParamSpace.NONE );
		}
	}

	public String toString()
	{
		return( String.valueOf( value ) + ' ' + ParamSpace.unitToString( unit ));
	}
}
