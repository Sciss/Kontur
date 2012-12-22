/*
 *  Axis.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2012 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur.gui

import java.awt.{Color, Dimension, FontMetrics, Graphics, Graphics2D, Rectangle, RenderingHints, TexturePaint}
import java.awt.geom.{GeneralPath, AffineTransform}
import java.awt.image.BufferedImage
import java.text.MessageFormat
import java.util.Locale
import javax.swing.{ JComponent, JViewport }
import scala.math._

import de.sciss.app.{ AbstractApplication, GraphicsHandler }
import de.sciss.gui.{ ComponentHost, VectorSpace }

/**
 *  A GUI element for displaying
 *  the timeline's axis (ruler)
 *  which is used to display the
 *  time indices and to allow the
 *  user to position and select the
 *  timeline.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 21-Jan-09
 *
 *	@todo		FIXEDBOUNDS is ignored in logarithmic mode now
 *	@todo		new label width calculation not performed in logarithmic mode
 */
object Axis {
  	/**
	 *	Defines the axis to have horizontal orient
	 */
	val HORIZONTAL		= 0x00
	/**
	 *	Defines the axis to have vertical orient
	 */
	val VERTICAL		= 0x01
	/**
	 *	Flag: Defines the axis to have flipped min/max values.
	 *	I.e. for horizontal orient, the maximum value
	 *	corresponds to the left edge, for vertical orient
	 *	the maximum corresponds to the bottom edge
	 */
	val MIRROIR			= 0x02
	/**
	 *	Flag: Requests the labels to be formatted as MIN:SEC.MILLIS
	 */
	val TIMEFORMAT		= 0x04
	/**
	 *	Flag: Requests that the label values be integers
	 */
	val INTEGERS		= 0x08
	/**
	 *	Flag: Requests that the space's min and max are always displayed
	 *		  and hence subdivision are made according to the bounds
	 */
	val FIXEDBOUNDS		= 0x10

	private val DECIMAL_RASTER	= Array( 100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L, 100L, 10L, 1L )
	private val INTEGERS_RASTER	= Array( 100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L )
	private val TIME_RASTER		= Array( 60000000L, 6000000L, 600000L, 60000L, 10000L, 1000L, 100L, 10L, 1L )
	private val MIN_LABSPC		= 16

	// the following are used for Number to String conversion using MessageFormat
	private val msgNormalPtrn = Array( "{0,number,0}",
							   "{0,number,0.0}",
							   "{0,number,0.00}",
							   "{0,number,0.000}" )
    private val msgTimePtrn	= Array( "{0,number,integer}:{1,number,00}",
							 "{0,number,integer}:{1,number,00.0}",
							 "{0,number,integer}:{1,number,00.00}",
							 "{0,number,integer}:{1,number,00.000}" )

	private val pntBarGradientPixels = Array( 0xFFB8B8B8, 0xFFC0C0C0, 0xFFC8C8C8, 0xFFD3D3D3,
									  0xFFDBDBDB, 0xFFE4E4E4, 0xFFEBEBEB, 0xFFF1F1F1,
									  0xFFF6F6F6, 0xFFFAFAFA, 0xFFFBFBFB, 0xFFFCFCFC,
									  0xFFF9F9F9, 0xFFF4F4F4, 0xFFEFEFEF )
	private val barExtent		= pntBarGradientPixels.length

//	private static final double	ln10			= Math.log( 10 );
}

