/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 03.02.2010
 * Time: 01:02:09
 */
package de.sciss.kontur.gui;

import java.awt.{ BorderLayout, Font }
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent }
import java.io.{ BufferedReader, File, InputStreamReader,
                 PipedInputStream, PipedOutputStream, PrintStream, PrintWriter }
import javax.swing.{ AbstractAction, JComponent, JEditorPane, JScrollPane, KeyStroke }
import de.sciss.app.{ AbstractWindow }
import de.sciss.common.{ BasicMenuFactory }
import de.sciss.kontur.{ Main }
import de.sciss.kontur.sc.{ SuperColliderClient, SuperColliderPlayer, SynthContext }
import de.sciss.kontur.session.{ Session }
import de.sciss.gui.{ LogTextArea }
import de.sciss.tint.sc.{ Server }

import jsyntaxpane.{ DefaultSyntaxKit, SyntaxDocument }
import scala.tools.nsc.{ Interpreter, InterpreterResults => IR, Settings }

class ScalaInterpreterFrame
extends AppWindow( AbstractWindow.REGULAR ) {

   val settings = {
      val set = new Settings()
      set.classpath.value += File.pathSeparator + System.getProperty( "java.class.path" )
      set
   }

   val interpreter = {
      val in = new Interpreter( settings /*, out*/ ) {
         override protected def parentClassLoader = classOf[ ScalaInterpreterFrame ].getClassLoader
      }
      in.setContextClassLoader()

      // useful bindings
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
      
      in
   }

   // ---- constructor ----
   {
      setTitle( getResourceString( "frameScalaInterpreter" ))
      val cp = getContentPane

//      val ggSplit = new JSplitPane( JSplitPane.VERTICAL_SPLIT )
//      val ggInput = new JTextArea( 6, 40 )
//      val ggOutput = new LogTextArea()
//      ggSplit.setTopComponent( ggInput )
//      ggSplit.setBottomComponent( ggOutput )
//      cp.add( ggSplit, BorderLayout.CENTER )

//      val w    = new PrintWriter( ggOutput.getLogStream )
//      val pipe = new PipedOutputStream()
//      val r    = new BufferedReader( new InputStreamReader( new PipedInputStream( pipe )))
//      val ps   = new PrintStream( pipe )

      DefaultSyntaxKit.initKit()
//      DefaultSyntaxKit.DEFAULT_FONT = new Font( "Menlo", Font.PLAIN, 10 )

      val ggEditor = new JEditorPane()
      val ggScroll = new JScrollPane( ggEditor )

      ggEditor.setContentType( "text/scala" )
      ggEditor.setText( "// type scala code here.\n// cmd+e executes selected text\n// or current line.\n" )
      ggEditor.setFont( new Font( "Menlo", Font.PLAIN, 12 ))
      val doc = ggEditor.getDocument().asInstanceOf[ SyntaxDocument ]

      val imap = ggEditor.getInputMap( JComponent.WHEN_FOCUSED )
      val amap = ggEditor.getActionMap()
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_E, BasicMenuFactory.MENU_SHORTCUT ), "exec" )
      amap.put( "exec", new AbstractAction {
         def actionPerformed( e: ActionEvent ) {
            var txt = ggEditor.getSelectedText
            if( txt == null ) txt = doc.getLineAt( ggEditor.getCaretPosition )
            if( txt != null ) interpret( txt )
         }
      })

      cp.add( ggScroll, BorderLayout.CENTER )

      init()
      setVisible( true )
      toFront()
   }

   override protected def autoUpdatePrefs = true
   override protected def alwaysPackSize  = false

   def interpret( code: String ) {
      val result = interpreter.interpret( code )
      match {
//       case IR.Error       => None
//       case IR.Success     => Some(code)
         case IR.Incomplete  => {
            println( "! Code incomplete !" )
         }
         case _ =>
      }
    }
}
