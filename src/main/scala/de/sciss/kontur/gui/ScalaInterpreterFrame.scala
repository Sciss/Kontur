/*
 *  ScalaInterpreterFrame.scala
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

import java.io.{File, FileInputStream}
import de.sciss.app.{AbstractWindow, AbstractCompoundEdit}
import de.sciss.kontur.sc.{ SuperColliderClient, SuperColliderPlayer, SynthContext }
import de.sciss.kontur.session.Session
import de.sciss.kontur.edit.Editor
import de.sciss.synth.Server
import tools.nsc.interpreter.NamedParam
import de.sciss.common.BasicApplication
import de.sciss.scalainterpreter.{SplitPane, CodePane, Interpreter, InterpreterPane}
import language.implicitConversions

object ScalaInterpreterFrame {
   final class ProvideEditing private[ScalaInterpreterFrame] ( e: Editor ) {
      def edit( name: String )( fun: AbstractCompoundEdit => Unit ) : Unit = defer {
         val ce = e.editBegin( name )
         var succ = false
         try {
            fun( ce )
            succ = true
         } finally {
            if( succ ) e.editEnd( ce ) else e.editCancel( ce )
         }
      }
   }

   def defer(thunk: => Unit): Unit = {
      java.awt.EventQueue.invokeLater(
         new Runnable() {
            def run(): Unit = thunk
         }
      )
   }

   class REPLSupport( val app: BasicApplication ) {
      def doc : Session             = app.getDocumentHandler.getActiveDocument.asInstanceOf[ Session ]
      def sc : SuperColliderClient  = SuperColliderClient.instance
      def scp : SuperColliderPlayer = { val d = doc; (if( d != null ) sc.getPlayer( d ) else None).orNull }
      def con : SynthContext        = { val p = scp; (if( p != null ) p.context else None).orNull }
      def s : Server                = sc.server.orNull

//      def Span( start: Long, stop: Long ) = Span( start, stop )

      def defer(thunk: => Unit): Unit = ScalaInterpreterFrame.defer( thunk )

      implicit def provideEditing( e: Editor ) : ProvideEditing = new ProvideEditing( e )
   }
}
class ScalaInterpreterFrame
extends AppWindow( AbstractWindow.REGULAR ) {
   import ScalaInterpreterFrame._

   // ---- constructor ----
   {
      setTitle( getResourceString( "frameScalaInterpreter" ))
      val cp = getContentPane

      val ipc  = InterpreterPane.Config()
      val ic   = Interpreter.Config()
      val cpc  = CodePane.Config()

//      ip.initialCode = Some(
//"""
//         import de.sciss.kontur.session._
//         import math._
//"""
//      )

      val replFile = new File( "interpreter.txt" )
      if( replFile.exists() ) try {
         val fin = new FileInputStream( replFile )
         val i   = fin.available()
         val arr = new Array[ Byte ]( i )
         fin.read( arr )
         val txt = new String( arr, "UTF-8" )
         cpc.text += txt

      } catch {
         case e: Throwable => e.printStackTrace()
      }

      val support = new REPLSupport( app )
      ic.bindings = List(
//         NamedParam( "app", app ),
         NamedParam( "replsupport", support )
//         NamedParam( "sc", SuperColliderClient.instance )
      )

      ic.imports = List(
         "de.sciss.kontur.session._",
         "math._",
         "replsupport._"
      )

//      ip.bindingsCreator = Some( (in: Interpreter ) => {
//         in.bind( "app",  classOf[ Main ].getName, app )
//         in.bind( "doc",  classOf[ Session ].getName, app.getDocumentHandler.getActiveDocument )
//         val doc = app.getDocumentHandler.getActiveDocument.asInstanceOf[ Session ]
//         val sc  = SuperColliderClient.instance
//         in.bind( "sc",   classOf[ SuperColliderClient ].getName, sc )
//         val scp = (if( doc != null ) sc.getPlayer( doc ) else None) orNull;
//         in.bind( "scp", classOf[ SuperColliderPlayer ].getName, scp )
//         val con = (if( scp != null ) scp.context else None) orNull;
//         in.bind( "con", classOf[ SynthContext ].getName, con )
//         val s = sc.server orNull; // if( con != null ) con.server else null
//         in.bind( "s", classOf[ Server ].getName, s )
//      })

//      val ip   = InterpreterPane( ipc, ic )

      val sp   = SplitPane( ipc, ic, cpc )
//      val ip   = sp.interpreter

//      val lp   = new LogPane()
//      lp.init()
//      ip.out = Some( lp.writer )
//      Console.setOut( lp.outputStream )
//      Console.setErr( lp.outputStream )
//      System.setErr( new PrintStream( lp.outputStream ))

//      ip.init()

//      val sp = new JSplitPane( SwingConstants.HORIZONTAL )
//      sp.setTopComponent( ip )
//      sp.setBottomComponent( lp )
      cp.add( sp.component )
//      val b = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
//      setSize( b.width / 2, b.height * 7 / 8 )
//      sp.setDividerLocation( b.height * 2 / 3 )
//      setLocationRelativeTo( null )

      init()
//      sp.setDividerLocation( cp.getHeight * 2 / 3 )
      setVisible( true )
      toFront()
   }

   override protected def autoUpdatePrefs = true
   override protected def alwaysPackSize  = false
}
