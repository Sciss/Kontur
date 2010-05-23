/*
 *  MatrixDiffusionGUI.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
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
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import java.awt.{ Color, Component, Cursor, Graphics, Insets, SystemColor }
import java.awt.event.{ ActionEvent, ActionListener, MouseEvent }
import javax.swing.{ AbstractCellEditor, Box, GroupLayout, JButton, JComponent, JLabel, JPanel, JScrollPane, JTable,
   JTextField, SwingConstants }
import SwingConstants._
import javax.swing.border.{ Border }
import javax.swing.event.{ MouseInputAdapter }
import javax.swing.table.{ AbstractTableModel, DefaultTableColumnModel, TableCellEditor, TableCellRenderer, TableColumn }
import scala.math._
import de.sciss.app.{ AbstractCompoundEdit, Document, DynamicAncestorAdapter, DynamicListening }
import de.sciss.gui.{ ParamField => PF }
import de.sciss.kontur.session.{ Diffusion, DiffusionEditor, DiffusionFactory, MatrixDiffusion, Renameable, Session }
import de.sciss.kontur.util.{ Matrix2D }
import de.sciss.util.{ Param, ParamSpace }
import de.sciss.synth.Model

object MatrixDiffusionGUI extends DiffusionGUIFactory {
   type T = MatrixDiffusionGUI

   def createPanel( doc: Session ) = {
      val gui = new MatrixDiffusionGUI( true )
      gui.setObjects( new MatrixDiffusion( doc ))
      gui
   }

   def fromPanel( gui: T ) : Option[ Diffusion ] = gui.objects.headOption
   def factory : DiffusionFactory = MatrixDiffusion
}

class MatrixDiffusionGUI( autoApply: Boolean )
extends JPanel with ObserverPage with DynamicListening {

   private var objects: List[ Diffusion ] = Nil
   private val ggName              = new JTextField( 16 )
   private val ggNumInputChannels  = new ParamField()
   private val ggNumOutputChannels = new ParamField()
   private val matrixModel         = new MatrixModel()
   private val matrixRenderer      = new MatrixCellRenderer
   private val matrixEditor        = new MatrixCellEditor
   private val tabMatrix           = new JTable( matrixModel )
   private val scrollMatrix        = new JScrollPane( tabMatrix )
   private val ggMatrix            = Box.createVerticalBox() // new JPanel()
   private val ggMatrixApply       = new JButton( "Apply" )
   private var isListening          = false

   private val diffListener: Model.Listener = {
      // XXX the updates could be more selective
      case Renameable.NameChanged( _, _ )             => updateGadgets
      case Diffusion.NumInputChannelsChanged( _, _ )  => updateGadgets
      case Diffusion.NumOutputChannelsChanged( _, _ ) => updateGadgets
      case MatrixDiffusion.MatrixChanged( _, _ )      => updateGadgets
   }

   // ---- constructor ----
   {
      val layout  = new GroupLayout( this )
      layout.setAutoCreateGaps( true )
      layout.setAutoCreateContainerGaps( true )
      setLayout( layout )

      val lbName = new JLabel( "Name:", RIGHT )
      val lbNumInputChannels = new JLabel( "Input Channels:", RIGHT )
      val lbNumOutputChannels = new JLabel( "Output Channels:", RIGHT )
      val spcChannels = new ParamSpace( 1, 0x10000, 1, 0, 0, 1 )
      ggNumInputChannels.addSpace( spcChannels )
      ggNumOutputChannels.addSpace( spcChannels )

      tabMatrix.setShowGrid( true )
      tabMatrix.setDefaultRenderer( classOf[ java.lang.Object ], matrixRenderer )
      tabMatrix.setDefaultEditor( classOf[ java.lang.Object ], matrixEditor )
      tabMatrix.setAutoResizeMode( JTable.AUTO_RESIZE_OFF )
      tabMatrix.getTableHeader().setReorderingAllowed( false )
      tabMatrix.getTableHeader().setResizingAllowed( false )
      tabMatrix.setColumnModel( new MatrixColumnModel )
      scrollMatrix.getViewport().setBackground( SystemColor.control )
      ggMatrix.add( scrollMatrix )
      ggMatrixApply.putClientProperty( "JButton.buttonType", "bevel" )
      ggMatrixApply.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) = applyMatrixChanges
      })
      val bTmp = Box.createHorizontalBox()
      bTmp.add( Box.createHorizontalGlue ); bTmp.add( ggMatrixApply )
      if( !autoApply ) ggMatrix.add( bTmp )

      List( lbName, lbNumInputChannels, lbNumOutputChannels,
         ggName, ggNumInputChannels, ggNumOutputChannels, ggMatrixApply ).foreach(
            _.putClientProperty( "JComponent.sizeVariant", "small" )
      )

      ggName.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) = editRename( ggName.getText() )
      })

      ggNumInputChannels.addListener( new PF.Listener {
         def paramValueChanged( e: PF.Event ) = if( !e.isAdjusting )
            editSetNumInputChannels( e.getValue.`val`.toInt )
         def paramSpaceChanged( e: PF.Event ) {}
      })

      ggNumOutputChannels.addListener( new PF.Listener {
         def paramValueChanged( e: PF.Event ) = if( !e.isAdjusting )
            editSetNumOutputChannels( e.getValue.`val`.toInt )
         def paramSpaceChanged( e: PF.Event ) {}
      })

      layout.setHorizontalGroup( layout.createParallelGroup()
         .addGroup( layout.createSequentialGroup()
            .addGroup( layout.createParallelGroup()
               .addComponent( lbName )
               .addComponent( lbNumInputChannels )
               .addComponent( lbNumOutputChannels )
            )
            .addGroup( layout.createParallelGroup()
               .addComponent( ggName )
               .addComponent( ggNumInputChannels )
               .addComponent( ggNumOutputChannels )
            )
         )
         .addComponent( ggMatrix )
      )

      layout.setVerticalGroup( layout.createSequentialGroup()
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbName )
            .addComponent( ggName )
         )
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
             .addComponent( lbNumInputChannels )
             .addComponent( ggNumInputChannels )
         )
         .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
             .addComponent( lbNumOutputChannels )
             .addComponent( ggNumOutputChannels )
         )
         .addComponent( ggMatrix )
      )

      new DynamicAncestorAdapter( this ).addTo( this )
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

   private def editSetNumInputChannels( newNum: Int ) {
      withEditor( "editSetNumInputChannels", (ed, ce) => ed match {
         case bdiff: MatrixDiffusion => bdiff.editSetNumInputChannels( ce, newNum )
         case _ =>
      })
   }

   private def editSetNumOutputChannels( newNum: Int ) {
      withEditor( "editSetNumOutputChannels", (ed, ce) => ed match {
         case bdiff: MatrixDiffusion => bdiff.editSetNumOutputChannels( ce, newNum )
         case _ =>
      })
   }

   private def editSetMatrix( newMatrix: Matrix2D[ Float ]) {
      objects match {
         case List( bdiff: MatrixDiffusion ) => {
            val ce = bdiff.editBegin( "editSetMatrix" )
            bdiff.editSetMatrix( ce, newMatrix )
            bdiff.editEnd( ce )
         }
         case _ =>
      }
   }

   private def collapse( s: String* ) : String = {
      if( s.isEmpty ) return ""
      val head = s.head
      val tail = s.tail
      if( tail.exists( _ != head )) "<Multiple Items>"
      else head
   }

    private def collapse( n: Double* ) : Double = {
        if( n.isEmpty ) return Double.NaN
        val head = n.head
        val tail = n.tail
        if( tail.exists( _ != head )) Double.NaN
        else head
    }

    private def updateGadgets {
        val enabled  = !objects.isEmpty
        val editable = enabled && objects.forall( _.editor.isDefined )
        ggName.setText( collapse( objects.map( _.name ): _* ))
        ggName.setEnabled( enabled )
        ggName.setEditable( editable )
        val pDummy = new Param( Double.NaN, ParamSpace.NONE )
        ggNumInputChannels.setValue( new Param( collapse(
              objects.map( _.numInputChannels.toDouble ): _* ),
            ParamSpace.NONE ))
        ggNumInputChannels.setEnabled( enabled )
        ggNumInputChannels.setEditable( editable )
        ggNumOutputChannels.setValue( new Param( collapse(
              objects.map( _.numOutputChannels.toDouble ): _* ),
            ParamSpace.NONE ))
        ggNumOutputChannels.setEnabled( enabled )
        ggNumOutputChannels.setEnabled( enabled )
        ggNumOutputChannels.setEditable( editable )

        objects match {
           case List( bdiff: MatrixDiffusion ) => {
              matrixModel.matrix   = bdiff.matrix
              matrixModel.editable = bdiff.editor.isDefined
              ggMatrixApply.setEnabled( false )
              ggMatrix.setVisible( true )
           }
           case _ => {
               ggMatrix.setVisible( false )
               matrixModel.editable = false
               matrixModel.matrix = matrixModel.emptyMatrix
           }
        }
    }

    def setObjects( diff: Diffusion* ) {
       objects.foreach( _.removeListener( diffListener ))
       objects = diff.toList
       if( isListening ) {
          updateGadgets
          objects.foreach( _.addListener( diffListener ))
       }
    }

   def startListening {
      isListening = true
      updateGadgets
      objects.foreach( _.addListener( diffListener ))
   }

   def stopListening {
      isListening = false
      objects.foreach( _.removeListener( diffListener ))
   }

    private def matrixChanged {
       ggMatrixApply.setEnabled( true )
       if( autoApply ) applyMatrixChanges 
    }

    private def applyMatrixChanges {
       ggMatrixApply.setEnabled( false )
       editSetMatrix( matrixModel.matrix )
    }

    // ---- ObserverPage interface ----+
    def component: JComponent = this
    def id = DiffusionObserverPage.id
    def title = "Diffusion" // XXX getResourceString

    def pageShown {}
    def pageHidden {}
    def documentChanged( newDoc: Document ) {}

    // ---- internal clases ----
    private class MatrixModel extends AbstractTableModel {
        val emptyMatrix = Matrix2D.fill( 0, 0, 0.0f )
        private var matrixVar = emptyMatrix
        var editable = false

        def matrix = matrixVar
        def matrix_=( newMatrix: Matrix2D[ Float ]) {
            val colsChanged = newMatrix.numColumns != matrixVar.numColumns
            matrixVar = newMatrix
            if( colsChanged ) {
                fireTableStructureChanged
            } else {
                fireTableDataChanged
            }
        }

        def getRowCount() : Int = matrixVar.numRows
        def getColumnCount() : Int = matrixVar.numColumns
        def getValueAt( row: Int, column: Int ) : AnyRef =
           MatrixCellValue( matrix( row, column ))

        override def getColumnName( column: Int ) = (column + 1).toString

        override def isCellEditable( row: Int, col: Int ) = editable

        override def setValueAt( value: AnyRef, row: Int, col: Int ) {
           value match {
              case MatrixCellValue( f ) => {
                  if( matrix( row, col ) != f ) {
                      matrix = matrix( row, col ) = f
                      matrixChanged
                  }
              }
              case _ =>
           }
        }
    }

    private class MatrixColumnModel extends DefaultTableColumnModel {
        override def addColumn( tc: TableColumn ) {
           tc.setCellRenderer( matrixRenderer )
           tc.setPreferredWidth( 24 ) // XXX
           super.addColumn( tc )
        }
    }

    private class MatrixCellRenderer extends JComponent with TableCellRenderer {
        override def getTableCellRendererComponent(
            tab: JTable, value: AnyRef, isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int ) : Component = {

            value match {
               case cell @ MatrixCellValue( _ ) => setCellValue( cell )
               case _ =>
            }
            this
        }

        def setCellValue( cell: MatrixCellValue ) {
            setBackground( IntensityColorScheme.getColor( cell.decibelsNorm() ))
            repaint()
        }

        override def paintComponent( g: Graphics ) {
            g.setColor( getBackground() )
            g.fillRect( 0, 0, getWidth(), getHeight() )
        }
    }

    private class MatrixCellEditor extends AbstractCellEditor
    with TableCellEditor with Border {
        private val comp    = new MatrixCellRenderer
        private var editVal: Option[ MatrixCellValue ] = None
        private var lastClickCol  = -1
        private var lastClickRow  = -1
        private var lastClickTime = 0L
        private var editCol = -1
        private var editRow = -1

        // ---- constructor ----
        {
           comp.setCursor( Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR ))
           val mia = new MouseInputAdapter {
              private var dragStartEvent : Option[ MouseEvent ] = None
              private var dragStartVal : Option[ MatrixCellValue ] = None
              private var dragCurrentVal : Option[ MatrixCellValue ] = None
              override def mousePressed( e: MouseEvent ) {
                 if( isDoubleClick ) {
                    lastClickTime = 0 // prevent triple click
                    editVal.foreach( cell => {
                        val newCell = cell.copy( if( cell.f == 0f || (cell.f > 0.5f && cell.f < 1f) ) 1f else 0f )
                        editVal = Some( newCell )
                        comp.setCellValue( newCell )
                        fireEditingStopped()
                    })
                 } else {
                    dragStartVal    = editVal
                    dragCurrentVal  = dragStartVal
                    dragStartEvent  = Some( e )
                 }

                 lastClickTime = System.currentTimeMillis()
                 lastClickRow  = editRow
                 lastClickCol  = editCol
              }

              private def isDoubleClick =
                 (lastClickRow == editRow) && (lastClickCol == editCol) &&
                 ((System.currentTimeMillis() - lastClickTime) < 666L)

              override def mouseDragged( e: MouseEvent ) {
                  updateDrag( e )
              }

              override def mouseReleased( e: MouseEvent ) {
                  dragStartEvent.foreach( e => {
                      editVal = dragCurrentVal
                      dragStartEvent = None
                      fireEditingStopped()
                  })
              }

              private def updateDrag( e: MouseEvent ) {
                  dragStartEvent.foreach( startE => {
                      dragStartVal.foreach( startCell => {
                          val newDB = max( -60f, min( 0f, max( -60f, startCell.decibels ) + (startE.getY - e.getY) * 0.25f ))
                          val newCell = MatrixCellValue.fromDecibels( if( newDB > -60f ) newDB else Float.NegativeInfinity )
                          val dragNewVal = Some( newCell )
                          if( dragNewVal != dragCurrentVal ) {
                              println( "Gain : " + newCell.decibels )
                              dragCurrentVal = Some( newCell )
                              comp.setCellValue( newCell )
                          }
                      })
                  })
              }
           }
           comp.addMouseListener( mia )
           comp.addMouseMotionListener( mia )
        }

        def getTableCellEditorComponent( tab: JTable, value: AnyRef,
            isSelected: Boolean, row: Int, col: Int ) : Component = {

            comp.getTableCellRendererComponent( tab, value, isSelected, true,
                                               row, col )
            editVal = value match {
               case cell @ MatrixCellValue( _ ) => Some( cell )
               case _ => None
            }
            editRow = row
            editCol = col
            comp
        }

        def getCellEditorValue() : AnyRef = editVal getOrElse null

        def getBorderInsets( c: Component ) = new Insets( 0, 0, 0, 0 )

        def isBorderOpaque() = true

        def paintBorder( c: Component, g:  Graphics, x: Int, y: Int, w: Int, h: Int ) {
            g.setColor( Color.red )
            g.drawLine( x, y, x + w, y + h )
            g.drawLine( x + w, y, x, y + h )
        }
    }

    object MatrixCellValue {
       def fromDecibels( db: Float ) = MatrixCellValue( pow( 10, db / 20 ).toFloat )
    }

    case class MatrixCellValue( f: Float ) {
        def decibels = (20 * log10( f )).toFloat
        def decibelsNorm( floor: Float = -60f ) = 1 - max( floor, decibels ) / floor
    }
}