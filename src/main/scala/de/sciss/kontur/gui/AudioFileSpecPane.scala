/*
 *  AudioFileSpecPane.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package gui

import legacy.SpringPanel
import de.sciss.synth.io.{AudioFileSpec, AudioFileType, SampleFormat}
import javax.swing.{JComponent, JComboBox}
import desktop.impl.PathField
import java.awt.Component
import java.io.File

object AudioFileSpecPane {
 	private final val sampleFormatItems = Vector[SampleFormat](
    SampleFormat.Int16, SampleFormat.Int24, SampleFormat.Int32, SampleFormat.Float
  )

// 	private static final StringItem[] RATE_ITEMS = {
// 		new StringItem( new Param( 32000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "32 kHz" ),
// 		new StringItem( new Param( 44100, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "44.1 kHz" ),
// 		new StringItem( new Param( 48000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "48 kHz" ),
// 		new StringItem( new Param( 88200, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "88.2 kHz" ),
// 		new StringItem( new Param( 96000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "96 kHz" )
// 	};
// 	private static final int			DEFAULT_ENCODING	= 1;		// default int24
// 	private static final int			DEFAULT_RATE		= 1;		// default 44.1 kHz
// 	private static final Param			DEFAULT_GAIN		= new Param(
// 		0.0, ParamSpace.AMP | ParamSpace.REL | ParamSpace.DECIBEL );
// 	private static final boolean		DEFAULT_NORMALIZE	= true;		// default normalization
}
/**
*  A multi component panel
*  that provides gadgets for
*  specification of the output
*  format of an audio file,
*  such as file format, resolution
*  or sample rate. It implements
*  the <code>PreferenceNodeSync</code>
*  interface, allowing the automatic
*  saving and recalling of its gadget's
*  values from/to preferences.
*
*  @todo   sample rates should be user adjustable through a
*			JComboBox with an editable field. this to-do has
*			low priority since meloncillo is not really
*			interested in audio files.
*/
class AudioFileSpecPane extends SpringPanel {
  import AudioFileSpecPane._

///**
//	 *  Constructor-Flag : create file type gadget
//	 */
//	public static final int FORMAT		= 1 << 0;
//	/**
//	 *  Constructor-Flag : create sample encoding gadget
//	 */
//	public static final int ENCODING	= 1 << 1;
//	/**
//	 *  Constructor-Flag : create sample rate gadget
//	 */
//	public static final int RATE		= 1 << 2;
//	/**
//	 *  Constructor-Flag : create gain gadget
//	 */
//	public static final int GAIN		= 1 << 4;
//	/**
//	 *  Constructor-Flag : create normalize option
//	 */
//	public static final int NORMALIZE	= 1 << 5;
//	/**
//	 *  Constructor-Flag : create channel num gadgets
//	 */
//	public static final int CHANNELS	= 1 << 6;
//
//	/**
//	 *  Constructor-Flag : conventient combination
//	 *  of <code>FORMAT</code>, <code>ENCODING</code> and <code>RATE</code>.
//	 */
//	public static final int FORMAT_ENCODING_RATE	= FORMAT | ENCODING | RATE;
//	/**
//	 *  Constructor-Flag : conventient combination
//	 *  of <code>FORMAT</code>, <code>ENCODING</code>, <code>RATE</code>, <code>CHANNELS</code>.
//	 */
//	public static final int NEW_FILE_FLAGS			= FORMAT | ENCODING | RATE | CHANNELS;
//	/**
//	 *  Constructor-Flag : conventient combination
//	 *  of <code>GAIN</code> and <code>NORMALIZE</code>.
//	 */
//	public static final int GAIN_NORMALIZE			= GAIN | NORMALIZE;


//	// prefs keys
//	private static final String		KEY_FORMAT		= "format";
//	private static final String		KEY_ENCODING	= "encoding";
//	private static final String		KEY_RATE		= "rate";
//	private static final String		KEY_GAIN		= "gain";
//	private static final String		KEY_NORMALIZE	= "normalize";
//	private static final String		KEY_CHANNELS	= "channels";

//	private var lbGainType      = Option.empty[JLabel]
//	private var ggNormalize     = Option.empty[JCheckBox]
	private var ggFileType	    = Option.empty[JComboBox]
	private var ggSampleFormat	= Option.empty[JComboBox]
//	private var ggRate		      = Option.empty[BasicParamField]
//	private var ggRateCombo	    = Option.empty[JComboBox]
//	private var ggGain		      = Option.empty[BasicParamField]
//	private var pChan		        = Option.empty[JToolBar]
//	private var chanGroup	      = Option.empty[ButtonGroup]
//	private var ggMono		      = Option.empty[JToggleButton]
//	private var ggStereo	      = Option.empty[JToggleButton]
//	private var ggMulti		      = Option.empty[JToggleButton]
//	private var ggChanNum	      = Option.empty[BasicParamField]

//	private var ggPaths		        = Vector.empty		// lazy; set with automaticFileSuffix method
//
//	private var					gainTypeWidth	= 0

