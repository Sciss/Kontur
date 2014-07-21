/*
 *  ControlRoomFrame.scala
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

package de.sciss.kontur.gui

import javax.swing.event.{ChangeEvent, ChangeListener}
import java.awt.event.{ActionEvent, ActionListener}
import de.sciss.gui.{AbstractWindowHandler, PeakMeterPanel, MultiStateButton, SpringPanel}
import de.sciss.kontur.Kontur
import java.awt.{BorderLayout, Color}
import java.awt.geom.Point2D
import de.sciss.kontur.sc.SuperColliderClient
import javax.swing.{JPanel, WindowConstants}
import de.sciss.app.{DynamicListening, AbstractApplication, AbstractWindow}

class ControlRoomFrame extends AppWindow( AbstractWindow.SUPPORT /* PALETTE */ )
with DynamicListening {
//   private val lmm      = new PeakMeterManager( superCollider.getMeterManager() )
   private val ggVolume    = new VolumeFader()
   private val ggLimiter   = new MultiStateButton()
   private val pmg	      = new PeakMeterPanel()
   private val b1          = new SpringPanel( 2, 4, 2, 4 )
   private var isListening = false

   // ---- constructor ----
   {
//      lmm.setDynamicComponent( b1 )

      setTitle( "Control Room" ) // XXX getResource
      setResizable( false )

      ggVolume.addChangeListener( new ChangeListener() {
 			def stateChanged( e: ChangeEvent ): Unit =
             superCollider.volume = ggVolume.volumeLinear
 		})

 		ggLimiter.setNumColumns( 8 )
 		ggLimiter.addItem( "Limiter" )
 // NOTE: BUG WITH CUSTOM COMPOSITE ON WIN-XP!!!
 //		ggLimiter.addItem( "Limiter", null, new Color( 0xFF, 0xFA, 0x9D ), new Color( 0xFA, 0xE7, 0x9D ));
 ggLimiter.addItem( "Limiter", null, new Color( 0xFF, 0xFA, 0x9D ))
 		ggLimiter.addActionListener( new ActionListener {
 			def actionPerformed( e: ActionEvent ): Unit = {
// 				superCollider.setLimiter( ggLimiter.getSelectedIndex() == 1 );
             superCollider.limiter = ggLimiter.getSelectedIndex == 1
 			}
 		})
 		if( superCollider.limiter ) ggLimiter.setSelectedIndex( 1 )

 		pmg.setBorder( true )
 		pmg.setCaption( true )
// 		oCfg = superCollider.getOutputConfig()
 		rebuildMeters()

      val b2 = new JPanel( new BorderLayout() )
 		b2.add( pmg, BorderLayout.WEST )
 		b2.add( ggVolume, BorderLayout.EAST )

 		b1.gridAdd( ggLimiter, 0, 0, -1, 1 )
 		b1.gridAdd( b2, 0, 1, -1, 1 )
// 		b1.gridAdd( ggOutputConfig, 0, 2, -1, 1 )
// 		b1.gridAdd( ggAudioBox, 0, 3, -1, 1 )
 		b1.makeCompactGrid()

      val cp = getContentPane
 		cp.add( b1, BorderLayout.CENTER )

 		AbstractWindowHandler.setDeepFont( b1 )

		// ---- listeners -----

     addListener(new AbstractWindow.Adapter {
       override def windowOpened(e: AbstractWindow.Event): Unit = startListening()

       override def windowClosing(e: AbstractWindow.Event): Unit = {
         setVisible(false)
         dispose()
       }
     })

      updateVolume()

      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE ) // window listener see above!
      init()
      app.addComponent( Kontur.COMP_CTRLROOM, this )
   }

   private def superCollider = SuperColliderClient.instance

   override protected def autoUpdatePrefs = true

   override protected def restoreVisibility = true

   override protected def getPreferredLocation : Point2D = new Point2D.Float( 0.95f, 0.2f )

   override def dispose(): Unit = {
  		AbstractApplication.getApplication.removeComponent( Kontur.COMP_CTRLROOM )
//  		lmm.dispose()
//  		if( grpMeters != null ) {
//  			try {
//  				grpMeters.free()
//  			} catch { case e1: IOException =>
//  				printError( "dispose", e1 )
//  			}
//  			grpMeters = null
//  		}
  		stopListening()

  		pmg.dispose()
  		super.dispose()
  	}

  	private def updateVolume(): Unit = {
  		ggVolume.volumeLinear = superCollider.volume
  	}

