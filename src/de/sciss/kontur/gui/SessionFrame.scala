/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow, DynamicAncestorAdapter }
import de.sciss.common.{ BasicApplication, ShowWindowAction }
import de.sciss.gui.{ GUIUtil }
import de.sciss.kontur.session.{ Session }
import java.awt.{ BorderLayout }
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.{ Action, DropMode, JScrollPane, JTree, ScrollPaneConstants }
import javax.swing.tree.{ TreeNode }

class SessionFrame( doc: Session )
extends AppWindow( AbstractWindow.REGULAR ) {

  	private var writeProtected	= false
	private var wpHaveWarned	= false
    private val actionShowWindow= new ShowWindowAction( this )

    // ---- constructor ----
    {
      val cp = getContentPane
      val sessionTreeModel = new SessionTreeModel( doc )
      val ggTree = new JTree( sessionTreeModel )
      ggTree.setDropMode( DropMode.INSERT )
      ggTree.setRootVisible( true )
      ggTree.setShowsRootHandles( true )
      val ggScroll = new JScrollPane( ggTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		                         	   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )

      ggTree.addMouseListener( new MouseAdapter() {
       override def mousePressed( e: MouseEvent ) {
          val selRow  = ggTree.getRowForLocation( e.getX(), e.getY() )
          if( selRow == -1 ) return
          val selPath = ggTree.getPathForLocation( e.getX(), e.getY() )
          val node = selPath.getLastPathComponent()

          if( e.isPopupTrigger ) popup( node, e )
          else if( e.getClickCount == 2 ) doubleClick( node, e )
       }
       
        private def popup( node: AnyRef, e: MouseEvent ): Unit = node match {
           case hcm: HasContextMenu => {
               hcm.createContextMenu.foreach( root => {
                   val pop = root.createPopup( SessionFrame.this )
                   pop.show( e.getComponent(), e.getX(), e.getY() )
               })
           }
             case _ =>
         }

        private def doubleClick( node: AnyRef, e: MouseEvent ): Unit = node match {
           case hdca: HasDoubleClickAction => hdca.doubleClickAction
             case _ =>
         }
      })

      cp.add( ggScroll, BorderLayout.CENTER )
      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!

      addDynamicListening( sessionTreeModel )

      init()
  	  updateTitle
      documentUpdate

//      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

    private def documentUpdate {
      
    }

	/**
	 *  Recreates the main frame's title bar
	 *  after a sessions name changed (clear/load/save as session)
	 */
	def updateTitle {
		writeProtected	= false

//		actionRevealFile.setFile( afds.length == 0 ? null : afds[ 0 ].file );

		val name = if( doc.getName() == null ) {
			getResourceString( "frameUntitled" )
		} else {
			doc.getName()
//			try {
//				for( int i = 0; i < afds.length; i++ ) {
//					f = afds[ i ].file;
//					if( f == null ) continue;
//					writeProtected |= !f.canWrite() || ((f.getParentFile() != null) && !f.getParentFile().canWrite());
//				}
//			} catch( SecurityException e ) { /* ignored */ }
		}

//		if( writeProtected ) {
//			val icn = GUIUtil.getNoWriteIcon()
//			if( lbWriteProtected.getIcon() != icn ) {
//				lbWriteProtected.setIcon( icn )
//
//		} else if( lbWriteProtected.getIcon() != null ) {
//			lbWriteProtected.setIcon( null );
//		}

		setTitle( (if( !internalFrames ) app.getName() else "") +
                   (if( doc.isDirty() ) " - \u2022" else " - ") + name )

        actionShowWindow.putValue( Action.NAME, name )
//		actionSave.setEnabled( !writeProtected && doc.isDirty() )
		setDirty( doc.isDirty() )

//		final AudioFileInfoPalette infoBox = (AudioFileInfoPalette) app.getComponent( Main.COMP_AUDIOINFO )
//		if( infoBox != null ) infoBox.updateDocumentName( doc )

//		if( writeProtected && !wpHaveWarned && doc.isDirty() ) {
//			final JOptionPane op = new JOptionPane( getResourceString( "warnWriteProtected" ), JOptionPane.WARNING_MESSAGE )
//			BasicWindowHandler.showDialog( op, getWindow(), getResourceString( "msgDlgWarn" ))
//			wpHaveWarned = true
//		}
	}
}
