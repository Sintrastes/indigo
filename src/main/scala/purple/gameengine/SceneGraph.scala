package purple.gameengine

object SceneGraphNode {
  def empty: SceneGraphNode = SceneGraphNodeBranch(Nil)
}
sealed trait SceneGraphNode {

  def flatten(acc: List[SceneGraphNodeLeaf]): List[SceneGraphNodeLeaf] = {
    this match {
      case l: SceneGraphNodeLeaf => l :: acc
      case b: SceneGraphNodeBranch =>
        b.children.flatMap(n => n.flatten(Nil)) ++ acc
    }
  }

}

// Types of SceneGraphNode
case class SceneGraphNodeBranch(children: List[SceneGraphNode]) extends SceneGraphNode
sealed trait SceneGraphNodeLeaf extends SceneGraphNode {
  val bounds: Rectangle
  val depth: Depth
  val imageAssetRef: String
  val effects: Effects

  def withAlpha(a: Double): SceneGraphNodeLeaf
  def withTint(red: Double, green: Double, blue: Double): SceneGraphNodeLeaf
  def flipHorizontal(h: Boolean): SceneGraphNodeLeaf
  def flipVertical(v: Boolean): SceneGraphNodeLeaf
}

// Data types
case class Point(x: Int, y: Int)
case class Rectangle(position: Point, size: Point)
case class Depth(zIndex: Int)

// Frames
case class Animations(spriteSheetSize: Point, cycle: Cycle, cycles: List[Cycle] = Nil) {
  private val nonEmtpyCycles: List[Cycle] = cycle +: cycles

  def currentCycle: Cycle =
    nonEmtpyCycles.find(_.current).getOrElse(nonEmtpyCycles.head)

  def currentCycleName: String = currentCycle.label

  def currentFrame: Frame = currentCycle.currentFrame

  def addCycle(cycle: Cycle) = Animations(spriteSheetSize, cycle, nonEmtpyCycles)

}

case class Cycle(label: String, frame: Frame, frames: List[Frame] = Nil, current: Boolean = false) {
  private val nonEmtpyFrames: List[Frame] = frame +: frames

  def currentFrame: Frame =
    nonEmtpyFrames.find(_.current).getOrElse(nonEmtpyFrames.head)

  def addFrame(frame: Frame) = Cycle(label, frame, nonEmtpyFrames, current)

}

case class Frame(bounds: Rectangle, current: Boolean = false)

// Concrete leaf types
case class Graphic(bounds: Rectangle, depth: Depth, imageAssetRef: String, effects: Effects = Effects.default) extends SceneGraphNodeLeaf {

  def withAlpha(a: Double): Graphic =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(red: Double, green: Double, blue: Double): Graphic =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Graphic =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Graphic =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

}

case class Sprite(bounds: Rectangle, depth: Depth, imageAssetRef: String, animations: Animations, effects: Effects = Effects.default) extends SceneGraphNodeLeaf {

  def withAlpha(a: Double): Sprite =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(red: Double, green: Double, blue: Double): Sprite =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Sprite =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Sprite =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

}

case class FontInfo(charSize: Point, imageAssetRef: String, fontChar: FontChar, fontChars: List[FontChar] = Nil) {
  private val nonEmtpyChars: List[FontChar] = fontChar +: fontChars

  def addChar(fontChar: FontChar) = FontInfo(charSize, imageAssetRef, fontChar, nonEmtpyChars)
}

case class FontChar(character: String, offset: Point)

sealed trait TextAlignment
case object AlignLeft extends TextAlignment
case object AlignCenter extends TextAlignment
case object AlignRight extends TextAlignment

case class Text(text: String, alignment: TextAlignment, position: Point, depth: Depth, fontInfo: FontInfo, effects: Effects = Effects.default) extends SceneGraphNodeLeaf {

  val bounds: Rectangle = Rectangle(position, Point(text.length * fontInfo.charSize.x, fontInfo.charSize.y))
  val imageAssetRef: String = fontInfo.imageAssetRef

  def withAlpha(a: Double): Text =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(red: Double, green: Double, blue: Double): Text =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Text =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Text =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

}

// Graphical effects
object Effects {
  val default = Effects(
    alpha = 1.0,
    tint = Tint(
      r = 1,
      g = 1,
      b = 1
    ),
    flip = Flip(
      horizontal = false,
      vertical = false
    )
  )
}
case class Effects(alpha: Double, tint: Tint, flip: Flip)
case class Tint(r: Double, g: Double, b: Double)
case class Flip(horizontal: Boolean, vertical: Boolean)
