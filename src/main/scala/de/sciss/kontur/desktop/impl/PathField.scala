package de.sciss.kontur
package desktop
package impl

import legacy.{BasicEvent, PathListener, PathEvent, GUIUtil, ColoredTextField, EventManager, PathList, SpringPanel}
import javax.swing.{SwingUtilities, KeyStroke, Timer, AbstractAction, JLabel, Icon, ImageIcon}
import java.awt.{Paint, Toolkit, Color}
import java.awt.event.{ActionListener, KeyEvent, ActionEvent, InputEvent}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.io.{FileFilter, File}

/**
 * This class is an updated (slim) version
 * of FScape's <code>PathField</code> and provides a GUI object
 * that displays a file's or folder's path,
 * optionally with format information, and allows
 * the user to browse the harddisk to change
 * to a different file.
 */
object PathField {
  private final val ABBR_LENGTH = 12

  private lazy val icnAlertStop: Icon = new ImageIcon(classOf[PathField].getResource("alertstop.png"))

  private def abbreviate(string: String, targetLen: Int = ABBR_LENGTH): String = {
    if (string.length <= ABBR_LENGTH) return string

    def keepLettersAndDigits(s: String): String = {
      val len = s.length
      val b   = new StringBuffer(len)
      var i = 0
      while ((i < len) && (b.length + len - i > targetLen)) {
        val c = s.charAt(i)
        if (Character.isLetterOrDigit(c)) b.append(c)
        i += 1
      }
      b.append(s.substring(i))
      b.toString
    }

    val short1 = keepLettersAndDigits(string)
    if (short1.length <= targetLen) return short1

    def dropVowels(s: String): String = {
      val len = s.length
      if (len == 0 ) return s
      val b = new StringBuffer(s.length)
      b.append(s.charAt(0))
      var i = 1
      while ((i < len - 1) && (b.length + len - i > targetLen)) {
        val c = s.charAt(i)
        if ("aeiouäöüß".indexOf(c) < 0) b.append(c)
        i += 1
      }
      b.append(s.substring(i))
      b.toString
    }

    val short2 = dropVowels(short1)
    if (short2.length <= targetLen) return short2

    def cutMiddle(s: String): String = {
      val len   = s.length
      val lenH  = (math.min(len, ABBR_LENGTH) >> 1) - 1
      s.substring(0, lenH) + '\'' + s.substring(len - lenH)
    }

    cutMiddle(short2)
  }

  sealed trait Mode
  case object Input  extends Mode
  case object Output extends Mode
  case object Folder extends Mode

  private final val numUserPaths = 9

  private lazy val userPaths: PathList = {
    val res = new PathList(numUserPaths, GUIUtil.getUserPrefs, PathList.KEY_USERPATHS)
    if (userPaths.getPathCount < numUserPaths) {
      val home  = new File(sys.props("user.home"))
      val sub   = home.listFiles
      userPaths.addPathToHead(home)
      if (sub != null) {
        var i = 0
        while (i < sub.length && userPaths.getPathCount < numUserPaths) {
          if (sub(i).isDirectory && !sub(i).isHidden) userPaths.addPathToTail(sub(i))
          i += 1
        }
      }
      while (userPaths.getPathCount < numUserPaths) {
        userPaths.addPathToTail(home)
      }
    }
    res
  }

  private final val colrError     = new Color(0xFF, 0x00, 0x00, 0x2F)
  private final val colrExists    = new Color(0x00, 0x00, 0xFF, 0x2F)
  protected final val colrPropSet = new Color(0x00, 0xFF, 0x00, 0x2F)
  
  private final val menuShortcut  = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask  
  private final val myMeta        = if (menuShortcut == InputEvent.CTRL_MASK) 
    InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK else menuShortcut
}

