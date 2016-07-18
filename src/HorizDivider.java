import java.awt.*;
import javax.swing.*;

// Class whose sole purpose is to break a row in a FlowLayout.
class HorizDivider extends JComponent
{
   public HorizDivider ()
      { setSize (getPreferredSize ()); }

   public Dimension getPreferredSize ()
   {
      // This should be as wide as its parent.
      Container parent = getParent ();
      if (parent == null)
         return new Dimension (100, 5);
      return new Dimension (parent.getWidth(), 5);
   }
}
