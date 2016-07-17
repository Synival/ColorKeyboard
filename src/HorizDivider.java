import java.awt.*;
import javax.swing.*;

class HorizDivider extends JComponent
{
   public HorizDivider ()
   {
      setSize (getPreferredSize ());
   }

   public Dimension getPreferredSize ()
   {
      if (getParent() == null)
         return new Dimension (100, 0);
      return new Dimension (getParent().getWidth(), 5);
   }
}
