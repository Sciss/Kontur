/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 03.02.2010
 * Time: 01:02:09
 */
package de.sciss.kontur.gui;

import java.awt.{ BorderLayout }
import java.awt.event.{ ActionEvent, KeyEvent }
import java.io.{ BufferedReader, BufferedWriter, InputStreamReader,
                 OutputStreamWriter, PipedInputStream, PipedOutputStream, PrintStream, PrintWriter }
import javax.swing.{ AbstractAction, JComponent, JSplitPane, JTextArea, KeyStroke }
import de.sciss.app.{ AbstractWindow }
import de.sciss.common.{ BasicMenuFactory }
import de.sciss.gui.{ LogTextArea }

import scala.tools.nsc.{ InterpreterLoop, Settings }

class ScalaInterpreterFrame
extends AppWindow( AbstractWindow.REGULAR ) {

   // ---- constructor ----
   {
      val cp = getContentPane

      val ggSplit = new JSplitPane( JSplitPane.VERTICAL_SPLIT )
      val ggInput = new JTextArea( 6, 40 )
      val ggOutput = new LogTextArea()
      ggSplit.setTopComponent( ggInput )
      ggSplit.setBottomComponent( ggOutput )
      cp.add( ggSplit, BorderLayout.CENTER )

      val w    = new PrintWriter( ggOutput.getLogStream )
      val pipe = new PipedOutputStream()
      val r    = new BufferedReader( new InputStreamReader( new PipedInputStream( pipe )))
      val ps   = new PrintStream( pipe )

      val imap = ggInput.getInputMap( JComponent.WHEN_FOCUSED )
      val amap = ggInput.getActionMap()
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_E, BasicMenuFactory.MENU_SHORTCUT ), "exec" )
      amap.put( "exec", new AbstractAction {
         def actionPerformed( e: ActionEvent ) {
            val txt = ggInput.getSelectedText()
            if( txt != null ) {
println( "DANG!" )
               ps.print( txt )
               ps.flush()
            }
         }
      })

      val repl       = new InterpreterLoop( r, w )
      val settings   = new Settings()
// Makes system hang:
//      settings.classpath.value = System.getProperty( "java.class.path" )

      init()
      setVisible( true )
      toFront()

      repl.main( settings )
   }

//   override protected def alwaysPackSize() = false
}