	private var pEnc		= Option.empty[SpringPanel]
//	private var pGain   = Option.empty[SpringPanel]

  private def pEncRemoval(opt: Option[Component]): Unit =
    opt.foreach { gg =>
      val p = gg.getParent
      assert(Some(p) == pEnc)
      p.remove(gg)
      if (p.getComponentCount == 0) {
        remove(p)
        pEnc = None
      }
    }

  private def pEncAddition(c: JComponent, x: Int): Unit = {
    val p = pEnc.getOrElse {
      val res = new SpringPanel(4, 2, 4, 2)
      gridAdd(res, 0, 0, -1, 1)
      pEnc = Some(res)
      res
    }
    p.gridAdd(c, x, 0)
  }

  def fileType: Boolean = ggFileType.isDefined
  def fileType_=(value: Boolean): Unit = {
    if (ggFileType.isDefined == value) return
    pEncRemoval(ggFileType)
    if (value) {
      val gg = new JComboBox()
      AudioFileType.writable.foreach(gg.addItem)
//      gg.setSelectedIndex(...)
      pEncAddition(gg, 0)
      ggFileType = Some(gg)
    }
  }
  
  def sampleFormat: Boolean = ggSampleFormat.isDefined
  def sampleFormat_=(value: Boolean): Unit = {
    if (ggSampleFormat.isDefined == value) return
    pEncRemoval(ggSampleFormat)
    if (value) {
      val gg = new JComboBox()
      sampleFormatItems.foreach(gg.addItem)
//      gg.setSelectedIndex( DEFAULT_ENCODING )
      pEncAddition(gg, 1)
      ggSampleFormat = Some(gg)
    }
  }

  def toSpec: AudioFileSpec = toSpec(AudioFileSpec(numChannels = 1, sampleRate = 44100))