class PathField(mode0: PathField.Mode = PathField.Input)
  extends SpringPanel with EventManager.Processor {

  import PathField._

  private var _mode = mode0

  private var _format = ("", true)
  protected var scheme: String = null
  protected var protoScheme: String = null
  private var superPaths = Seq.empty[PathField]
  private final var collChildren = Vector.empty[PathField]
  private final val elm: EventManager = new EventManager(this)

  private var _warnWhenExists        = _mode == Output
  private var _errWhenExistsNot      = _mode != Output
  private var _errWhenWriteProtected = _mode == Output
  
  private val lbWarn = new JLabel
  gridAdd(lbWarn, 0, 0)
  private val ggPath = new IOTextField
  private val ggChoose = createPathButton()
  ggChoose.mode = mode
  gridAdd(ggPath, 1, 0)
  private var ggFormat = Option.empty[ColoredTextField]

  gridAdd(ggChoose, 2, 0)
  makeCompactGrid()
  deriveFrom(new Array[PathField](0), "")
  this.addPropertyChangeListener("font", new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent) {
      val fnt = (e.getSource.asInstanceOf[PathField]).getFont
      ggPath.setFont(fnt)
      ggFormat.foreach(_.setFont(fnt))
    }
  })
  
  private var _dialogText = ""

  private object listener extends PathListener with ActionListener with PropertyChangeListener {
    def propertyChange(pce: PropertyChangeEvent) {
      val key   = pce.getPropertyName
      val value = pce.getNewValue
      ggPath.putClientProperty(key, value)
      ggFormat.foreach(_.putClientProperty(key, value))
    }

    def pathChanged(e: PathEvent) {
      val f = e.getPath
      scheme = createScheme(f.getPath)
      setFileAndDispatchEvent(f)
    }

    def actionPerformed(e: ActionEvent) {
      val str = if (ggPath.getText.length == 0) {
        evalScheme(scheme)
      }
      else {
        val res = ggPath.getText
        scheme = createScheme(res)
        res
      }
      setFileAndDispatchEvent(new File(str))
    }
  }

  ggPath.addActionListener(listener)
  ggChoose.addPathListener(listener)
  addPropertyChangeListener("JComponent.sizeVariant", listener)

  def dialogText = _dialogText
  def dialogText_=(value: String) {
    _dialogText = value
    ggChoose.dialogText = value
  }

  def showFormat: Boolean = ggFormat.isDefined
  def showFormat_=(value: Boolean) {
    if (ggFormat.isDefined != value) {
      if (value) {
        val res = new ColoredTextField
        res.setFont(getFont)
        res.setEditable(false)
        res.setBackground(null)
        gridAdd(res, 1, 1)
        ggFormat = Some(res)
      } else {
        ggFormat.foreach(remove _)
        ggFormat = None
      }
    }
  }

  /**
   * Returns the path displayed in the gadget.
   *
   * @return the <code>File</code> corresponding to the path string in the gadget
   */
  def file: File = {
    val path = ggPath.getText
    new File(path)
  }

  /**
   * Sets the gadget's path. This is path will be
   * used as default setting when the file chooser is shown
   *
   * @param  value	the new path for the button
   */
  def file_=(value: File) {
    setFileIgnoreScheme(value)
    scheme = createScheme(value.getPath)
  }

  def warnWhenExists = _warnWhenExists
  def warnWhenExists_=(value: Boolean) {
    if (_warnWhenExists != value) {
      _warnWhenExists = value
      updateIconAndColour()
    }
  }

  def errorWhenExistsNot = _errWhenExistsNot
  def errorWhenExistsNot_=(value: Boolean) {
    if (_errWhenExistsNot != value) {
      _errWhenExistsNot = value
      updateIconAndColour()
    }
  }

  def errorWhenWriteProtected = _errWhenWriteProtected  
  def errorWhenWriteProtected_=(value: Boolean) {
    if (_errWhenWriteProtected != value) {
      _errWhenWriteProtected = value
      updateIconAndColour()
    }
  }

  private def setFileIgnoreScheme(file: File) {
    ggPath.setText(file.getPath)
    ggChoose.file = if (file.getPath == "") None else Some(file)
    collChildren.foreach(_.motherSpeaks(file))
    updateIconAndColour()
  }

  /**
   * Sets a new path and dispatches a <code>PathEvent</code>
   * to registered listeners
   *
   * @param  f	the new path for the gadget and the event
   */
  protected def setFileAndDispatchEvent(f: File) {
    setFileIgnoreScheme(f)
    elm.dispatchEvent(new PathEvent(this, PathEvent.CHANGED, System.currentTimeMillis, f))
  }

  /**
   * Gets the string displayed in the format gadget.
   *
   * @return		a tuple consisting of the currently displayed format text or an empty string if the path field
   *          has no format gadget, and a boolean indicating whether the format was determined successfully
   *          (`true`) or not (`false`
   */
  def format = _format

  /**
   * Changes the contents of the format gadget.
   *
   * @param  value			a tuple consisting of the text to be displayed in the format gadget
   *                    and a success indicator
   */
  def format_=(value: (String, Boolean)) {
    if (_format != value) {
      _format = value
      ggFormat.foreach { gg =>
        val (text, success) = value
        gg.setText(text)
        gg.setPaint(if (success) null else colrError)
      }
    }
  }

  /**
   * Gets the type of the path field.
   *
   * @return the gadget's type as specified in the constructor
   *         use bitwise-AND with <code>TYPE_BASICMASK</code> to query the
   *         file access type.
   */
  def mode = _mode

  def mode_=(value: Mode) {
    if (_mode != value) {
      _mode = value
      warnWhenExists          = value == Output
      errorWhenExistsNot      = value != Output
      errorWhenWriteProtected = value == Output
      ggChoose.mode           = value
    }
  }

  def filter: Option[FileFilter] = ggChoose.filter // Option(ggChoose.getFilter)

  /**
   * Sets the filter to use for enabling or disabling items
   * in the file dialog.
   *
   * @param	value the new filter or null to remove an existing filter
   */
  def filter_=(value: Option[FileFilter]) {
    ggChoose.filter = filter
  }

  override def getBaseline(width: Int, height: Int) =
    ggPath.getBaseline(width, height) + ggPath.getY

  def isEditable = ggPath.isEditable

  def setEditable(value: Boolean) {
    ggPath.setEditable(value)
    ggChoose.setEnabled(value)
  }

  def selectDirectoryPart() {
    val s = ggPath.getText
    val i = s.lastIndexOf(File.separatorChar) + 1
    ggPath.select(0, i)
  }

  /**
   * Selects the file name portion of the text contents.
   *
   * @param	extension	whether to include the file name suffix or not
   */
  def selectFilenamePart(extension: Boolean) {
    val s = ggPath.getText
    val i = s.lastIndexOf(File.separatorChar) + 1
    val j = if (extension) s.length else s.lastIndexOf('.')
    ggPath.select(i, if (j >= i) j else s.length)
  }

  override def requestFocusInWindow: Boolean = ggPath.requestFocusInWindow

  /**
   * <code>PathField</code> offers a mechanism to automatically derive
   * a path name from a "mother" <code>PathField</code>. This applies
   * usually to output files whose names are derived from
   * PathFields which represent input paths. The provided
   * 'scheme' String can contain the Tags
   * <pre>
   * $Dx = Directory of superPath x; $Fx = Filename; $E = Extension; $Bx = Brief filename
   * </pre>
   * where 'x' is the index in the provided array of
   * mother PathFields. Whenever the mother contents
   * changes, the child PathField will recalculate its
   * name. When the user changes the contents of the child
   * PathField, an algorithm tries to find out which components
   * are related to the mother's pathname, parts that cannot
   * be identified will not be automatically changing any more
   * unless the user completely clears the PathField (i.e.
   * restores full automation).
   * <p>
   * The user can abbreviate or extend filenames by pressing the appropriate
   * key; in this case the $F and $B tags are exchanged in the scheme.
   *
   * @param  superPaths array of mother path fields to listen to
   * @param  scheme automatic formatting scheme which can incorporate
   *              placeholders for the mother fields' paths.
   */
  def deriveFrom(superPaths: Seq[PathField], scheme: String) {
    this.superPaths   = superPaths
    this.scheme       = scheme
    this.protoScheme  = scheme
    superPaths.foreach(_.addChildPathField(this))
  }

  private def addChildPathField(child: PathField) {
    if (!collChildren.contains(child)) collChildren :+= child
  }

  private def motherSpeaks(superPath: File) {
    setFileAndDispatchEvent(new File(evalScheme(scheme)))
  }

  /**
   * Registers a <code>PathListener</code>
   * which will be informed about changes of
   * the path (i.e. user selections in the
   * file chooser or text editing).
   *
   * @param  listener	the <code>PathListener</code> to register
   * @see	de.sciss.app.EventManager#addListener( Object )
   */
  def addPathListener(listener: PathListener) {
    elm.addListener(listener)
  }

  /**
   * Unregisters a <code>PathListener</code>
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
          listener.pathChanged(e.asInstanceOf[PathEvent])
          i += 1
        }
    }
  }

  private def updateIconAndColour() {
    val f           = file
    val parent      = f.getParentFile
    val folder      = mode == Folder
    val (c, icn, tt) = if (_warnWhenExists || _errWhenExistsNot || _errWhenWriteProtected) {
      val (parentExists, exists, wp) = try {
        val _parentExists = (parent != null) && parent.isDirectory
        val _exists       = if (folder) f.isDirectory else f.isFile
        val _wp           = _errWhenWriteProtected && {
          _parentExists && ((_exists && !f.canWrite) || (!_exists && !parent.canWrite))
        }
        (_parentExists, _exists, _wp)
      }
      catch {
        case e: SecurityException => (false, false, false)
      }
      if (_errWhenWriteProtected && (wp || !parentExists)) {
        (colrError, GUIUtil.getNoWriteIcon, GUIUtil.getResourceString(if (folder) "ttWarnFolderWriteProtected" else "ttWarnFileWriteProtected"))
      }
      else if (_errWhenExistsNot && !exists) {
        (colrError, icnAlertStop, GUIUtil.getResourceString(if (folder) "ttWarnFolderExistsNot" else "ttWarnFileExistsNot"))
      }
      else if (_warnWhenExists && exists) {
        (colrExists, icnAlertStop, GUIUtil.getResourceString(if (folder) "ttWarnFolderExists" else "ttWarnFileExists"))
      }
      else {
        (null, null, null)
      }
    } else {
      (null, null, null)
    }

    if (c != ggPath.getPaint)         ggPath.setPaint(c)
    if (lbWarn.getIcon != icn)        lbWarn.setIcon(icn)
    if (lbWarn.getToolTipText != tt)  lbWarn.setToolTipText(tt)
  }

  protected def evalScheme(_s: String): String = {
    var s = _s
    var i = s.indexOf("$D")
    while ((i >= 0) && (i < s.length - 2)) {
      val j = s.charAt(i + 2) - 48
      var txt2 = try {
        superPaths(j).file.getPath
      }
      catch {
        case e1: ArrayIndexOutOfBoundsException => ""
      }
      txt2 = txt2.substring(0, txt2.lastIndexOf(File.separatorChar) + 1)
      s = s.substring(0, i) + txt2 + s.substring(i + 3)
      i = s.indexOf("$D", i)
    }

    i = s.indexOf("$F")
    while ((i >= 0) && (i < s.length - 2)) {
      val j = s.charAt(i + 2) - 48
      var txt2 = try {
        superPaths(j).file.getPath
      }
      catch {
        case e1: ArrayIndexOutOfBoundsException => ""
      }
      txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1)
      val k = txt2.lastIndexOf('.')
      s = s.substring(0, i) + (if ((k > 0)) txt2.substring(0, k) else txt2) + s.substring(i + 3)
      i = s.indexOf("$F", i)
    }

    i = s.indexOf("$X")
    while ((i >= 0) && (i < s.length - 2)) {
      val j = s.charAt(i + 2) - 48
      var txt2 = try {
        superPaths(j).file.getPath
      }
      catch {
        case e1: ArrayIndexOutOfBoundsException => ""
      }
      txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1)
      val k = txt2.lastIndexOf('.')
      s = s.substring(0, i) + (if ((k > 0)) txt2.substring(k) else "") + s.substring(i + 3)
      i = s.indexOf("$X", i)
    }

    i = s.indexOf("$B")
    while ((i >= 0) && (i < s.length - 2)) {
      val j = s.charAt(i + 2) - 48
      var txt2 = try {
        superPaths(j).file.getPath
      }
      catch {
        case e1: ArrayIndexOutOfBoundsException => ""
      }
      txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1)
      val k = txt2.lastIndexOf('.')
      txt2 = abbreviate(if ((k > 0)) txt2.substring(0, k) else txt2)
      s = s.substring(0, i) + txt2 + s.substring(i + 3)
      i = s.indexOf("$B", i)
    }

    s
  }

  private def createScheme(applied: String): String = {
    if (applied.isEmpty) return protoScheme

    var s = applied
    var i = 0
    var k = 0
    while (i < superPaths.length && k == 0) {
      var txt2  = superPaths(i).file.getPath
      txt2      = txt2.substring(0, txt2.lastIndexOf(File.separatorChar) + 1)
      if (s.startsWith(txt2)) {
        s = "$D" + (i + 48).toChar + s.substring(txt2.length)
        k = 3
      }
      i += 1
    }

    k = math.max(k, s.lastIndexOf(File.separatorChar) + 1)
    i = 0
    var checkedAbbrev = -1
    var checkedFull   = false
    while (i < superPaths.length) {
      var txt2  = superPaths(i).file.getPath
      txt2      = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1)
      var m     = txt2.lastIndexOf('.')
      txt2 = if ((m > 0)) txt2.substring(0, m) else txt2
      val cont  = if ((protoScheme.indexOf("$B" + (i + 48).toChar) < 0) || (checkedAbbrev == i)) {
        m = s.indexOf(txt2, k)
        checkedFull = true
        if (m >= 0) {
          s = s.substring(0, m) + "$F" + (i + 48).toChar + s.substring(m + txt2.length)
          k = m + 3
          true
        } else {
          false
        }
      }
      else {
        checkedFull = false
        false
      }

      if (!cont && checkedAbbrev != i) {
        txt2 = abbreviate(txt2)
        m = s.indexOf(txt2, k)
        if (m >= 0) {
          s = s.substring(0, m) + "$B" + (i + 48).toChar + s.substring(m + txt2.length)
          k = m + 3

        } else if (!checkedFull) {
          checkedAbbrev = i
          i -= 1
        }
        i += 1
      }
    }

    s
  }

  protected def abbrScheme(orig: String): String = {
    val i = orig.lastIndexOf("$F")
    if (i >= 0) {
      (orig.substring(0, i) + "$B" + orig.substring(i + 2))
    } else {
      orig
    }
  }

  protected def expandScheme(orig: String): String = {
    val i = orig.indexOf("$B")
    if (i >= 0) {
      (orig.substring(0, i) + "$F" + orig.substring(i + 2))
    } else {
      orig
    }
  }

  protected def udirScheme(orig: String, idx: Int): String = {
    val udir = userPaths.getPath(idx)
    if (udir == null) return orig
    val i = if (orig.startsWith("$D")) {
      3
    } else {
      orig.lastIndexOf(File.separatorChar) + 1
    }
    (new File(udir, orig.substring(i)).getPath)
  }

  protected def createPathButton(): PathButton = new PathButton(mode)

  private class IOTextField extends ColoredTextField(32) {
    init()

    def init() {
      val inputMap  = getInputMap
      val actionMap = getActionMap
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, myMeta + InputEvent.ALT_MASK), "abbr")
      actionMap.put("abbr", new AbstractAction {
        def actionPerformed(e: ActionEvent) {
          scheme = abbrScheme(scheme)
          setFileAndDispatchEvent(new File(evalScheme(scheme)))
        }
      })
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, myMeta + InputEvent.ALT_MASK), "expd")
      actionMap.put("expd", new AbstractAction {
        def actionPerformed(e: ActionEvent) {
          scheme = expandScheme(scheme)
          setFileAndDispatchEvent(new File(evalScheme(scheme)))
        }
      })
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, myMeta), "auto")
      actionMap.put("auto", new AbstractAction {
        def actionPerformed(e: ActionEvent) {
          scheme = protoScheme
          setFileAndDispatchEvent(new File(evalScheme(scheme)))
        }
      })
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "lost")
      actionMap.put("lost", new AbstractAction {
        def actionPerformed(e: ActionEvent) {
          val rp = SwingUtilities.getRootPane(IOTextField.this)
          if (rp != null) rp.requestFocus()
        }
      })
      var i = 0
      while (i < numUserPaths) {
        var s = "sudir" + i
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1 + i, myMeta + InputEvent.ALT_MASK), s)
        actionMap.put(s, new SetUserDirAction(i))
        s = "rudir" + i
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1 + i, myMeta), s)
        actionMap.put(s, new RecallUserDirAction(i))
        i += 1
      }
    }

    private class SetUserDirAction(idx: Int) extends AbstractAction {
      private val visualFeedback = new Timer(250, this)
      visualFeedback.setRepeats(false)

      private var oldPaint: Paint = null

      def actionPerformed(e: ActionEvent) {
        if (e.getSource == visualFeedback) {
          ggPath.setPaint(oldPaint)
        }
        else {
          val dir = file.getParentFile
          if (dir != null) {
            userPaths.setPath(idx, dir)
            if (visualFeedback.isRunning) {
              visualFeedback.restart()
            } else {
              oldPaint = ggPath.getPaint
              ggPath.setPaint(colrPropSet)
              visualFeedback.start()
            }
          }
        }
      }
    }

    private class RecallUserDirAction(idx: Int) extends AbstractAction {
      def actionPerformed(e: ActionEvent) {
        scheme = udirScheme(scheme, idx)
        setFileAndDispatchEvent(new File(evalScheme(scheme)))
      }
    }
  }
}