//  	private def startMeters() {
//  		val s		= superCollider.getServer
//  		val so	= superCollider.getServerOptions
//  		val mg   = superCollider.getMasterGroup
//
//  		if( (s == null) || (oCfg == null) || (mg == null) ) return
//
//  		val channels = new Array[ Int ]( oCfg.mapping.length )
//  		val numOutputBusChannels = so.getNumOutputBusChannels()
//      var ch = 0; while( ch < channels.length ) {
//  			if( oCfg.mapping( ch ) < numOutputBusChannels ) {
//  				channels( ch ) = oCfg.mapping( ch )
//  			} else {
//  				channels( ch ) = -1
//  			}
//  		ch += 1 }
//
//  		try {
//  			if( grpMeters == null ) {
//  				grpMeters = Group.basicNew( s );
//  				val bndl = new OSCBundle()
//  				bndl.addPacket( grpMeters.addBeforeMsg( mg ))
//  				grpMeters.setName( "CtrlRmMeters" )
//  				NodeWatcher.newFrom( s ).register( grpMeters )
//  				s.sendBundle( bndl )
//  			}
//  			lmm.setGroup( grpMeters )
//  			lmm.setInputs( s, channels )
//  		}
//  		catch { case e1: IOException =>
//  			printError( "startMeters", e1 )
//  		}
//  	}
//
//  	private def stopMeters() {
//  		lmm.clearInputs()
//  	}

//   private def printError( name: String, t: Throwable ) {
//  		Console.err.println( name + " : " + t.getClass.getName + " : " + t.getLocalizedMessage )
//   }

  	private def rebuildMeters(): Unit = {
//  		val oCfg = superCollider.getOutputConfig
//
//  		if( oCfg != null ) {
//  			pmg.setNumChannels( oCfg.numChannels )
//  		} else {
//  			pmg.setNumChannels( 0 )
//  		}
      pmg.setNumChannels( 8 )  // XXX
      b1.makeCompactGrid()
  		pack()
//  		lmm.setView( pmg )
  	}

//  	private def registerTaskSyncs() {
//  		require( EventQueue.isDispatchThread )
//
//  		val dh = AbstractApplication.getApplication.getDocumentHandler
//  		lmm.clearTaskSyncs()
//
//      var i = 0; while( i < dh.getDocumentCount ) {
//  			dh.getDocument( i ) match {
//           case doc: Session =>
//              val p = superCollider.getPlayerForDocument( doc )
//            if( p != null ) {
//               lmm.addTaskSync( p.getOutputSync )
//               mapPlayers.put( doc, p )
//            }
//           case _ =>
//         }
//  		i += 1 }
//  	}

//  	private def unregisterTaskSyncs() {
//  		require( EventQueue.isDispatchThread )
////
////  		lmm.clearTaskSyncs()
////      mapPlayers.clear()
//  	}

   def startListening(): Unit = {
   	isListening = true
//	   superCollider.addServerListener( this )
//	   superCollider.addClientListener( this )
//	ggAudioBox.addActionListener( audioBoxListener )
//	AbstractApplication.getApplication.getDocumentHandler.addDocumentListener( this )
//	   registerTaskSyncs()
//	   startMeters()
   }

   def stopListening(): Unit = {
      isListening = false
//	   stopMeters()
//	   unregisterTaskSyncs()
//	   AbstractApplication.getApplication.getDocumentHandler.removeDocumentListener( this )
//	   ggAudioBox.removeActionListener( audioBoxListener )
//	   superCollider.removeClientListener( this )
//	   superCollider.removeServerListener( this )
   }
}