  def toSpec(in: AudioFileSpec): AudioFileSpec = {
    val out1 = ggFileType.map(_.getSelectedItem) match {
        case Some(tpe: AudioFileType) => in.copy(fileType = tpe)
        case _ => in
    }

    ggSampleFormat.map(_.getSelectedItem) match {
      case Some(fmt: SampleFormat) => out1.copy(sampleFormat = fmt)
      case _ => out1
    }
  }

//	public void setFlags( int flags )
//	{
//		final int		flagsAdded		= flags & ~this.flags;
//		final int		flagsRemoved	= this.flags & ~flags;
//
//		StringItem[]	items;
//
//		if( (flagsRemoved & FORMAT_ENCODING_RATE) != 0 ) {
//			if( (flagsRemoved & RATE) != 0 ) {
//				if( prefs != null ) ggRate.setPreferences( null, null );
//				pEnc.remove( ggRateCombo );
//				ggRateCombo	= null;
//				ggRate		= null;
//			}
//			if( pEnc.getComponentCount() == 0 ) {
//				remove( pEnc );
//				pEnc = null;
//			}
//		}
//
//		if( (flagsRemoved & CHANNELS) != 0 ) {
//			ggMulti.removeItemListener( this );
//			if( prefs != null ) ggChanNum.setPreferences( null, null );
//			remove( pChan );
//			pChan.remove( ggMono );
//			pChan.remove( ggStereo );
//			pChan.remove( ggMulti );
//			pChan.remove( ggChanNum );
//			chanGroup.remove( ggMono );
//			chanGroup.remove( ggStereo );
//			chanGroup.remove( ggMulti );
//			ggMono		= null;
//			ggStereo	= null;
//			ggMulti		= null;
//			ggChanNum	= null;
//			pChan		= null;
//			chanGroup	= null;
//		}
//
//		if( (flagsRemoved & GAIN_NORMALIZE) != 0 ) {
//			if( (flagsRemoved & GAIN) != 0 ) {
//				if( prefs != null ) ggGain.setPreferences( null, null );
//				pGain.remove( ggGain );
//				ggGain	= null;
//			}
//			if( (flagsRemoved & NORMALIZE) != 0 ) {
//				ggNormalize.removeItemListener( this );
//				if( prefs != null ) ggNormalize.setPreferences( null, null );
//				pGain.remove( ggNormalize );
//				ggNormalize	= null;
//			}
//			if( pGain.getComponentCount() == 0 ) {
//				remove( pGain );
//				pGain = null;
//			}
//		}
//
//		if( (flagsAdded & FORMAT_ENCODING_RATE) != 0 ) {
//			if( pEnc == null ) {
//				pEnc = new SpringPanel( 4, 2, 4, 2 );
//				gridAdd( pEnc, 0, 0, -1, 1 );
//			}
//			if( (flagsAdded & FORMAT) != 0 ) {
//				ggFileType	= new PrefComboBox();
//				items		= AudioFileDescr.getFormatItems();
//				for( int i = 0; i < items.length; i++ ) {
//					ggFileType.addItem( items[ i ]);
//				}
//				ggFileType.setSelectedIndex( 0 );
//				if( prefs != null ) ggFileType.setPreferences( prefs, KEY_FORMAT );
//				ggFileType.addItemListener( this );
//				pEnc.gridAdd( ggFileType, 0, 0 );
//			}
//			if( (flagsAdded & ENCODING) != 0 ) {
//				ggSampleFormat	= new PrefComboBox();
//				items		= ENCODING_ITEMS;
//				for( int i = 0; i < items.length; i++ ) {
//					ggSampleFormat.addItem( items[ i ]);
//				}
//				ggSampleFormat.setSelectedIndex( DEFAULT_ENCODING );
//				if( prefs != null ) ggSampleFormat.setPreferences( prefs, KEY_ENCODING );
//				pEnc.gridAdd( ggSampleFormat, 1, 0 );
//			}
//			if( (flagsAdded & RATE) != 0 ) {
//				ggRateCombo = new JComboBox();
//				ggRate		= new PrefParamField();
//				ggRate.addSpace( ParamSpace.spcFreqHertz );
//				items		= RATE_ITEMS;
//				for( int i = 0; i < items.length; i++ ) {
//					ggRateCombo.addItem( items[ i ]);
//				}
////				ggRate.setBackground( Color.white );
////final javax.swing.plaf.basic.BasicComboBoxRenderer bcbr = new javax.swing.plaf.basic.BasicComboBoxRenderer();
//ggRate.setBorder( new ComboBoxEditorBorder() );
////ggRate.setMaximumSize( ggRate.getPreferredSize() );
//				ggRateCombo.setEditor( ggRate );
//				ggRateCombo.setEditable( true );
//				ggRateCombo.setSelectedIndex( DEFAULT_RATE );
//				if( prefs != null ) ggRate.setPreferences( prefs, KEY_RATE );
//				pEnc.gridAdd( ggRateCombo, 2, 0 );
//			}
//			pEnc.makeCompactGrid();
//		}
//
//		if( (flagsAdded & CHANNELS) != 0 ) {
//			pChan		= new JToolBar();
//			pChan.setFloatable( false );
//			chanGroup	= new ButtonGroup();
//			ggMono		= new JToggleButton( getResourceString( "buttonMono" ));
//			ggMono.setSelected( true );
//			chanGroup.add( ggMono );
//			pChan.add( ggMono );
//			ggStereo	= new JToggleButton( getResourceString( "buttonStereo" ));
//			chanGroup.add( ggStereo );
//			pChan.add( ggStereo );
//			ggChanNum	= new PrefParamField();
//			ggChanNum.addSpace( new ParamSpace( 0, 0xFFFF, 1, 0, 0, 4, ParamSpace.NONE ));
//			ggChanNum.setEnabled( false );
//			ggMulti		= new JToggleButton( getResourceString( "buttonMultichannel" ));
//			if( prefs != null ) ggChanNum.setPreferences( prefs, KEY_CHANNELS );
//			ggMulti.addItemListener( this );
//			chanGroup.add( ggMulti );
//			pChan.add( ggMulti );
//			pChan.add( ggChanNum );
//
//			gridAdd( pChan, 0, 1, -1, 1 );
//		}
//
//		if( (flagsAdded & GAIN_NORMALIZE) != 0 ) {
//			if( pGain == null ) {
//				pGain = new SpringPanel( 4, 2, 4, 2 );
//				gridAdd( pGain, 0, 2, -1, 1 );
//			}
//			if( (flagsAdded & GAIN) != 0 ) {
//				ggGain  = new PrefParamField();
//				ggGain.addSpace( ParamSpace.spcAmpDecibels );
//				ggGain.addSpace( ParamSpace.spcAmpPercentF );
//				ggGain.setValue( DEFAULT_GAIN );
//				pGain.gridAdd( ggGain, 0, 0 );
//				lbGainType  = new JLabel();
//				setGainLabel( true );
//				final int w1 = lbGainType.getPreferredSize().width;
//				setGainLabel( false );
//				final int w2 = lbGainType.getPreferredSize().width;
//				gainTypeWidth = Math.max( w1, w2 );
//				GUIUtil.constrainWidth( lbGainType, gainTypeWidth );
//				pGain.gridAdd( lbGainType, 1, 0 );
//			}
//			if( (flagsAdded & NORMALIZE) != 0 ) {
//				ggNormalize = new PrefCheckBox( getResourceString( "labelNormalize" ));
//				ggNormalize.setSelected( DEFAULT_NORMALIZE );
//				ggNormalize.addItemListener( this );
//				pGain.gridAdd( ggNormalize, 2, 0 );
//				if( ggGain != null ) setGainLabel();
//			}
//			pGain.makeCompactGrid();
//		}
//
//		if( flags != this.flags ) {
//			this.flags = flags;
//			makeCompactGrid();
//		}
//	}

//	/**
//	 *  Copy the internal state of
//	 *  the <code>AudioFileFormatPane</code> into the
//	 *  <code>AudioFileDescr</code> object. This will
//	 *  fill in the <code>type</code>,
//	 *  <code>bitsPerSample</code>, <code>sampleFormat</code>,
//	 *  <code>rate</code> and <code>channels</code> fields,
//	 *  provided that the pane was specified
//	 *  to contain corresponding gadgets
//	 *
//	 *  @param  target  the description whose
//	 *					format values are to be overwritten.
//	 */
//	public void toDescr( AudioFileDescr target )
//	{
//		if( ggFileType != null ) {
//			target.type = ggFileType.getSelectedIndex();
//		}
//		if( ggSampleFormat != null ) {
//			target.bitsPerSample	= BITSPERSMP[ ggSampleFormat.getSelectedIndex() ];
//			target.sampleFormat		= ENCODINGS[ ggSampleFormat.getSelectedIndex() ];
//		}
//		if( ggRate != null ) {
//			target.rate				= ggRate.getValue().val;
//		}
//		if( chanGroup != null ) {
//			if( ggMono.isSelected() ) {
//				target.channels		= 1;
//			} else if( ggStereo.isSelected() ) {
//				target.channels		= 2;
//			} else {
//				target.channels		= (int) ggChanNum.getValue().val;
//			}
//		}
//	}

//	/**
//	 *  Return the value of the
//	 *  gain gadget (in decibels).
//	 *  If the pane was not created without
//	 *  a dedicated gain gadget, this
//	 *  method returns 0.0.
//	 *
//	 *  @return		the pane's gain setting
//	 *				or 0.0 if no gain gadget exists.
//	 */
//	public double getGain()
//	{
//		if( ggGain != null ) {
//			return ggGain.getTranslator().translate( ggGain.getValue(), ParamSpace.spcAmpDecibels ).val;
//		} else {
//			return 0.0;
//		}
//	}
//
//	/**
//	 *	Returns a text representation of the encoding
//	 *	(integer/floating point and bit depth). This String
//	 *	is compatible with SuperCollider's <code>/b_write</code> command.
//	 *
//	 *	@return	encoding string, such as <code>&quot;int16&quot;</code>
//	 */
//	public String getEncodingString()
//	{
//		return ((StringItem) ggSampleFormat.getSelectedItem()).getKey();
//	}
//
//	/**
//	 *	Returns a text representation of the file format type.
//	 *	This String is compatible with SuperCollider's <code>/b_write</code> command.
//	 *
//	 *	@return	file format string, such as <code>&quot;aiff&quot;</code>
//	 */
//	public String getFormatString()
//	{
//		return ((StringItem) ggFileType.getSelectedItem()).getKey();
//	}
//
//	/**
//	 *  Return the state of the 'normalized'
//	 *  checkbox of the pane.
//	 *
//	 *  @return		<code>true</code> if the pane's
//	 *				'normalized' checkbox was checked,
//	 *				<code>false</code> otherwise or if no checkbox
//	 *				gadget exists.
//	 */
//	public boolean getNormalized()
//	{
//		if( ggNormalize != null ) {
//			return ggNormalize.isSelected();
//		} else {
//			return false;
//		}
//	}

