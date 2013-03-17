/*
 *  ConvolutionDiffusionGUI.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.kontur
package gui

import java.awt.event.{ ActionEvent, ActionListener }
import java.io.{ File, FilenameFilter, IOException }
import java.util.Locale
import java.text.MessageFormat
import javax.swing.{ GroupLayout, JLabel, JPanel, JTextField, SwingConstants }
import SwingConstants._
import de.sciss.dsp.Util.nextPowerOfTwo
import de.sciss.synth.io.AudioFile
import session.{ Diffusion, DiffusionEditor, DiffusionFactory, ConvolutionDiffusion, Renamable, Session }
import util.Model
import legacy.{PathListener, PathEvent, ParamSpace, AbstractCompoundEdit}
import desktop.impl.BasicParamField

object ConvolutionDiffusionGUI extends DiffusionGUIFactory {
   type T = ConvolutionDiffusionGUI

   def createPanel( doc: Session ) = {
      val gui = new ConvolutionDiffusionGUI( true )
      gui.setObjects( new ConvolutionDiffusion( doc ))
      gui
   }

   def fromPanel( gui: T ) : Option[ Diffusion ] = gui.objects.headOption
   def factory : DiffusionFactory = ConvolutionDiffusion
}

class ConvolutionDiffusionGUI( autoApply: Boolean )
extends JPanel with desktop.impl.DynamicComponentImpl with FilenameFilter {

   private var objects: List[ Diffusion ] = Nil
   private val ggName         = new JTextField( 16 )
   private val ggPath         = new PathField( legacy.PathField.TYPE_INPUTFILE | legacy.PathField.TYPE_FORMATFIELD, "Choose Impulse Response Audiofile" )
   private val ggGain         = new BasicParamField()
   private val ggDelay        = new BasicParamField()
   private val msgPtrn		   = "{0,choice,0#no channels|1#mono|2#stereo|2<{0,number,integer}-ch}, {1,number,########} frames / fft {2,number,########}, {3,number,0.###} kHz, {4,number,integer}:{5,number,00.000}"
   private val msgForm		   = new MessageFormat( msgPtrn, Locale.US )

   private val diffListener: Model.Listener = {
      // XXX the updates could be more selective
      case Renamable.NameChanged( _, _ )              => updateGadgets()
      case ConvolutionDiffusion.PathChanged( _, _ )   => updateGadgets()
   }

   private val spcAbsGain     = new ParamSpace( 0.0, Double.MaxValue, 0.0, 0, 10, 0.0, ParamSpace.ABS | ParamSpace.AMP )

   // ---- constructor ----
   {
      val layout  = new GroupLayout( this )
      layout.setAutoCreateGaps( true )
      layout.setAutoCreateContainerGaps( true )
      setLayout( layout )

      ggPath.setFilter( this )
      ggGain.addSpace( ParamSpace.spcAmpDecibels )
      ggDelay.addSpace( ParamSpace.spcTimeMillis )

      val lbName     = new JLabel( "Name:", RIGHT )
      val lbPath     = new JLabel( "Path:", RIGHT )
//    val lbFFTSize  = new JLabel( "FFT Size:", RIGHT )
      val lbGain     = new JLabel( "Gain:", RIGHT )
      val lbDelay    = new JLabel( "Delay:", RIGHT )

      List( lbName, lbPath, lbGain, lbDelay, ggName, ggPath, ggGain, ggDelay ).foreach(
            _.putClientProperty( "JComponent.sizeVariant", "small" )
      )

      ggName.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) { editRename( ggName.getText )}
      })

      ggPath.addPathListener( new PathListener {
         def pathChanged( e: PathEvent ) { editSetPath( e.getPath )}
      })

     ggGain.addListener(new BasicParamField.Listener {
       def paramValueChanged(e: BasicParamField.Event) {
         if (!e.isAdjusting) editSetGain(e.getTranslatedValue(spcAbsGain).value.toFloat)
       }

       def paramSpaceChanged(e: BasicParamField.Event) {}
     })

     ggDelay.addListener(new BasicParamField.Listener {
       def paramValueChanged(e: BasicParamField.Event) {
         if (!e.isAdjusting) editSetDelay((e.value.value / 1000).toFloat)
       }

       def paramSpaceChanged(e: BasicParamField.Event) {}
     })

     layout.setHorizontalGroup( layout.createSequentialGroup()
         .addGroup( layout.createParallelGroup()
            .addComponent( lbName )
            .addComponent( lbPath )
            .addComponent( lbGain )
            .addComponent( lbDelay )
         )
         .addGroup( layout.createParallelGroup()
            .addComponent( ggName )
            .addComponent( ggPath )
            .addComponent( ggGain )
            .addComponent( ggDelay )
         )
      )

      layout.setVerticalGroup( layout.createSequentialGroup()
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbName )
            .addComponent( ggName )
         )
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
             .addComponent( lbPath )
             .addComponent( ggPath )
         )
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
             .addComponent( lbGain )
             .addComponent( ggGain )
         )
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
             .addComponent( lbDelay )
             .addComponent( ggDelay )
         )
      )
   }

   // ---- FilenameFilter ----
   def accept( dir: File, name: String ) = {
      try {
         AudioFile.identify( new File( dir, name )).isDefined
      }
      catch { case e: IOException => false }
   }

   private def withEditor( editName: String, fun: (DiffusionEditor, AbstractCompoundEdit) => Unit ) {
      val eds = objects.filter( _.editor.isDefined ).map( _.editor.get )
      if( eds.isEmpty ) return
      val ed = eds.head
      val ce = ed.editBegin( editName )
      eds.foreach( ed2 => fun.apply( ed2, ce ))
      ed.editEnd( ce )
   }

   private def editRename( newName: String ) {
      withEditor( "editRename", (ed, ce) => ed.editRename( ce, newName ))
   }

   private def editSetPath( newPath: File ) {
      withEditor( "editSetPath", (ed, ce) => ed match {
         case cdiff: ConvolutionDiffusion => cdiff.editSetPath( ce, Some( newPath ))
         case _ =>
      })
   }

   private def editSetGain( newGain: Float ) {
      println( "new gain = " + newGain )
      withEditor( "editSetGain", (ed, ce) => ed match {
         case cdiff: ConvolutionDiffusion => cdiff.editSetGain( ce, newGain )
         case _ =>
      })
   }

   private def editSetDelay( newDelay: Float ) {
      println( "new delay = " + newDelay )
      withEditor( "editSetDelay", (ed, ce) => ed match {
         case cdiff: ConvolutionDiffusion => cdiff.editSetDelay( ce, newDelay )
         case _ =>
      })
   }

   private def collapse( s: String* ) : String = {
      if( s.isEmpty ) return ""
      val head = s.head
      val tail = s.tail
      if( tail.exists( _ != head )) "<Multiple Items>"
      else head
   }

   private def updateGadgets() {
      val enabled  = !objects.isEmpty
      val editable = enabled && objects.forall( _.editor.isDefined )
      ggName.setText( collapse( objects.map( _.name ): _* ))
      ggName.setEnabled( enabled )
      ggName.setEditable( editable )

      objects match {
         case List( cdiff: ConvolutionDiffusion ) => {
            val path = cdiff.path
            ggPath.setEditable( true )
            ggPath.setPath( path getOrElse new File( "" ))
            ggPath.setFormat( path.map( p => {
               val millis  = (cdiff.numFrames / cdiff.sampleRate * 1000 + 0.5).toInt
               val fftSize = nextPowerOfTwo( cdiff.numFrames.toInt ) << 1
               msgForm.format( Array( int2Integer( cdiff.numOutputChannels ),
                  long2Long( cdiff.numFrames ), int2Integer( fftSize ),
                  float2Float( (cdiff.sampleRate / 1000).toFloat ),
                  int2Integer( millis / 60000 ),
                  double2Double( (millis % 60000).toDouble / 1000 ))) // stupid shit XXX
            }) getOrElse "<No Path Chosen>", true )
         }
         case _ => {
            ggPath.setEditable( false )
            ggPath.setPath( new File( "" ))
            ggPath.setFormat( "", true )
         }
      }
   }

  def setObjects(diff: Diffusion*) {
    objects.foreach(_.removeListener(diffListener))
    objects = diff.toList
    if (isListening) {
      updateGadgets()
      objects.foreach(_.addListener(diffListener))
    }
  }

  protected def componentShown() {
    updateGadgets()
    objects.foreach(_.addListener(diffListener))
  }

  protected def componentHidden() {
    objects.foreach(_.removeListener(diffListener))
  }
}