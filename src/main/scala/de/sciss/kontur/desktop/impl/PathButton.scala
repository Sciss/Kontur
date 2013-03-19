package de.sciss.kontur
package desktop
package impl

import java.awt.datatransfer.{UnsupportedFlavorException, Transferable, DataFlavor}
import java.io.{FilenameFilter, FileFilter, IOException, File}
import legacy.{BasicEvent, PathListener, PathEvent, GUIUtil, EventManager, ModificationButton}
import javax.swing.event.MouseInputAdapter
import java.awt.event.MouseEvent
import javax.swing.{SwingUtilities, TransferHandler, JComponent}
import java.awt.{Component, Frame, FileDialog, Dialog}
import annotation.tailrec
import net.roydesign.ui.FolderDialog

/**
 * This class is a rewritten version
 * of FScape's <code>PathIcon</code> and provides
 * a simple ToolIcon like button to
 * allow the user to select a file
 * from the harddisk. Besides, the user
 * can drag files from the Finder onto
 * the button's icon to set the button's
 * path.
 */
object PathButton {
  private final val supportedFlavors = DataFlavor.javaFileListFlavor :: DataFlavor.stringFlavor :: Nil

  private class PathTransferable(f: File) extends Transferable {
    def getTransferDataFlavors: Array[DataFlavor] = supportedFlavors.toArray
    def isDataFlavorSupported(flavor: DataFlavor): Boolean = supportedFlavors.contains(flavor)

    def getTransferData(flavor: DataFlavor): AnyRef = {
      if (flavor == DataFlavor.javaFileListFlavor) {
        val coll: java.util.List[File] = new java.util.ArrayList[File](1)
        coll.add(f)
        coll
      }
      else if (flavor == DataFlavor.stringFlavor) {
        f.getAbsolutePath
      }
      throw new UnsupportedFlavorException(flavor)
    }
  }
}
class PathButton(var mode: PathField.Mode = PathField.Input)
  extends ModificationButton(ModificationButton.SHAPE_LIST) with EventManager.Processor {

  import PathButton._

  private final val elm: EventManager = new EventManager(this)

  setToolTipText(GUIUtil.getResourceString("buttonChoosePathTT"))
  setTransferHandler(new PathTransferHandler)

  var dialogText  = ""
  var file        = Option.empty[File]
  var filter      = Option.empty[FileFilter]

  private object mia extends MouseInputAdapter {
    private var dndInit     = Option.empty[MouseEvent]
    private var dndStarted  = false

    override def mousePressed(e: MouseEvent) {
      dndInit     = Some(e)
      dndStarted  = false
    }

    override def mouseReleased(e: MouseEvent) {
      if (!dndStarted && contains(e.getPoint)) showFileChooser()
      dndInit     = None
      dndStarted  = false
    }

    override def mouseDragged(e: MouseEvent) {
      dndInit.foreach { e0 =>
        if (!dndStarted && ((math.abs(e.getX - e0.getX) > 5) || (math.abs(e.getY - e0.getY) > 5))) {
          e.getSource match {
            case c: JComponent =>
              c.getTransferHandler.exportAsDrag(c, e, TransferHandler.COPY)
              dndStarted = true
            case _ =>
          }
        }
      }
    }
  }

  addMouseListener(mia)
  addMouseMotionListener(mia)

  private def setFileAndDispatchEvent(f: File) {
    file = Some(f)
    elm.dispatchEvent(new PathEvent(this, PathEvent.CHANGED, System.currentTimeMillis, f))
  }

  /**
   * Register a <code>PathListener</code>
   * which will be informed about changes of
   * the path (i.e. user selections in the
   * file chooser).
   *
   * @param  listener	the <code>PathListener</code> to register
   * @see	de.sciss.app.EventManager#addListener( Object )
   */
  def addPathListener(listener: PathListener) {
    elm.addListener(listener)
  }

  /**
   * Unregister a <code>PathListener</code>
   * from receiving path change events.
   *
   * @param  listener	the <code>PathListener</code> to unregister
   * @see	de.sciss.app.EventManager#removeListener( Object )
   */
  def removePathListener(listener: PathListener) {
    elm.removeListener(listener)
  }

  def processEvent(e: BasicEvent) {
    e match {
      case pe: PathEvent =>
        var i = 0
        while (i < elm.countListeners) {
          val listener = elm.getListener(i).asInstanceOf[PathListener]
          listener.pathChanged(pe)
          i += 1
        }
    }
  }

  protected def showDialog(dlg: Dialog) {
    dlg.setVisible(true)
  }

  private def showFileChooser() {
    @tailrec def findFrame(parent: Component): Frame = {
      SwingUtilities.getWindowAncestor(parent) match {
        case f: Frame => f
        case w if w != null => findFrame(w)
        case _ => null
      }
    }

    val win     = findFrame(this)
    val dlgTxt  = if (dialogText == "") null else dialogText
    val fDlg = mode match {
      case PathField.Input =>
        new FileDialog(win, dlgTxt, FileDialog.LOAD)
      case PathField.Output =>
        new FileDialog(win, dlgTxt, FileDialog.SAVE)
      case PathField.Folder =>
        new FolderDialog(win, dlgTxt)
    }
    file.foreach { f =>
      fDlg.setFile(f.getName)
      fDlg.setDirectory(f.getParent)
    }
    filter.foreach(flt => fDlg.setFilenameFilter(new FilenameFilter {
      def accept(dir: File, name: String) = flt.accept(new File(dir, name))
    }))
    showDialog(fDlg)
    val fDir  = fDlg.getDirectory
    val fFile = fDlg.getFile
    if ((fFile != null) && (fDir != null)) {
      val p = if (mode == PathField.Folder) {
        new File(fDir)
      } else {
        new File(fDir + fFile)
      }
      setFileAndDispatchEvent(p)
    }
    fDlg.dispose()
  }

  private class PathTransferHandler extends TransferHandler {
    /**
     * Overridden to import a Pathname (Fileliste or String) if it is available.
     */
    override def importData(c: JComponent, t: Transferable): Boolean = {
      val newPath = if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        t.getTransferData(DataFlavor.javaFileListFlavor) match {
          case fileList: java.util.List[_] if !fileList.isEmpty =>
            fileList.get(0) match {
              case f: File => Some(f)
              case _ => None
            }
          case _ => None
        }

      } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        Some(new File(t.getTransferData(DataFlavor.stringFlavor).asInstanceOf[String]))
      } else {
        None
      }

      newPath.foreach(setFileAndDispatchEvent)
      newPath.isDefined
    }

    override def getSourceActions(c: JComponent): Int = TransferHandler.COPY

    protected override def createTransferable(c: JComponent): Transferable = file match {
      case Some(f) => new PathTransferable(f)
      case _ => null
    }

    protected override def exportDone(source: JComponent, data: Transferable, action: Int) {}

    override def canImport(c: JComponent, flavors: Array[DataFlavor]): Boolean = {
      {
        var i: Int = 0
        while (i < flavors.length) {
          {
            {
              var j: Int = 0
              while (j < supportedFlavors.length) {
                {
                  if (flavors(i) == supportedFlavors(j)) return true
                }
                ({
                  j += 1; j - 1
                })
              }
            }
          }
          ({
            i += 1; i - 1
          })
        }
      }
      false
    }
  }
}