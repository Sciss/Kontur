package legacy;

import java.util.HashMap;
import java.util.Map;

public class DefaultUnitTranslator
implements ParamSpace.Translator
{
	private static final double twentyByLn10	= 20.0 / Math.log( 10 );

	private final Map	mapCoeffs		= new HashMap();	// Integer( sourceUnit ) -> Coefficient

	private static final int DIM_UNIT_MASK				= ParamSpace.DIM_MASK | ParamSpace.UNIT_MASK;
	private static final int DIM_UNIT_REL_MASK			= DIM_UNIT_MASK | ParamSpace.REL_MASK;
	private static final int DIM_UNIT_REL_SCALE_MASK	= DIM_UNIT_REL_MASK | ParamSpace.SCALE_MASK;

	public Param translate( Param oldParam, ParamSpace newSpace )
	{
		if( newSpace == null ) {
			return new Param( oldParam.value, ParamSpace.NONE );
		}
		if( oldParam.unit == ParamSpace.NONE ) {
			return fitParam( oldParam.value, ParamSpace.NONE, newSpace );
		}

		int			newUnit		= newSpace.unit & DIM_UNIT_REL_MASK;
		int			tempUnit	= oldParam.unit & DIM_UNIT_REL_SCALE_MASK;
		int			tempUnit2;
		double		tempVal		= oldParam.value;
		Coefficient	c;

		tempVal	  = removeScaling( tempVal, tempUnit );
		tempUnit &= ~ParamSpace.SCALE_MASK;

		if( tempUnit == newUnit ) return fitParam( tempVal, tempUnit, newSpace );
		c = getCoeff( tempUnit, newUnit );
		if( c != null ) return fitParam( tempVal * c.coeff, newUnit, newSpace );

		// absolutize
		switch( tempUnit & ParamSpace.REL_MASK ) {
		case ParamSpace.OFF:
			tempUnit &= ~ParamSpace.REL_MASK;
			if( (tempUnit & ParamSpace.UNIT_MASK) == 0 ) {
				tempUnit2	= tempUnit | ParamSpace.DEFAULT_UNIT[ tempUnit & ParamSpace.DIM_MASK ];
				c = getCoeff( tempUnit | ParamSpace.REL, tempUnit2 );
				if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve
				tempUnit	= tempUnit2;
				tempVal		= (tempVal + 1.0) * c.coeff;
			} else {
				c = getCoeff( (tempUnit & ~ParamSpace.UNIT_MASK) | ParamSpace.REL, tempUnit );
				if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve
				tempVal += c.coeff;
			}

			if( tempUnit == newUnit ) return fitParam( tempVal, tempUnit, newSpace );
			c = getCoeff( tempUnit, newUnit );
			if( c != null ) return fitParam( tempVal * c.coeff, tempUnit, newSpace );
			break;

		case ParamSpace.REL:
			tempUnit &= ~ParamSpace.REL_MASK;
			tempUnit |= ParamSpace.DEFAULT_UNIT[ tempUnit & ParamSpace.DIM_MASK ];
			c = getCoeff( (tempUnit & ~ParamSpace.UNIT_MASK) | ParamSpace.REL, tempUnit );
			if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve
			tempVal *= c.coeff;

			if( tempUnit == newUnit ) return fitParam( tempVal, tempUnit, newSpace );
			c = getCoeff( tempUnit, newUnit );
			if( c != null ) return fitParam( tempVal * c.coeff, tempUnit, newSpace );
			break;

		default:
			break;
		}

		// bring to default unit
		if( (tempUnit & ParamSpace.UNIT_MASK) != ParamSpace.DEFAULT_UNIT[ tempUnit & ParamSpace.DIM_MASK ]) {
//System.err.println( "None default : "+(tempUnit & ParamSpace.UNIT_MASK)+" != " +ParamSpace.DEFAULT_UNIT[ tempUnit & ParamSpace.DIM_MASK ]);
			tempUnit2	= (tempUnit & ~ParamSpace.UNIT_MASK) | ParamSpace.DEFAULT_UNIT[ tempUnit & ParamSpace.DIM_MASK ];
			c = getCoeff( tempUnit, tempUnit2 );
			if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve

			tempUnit = tempUnit2;
			tempVal *= c.coeff;

			if( tempUnit == newUnit ) return fitParam( tempVal, tempUnit, newSpace );
		}

		// bring to same dimension
		if( (tempUnit & ParamSpace.DIM_MASK) != (newUnit & ParamSpace.DIM_MASK) ) {
			tempUnit2	= newUnit & DIM_UNIT_MASK;
			c = getCoeff( tempUnit, tempUnit2 );
			if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve

			tempUnit = tempUnit2;
			tempVal *= c.coeff;

			if( tempUnit == newUnit ) return fitParam( tempVal, tempUnit, newSpace );
		}

		switch( newUnit & ParamSpace.REL_MASK ) {
		case ParamSpace.REL:
			c = getCoeff( tempUnit, newUnit );
			if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve
			return fitParam( tempVal * c.coeff, tempUnit, newSpace );

		case ParamSpace.OFF:
			c = getCoeff( tempUnit, (newUnit & ~(ParamSpace.REL_MASK | ParamSpace.UNIT_MASK)) | ParamSpace.REL );
			if( c == null ) return failParam( oldParam.value, newSpace );	// cannot resolve
			if( (newUnit & ParamSpace.UNIT_MASK) == 0 ) {
				return fitParam( tempVal * c.coeff - 1.0, newUnit, newSpace );
			} else {
				return fitParam( tempVal - 1.0 / c.coeff, newUnit, newSpace );
			}
		default:
//			assert false : newUnit;
			return failParam( oldParam.value, newSpace );	// could not resolve
		}
	}

	public void setLengthAndRate( long frames, double rate )
	{
		setCoefficient( ParamSpace.TIME | ParamSpace.SECS, ParamSpace.TIME | ParamSpace.SMPS, rate );
		setCoefficient( ParamSpace.TIME | ParamSpace.SECS | ParamSpace.OFF, ParamSpace.TIME | ParamSpace.SMPS | ParamSpace.OFF, rate );
		setCoefficient( ParamSpace.TIME | ParamSpace.REL, ParamSpace.TIME | ParamSpace.SECS, frames / rate );
	}

	private Coefficient getCoeff( int sourceUnit, int targetUnit )
	{
		return (Coefficient) mapCoeffs.get( (sourceUnit << 16) | targetUnit );
	}

	private static Param failParam( double val, ParamSpace space )
	{
		System.err.println( "failed unit conversion ("+space.unit+")" );
		return new Param( space.fitValue( val ), space.unit );
	}

	private static Param fitParam( double val, int tempUnit, ParamSpace space )
	{
		switch( (space.unit & ~tempUnit) & ParamSpace.SCALE_MASK ) {
		case ParamSpace.PERCENT:
			val *= 100;
			break;
		case ParamSpace.DECIBEL:
			val = Math.log( val ) * twentyByLn10;
			break;
		case ParamSpace.MILLI:
			val *= 1000;
			break;
		case ParamSpace.CENTI:
			val *= 100;
			break;
		case ParamSpace.KILO:
			val /= 1000;
			break;
		default:
			break;
		}

		return new Param( space.fitValue( val ), space.unit );
	}

	private static double removeScaling( double val, int unit )
	{
		// remove custom scaling
		switch( unit & ParamSpace.SCALE_MASK ) {
		case ParamSpace.PERCENT:
			return val / 100;
		case ParamSpace.DECIBEL:
			return Math.exp( val / twentyByLn10 );
		case ParamSpace.MILLI:
			return val / 1000;
		case ParamSpace.CENTI:
			return val / 100;
		case ParamSpace.KILO:
			return val * 1000;
		default:
			return val;
		}
	}

	public void setCoefficient( int sourceUnit, int targetUnit, double coeff )
	{
//		sourceUnit &= ParamSpace.CRUCIAL_MASK;
//		targetUnit &= ParamSpace.CRUCIAL_MASK;

		mapCoeffs.put( (sourceUnit << 16) | targetUnit,
			new Coefficient( sourceUnit, targetUnit, coeff ));
		mapCoeffs.put( (targetUnit << 16) | sourceUnit,
			new Coefficient( targetUnit, sourceUnit, 1.0 / coeff ));
	}

	private static class Coefficient
	{
//		private final int		sourceUnit;
//		private final int		targetUnit;
		protected final double	coeff;

		protected Coefficient( int sourceUnit, int targetUnit, double coeff )
		{
//			this.sourceUnit	= sourceUnit;
//			this.targetUnit	= targetUnit;
			this.coeff		= coeff;
		}
	}
}