class Axis( orient: Int = Axis.HORIZONTAL, private var flagsVar: Int = 0, host: Option[ ComponentHost ] = None )
extends JComponent //implements Disposable
with TopPaintable
{
    import Axis._

	private var recentWidth		= 0
	private var recentHeight	= 0
	private var doRecalc		= true

	private val kPeriod			= 1000.0
	private var labels = new Array[ Label ]( 0 )
//	private var labelPos		= Array[ Int ]()
	private val shpTicks		= new GeneralPath()

//	private val fntLabel =
    setFont(  AbstractApplication.getApplication.getGraphicsHandler
      .getFont( GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI ))

	private val msgForm			= new MessageFormat( msgNormalPtrn( 0 ), Locale.US )  // XXX US locale
	private	val msgArgs			= new Array[ AnyRef ]( 2 )

	private val trnsVertical	= new AffineTransform()

	private var msgPtrn: Array[ String ] = null
	private var labelRaster: Array[ Long ] = null
	private var labelMinRaster = 0L
	private var spaceVar: VectorSpace = null // XXX should not use null

	private var flMirroir     = false		// MIRROIR set
	private var flTimeFormat  = false		// TIMEFORMAT set
	private var flIntegers    = false		// INTEGERS set
	private var flFixedBounds = false		// FIXEDBOUNDS set

    private val (imgWidth, imgHeight) =
		if( orient == HORIZONTAL ) {
			setMaximumSize( new Dimension( getMaximumSize.width, barExtent ))
			setMinimumSize( new Dimension( getMinimumSize.width, barExtent ))
			setPreferredSize( new Dimension( getPreferredSize.width, barExtent ))
			(1, barExtent)
		} else {
			setMaximumSize( new Dimension( barExtent, getMaximumSize.height ))
			setMinimumSize( new Dimension( barExtent, getMinimumSize.height ))
			setPreferredSize( new Dimension( barExtent, getPreferredSize.height ))
            (barExtent, 1)
		}

     private val img = new BufferedImage( imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB )
     img.setRGB( 0, 0, imgWidth, imgHeight, pntBarGradientPixels, 0, imgWidth )
     private val pntBackground = new TexturePaint( img, new Rectangle( 0, 0, imgWidth, imgHeight ))

     var viewPort: Option[ JViewport ] = None

    // ---- constructor ----
    {
		flagsUpdated()
		setOpaque( true )
  	}

    def flags = flagsVar
	def flags_=( newFlags: Int ) {
		if( flagsVar == newFlags ) return
        flagsVar = newFlags
        flagsUpdated()
    }

    private def flagsUpdated() {
		flMirroir		= (flags & MIRROIR) != 0
		flTimeFormat	= (flags & TIMEFORMAT) != 0
		flIntegers		= (flags & INTEGERS) != 0
		flFixedBounds	= (flags & FIXEDBOUNDS) != 0

		if( flTimeFormat ) {
			msgPtrn		= msgTimePtrn
			labelRaster	= TIME_RASTER
		} else {
			msgPtrn		= msgNormalPtrn
			labelRaster	= if( flIntegers ) INTEGERS_RASTER else DECIMAL_RASTER
		}
		labelMinRaster	= labelRaster( labelRaster.length - 1 )

		triggerRedisplay()
	}

    def space = spaceVar
	def space_=( newSpace: VectorSpace ) {
        spaceVar = newSpace
		triggerRedisplay()
	}

    protected def setSpaceNoRepaint( newSpace: VectorSpace ) {
       spaceVar = newSpace
		doRecalc = true
    }

    private val normalRect = new Rectangle
    private def normalBounds: Rectangle = {
        normalRect.x      = 0
        normalRect.y      = 0
        normalRect.width  = getWidth
        normalRect.height = getHeight
        normalRect
    }

    private def portBounds: Rectangle = {
// println( "vp rect = " + viewPort.get.getViewRect )
      val r = viewPort.get.getViewRect
      if( r != normalRect ) {
          normalRect.setBounds( r )
          viewRectChanged( r )
      }
      normalRect
    }

    // subclasses might want to use this
    protected def viewRectChanged( r: Rectangle ) {}

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

//    println( "axis " + viewPort.get.getViewPosition + "; " + getWidth )

		val g2        = g.asInstanceOf[ Graphics2D ]
//		val w         = getWidth()
//		val h         = getHeight()
		val trnsOrig  = g2.getTransform
		val fm        = g2.getFontMetrics

//		g2.setFont( fntLabel );

        val r = if( viewPort.isEmpty ) normalBounds else portBounds

		if( doRecalc || (r.width != recentWidth) || (r.height != recentHeight) ) {
			recentWidth		= r.width
			recentHeight	= r.height
			recalcLabels( g )
			if( orient == VERTICAL ) recalcTransforms()
			doRecalc		= false
		}

		g2.setPaint( pntBackground )
		g2.fillRect( r.x, r.y, r.width, r.height )

//g2.setColor( Color.red )
//g2.fillRect( viewPort.get.getViewPosition.x + 20, 0, 10, 20 )

        g2.translate( r.x, r.y )
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF )

		val y = if( orient == VERTICAL ) {
			g2.transform( trnsVertical )
			r.width - 3 - fm.getMaxDescent
		} else {
			r.height - 3 - fm.getMaxDescent
		}
		g2.setColor( Color.lightGray )
		g2.draw( shpTicks )

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
		g2.setColor( Color.black )

        labels.foreach( l => {
			g2.drawString( l.name, l.pos, y )
		})

		g2.setTransform( trnsOrig )
        paintOnTop( g2 )
    }

	private def recalcTransforms() {
		trnsVertical.setToRotation( -Pi / 2, recentHeight.toDouble / 2,
										     recentHeight.toDouble / 2 )
	}

	private def calcStringWidth( fntMetr: FontMetrics, value: Double ) : Int = {
		if( flTimeFormat ) {
			msgArgs( 0 )	= (value / 60).toInt.asInstanceOf[ java.lang.Integer ]
			msgArgs( 1 )	= (value % 60).asInstanceOf[ java.lang.Double ]
		} else {
			msgArgs( 0 )	= value.asInstanceOf[ java.lang.Double ]
		}
		fntMetr.stringWidth( msgForm.format( msgArgs ))
	}

	private def calcMinLabSpc( fntMetr: FontMetrics, mini: Double, maxi: Double ) : Int = {
		max( calcStringWidth( fntMetr, mini ), calcStringWidth( fntMetr, maxi )) + MIN_LABSPC
	}

	private def recalcLabels( g: Graphics ) {
		val fntMetr	= g.getFontMetrics
//		int					shift, width, height, numTicks, numLabels, ptrnIdx, ptrnIdx2, minLbDist;
//		double				scale, pixelOff, pixelStep, tickStep, minK, maxK;
//		long				raster, n;
//		double				valueOff, valueStep, spcMin, spcMax;

		shpTicks.reset()
		if( spaceVar == null ) {
            labels    = new Array[ Label ]( 0 )
			return
		}

        val (width, height, spcMin, spcMax) = if( orient == HORIZONTAL ) {
			if( spaceVar.hlog ) {
				recalcLogLabels()
				return
			}
			(recentWidth, recentHeight, spaceVar.hmin, spaceVar.hmax)
		} else {
			if( spaceVar.vlog ) {
				recalcLogLabels()
				return
			}
			(recentHeight, recentWidth, spaceVar.vmin, spaceVar.vmax)
		}
		val scale	= width / (spcMax - spcMin)
		val minK	= kPeriod * spcMin
		val maxK	= kPeriod * spcMax

    	var (numTicks: Int, valueOff: Double, pixelOff: Double, valueStep: Double) =
        if( flFixedBounds ) {
			val ptrnIdxTmp = {
              val n = abs( minK ).toLong
              if( (n % 1000) == 0 ) {
				0
              } else if( (n % 100) == 0 ) {
				1
              } else if( (n % 10) == 0 ) {
				2
              } else {
				3
              }
            }

			val ptrnIdx1 = {
              val n = abs( maxK ).toLong
              if( (n % 1000) == 0 ) {
				ptrnIdxTmp
              } else if( (n % 100) == 0 ) {
				max( ptrnIdxTmp, 1 )
              } else if( (n % 10) == 0 ) {
				max( ptrnIdxTmp, 2 )
              } else {
              	3
              }
            }

			// make a first label width calculation with coarsest display
			msgForm.applyPattern( msgPtrn( ptrnIdx1 ))
			var minLbDist	= calcMinLabSpc( fntMetr, spcMin, spcMax )
			var numLabels	= max( 1, width / minLbDist )

			// ok, easy way : only divisions by powers of two
            var shift = 0
            while( numLabels > 2 ) {
              shift += 1
              numLabels >>= 1
            }
			numLabels <<= shift
			val valueStep	= (maxK - minK) / numLabels

			
			val ptrnIdx2 = {
              val n = valueStep.toLong
              if( (n % 1000) == 0 ) {
				ptrnIdx1
              } else if( (n % 100) == 0 ) {
				max( ptrnIdx1, 1 )
              } else if( (n % 10) == 0 ) {
				max( ptrnIdx1, 2 )
              } else {
				3
              }
            }

			if( ptrnIdx2 != ptrnIdx1 ) {	// ok, labels get bigger, recalc numLabels ...
				msgForm.applyPattern( msgPtrn( ptrnIdx2 ))
				val minLbDist = calcMinLabSpc( fntMetr, spcMin, spcMax )
				numLabels = max( 1, width / minLbDist )
                shift = 0
                while( numLabels > 2 ) {
                  shift += 1
                  numLabels >>= 1
                }
				numLabels <<= shift
				val valueStep = (maxK - minK) / numLabels

				// nochmal ptrnIdx berechnen, evtl. reduziert sich die aufloesung wieder...
				msgForm.applyPattern( msgPtrn({
                  val n = valueStep.toLong
                  if( (n % 1000) == 0 ) {
					ptrnIdx1
                  } else if( (n % 100) == 0 ) {
					max( ptrnIdx1, 1 )
                  } else if( (n % 10) == 0 ) {
					max( ptrnIdx1, 2 )
                  } else {
					3
                  }
                }))
			}

			(4, minK, 0, valueStep)

		} else {
			// make a first label width calculation with coarsest display
			msgForm.applyPattern( msgPtrn( 0 ))
			var minLbDist = calcMinLabSpc( fntMetr, spcMin, spcMax )
			var numLabels = max( 1, width / minLbDist )

			// now valueStep =^= 1000 * minStep
			var valueStep = ceil( (maxK - minK) / numLabels )
			// die Grossenordnung von valueStep ist Indikator fuer Message Pattern
			var ptrnIdx1 = if( flIntegers ) 0 else 3
			var raster = labelMinRaster
            var i = 0; var break = false
            while( (i < labelRaster.length) && !break ) {
				if( valueStep >= labelRaster( i )) {
					ptrnIdx1  = max( 0, i - 5 )
					raster    = labelRaster( i )
					break     = true
				} else {
                  i += 1
                }
			}
			msgForm.applyPattern( msgPtrn( ptrnIdx1 ))
			if( ptrnIdx1 > 0 ) {	// have to recheck label width!
				minLbDist	= max( calcStringWidth( fntMetr, spcMin ), calcStringWidth( fntMetr, spcMax )) + MIN_LABSPC
				numLabels	= max( 1, width / minLbDist )
				valueStep	= ceil( (maxK - minK) / numLabels )
			}
			valueStep	= max( 1, floor( (valueStep + raster - 1) / raster ))
            if( valueStep == 7 || valueStep == 9 ) valueStep = 10

            val numTicks = valueStep.toInt match {
				case 2 => 4
                case 4 => 4
                case 8 => 4
				case 3 => 6
				case 6 => 6
                case _ => 5
            }
			valueStep   *= raster
            val valueOff = floor( abs( minK ) / valueStep ) * (if( minK >= 0 ) valueStep else -valueStep)
            val pixelOff = (valueOff - minK) / kPeriod * scale + 0.5

            (numTicks, valueOff, pixelOff, valueStep)
        }
		val pixelStep   = valueStep / kPeriod * scale
		var tickStep	= pixelStep / numTicks
		val numLabels	= max( 0, ((width - pixelOff + pixelStep - 1.0) / pixelStep).toInt )

		if( labels.length != numLabels ) labels = new Array[ Label ]( numLabels )

		if( flMirroir ) {
			pixelOff	= width - pixelOff
			tickStep	= -tickStep
		}

        var i = 0
        while( i < numLabels ) {
			if( flTimeFormat ) {
				msgArgs( 0 )	= (valueOff / 60000).toInt.asInstanceOf[ java.lang.Integer ]
				msgArgs( 1 )	= ((valueOff % 60000) / 1000).asInstanceOf[ java.lang.Double ]
			} else {
				msgArgs( 0 )	= (valueOff / kPeriod).asInstanceOf[ java.lang.Double ]
			}
            labels( i ) = new Label( msgForm.format( msgArgs ), (pixelOff + 2).toInt )
			valueOff += valueStep
			shpTicks.moveTo( pixelOff.toFloat, 1 )
			shpTicks.lineTo( pixelOff.toFloat, height - 2 )
			pixelOff += tickStep
            var k = 1
            while( k < numTicks ) {
				shpTicks.moveTo( pixelOff.toFloat, height - 4 )
				shpTicks.lineTo( pixelOff.toFloat, height - 2 )
				pixelOff += tickStep
                k += 1
			}
            i += 1
		}
	}


	private def recalcLogLabels() {
throw new IllegalStateException( "LOG NOT YET IMPLEMENTED" )
/*
		int				numLabels, width, height, numTicks, mult, expon, newPtrnIdx, ptrnIdx;
		double			spaceOff, factor, d1, pixelOff, min, max;

		if( orient == HORIZONTAL ) {
			width		= recentWidth;
			height		= recentHeight;
			min			= spaceVar.hmin
			max			= spaceVar.hmax
		} else {
			width		= recentHeight;
			height		= recentWidth;
			min			= space.vmin;
			max			= space.vmax;
		}

		factor	= Math.pow( max / min, (double) 72 / (double) width );	// XXX
		expon	= (int) (Math.log( factor ) / ln10);
		mult	= (int) (Math.ceil( factor / Math.pow( 10, expon )) + 0.5);

//System.out.println( "orig : factor " + factor + "; expon " + expon + "; mult " + mult );

		if( mult > 5 ) {
			expon++;
			mult = 1;
		} else if( mult > 3 ) {
			mult = 4;
		} else if( mult > 2 ) {
			mult = 5;
		}
		factor	= mult * Math.pow( 10, expon );

		numLabels = (int) (Math.ceil( Math.log( max/min ) / Math.log( factor )) + 0.5);

//System.out.println( "max " + max + "; min " + min + "; width " + width + "; numLabels " + numLabels + "; factor " + factor + "; expon " + expon + "; mult " + mult );

		if( labels.length != numLabels ) labels = new String[ numLabels ];
		if( labelPos.length != numLabels ) labelPos = new int[ numLabels ];

//		if( (min * (factor - 1.0)) % 10 == 0.0 ) {
//			numTicks	= 10;
//		} else {
			numTicks	= 8;
//		}
//		tickFactor	= Math.pow( factor, 1.0 / numTicks );

//System.err.println( "factor "+factor+"; expon "+expon+"; mult "+mult+"; tickFactor "+tickFactor+"; j "+j );

		ptrnIdx = -1;

		for( int i = 0; i < numLabels; i++ ) {
			spaceOff	= min * Math.pow( factor, i );
			newPtrnIdx	= 3;
			for( int k = 1000; k > 1 && (((spaceOff * k) % 1.0) == 0); k /= 10 ) {
				newPtrnIdx--;
			}
			if( ptrnIdx != newPtrnIdx ) {
				msgForm.applyPattern( msgPtrn[ newPtrnIdx ]);
				ptrnIdx = newPtrnIdx;
			}

			if( orient == HORIZONTAL ) {
				pixelOff	= space.hSpaceToUnity( spaceOff ) * width;
			} else {
				pixelOff	= space.vSpaceToUnity( spaceOff ) * width;
			}
//System.err.println( "#"+i+" : spaceOff = "+spaceOff+"; pixelOff "+pixelOff );
			msgArgs[ 0 ]	= new Double( spaceOff );
			labels[ i ]		= msgForm.format( msgArgs );
			labelPos[ i ]	= (int) pixelOff + 2;
			shpTicks.moveTo( (float) pixelOff, 1 );
			shpTicks.lineTo( (float) pixelOff, height - 2 );
			d1			= spaceOff * (factor - 1) / numTicks;
			for( int n = 1; n < numTicks; n++ ) {
				if( orient == HORIZONTAL ) {
					pixelOff	= space.hSpaceToUnity( spaceOff + d1 * n ) * width;
				} else {
					pixelOff	= space.vSpaceToUnity( spaceOff + d1 * n ) * width;
				}
				shpTicks.moveTo( (float) pixelOff, height - 4 );
				shpTicks.lineTo( (float) pixelOff, height - 2 );
			}
		}
        */
	}

	private def triggerRedisplay() {
		doRecalc = true
		if( host.isDefined ) {
			host.get.update( this )
		} else if( isVisible ) {
			repaint()
		}
	}

	// -------------- Disposable interface --------------

	def dispose() {
		labels      = null
		shpTicks.reset()
		img.flush()
	}

    private class Label( val name: String, val pos: Int )
}