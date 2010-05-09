/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 03.02.2010
 * Time: 01:02:09
 */
package de.sciss.kontur.gui;

import java.io.PrintStream
import javax.swing.{ JSplitPane, SwingConstants }
import tools.nsc.Interpreter
import de.sciss.app.AbstractWindow
import de.sciss.kontur.Main
import de.sciss.kontur.sc.{ SuperColliderClient, SuperColliderPlayer, SynthContext }
import de.sciss.kontur.session.Session
import de.sciss.synth.Server
import de.sciss.scalainterpreter.{ LogPane, ScalaInterpreterPane }

/**
 *    @version 0.11, 09-May-10
 */
class ScalaInterpreterFrame
extends AppWindow( AbstractWindow.REGULAR ) {
   // ---- constructor ----
   {
      setTitle( getResourceString( "frameScalaInterpreter" ))
      val cp = getContentPane

      val ip = new ScalaInterpreterPane
      ip.initialCode = Some(
"""
         import de.sciss.kontur.session._
         import math._
"""
      )

      ip.bindingsCreator = Some( (in: Interpreter ) => {
         in.bind( "app",  classOf[ Main ].getName, app )
         in.bind( "doc",  classOf[ Session ].getName, app.getDocumentHandler.getActiveDocument )
         val doc = app.getDocumentHandler.getActiveDocument.asInstanceOf[ Session ]
         val sc  = SuperColliderClient.instance
         in.bind( "sc",   classOf[ SuperColliderClient ].getName, sc )
         val scp = (if( doc != null ) sc.getPlayer( doc ) else None) orNull;
         in.bind( "scp", classOf[ SuperColliderPlayer ].getName, scp )
         val con = (if( scp != null ) scp.context else None) orNull;
         in.bind( "con", classOf[ SynthContext ].getName, con )
         val s = sc.server orNull; // if( con != null ) con.server else null
         in.bind( "s", classOf[ Server ].getName, s )
      })

      val lp = new LogPane
      lp.init
      ip.out = Some( lp.writer )
      Console.setOut( lp.outputStream )
      Console.setErr( lp.outputStream )
      System.setErr( new PrintStream( lp.outputStream ))

      ip.init
      val sp = new JSplitPane( SwingConstants.HORIZONTAL )
      sp.setTopComponent( ip )
      sp.setBottomComponent( lp )
      cp.add( sp )
//      val b = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
//      setSize( b.width / 2, b.height * 7 / 8 )
//      sp.setDividerLocation( b.height * 2 / 3 )
//      setLocationRelativeTo( null )

      init()
      sp.setDividerLocation( cp.getHeight * 2 / 3 )
      setVisible( true )
      toFront()
   }

   override protected def autoUpdatePrefs = true
   override protected def alwaysPackSize  = false
}