  private var _linkedPathField = Option.empty[PathField]
  def linkedPathField = _linkedPathField

	/**
	 *	Registers a <code>PathField</code> to
	 *	be updated upon format switches.
	 *	When the user selects a different format,
	 *	the path's suffix will be updated accordingly.
	 *
	 *	@param	value	the path field to update
	 *					or `None` to stop
	 *					updating.
	 */
	def linkedPathField_=(value: Option[PathField]): Unit = {
		_linkedPathField = value
		updateFileSuffix()
	}

//	/**
//	 *  Copy a sound format from the given
//	 *  <code>AudioFileDescr</code> to the
//	 *  corresponding gadgets in the pane.
//	 */
//	public void fromDescr( AudioFileDescr source )
//	{
//		if( ggFileType != null ) {
//			ggFileType.setSelectedIndex( source.type );
//		}
//		if( ggSampleFormat != null ) {
//			for( int i = 0; i < ENCODINGS.length; i++ ) {
//				if( (BITSPERSMP[ i ] == source.bitsPerSample) &&
//					(ENCODINGS[ i ] == source.sampleFormat) ) {
//
//					ggSampleFormat.setSelectedIndex( i );
//					break;
//				}
//			}
//		}
//		if( ggRate != null ) {
////			for( int i = 0; i < RATES.length; i++ ) {
////				if( RATES[ i ] == source.rate ) {
////					ggRate.setSelectedIndex( i );
////					break;
////				}
////			}
//			ggRate.setValue( new Param( source.rate, ParamSpace.spcFreqHertz.unit ));
//		}
//		if( chanGroup != null ) {
//			switch( source.channels ) {
//			case 1:
//				chanGroup.setSelected( ggMono.getModel(), true );
//				ggChanNum.setEnabled( false );
//				break;
//			case 2:
//				chanGroup.setSelected( ggStereo.getModel(), true );
//				ggChanNum.setEnabled( false );
//				break;
//			default:
//				chanGroup.setSelected( ggMulti.getModel(), true );
//				ggChanNum.setEnabled( true );
//				break;
//			}
//		}
//	}

//	// update the gain's label when the normalize checkbox is toggled
//	private void setGainLabel()
//	{
//		setGainLabel( (ggNormalize != null) && ggNormalize.isSelected() );
//		GUIUtil.constrainWidth( lbGainType, gainTypeWidth );
//	}
//
//	private void setGainLabel( boolean normalize )
//	{
//		lbGainType.setText( getResourceString( normalize ? "labelPeak" : "labelGain" ));
//	}

	// synchronizes a path field's path extension
	// with the selected encoding
	private def updateFileSuffix(): Unit =
    ggFileType.foreach { ggSource =>
      ggSource.getSelectedItem match {
        case tpe: AudioFileType =>
          _linkedPathField.foreach { ggTarget =>
            import io.Implicits._
            val file    = ggTarget.file
            val newFile = file.updateSuffix(tpe.extension)
            if (newFile != file) {
              ggTarget.file = newFile
            }
          }
        case _ =>
      }
    }

//	// we're listening to the normalize checkbox + format combo + multi-channel
//	public void itemStateChanged( ItemEvent e )
//	{
//		if( e.getSource() == ggNormalize ) {
//			setGainLabel();
//		} else if( e.getSource() == ggFileType ) {
//			updateFileSuffix();
//		} else if( e.getSource() == ggMulti ) {
//			final boolean isMulti = ggMulti.isSelected();
//			ggChanNum.setEnabled( isMulti );
//			if( isMulti ) ggChanNum.requestFocusInWindow(); // focusNumber();
//		}
//	}
}
